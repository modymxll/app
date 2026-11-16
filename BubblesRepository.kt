package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.error.OperationType
import com.example.data.error.handleFirestoreError
import com.example.data.error.unwrapFirestoreException
import com.example.data.local.PendingSyncActionEntity
import com.example.data.model.AdminMarker
import com.example.data.model.AppNotification
import com.example.data.model.AppSettings
import com.example.data.model.AuditLog
import com.example.data.model.Customer
import com.example.data.model.CustomerLocation
import com.example.data.model.Driver
import com.example.data.model.DriverAttendance
import com.example.data.model.GpsLocationPoint
import com.example.data.model.PaymentRecord
import com.example.data.model.RecurringSchedule
import com.example.data.model.UserProfile
import com.example.data.model.Visit
import com.example.data.validation.GeoUtils
import com.example.data.validation.RecurringVisitGenerator
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot.ServerTimestampBehavior
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class BubblesRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

    private fun currentDisplayName(): String =
        auth.currentUser?.displayName?.takeIf { it.isNotBlank() }
            ?: auth.currentUser?.email?.substringBefore("@")
            ?: "Bubbles User"

    private fun <T> Query.asSafeListFlow(
        path: String,
        uid: String,
        mapper: (List<T>) -> List<T> = { it },
        clazz: Class<T>
    ): Flow<List<T>> = flow {
        emitAll(
            snapshots()
                .map { snap -> mapper(snap.toObjects(clazz, ServerTimestampBehavior.ESTIMATE)) }
                .catch { error ->
                    val firestoreError = error.unwrapFirestoreException()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(toLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    // --- Bootstrap & Auth Profile ---

    suspend fun ensureUserProfileAndBootstrap(
        bootstrappedAdminEmail: String = "ivconquer@gmail.com"
    ): Result<UserProfile> = runCatching {
        val user = auth.currentUser ?: error("Not signed in")
        val uid = user.uid
        val email = user.email?.trim()?.lowercase()
        val isBootstrapped = user.isEmailVerified && email == bootstrappedAdminEmail

        val userRef = db.collection("users").document(uid)
        val existingUserSnap = try {
            userRef.get().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, userRef.path)
            throw e
        }

        val existingAdminSnap = try {
            db.collection("admins").document(uid).get().await()
        } catch (_: Exception) {
            null
        }
        val isAdminUser = isBootstrapped || (existingAdminSnap?.exists() == true)

        if (isBootstrapped && existingAdminSnap?.exists() != true) {
            val adminRef = db.collection("admins").document(uid)
            try {
                adminRef.set(
                    AdminMarker(userId = uid, email = user.email ?: bootstrappedAdminEmail).toCreateMap()
                ).await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.CREATE, adminRef.path)
            }
        }

        if (isAdminUser) {
            val settingsRef = db.collection("app_settings").document("default")
            val settingsSnap = runCatching { settingsRef.get().await() }.getOrNull()
            if (settingsSnap?.exists() != true) {
                runCatching { settingsRef.set(AppSettings().toSaveMap()).await() }
            }
        }

        // Auto-claim pre-registered Driver document matching verified Google email
        var linkedDriverId = existingUserSnap.toObject(UserProfile::class.java, ServerTimestampBehavior.ESTIMATE)?.driverId
        if (!email.isNullOrBlank() && user.isEmailVerified) {
            val matchingDrivers = runCatching {
                db.collection("drivers").whereEqualTo("email", email).get().await()
                    .toObjects(Driver::class.java, ServerTimestampBehavior.ESTIMATE)
            }.getOrDefault(emptyList())
            val firstMatch = matchingDrivers.firstOrNull { !it.isDeleted }
            if (firstMatch != null) {
                linkedDriverId = firstMatch.id
                if (firstMatch.userId.isBlank()) {
                    runCatching {
                        db.collection("drivers").document(firstMatch.id).update(
                            mapOf(
                                "userId" to uid,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        ).await()
                    }
                }
            }
        }

        if (!existingUserSnap.exists()) {
            val profile = UserProfile(
                userId = uid,
                displayName = currentDisplayName(),
                email = email,
                role = if (isAdminUser) "ADMIN" else "DRIVER",
                driverId = linkedDriverId
            )
            try {
                userRef.set(profile.toCreateMap()).await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.CREATE, userRef.path)
                throw e
            }
            userRef.get().await().toObject(UserProfile::class.java, ServerTimestampBehavior.ESTIMATE) ?: profile
        } else {
            val existing = existingUserSnap.toObject(UserProfile::class.java, ServerTimestampBehavior.ESTIMATE)!!
            val desiredRole = if (isAdminUser) "ADMIN" else existing.role
            if (existing.driverId != linkedDriverId || existing.role != desiredRole) {
                val updates = mutableMapOf<String, Any>("updatedAt" to FieldValue.serverTimestamp())
                if (linkedDriverId != null) updates["driverId"] = linkedDriverId
                if (isAdminUser && existing.role != "ADMIN") updates["role"] = "ADMIN"
                runCatching { userRef.update(updates).await() }
                existing.copy(role = desiredRole, driverId = linkedDriverId)
            } else {
                existing
            }
        }
    }

    // --- Real-Time Observations ---

    fun observeUserProfile(uid: String): Flow<UserProfile?> = flow {
        val docRef = db.collection("users").document(uid)
        emitAll(
            docRef.snapshots()
                .map { it.toObject(UserProfile::class.java, ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    val fe = error.unwrapFirestoreException()
                    if (fe == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) handleFirestoreError(fe ?: error, OperationType.GET, docRef.path)
                    throw error
                }
        )
    }

    fun observeAppSettings(uid: String): Flow<AppSettings> = flow {
        val docRef = db.collection("app_settings").document("default")
        emitAll(
            docRef.snapshots()
                .map { snap ->
                    snap.toObject(AppSettings::class.java, ServerTimestampBehavior.ESTIMATE) ?: AppSettings()
                }
                .catch { error ->
                    val fe = error.unwrapFirestoreException()
                    if (fe == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) handleFirestoreError(fe ?: error, OperationType.GET, docRef.path)
                    emit(AppSettings())
                }
        )
    }

    fun observeAllUsers(uid: String): Flow<List<UserProfile>> =
        db.collection("users").asSafeListFlow("users", uid, clazz = UserProfile::class.java)

    fun observeAllDrivers(uid: String): Flow<List<Driver>> =
        db.collection("drivers").asSafeListFlow(
            path = "drivers",
            uid = uid,
            mapper = { list -> list.filter { !it.isDeleted }.sortedBy { it.name } },
            clazz = Driver::class.java
        )

    fun observeMyDriver(uid: String): Flow<Driver?> =
        db.collection("drivers")
            .whereEqualTo("userId", uid)
            .asSafeListFlow(
                path = "drivers",
                uid = uid,
                mapper = { list -> list.filter { !it.isDeleted } },
                clazz = Driver::class.java
            )
            .map { it.firstOrNull() }

    fun observeCustomers(uid: String): Flow<List<Customer>> =
        db.collection("customers").asSafeListFlow(
            path = "customers",
            uid = uid,
            mapper = { list -> list.sortedBy { it.name } },
            clazz = Customer::class.java
        )

    fun observeLocations(uid: String): Flow<List<CustomerLocation>> =
        db.collection("locations").asSafeListFlow(
            path = "locations",
            uid = uid,
            mapper = { list -> list.sortedBy { it.branchName } },
            clazz = CustomerLocation::class.java
        )

    fun observeAllVisits(uid: String): Flow<List<Visit>> =
        db.collection("visits").asSafeListFlow(
            path = "visits",
            uid = uid,
            mapper = { list -> list.sortedBy { it.scheduledAtEpochMs } },
            clazz = Visit::class.java
        )

    fun observeVisitsForDriver(uid: String, driverId: String?): Flow<List<Visit>> {
        val query = if (!driverId.isNullOrBlank()) {
            db.collection("visits").whereEqualTo("assignedDriverId", driverId)
        } else {
            db.collection("visits").whereEqualTo("assignedDriverUserId", uid)
        }
        return query.asSafeListFlow(
            path = "visits",
            uid = uid,
            mapper = { list -> list.sortedBy { it.scheduledAtEpochMs } },
            clazz = Visit::class.java
        )
    }

    fun observeRecurringSchedules(uid: String): Flow<List<RecurringSchedule>> =
        db.collection("recurring_schedules").asSafeListFlow(
            path = "recurring_schedules",
            uid = uid,
            clazz = RecurringSchedule::class.java
        )

    fun observeAttendance(uid: String, isAdmin: Boolean): Flow<List<DriverAttendance>> {
        val query = if (isAdmin) {
            db.collection("driver_attendance")
        } else {
            db.collection("driver_attendance").whereEqualTo("userId", uid)
        }
        return query.asSafeListFlow(
            path = "driver_attendance",
            uid = uid,
            mapper = { list -> list.sortedByDescending { it.startedAtEpochMs } },
            clazz = DriverAttendance::class.java
        )
    }

    fun observeGpsHistory7Days(uid: String, isAdmin: Boolean): Flow<List<GpsLocationPoint>> {
        val sevenDaysAgo = System.currentTimeMillis() - 7L * 86_400_000L
        val query = if (isAdmin) {
            db.collection("gps_locations")
        } else {
            db.collection("gps_locations").whereEqualTo("userId", uid)
        }
        return query.asSafeListFlow(
            path = "gps_locations",
            uid = uid,
            mapper = { list ->
                list.filter { it.recordedAtEpochMs >= sevenDaysAgo }
                    .sortedBy { it.recordedAtEpochMs }
            },
            clazz = GpsLocationPoint::class.java
        )
    }

    fun observePaymentRecords(uid: String, isAdmin: Boolean): Flow<List<PaymentRecord>> {
        val query = if (isAdmin) {
            db.collection("payment_records")
        } else {
            db.collection("payment_records").whereEqualTo("driverUserId", uid)
        }
        return query.asSafeListFlow(
            path = "payment_records",
            uid = uid,
            mapper = { list -> list.sortedByDescending { it.recordedAtEpochMs } },
            clazz = PaymentRecord::class.java
        )
    }

    fun observeNotifications(uid: String, isAdmin: Boolean): Flow<List<AppNotification>> {
        val query = if (isAdmin) {
            db.collection("notifications")
        } else {
            db.collection("notifications").whereIn("targetUserId", listOf(uid, "ALL"))
        }
        return query.asSafeListFlow(
            path = "notifications",
            uid = uid,
            mapper = { list -> list.sortedByDescending { it.createdAt?.toDate()?.time ?: 0L } },
            clazz = AppNotification::class.java
        )
    }

    fun observeAuditLogs(uid: String): Flow<List<AuditLog>> =
        db.collection("audit_logs").asSafeListFlow(
            path = "audit_logs",
            uid = uid,
            mapper = { list -> list.sortedByDescending { it.createdAt?.toDate()?.time ?: 0L } },
            clazz = AuditLog::class.java
        )

    // --- Driver CRUD & Status ---

    suspend fun createDriver(driver: Driver): Result<String> = runCatching {
        val actorUid = requireUserId()
        val id = driver.id.ifBlank { "drv_${UUID.randomUUID().toString().take(12)}" }
        val docRef = db.collection("drivers").document(id)
        try {
            docRef.set(driver.copy(id = id).toCreateMap()).await()
            appendAuditLog(
                actorUid = actorUid,
                actionType = "CREATE_DRIVER",
                entityType = "DRIVER",
                entityId = id,
                oldValue = "-",
                newValue = "${driver.name} (${driver.phone})"
            )
            id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateDriverByAdmin(
        driver: Driver,
        newStatus: String? = null,
        newName: String? = null,
        newPhone: String? = null,
        newEmail: String? = null
    ): Result<Unit> = runCatching {
        val actorUid = requireUserId()
        require(driver.id.isNotBlank())
        val docRef = db.collection("drivers").document(driver.id)
        val updates = mutableMapOf<String, Any>("updatedAt" to FieldValue.serverTimestamp())
        if (newStatus != null) {
            updates["status"] = newStatus
            if (newStatus != "ON_SHIFT") updates["gpsActive"] = false
        }
        if (newName != null) updates["name"] = newName
        if (newPhone != null) updates["phone"] = newPhone
        if (!newEmail.isNullOrBlank()) updates["email"] = newEmail.trim().lowercase()
        try {
            docRef.update(updates).await()
            if (newStatus != null && newStatus != driver.status) {
                appendAuditLog(
                    actorUid = actorUid,
                    actionType = "UPDATE_DRIVER_STATUS",
                    entityType = "DRIVER",
                    entityId = driver.id,
                    oldValue = driver.status,
                    newValue = newStatus
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun softDeleteDriver(driver: Driver): Result<Unit> = runCatching {
        val actorUid = requireUserId()
        val docRef = db.collection("drivers").document(driver.id)
        try {
            docRef.update(
                mapOf(
                    "isDeleted" to true,
                    "status" to "INACTIVE",
                    "gpsActive" to false,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            appendAuditLog(
                actorUid = actorUid,
                actionType = "SOFT_DELETE_DRIVER",
                entityType = "DRIVER",
                entityId = driver.id,
                oldValue = driver.status,
                newValue = "DELETED (history retained)"
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    // --- Customers & Multi-Branch Locations ---

    suspend fun createCustomer(customer: Customer): Result<String> = runCatching {
        val actorUid = requireUserId()
        val id = customer.id.ifBlank { "cust_${UUID.randomUUID().toString().take(12)}" }
        val docRef = db.collection("customers").document(id)
        try {
            docRef.set(customer.copy(id = id).toCreateMap()).await()
            appendAuditLog(
                actorUid = actorUid,
                actionType = "CREATE_CUSTOMER",
                entityType = "CUSTOMER",
                entityId = id,
                oldValue = "-",
                newValue = "${customer.name.ifBlank { customer.primaryPhone }} (${customer.primaryPhone})"
            )
            id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateCustomer(customer: Customer): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("customers").document(customer.id)
        try {
            docRef.update(
                mapOf(
                    "name" to customer.name,
                    "primaryPhone" to customer.primaryPhone,
                    "secondaryPhone" to (customer.secondaryPhone ?: ""),
                    "notes" to (customer.notes ?: ""),
                    "isActive" to customer.isActive,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun createLocation(location: CustomerLocation): Result<String> = runCatching {
        val actorUid = requireUserId()
        val id = location.id.ifBlank { "loc_${UUID.randomUUID().toString().take(12)}" }
        val docRef = db.collection("locations").document(id)
        try {
            docRef.set(location.copy(id = id).toCreateMap()).await()
            appendAuditLog(
                actorUid = actorUid,
                actionType = "CREATE_LOCATION",
                entityType = "LOCATION",
                entityId = id,
                oldValue = "-",
                newValue = "${location.branchName} (${location.district ?: location.city ?: ""})"
            )
            id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    // --- Visits & Recurring Schedules ---

    suspend fun createVisit(visit: Visit): Result<String> = runCatching {
        val actorUid = requireUserId()
        val id = visit.id.ifBlank { "vst_${UUID.randomUUID().toString().take(12)}" }
        val docRef = db.collection("visits").document(id)
        try {
            docRef.set(visit.copy(id = id).toCreateMap()).await()
            appendAuditLog(
                actorUid = actorUid,
                actionType = "CREATE_VISIT",
                entityType = "VISIT",
                entityId = id,
                oldValue = "-",
                newValue = "${visit.locationName} (${visit.operationType}) -> ${visit.assignedDriverName ?: visit.assignedDriverId}"
            )
            if (visit.assignedDriverUserId.isNotBlank()) {
                sendNotification(
                    AppNotification(
                        targetUserId = visit.assignedDriverUserId,
                        senderUserId = actorUid,
                        title = "زيارة جديدة: ${visit.locationName}",
                        body = "تم إسناد ${visit.operationType} لفرع ${visit.locationName}",
                        type = "VISIT_ASSIGNED",
                        relatedVisitId = id
                    )
                )
            }
            id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun createRecurringScheduleAndGenerateVisits(
        schedule: RecurringSchedule,
        location: CustomerLocation,
        customerName: String,
        priority: String,
        notes: String?
    ): Result<String> = runCatching {
        val actorUid = requireUserId()
        val schedId = schedule.id.ifBlank { "rec_${UUID.randomUUID().toString().take(12)}" }
        val schedRef = db.collection("recurring_schedules").document(schedId)
        try {
            schedRef.set(schedule.copy(id = schedId).toCreateMap()).await()
            val occurrences = RecurringVisitGenerator.generateOccurrences(
                scheduleId = schedId,
                weekdaysCsv = schedule.weekdaysCsv,
                timeHHmm = schedule.timeHHmm,
                startEpochMs = schedule.startEpochMs,
                endEpochMs = schedule.endEpochMs,
                isContinuous = schedule.isContinuous
            )
            val existingKeys = db.collection("visits")
                .whereEqualTo("recurringScheduleId", schedId)
                .get()
                .await()
                .documents
                .mapNotNull { it.getString("occurrenceKey") }
                .toSet()

            for ((occKey, scheduledMs) in occurrences) {
                if (occKey in existingKeys) continue
                val visitId = "vst_${UUID.randomUUID().toString().take(12)}"
                val visit = Visit(
                    id = visitId,
                    customerId = location.customerId,
                    locationId = location.id,
                    customerName = customerName,
                    locationName = location.branchName,
                    district = location.district,
                    address = location.address,
                    primaryPhone = location.primaryPhone,
                    secondaryPhone = location.secondaryPhone,
                    accessInstructions = location.accessInstructions,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    assignedDriverId = schedule.assignedDriverId,
                    assignedDriverUserId = schedule.assignedDriverUserId,
                    assignedDriverName = schedule.assignedDriverName,
                    operationType = schedule.operationType,
                    priority = priority,
                    status = "NOT_STARTED",
                    scheduledAtEpochMs = scheduledMs,
                    recurringScheduleId = schedId,
                    occurrenceKey = occKey,
                    notes = notes
                )
                db.collection("visits").document(visitId).set(visit.toCreateMap()).await()
            }
            appendAuditLog(
                actorUid = actorUid,
                actionType = "CREATE_RECURRING_SCHEDULE",
                entityType = "RECURRING_SCHEDULE",
                entityId = schedId,
                oldValue = "-",
                newValue = "${location.branchName} (${schedule.weekdaysCsv} @ ${schedule.timeHHmm})"
            )
            schedId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, schedRef.path)
            throw e
        }
    }

    suspend fun updateVisitByAdmin(
        visit: Visit,
        newDriverId: String,
        newDriverUserId: String,
        newDriverName: String,
        newScheduledEpochMs: Long,
        newStatus: String,
        newPriority: String
    ): Result<Unit> = runCatching {
        val actorUid = requireUserId()
        val docRef = db.collection("visits").document(visit.id)
        try {
            docRef.update(
                mapOf(
                    "assignedDriverId" to newDriverId,
                    "assignedDriverUserId" to newDriverUserId,
                    "assignedDriverName" to newDriverName,
                    "scheduledAtEpochMs" to newScheduledEpochMs,
                    "status" to newStatus,
                    "priority" to newPriority,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            appendAuditLog(
                actorUid = actorUid,
                actionType = "UPDATE_VISIT",
                entityType = "VISIT",
                entityId = visit.id,
                oldValue = "${visit.status} (${visit.assignedDriverName ?: visit.assignedDriverId})",
                newValue = "$newStatus ($newDriverName)"
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun stopRecurringScheduleFutureOccurrences(schedule: RecurringSchedule): Result<Unit> = runCatching {
        val actorUid = requireUserId()
        val now = System.currentTimeMillis()
        try {
            db.collection("recurring_schedules").document(schedule.id).update(
                mapOf("isActive" to false, "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            val futureSnap = db.collection("visits")
                .whereEqualTo("recurringScheduleId", schedule.id)
                .get()
                .await()
            for (doc in futureSnap.documents) {
                val st = doc.getString("status") ?: "NOT_STARTED"
                val schedMs = doc.getLong("scheduledAtEpochMs") ?: 0L
                if (schedMs > now && st == "NOT_STARTED") {
                    doc.reference.update(
                        mapOf("status" to "CANCELLED", "updatedAt" to FieldValue.serverTimestamp())
                    ).await()
                }
            }
            appendAuditLog(
                actorUid = actorUid,
                actionType = "STOP_RECURRING",
                entityType = "RECURRING_SCHEDULE",
                entityId = schedule.id,
                oldValue = "ACTIVE",
                newValue = "STOPPED (historical visits preserved)"
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "recurring_schedules/${schedule.id}")
            throw e
        }
    }

    // --- Driver Shift & GPS Telemetry ---

    suspend fun startDriverShift(driver: Driver, notifyAdmin: Boolean): Result<String> = runCatching {
        val uid = requireUserId()
        val now = System.currentTimeMillis()
        val attId = "att_${UUID.randomUUID().toString().take(12)}"
        val attRef = db.collection("driver_attendance").document(attId)
        try {
            val att = DriverAttendance(
                id = attId,
                driverId = driver.id,
                userId = uid,
                driverName = driver.name,
                startedAtEpochMs = now,
                isActive = true
            )
            attRef.set(att.toCreateMap()).await()
            db.collection("drivers").document(driver.id).update(
                mapOf(
                    "status" to "ON_SHIFT",
                    "gpsActive" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            appendAuditLog(uid, "START_SHIFT", "ATTENDANCE", attId, driver.status, "ON_SHIFT")
            if (notifyAdmin) {
                sendNotification(
                    AppNotification(
                        targetUserId = "ALL",
                        senderUserId = uid,
                        title = "بدء دوام المندوب",
                        body = "بدأ ${driver.name} الدوام وتفعيل تتبع GPS",
                        type = "SHIFT_START"
                    )
                )
            }
            attId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, attRef.path)
            throw e
        }
    }

    suspend fun endDriverShift(driver: Driver, activeAttendance: DriverAttendance?, notifyAdmin: Boolean): Result<Unit> = runCatching {
        val uid = requireUserId()
        val now = System.currentTimeMillis()
        try {
            if (activeAttendance != null) {
                val durationMin = ((now - activeAttendance.startedAtEpochMs) / 60_000L).coerceAtLeast(1L)
                db.collection("driver_attendance").document(activeAttendance.id).update(
                    mapOf(
                        "endedAtEpochMs" to now,
                        "durationMinutes" to durationMin,
                        "isActive" to false,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            db.collection("drivers").document(driver.id).update(
                mapOf(
                    "status" to "OFF_SHIFT",
                    "gpsActive" to false,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            appendAuditLog(uid, "END_SHIFT", "DRIVER", driver.id, "ON_SHIFT", "OFF_SHIFT")
            if (notifyAdmin) {
                sendNotification(
                    AppNotification(
                        targetUserId = "ALL",
                        senderUserId = uid,
                        title = "انتهاء دوام المندوب",
                        body = "أنهى ${driver.name} الدوام وتوقف تتبع GPS",
                        type = "SHIFT_END"
                    )
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "drivers/${driver.id}")
            throw e
        }
    }

    suspend fun recordGpsPoint(point: GpsLocationPoint): Result<Unit> = runCatching {
        requireUserId()
        val id = point.id.ifBlank { "gps_${UUID.randomUUID().toString().take(12)}" }
        val docRef = db.collection("gps_locations").document(id)
        try {
            docRef.set(point.toCreateMap()).await()
            db.collection("drivers").document(point.driverId).update(
                mapOf(
                    "gpsActive" to true,
                    "lastLat" to point.latitude,
                    "lastLng" to point.longitude,
                    "lastAccuracyMeters" to point.accuracyMeters,
                    "lastGpsAtEpochMs" to point.recordedAtEpochMs,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    // --- Driver Visit Execution & Payments ---

    suspend fun recordCheckInArrival(
        visit: Visit,
        driverLat: Double?,
        driverLng: Double?,
        settings: AppSettings,
        nowMs: Long = System.currentTimeMillis()
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val (inside, distance) = GeoUtils.isInsideGeofence(
            driverLat, driverLng, visit.latitude, visit.longitude, settings.geofenceRadiusMeters.toInt()
        )
        val (delaySec, delayMin) = GeoUtils.calculateDelay(
            visit.scheduledAtEpochMs, nowMs, settings.gracePeriodMinutes
        )
        val newStatus = if (delaySec > 0L) "LATE" else "ARRIVED"
        val docRef = db.collection("visits").document(visit.id)
        try {
            docRef.update(
                mapOf(
                    "status" to newStatus,
                    "arrivedAtEpochMs" to nowMs,
                    "delaySeconds" to delaySec,
                    "delayMinutes" to delayMin,
                    "insideGeofence" to inside,
                    "arrivalDistanceMeters" to distance.toLong(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            appendAuditLog(
                actorUid = uid,
                actionType = "CHECK_IN_ARRIVAL",
                entityType = "VISIT",
                entityId = visit.id,
                oldValue = visit.status,
                newValue = "$newStatus (Geofence=${if (inside) "IN" else "OUT"}, ${distance}m, delay=${delayMin}m)"
            )
            if (settings.notifyArrival) {
                sendNotification(
                    AppNotification(
                        targetUserId = "ALL",
                        senderUserId = uid,
                        title = "وصول المندوب - ${visit.locationName}",
                        body = if (delayMin > 0L) "تأخر $delayMin د عن الموعد (${distance}م)" else "وصل في الموعد (${distance}م)",
                        type = if (delayMin > 0L) "DELAY_ALERT" else "ARRIVAL",
                        relatedVisitId = visit.id
                    )
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun completeVisitExecution(
        visit: Visit,
        driverId: String,
        driverName: String,
        invoiceNumber: String,
        amountDue: Double,
        amountCollected: Double,
        paymentMethod: String,
        pickupNotes: String,
        pickupPhotoUri: String?,
        driverLat: Double?,
        driverLng: Double?,
        settings: AppSettings,
        nowMs: Long = System.currentTimeMillis()
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val effectiveCollected = if (paymentMethod == "UNPAID") 0.0 else amountCollected.coerceAtLeast(0.0)
        val effectiveDue = amountDue.coerceAtLeast(0.0)
        val remaining = (effectiveDue - effectiveCollected).coerceAtLeast(0.0)
        val payStatus = when {
            paymentMethod == "UNPAID" || effectiveCollected <= 0.0 -> "UNPAID"
            remaining <= 0.0 -> "PAID"
            else -> "PARTIAL"
        }
        val (delaySec, delayMin) = GeoUtils.calculateDelay(
            visit.scheduledAtEpochMs, visit.arrivedAtEpochMs ?: nowMs, settings.gracePeriodMinutes
        )
        val visitRef = db.collection("visits").document(visit.id)
        val visitUpdates = mutableMapOf<String, Any>(
            "status" to "COMPLETED",
            "completedAtEpochMs" to nowMs,
            "delaySeconds" to (visit.delaySeconds ?: delaySec),
            "delayMinutes" to (visit.delayMinutes ?: delayMin),
            "invoiceNumber" to invoiceNumber,
            "amountCollected" to effectiveCollected,
            "paymentMethod" to paymentMethod,
            "paymentStatus" to payStatus,
            "pickupNotes" to pickupNotes,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if ((visit.amountDue ?: 0.0) == 0.0) {
            visitUpdates["amountDue"] = effectiveDue
        }
        if (!pickupPhotoUri.isNullOrBlank()) {
            visitUpdates["pickupPhotoUri"] = pickupPhotoUri
        }
        try {
            visitRef.update(visitUpdates).await()
            if (visit.operationType == "DELIVERY" || visit.operationType == "BOTH") {
                val payId = "pay_${UUID.randomUUID().toString().take(12)}"
                val payment = PaymentRecord(
                    id = payId,
                    visitId = visit.id,
                    driverId = driverId,
                    driverUserId = uid,
                    driverName = driverName,
                    customerId = visit.customerId,
                    locationId = visit.locationId,
                    invoiceNumber = invoiceNumber,
                    amountDue = effectiveDue,
                    amountCollected = effectiveCollected,
                    amountRemaining = remaining,
                    paymentMethod = paymentMethod,
                    paymentStatus = payStatus,
                    deliveryStatus = "COMPLETED",
                    latitude = driverLat,
                    longitude = driverLng,
                    notes = pickupNotes.takeIf { it.isNotBlank() },
                    recordedAtEpochMs = nowMs
                )
                db.collection("payment_records").document(payId).set(payment.toCreateMap()).await()
            }
            appendAuditLog(
                actorUid = uid,
                actionType = "COMPLETE_VISIT",
                entityType = "VISIT",
                entityId = visit.id,
                oldValue = "${visit.status} / ${visit.paymentStatus}",
                newValue = "COMPLETED / $payStatus ($effectiveCollected SAR, $paymentMethod, $invoiceNumber)"
            )
            if (settings.notifyPickupDelivery || settings.notifyPayments) {
                sendNotification(
                    AppNotification(
                        targetUserId = "ALL",
                        senderUserId = uid,
                        title = "إتمام زيارة ${visit.locationName}",
                        body = "فاتورة: ${invoiceNumber.ifBlank { "-" }} • تحصيل: ${effectiveCollected.toInt()} ر.س ($paymentMethod)",
                        type = "VISIT_COMPLETED",
                        relatedVisitId = visit.id
                    )
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, visitRef.path)
            throw e
        }
    }

    suspend fun markVisitNotExecuted(
        visit: Visit,
        reason: String,
        nowMs: Long = System.currentTimeMillis()
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("visits").document(visit.id)
        try {
            docRef.update(
                mapOf(
                    "status" to "NOT_EXECUTED",
                    "completedAtEpochMs" to nowMs,
                    "nonExecutionReason" to reason,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            appendAuditLog(uid, "VISIT_NOT_EXECUTED", "VISIT", visit.id, visit.status, "NOT_EXECUTED: $reason")
            sendNotification(
                AppNotification(
                    targetUserId = "ALL",
                    senderUserId = uid,
                    title = "تعذر تنفيذ زيارة ${visit.locationName}",
                    body = reason,
                    type = "NOT_EXECUTED",
                    relatedVisitId = visit.id
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun replayOfflineAction(action: PendingSyncActionEntity): Result<Unit> = runCatching {
        val snap = db.collection("visits").document(action.visitId).get().await()
        val visit = snap.toObject(Visit::class.java, ServerTimestampBehavior.ESTIMATE)
            ?: return@runCatching
        val settings = db.collection("app_settings").document("default").get().await()
            .toObject(AppSettings::class.java, ServerTimestampBehavior.ESTIMATE) ?: AppSettings()
        when (action.actionType) {
            "ARRIVAL" -> recordCheckInArrival(visit, action.latitude, action.longitude, settings, action.recordedAtEpochMs).getOrThrow()
            "COMPLETE_VISIT" -> completeVisitExecution(
                visit = visit,
                driverId = action.driverId,
                driverName = action.driverName,
                invoiceNumber = action.invoiceNumber,
                amountDue = action.amountDue,
                amountCollected = action.amountCollected,
                paymentMethod = action.paymentMethod,
                pickupNotes = action.pickupNotes,
                pickupPhotoUri = action.pickupPhotoUri.takeIf { it.isNotBlank() },
                driverLat = action.latitude,
                driverLng = action.longitude,
                settings = settings,
                nowMs = action.recordedAtEpochMs
            ).getOrThrow()
            "NOT_EXECUTED" -> markVisitNotExecuted(visit, action.nonExecutionReason, action.recordedAtEpochMs).getOrThrow()
        }
    }

    // --- Notifications, Settings & Audit ---

    suspend fun sendNotification(notification: AppNotification): Result<String> = runCatching {
        val uid = requireUserId()
        val id = notification.id.ifBlank { "ntf_${UUID.randomUUID().toString().take(12)}" }
        val docRef = db.collection("notifications").document(id)
        try {
            docRef.set(notification.copy(id = id, senderUserId = uid).toCreateMap()).await()
            id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun markNotificationRead(notificationId: String, isRead: Boolean): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("notifications").document(notificationId)
        try {
            docRef.update(
                mapOf("isRead" to isRead, "updatedAt" to FieldValue.serverTimestamp())
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun updateAppSettings(settings: AppSettings): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("app_settings").document("default")
        try {
            docRef.set(settings.toSaveMap()).await()
            appendAuditLog(
                actorUid = uid,
                actionType = "UPDATE_SETTINGS",
                entityType = "APP_SETTINGS",
                entityId = "default",
                oldValue = "-",
                newValue = "grace=${settings.gracePeriodMinutes}m, geofence=${settings.geofenceRadiusMeters}m, gps=${settings.gpsMovingIntervalSec}s"
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun appendAuditLog(
        actorUid: String,
        actionType: String,
        entityType: String,
        entityId: String,
        oldValue: String,
        newValue: String,
        metadata: String? = null
    ) {
        val id = "aud_${UUID.randomUUID().toString().take(12)}"
        val docRef = db.collection("audit_logs").document(id)
        val entry = AuditLog(
            id = id,
            actorUserId = actorUid,
            actorName = currentDisplayName(),
            actionType = actionType,
            entityType = entityType,
            entityId = entityId,
            oldValue = oldValue,
            newValue = newValue,
            metadata = metadata
        )
        try {
            docRef.set(entry.toCreateMap()).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
        }
    }
}
