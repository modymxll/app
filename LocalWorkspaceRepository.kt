package com.example.data.local

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
import com.example.data.model.Visit
import com.example.data.validation.GeoUtils
import com.example.data.validation.RecurringVisitGenerator
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Date
import java.util.UUID
import kotlin.random.Random

class LocalWorkspaceRepository(
    private val dao: WorkspaceDao
) {
    private fun Long.toTs(): Timestamp = Timestamp(Date(this))

    // --- Authentication Lookups ---

    suspend fun findDriverByAccessCode(code: String): Driver? =
        dao.findDriverByAccessCode(code.trim())?.toDomain()

    suspend fun findDriverByCredentials(username: String, code: String): Driver? =
        dao.findDriverByCredentials(username.trim(), code.trim())?.toDomain()

    // --- Real-Time Observations ---

    fun observeAppSettings(): Flow<AppSettings> =
        dao.observeSettings().map { it?.toDomain() ?: AppSettings() }

    fun observeAllDrivers(): Flow<List<Driver>> =
        dao.observeActiveDrivers().map { list -> list.map { it.toDomain() } }

    fun observeDriver(driverId: String): Flow<Driver?> =
        dao.observeDriverById(driverId).map { it?.toDomain() }

    fun observeCustomers(): Flow<List<Customer>> =
        dao.observeCustomers().map { list -> list.map { it.toDomain() } }

    fun observeLocations(): Flow<List<CustomerLocation>> =
        dao.observeLocations().map { list -> list.map { it.toDomain() } }

    fun observeAllVisits(): Flow<List<Visit>> =
        dao.observeAllVisits().map { list -> list.map { it.toDomain() } }

    fun observeVisitsForDriver(driverId: String): Flow<List<Visit>> =
        dao.observeVisitsForDriver(driverId).map { list -> list.map { it.toDomain() } }

    fun observeRecurringSchedules(): Flow<List<RecurringSchedule>> =
        dao.observeRecurringSchedules().map { list -> list.map { it.toDomain() } }

    fun observeAttendance(isAdmin: Boolean, driverId: String?): Flow<List<DriverAttendance>> {
        val flow = if (isAdmin || driverId.isNullOrBlank()) {
            dao.observeAllAttendance()
        } else {
            dao.observeDriverAttendance(driverId)
        }
        return flow.map { list -> list.map { it.toDomain() } }
    }

    fun observeGpsHistory7Days(isAdmin: Boolean, driverId: String?): Flow<List<GpsLocationPoint>> {
        val sevenDaysAgo = System.currentTimeMillis() - 7L * 86_400_000L
        val flow = if (isAdmin || driverId.isNullOrBlank()) {
            dao.observeGpsPointsSince(sevenDaysAgo)
        } else {
            dao.observeDriverGpsPointsSince(driverId, sevenDaysAgo)
        }
        return flow.map { list -> list.map { it.toDomain() } }
    }

    fun observePaymentRecords(isAdmin: Boolean, driverId: String?): Flow<List<PaymentRecord>> {
        val flow = if (isAdmin || driverId.isNullOrBlank()) {
            dao.observeAllPayments()
        } else {
            dao.observeDriverPayments(driverId)
        }
        return flow.map { list -> list.map { it.toDomain() } }
    }

    fun observeNotifications(isAdmin: Boolean, driverId: String?): Flow<List<AppNotification>> {
        val flow = if (isAdmin || driverId.isNullOrBlank()) {
            dao.observeAllNotifications()
        } else {
            dao.observeDriverNotifications(driverId)
        }
        return flow.map { list -> list.map { it.toDomain() } }
    }

    fun observeAuditLogs(): Flow<List<AuditLog>> =
        dao.observeAuditLogs().map { list -> list.map { it.toDomain() } }

    // --- Driver CRUD ---

    suspend fun createDriver(
        actorId: String,
        actorName: String,
        name: String,
        username: String,
        phone: String,
        email: String?,
        requestedCode: String?,
        status: String
    ): Result<Pair<String, String>> = runCatching {
        val now = System.currentTimeMillis()
        val id = "drv_${UUID.randomUUID().toString().take(10)}"
        val existingCodes = dao.getAllDriverCodes().toSet() + setOf("605790")
        val finalCode = requestedCode?.trim()?.takeIf { it.length >= 4 && it !in existingCodes }
            ?: generateUniqueDriverCode(existingCodes)

        val entity = LocalDriverEntity(
            id = id,
            userId = id,
            email = email?.trim()?.lowercase()?.takeIf { it.isNotBlank() },
            name = name.trim(),
            username = username.trim(),
            phone = phone,
            accessCode = finalCode,
            status = status,
            gpsActive = false,
            lastLat = null,
            lastLng = null,
            lastAccuracyMeters = null,
            lastGpsAtEpochMs = null,
            isDeleted = false,
            createdAtEpochMs = now,
            updatedAtEpochMs = now
        )
        dao.upsertDriver(entity)
        appendAudit(actorId, actorName, "CREATE_DRIVER", "DRIVER", id, "-", "${entity.name} (${entity.phone}) • كود: $finalCode")
        Pair(id, finalCode)
    }

    private fun generateUniqueDriverCode(excluded: Set<String>): String {
        repeat(40) {
            val candidate = Random.nextInt(100000, 999999).toString()
            if (candidate !in excluded) return candidate
        }
        return (System.currentTimeMillis() % 899999 + 100000).toString()
    }

    suspend fun updateDriverStatus(actorId: String, actorName: String, driver: Driver, newStatus: String): Result<Unit> = runCatching {
        val existing = dao.getDriverById(driver.id) ?: return@runCatching
        val now = System.currentTimeMillis()
        dao.upsertDriver(
            existing.copy(
                status = newStatus,
                gpsActive = if (newStatus == "ON_SHIFT") existing.gpsActive else false,
                updatedAtEpochMs = now
            )
        )
        appendAudit(actorId, actorName, "UPDATE_DRIVER_STATUS", "DRIVER", driver.id, existing.status, newStatus)
    }

    suspend fun softDeleteDriver(actorId: String, actorName: String, driver: Driver): Result<Unit> = runCatching {
        val existing = dao.getDriverById(driver.id) ?: return@runCatching
        val now = System.currentTimeMillis()
        dao.upsertDriver(
            existing.copy(
                isDeleted = true,
                status = "INACTIVE",
                gpsActive = false,
                updatedAtEpochMs = now
            )
        )
        appendAudit(actorId, actorName, "SOFT_DELETE_DRIVER", "DRIVER", driver.id, existing.status, "DELETED (history retained)")
    }

    // --- Customers & Locations ---

    suspend fun createCustomer(actorId: String, actorName: String, customer: Customer): Result<String> = runCatching {
        val now = System.currentTimeMillis()
        val id = customer.id.ifBlank { "cust_${UUID.randomUUID().toString().take(10)}" }
        dao.upsertCustomer(
            LocalCustomerEntity(
                id = id,
                name = customer.name,
                primaryPhone = customer.primaryPhone,
                secondaryPhone = customer.secondaryPhone,
                notes = customer.notes,
                isActive = customer.isActive,
                createdAtEpochMs = now,
                updatedAtEpochMs = now
            )
        )
        appendAudit(actorId, actorName, "CREATE_CUSTOMER", "CUSTOMER", id, "-", "${customer.name.ifBlank { customer.primaryPhone }} (${customer.primaryPhone})")
        id
    }

    suspend fun createLocation(actorId: String, actorName: String, location: CustomerLocation): Result<String> = runCatching {
        val now = System.currentTimeMillis()
        val id = location.id.ifBlank { "loc_${UUID.randomUUID().toString().take(10)}" }
        dao.upsertLocation(
            LocalLocationEntity(
                id = id,
                customerId = location.customerId,
                customerName = location.customerName,
                branchName = location.branchName,
                invoiceNumber = location.invoiceNumber,
                primaryPhone = location.primaryPhone,
                secondaryPhone = location.secondaryPhone,
                address = location.address,
                city = location.city,
                district = location.district,
                latitude = location.latitude,
                longitude = location.longitude,
                accessInstructions = location.accessInstructions,
                notes = location.notes,
                createdAtEpochMs = now,
                updatedAtEpochMs = now
            )
        )
        appendAudit(actorId, actorName, "CREATE_LOCATION", "LOCATION", id, "-", "${location.branchName} (${location.district ?: location.city ?: ""})")
        id
    }

    // --- Visits & Recurring Schedules ---

    suspend fun createVisit(actorId: String, actorName: String, visit: Visit): Result<String> = runCatching {
        val now = System.currentTimeMillis()
        val id = visit.id.ifBlank { "vst_${UUID.randomUUID().toString().take(10)}" }
        dao.upsertVisit(visit.toEntity(id, now, now))
        appendAudit(actorId, actorName, "CREATE_VISIT", "VISIT", id, "-", "${visit.locationName} (${visit.operationType}) -> ${visit.assignedDriverName ?: visit.assignedDriverId}")
        if (visit.assignedDriverId.isNotBlank()) {
            sendNotification(
                AppNotification(
                    targetUserId = visit.assignedDriverId,
                    senderUserId = actorId,
                    title = "زيارة جديدة: ${visit.locationName}",
                    body = "تم إسناد ${visit.operationType} لفرع ${visit.locationName}",
                    type = "VISIT_ASSIGNED",
                    relatedVisitId = id
                )
            )
        }
        id
    }

    suspend fun createRecurringScheduleAndGenerateVisits(
        actorId: String,
        actorName: String,
        schedule: RecurringSchedule,
        location: CustomerLocation,
        customerName: String,
        priority: String,
        notes: String?
    ): Result<String> = runCatching {
        val now = System.currentTimeMillis()
        val schedId = schedule.id.ifBlank { "rec_${UUID.randomUUID().toString().take(10)}" }
        dao.upsertRecurringSchedule(
            LocalRecurringScheduleEntity(
                id = schedId,
                customerId = schedule.customerId,
                locationId = schedule.locationId,
                customerName = customerName,
                locationName = location.branchName,
                assignedDriverId = schedule.assignedDriverId,
                assignedDriverUserId = schedule.assignedDriverId,
                assignedDriverName = schedule.assignedDriverName,
                operationType = schedule.operationType,
                weekdaysCsv = schedule.weekdaysCsv,
                timeHHmm = schedule.timeHHmm,
                startEpochMs = schedule.startEpochMs,
                endEpochMs = schedule.endEpochMs,
                isContinuous = schedule.isContinuous,
                isActive = true,
                createdAtEpochMs = now,
                updatedAtEpochMs = now
            )
        )
        val existingKeys = dao.getVisitsByRecurringId(schedId).mapNotNull { it.occurrenceKey }.toSet()
        val occurrences = RecurringVisitGenerator.generateOccurrences(
            scheduleId = schedId,
            weekdaysCsv = schedule.weekdaysCsv,
            timeHHmm = schedule.timeHHmm,
            startEpochMs = schedule.startEpochMs,
            endEpochMs = schedule.endEpochMs,
            isContinuous = schedule.isContinuous
        )
        for ((occKey, scheduledMs) in occurrences) {
            if (occKey in existingKeys) continue
            val visitId = "vst_${UUID.randomUUID().toString().take(10)}"
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
                assignedDriverUserId = schedule.assignedDriverId,
                assignedDriverName = schedule.assignedDriverName,
                operationType = schedule.operationType,
                priority = priority,
                status = "NOT_STARTED",
                scheduledAtEpochMs = scheduledMs,
                recurringScheduleId = schedId,
                occurrenceKey = occKey,
                notes = notes
            )
            dao.upsertVisit(visit.toEntity(visitId, now, now))
        }
        appendAudit(actorId, actorName, "CREATE_RECURRING_SCHEDULE", "RECURRING_SCHEDULE", schedId, "-", "${location.branchName} (${schedule.weekdaysCsv} @ ${schedule.timeHHmm})")
        schedId
    }

    suspend fun updateVisitByAdmin(
        actorId: String,
        actorName: String,
        visit: Visit,
        newDriver: Driver,
        newStatus: String,
        newPriority: String
    ): Result<Unit> = runCatching {
        val existing = dao.getVisitById(visit.id) ?: return@runCatching
        val now = System.currentTimeMillis()
        dao.upsertVisit(
            existing.copy(
                assignedDriverId = newDriver.id,
                assignedDriverUserId = newDriver.id,
                assignedDriverName = newDriver.name,
                status = newStatus,
                priority = newPriority,
                updatedAtEpochMs = now
            )
        )
        appendAudit(actorId, actorName, "UPDATE_VISIT", "VISIT", visit.id, "${existing.status} (${existing.assignedDriverName ?: existing.assignedDriverId})", "$newStatus (${newDriver.name})")
    }

    suspend fun stopRecurringSchedule(actorId: String, actorName: String, schedule: RecurringSchedule): Result<Unit> = runCatching {
        val now = System.currentTimeMillis()
        dao.upsertRecurringSchedule(
            LocalRecurringScheduleEntity(
                id = schedule.id,
                customerId = schedule.customerId,
                locationId = schedule.locationId,
                customerName = schedule.customerName,
                locationName = schedule.locationName,
                assignedDriverId = schedule.assignedDriverId,
                assignedDriverUserId = schedule.assignedDriverUserId,
                assignedDriverName = schedule.assignedDriverName,
                operationType = schedule.operationType,
                weekdaysCsv = schedule.weekdaysCsv,
                timeHHmm = schedule.timeHHmm,
                startEpochMs = schedule.startEpochMs,
                endEpochMs = schedule.endEpochMs,
                isContinuous = schedule.isContinuous,
                isActive = false,
                createdAtEpochMs = schedule.createdAt?.toDate()?.time ?: now,
                updatedAtEpochMs = now
            )
        )
        val related = dao.getVisitsByRecurringId(schedule.id)
        for (v in related) {
            if (v.scheduledAtEpochMs > now && v.status == "NOT_STARTED") {
                dao.upsertVisit(v.copy(status = "CANCELLED", updatedAtEpochMs = now))
            }
        }
        appendAudit(actorId, actorName, "STOP_RECURRING", "RECURRING_SCHEDULE", schedule.id, "ACTIVE", "STOPPED (historical visits preserved)")
    }

    // --- Driver Shift & GPS ---

    suspend fun startDriverShift(driver: Driver, notifyAdmin: Boolean): Result<String> = runCatching {
        val now = System.currentTimeMillis()
        val attId = "att_${UUID.randomUUID().toString().take(10)}"
        dao.upsertAttendance(
            LocalAttendanceEntity(
                id = attId,
                driverId = driver.id,
                userId = driver.id,
                driverName = driver.name,
                startedAtEpochMs = now,
                endedAtEpochMs = null,
                durationMinutes = null,
                isActive = true,
                createdAtEpochMs = now,
                updatedAtEpochMs = now
            )
        )
        dao.getDriverById(driver.id)?.let { d ->
            dao.upsertDriver(d.copy(status = "ON_SHIFT", gpsActive = true, updatedAtEpochMs = now))
        }
        appendAudit(driver.id, driver.name, "START_SHIFT", "ATTENDANCE", attId, driver.status, "ON_SHIFT")
        if (notifyAdmin) {
            sendNotification(
                AppNotification(
                    targetUserId = "ALL",
                    senderUserId = driver.id,
                    title = "بدء دوام المندوب",
                    body = "بدأ ${driver.name} الدوام وتفعيل تتبع GPS",
                    type = "SHIFT_START"
                )
            )
        }
        attId
    }

    suspend fun endDriverShift(driver: Driver, activeAttendance: DriverAttendance?, notifyAdmin: Boolean): Result<Unit> = runCatching {
        val now = System.currentTimeMillis()
        if (activeAttendance != null) {
            dao.getAttendanceById(activeAttendance.id)?.let { att ->
                val durMin = ((now - att.startedAtEpochMs) / 60_000L).coerceAtLeast(1L)
                dao.upsertAttendance(
                    att.copy(
                        endedAtEpochMs = now,
                        durationMinutes = durMin,
                        isActive = false,
                        updatedAtEpochMs = now
                    )
                )
            }
        }
        dao.getDriverById(driver.id)?.let { d ->
            dao.upsertDriver(d.copy(status = "OFF_SHIFT", gpsActive = false, updatedAtEpochMs = now))
        }
        appendAudit(driver.id, driver.name, "END_SHIFT", "DRIVER", driver.id, "ON_SHIFT", "OFF_SHIFT")
        if (notifyAdmin) {
            sendNotification(
                AppNotification(
                    targetUserId = "ALL",
                    senderUserId = driver.id,
                    title = "انتهاء دوام المندوب",
                    body = "أنهى ${driver.name} الدوام وتوقف تتبع GPS",
                    type = "SHIFT_END"
                )
            )
        }
    }

    suspend fun recordGpsPoint(point: GpsLocationPoint): Result<Unit> = runCatching {
        val id = point.id.ifBlank { "gps_${UUID.randomUUID().toString().take(10)}" }
        dao.insertGpsPoint(
            LocalGpsPointEntity(
                id = id,
                driverId = point.driverId,
                userId = point.userId,
                attendanceId = point.attendanceId,
                latitude = point.latitude,
                longitude = point.longitude,
                accuracyMeters = point.accuracyMeters,
                speedMps = point.speedMps,
                isStopped = point.isStopped,
                recordedAtEpochMs = point.recordedAtEpochMs
            )
        )
        dao.getDriverById(point.driverId)?.let { d ->
            dao.upsertDriver(
                d.copy(
                    gpsActive = true,
                    lastLat = point.latitude,
                    lastLng = point.longitude,
                    lastAccuracyMeters = point.accuracyMeters,
                    lastGpsAtEpochMs = point.recordedAtEpochMs,
                    updatedAtEpochMs = System.currentTimeMillis()
                )
            )
        }
    }

    // --- Driver Visit Execution & Payments ---

    suspend fun recordCheckInArrival(
        actorId: String,
        actorName: String,
        visit: Visit,
        driverLat: Double?,
        driverLng: Double?,
        settings: AppSettings,
        nowMs: Long = System.currentTimeMillis()
    ): Result<Unit> = runCatching {
        val existing = dao.getVisitById(visit.id) ?: return@runCatching
        val (inside, distance) = GeoUtils.isInsideGeofence(
            driverLat, driverLng, existing.latitude, existing.longitude, settings.geofenceRadiusMeters.toInt()
        )
        val (delaySec, delayMin) = GeoUtils.calculateDelay(
            existing.scheduledAtEpochMs, nowMs, settings.gracePeriodMinutes
        )
        val newStatus = if (delaySec > 0L) "LATE" else "ARRIVED"
        dao.upsertVisit(
            existing.copy(
                status = newStatus,
                arrivedAtEpochMs = nowMs,
                delaySeconds = delaySec,
                delayMinutes = delayMin,
                insideGeofence = inside,
                arrivalDistanceMeters = distance.toLong(),
                updatedAtEpochMs = nowMs
            )
        )
        appendAudit(
            actorId, actorName, "CHECK_IN_ARRIVAL", "VISIT", visit.id,
            existing.status, "$newStatus (Geofence=${if (inside) "IN" else "OUT"}, ${distance}m, delay=${delayMin}m)"
        )
        if (settings.notifyArrival) {
            sendNotification(
                AppNotification(
                    targetUserId = "ALL",
                    senderUserId = actorId,
                    title = "وصول المندوب - ${visit.locationName}",
                    body = if (delayMin > 0L) "تأخر $delayMin د عن الموعد (${distance}م)" else "وصل في الموعد (${distance}م)",
                    type = if (delayMin > 0L) "DELAY_ALERT" else "ARRIVAL",
                    relatedVisitId = visit.id
                )
            )
        }
    }

    suspend fun completeVisitExecution(
        actorId: String,
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
        val existing = dao.getVisitById(visit.id) ?: return@runCatching
        val effectiveCollected = if (paymentMethod == "UNPAID") 0.0 else amountCollected.coerceAtLeast(0.0)
        val effectiveDue = amountDue.coerceAtLeast(0.0)
        val remaining = (effectiveDue - effectiveCollected).coerceAtLeast(0.0)
        val payStatus = when {
            paymentMethod == "UNPAID" || effectiveCollected <= 0.0 -> "UNPAID"
            remaining <= 0.0 -> "PAID"
            else -> "PARTIAL"
        }
        val (delaySec, delayMin) = GeoUtils.calculateDelay(
            existing.scheduledAtEpochMs, existing.arrivedAtEpochMs ?: nowMs, settings.gracePeriodMinutes
        )
        dao.upsertVisit(
            existing.copy(
                status = "COMPLETED",
                completedAtEpochMs = nowMs,
                delaySeconds = existing.delaySeconds ?: delaySec,
                delayMinutes = existing.delayMinutes ?: delayMin,
                invoiceNumber = invoiceNumber.takeIf { it.isNotBlank() },
                amountDue = if ((existing.amountDue ?: 0.0) == 0.0) effectiveDue else existing.amountDue,
                amountCollected = effectiveCollected,
                paymentMethod = paymentMethod,
                paymentStatus = payStatus,
                pickupNotes = pickupNotes.takeIf { it.isNotBlank() },
                pickupPhotoUri = pickupPhotoUri?.takeIf { it.isNotBlank() } ?: existing.pickupPhotoUri,
                updatedAtEpochMs = nowMs
            )
        )
        if (existing.operationType == "DELIVERY" || existing.operationType == "BOTH") {
            val payId = "pay_${UUID.randomUUID().toString().take(10)}"
            dao.insertPayment(
                LocalPaymentRecordEntity(
                    id = payId,
                    visitId = visit.id,
                    driverId = driverId,
                    driverUserId = actorId,
                    driverName = driverName,
                    customerId = existing.customerId,
                    locationId = existing.locationId,
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
            )
        }
        appendAudit(
            actorId, driverName, "COMPLETE_VISIT", "VISIT", visit.id,
            "${existing.status} / ${existing.paymentStatus}",
            "COMPLETED / $payStatus ($effectiveCollected SAR, $paymentMethod, $invoiceNumber)"
        )
        if (settings.notifyPickupDelivery || settings.notifyPayments) {
            sendNotification(
                AppNotification(
                    targetUserId = "ALL",
                    senderUserId = actorId,
                    title = "إتمام زيارة ${visit.locationName}",
                    body = "فاتورة: ${invoiceNumber.ifBlank { "-" }} • تحصيل: ${effectiveCollected.toInt()} ر.س ($paymentMethod)",
                    type = "VISIT_COMPLETED",
                    relatedVisitId = visit.id
                )
            )
        }
    }

    suspend fun markVisitNotExecuted(
        actorId: String,
        actorName: String,
        visit: Visit,
        reason: String,
        nowMs: Long = System.currentTimeMillis()
    ): Result<Unit> = runCatching {
        val existing = dao.getVisitById(visit.id) ?: return@runCatching
        dao.upsertVisit(
            existing.copy(
                status = "NOT_EXECUTED",
                completedAtEpochMs = nowMs,
                nonExecutionReason = reason,
                updatedAtEpochMs = nowMs
            )
        )
        appendAudit(actorId, actorName, "VISIT_NOT_EXECUTED", "VISIT", visit.id, existing.status, "NOT_EXECUTED: $reason")
        sendNotification(
            AppNotification(
                targetUserId = "ALL",
                senderUserId = actorId,
                title = "تعذر تنفيذ زيارة ${visit.locationName}",
                body = reason,
                type = "NOT_EXECUTED",
                relatedVisitId = visit.id
            )
        )
    }

    // --- Notifications, Settings & Audit ---

    suspend fun sendNotification(notification: AppNotification): Result<String> = runCatching {
        val now = System.currentTimeMillis()
        val id = notification.id.ifBlank { "ntf_${UUID.randomUUID().toString().take(10)}" }
        dao.insertNotification(
            LocalNotificationEntity(
                id = id,
                targetUserId = notification.targetUserId,
                senderUserId = notification.senderUserId,
                title = notification.title,
                body = notification.body,
                type = notification.type,
                relatedVisitId = notification.relatedVisitId,
                isRead = notification.isRead,
                createdAtEpochMs = now
            )
        )
        id
    }

    suspend fun markNotificationRead(notificationId: String, isRead: Boolean): Result<Unit> = runCatching {
        dao.markNotificationRead(notificationId, isRead)
    }

    suspend fun updateAppSettings(actorId: String, actorName: String, settings: AppSettings): Result<Unit> = runCatching {
        val now = System.currentTimeMillis()
        dao.upsertSettings(
            LocalSettingsEntity(
                id = "default",
                gracePeriodMinutes = settings.gracePeriodMinutes.coerceIn(0L, 120L),
                geofenceRadiusMeters = settings.geofenceRadiusMeters.coerceIn(20L, 2000L),
                gpsMovingIntervalSec = settings.gpsMovingIntervalSec.coerceIn(10L, 300L),
                gpsIdleIntervalSec = settings.gpsIdleIntervalSec.coerceIn(30L, 900L),
                notifyShiftEvents = settings.notifyShiftEvents,
                notifyArrival = settings.notifyArrival,
                notifyPickupDelivery = settings.notifyPickupDelivery,
                notifyPayments = settings.notifyPayments,
                notifyDelays = settings.notifyDelays,
                notifyGpsDisconnect = settings.notifyGpsDisconnect,
                updatedAtEpochMs = now
            )
        )
        appendAudit(actorId, actorName, "UPDATE_SETTINGS", "APP_SETTINGS", "default", "-", "grace=${settings.gracePeriodMinutes}m, geofence=${settings.geofenceRadiusMeters}m")
    }

    private suspend fun appendAudit(
        actorId: String,
        actorName: String,
        actionType: String,
        entityType: String,
        entityId: String,
        oldValue: String,
        newValue: String
    ) {
        val now = System.currentTimeMillis()
        dao.insertAuditLog(
            LocalAuditLogEntity(
                id = "aud_${UUID.randomUUID().toString().take(10)}",
                actorUserId = actorId,
                actorName = actorName,
                actionType = actionType,
                entityType = entityType,
                entityId = entityId,
                oldValue = oldValue,
                newValue = newValue,
                metadata = null,
                createdAtEpochMs = now
            )
        )
    }

    // --- Entity <-> Domain Mappers ---

    private fun LocalDriverEntity.toDomain() = Driver(
        id = id,
        userId = userId,
        email = email,
        name = name,
        username = username,
        phone = phone,
        status = status,
        gpsActive = gpsActive,
        lastLat = lastLat,
        lastLng = lastLng,
        lastAccuracyMeters = lastAccuracyMeters,
        lastGpsAtEpochMs = lastGpsAtEpochMs,
        isDeleted = isDeleted,
        accessCode = accessCode,
        createdAt = createdAtEpochMs.toTs(),
        updatedAt = updatedAtEpochMs.toTs()
    )

    private fun LocalCustomerEntity.toDomain() = Customer(
        id = id,
        name = name,
        primaryPhone = primaryPhone,
        secondaryPhone = secondaryPhone,
        notes = notes,
        isActive = isActive,
        createdAt = createdAtEpochMs.toTs(),
        updatedAt = updatedAtEpochMs.toTs()
    )

    private fun LocalLocationEntity.toDomain() = CustomerLocation(
        id = id,
        customerId = customerId,
        customerName = customerName,
        branchName = branchName,
        invoiceNumber = invoiceNumber,
        primaryPhone = primaryPhone,
        secondaryPhone = secondaryPhone,
        address = address,
        city = city,
        district = district,
        latitude = latitude,
        longitude = longitude,
        accessInstructions = accessInstructions,
        notes = notes,
        createdAt = createdAtEpochMs.toTs(),
        updatedAt = updatedAtEpochMs.toTs()
    )

    private fun LocalVisitEntity.toDomain() = Visit(
        id = id,
        customerId = customerId,
        locationId = locationId,
        customerName = customerName,
        locationName = locationName,
        district = district,
        address = address,
        primaryPhone = primaryPhone,
        secondaryPhone = secondaryPhone,
        accessInstructions = accessInstructions,
        latitude = latitude,
        longitude = longitude,
        assignedDriverId = assignedDriverId,
        assignedDriverUserId = assignedDriverUserId,
        assignedDriverName = assignedDriverName,
        operationType = operationType,
        priority = priority,
        status = status,
        scheduledAtEpochMs = scheduledAtEpochMs,
        arrivedAtEpochMs = arrivedAtEpochMs,
        startedAtEpochMs = startedAtEpochMs,
        completedAtEpochMs = completedAtEpochMs,
        delaySeconds = delaySeconds,
        delayMinutes = delayMinutes,
        insideGeofence = insideGeofence,
        arrivalDistanceMeters = arrivalDistanceMeters,
        invoiceNumber = invoiceNumber,
        posInvoiceRef = posInvoiceRef,
        amountDue = amountDue,
        amountCollected = amountCollected,
        paymentMethod = paymentMethod,
        paymentStatus = paymentStatus,
        pickupNotes = pickupNotes,
        pickupPhotoUri = pickupPhotoUri,
        nonExecutionReason = nonExecutionReason,
        recurringScheduleId = recurringScheduleId,
        occurrenceKey = occurrenceKey,
        notes = notes,
        createdAt = createdAtEpochMs.toTs(),
        updatedAt = updatedAtEpochMs.toTs()
    )

    private fun Visit.toEntity(id: String, createdMs: Long, updatedMs: Long) = LocalVisitEntity(
        id = id,
        customerId = customerId,
        locationId = locationId,
        customerName = customerName,
        locationName = locationName,
        district = district,
        address = address,
        primaryPhone = primaryPhone,
        secondaryPhone = secondaryPhone,
        accessInstructions = accessInstructions,
        latitude = latitude,
        longitude = longitude,
        assignedDriverId = assignedDriverId,
        assignedDriverUserId = assignedDriverUserId,
        assignedDriverName = assignedDriverName,
        operationType = operationType,
        priority = priority,
        status = status,
        scheduledAtEpochMs = scheduledAtEpochMs,
        arrivedAtEpochMs = arrivedAtEpochMs,
        startedAtEpochMs = startedAtEpochMs,
        completedAtEpochMs = completedAtEpochMs,
        delaySeconds = delaySeconds,
        delayMinutes = delayMinutes,
        insideGeofence = insideGeofence,
        arrivalDistanceMeters = arrivalDistanceMeters,
        invoiceNumber = invoiceNumber,
        posInvoiceRef = posInvoiceRef,
        amountDue = amountDue,
        amountCollected = amountCollected,
        paymentMethod = paymentMethod,
        paymentStatus = paymentStatus,
        pickupNotes = pickupNotes,
        pickupPhotoUri = pickupPhotoUri,
        nonExecutionReason = nonExecutionReason,
        recurringScheduleId = recurringScheduleId,
        occurrenceKey = occurrenceKey,
        notes = notes,
        createdAtEpochMs = createdMs,
        updatedAtEpochMs = updatedMs
    )

    private fun LocalRecurringScheduleEntity.toDomain() = RecurringSchedule(
        id = id,
        customerId = customerId,
        locationId = locationId,
        customerName = customerName,
        locationName = locationName,
        assignedDriverId = assignedDriverId,
        assignedDriverUserId = assignedDriverUserId,
        assignedDriverName = assignedDriverName,
        operationType = operationType,
        weekdaysCsv = weekdaysCsv,
        timeHHmm = timeHHmm,
        startEpochMs = startEpochMs,
        endEpochMs = endEpochMs,
        isContinuous = isContinuous,
        isActive = isActive,
        createdAt = createdAtEpochMs.toTs(),
        updatedAt = updatedAtEpochMs.toTs()
    )

    private fun LocalAttendanceEntity.toDomain() = DriverAttendance(
        id = id,
        driverId = driverId,
        userId = userId,
        driverName = driverName,
        startedAtEpochMs = startedAtEpochMs,
        endedAtEpochMs = endedAtEpochMs,
        durationMinutes = durationMinutes,
        isActive = isActive,
        createdAt = createdAtEpochMs.toTs(),
        updatedAt = updatedAtEpochMs.toTs()
    )

    private fun LocalGpsPointEntity.toDomain() = GpsLocationPoint(
        id = id,
        driverId = driverId,
        userId = userId,
        attendanceId = attendanceId,
        latitude = latitude,
        longitude = longitude,
        accuracyMeters = accuracyMeters,
        speedMps = speedMps,
        isStopped = isStopped,
        recordedAtEpochMs = recordedAtEpochMs,
        createdAt = recordedAtEpochMs.toTs()
    )

    private fun LocalPaymentRecordEntity.toDomain() = PaymentRecord(
        id = id,
        visitId = visitId,
        driverId = driverId,
        driverUserId = driverUserId,
        driverName = driverName,
        customerId = customerId,
        locationId = locationId,
        invoiceNumber = invoiceNumber,
        amountDue = amountDue,
        amountCollected = amountCollected,
        amountRemaining = amountRemaining,
        paymentMethod = paymentMethod,
        paymentStatus = paymentStatus,
        deliveryStatus = deliveryStatus,
        latitude = latitude,
        longitude = longitude,
        notes = notes,
        recordedAtEpochMs = recordedAtEpochMs,
        createdAt = recordedAtEpochMs.toTs(),
        updatedAt = recordedAtEpochMs.toTs()
    )

    private fun LocalNotificationEntity.toDomain() = AppNotification(
        id = id,
        targetUserId = targetUserId,
        senderUserId = senderUserId,
        title = title,
        body = body,
        type = type,
        relatedVisitId = relatedVisitId,
        isRead = isRead,
        createdAt = createdAtEpochMs.toTs(),
        updatedAt = createdAtEpochMs.toTs()
    )

    private fun LocalAuditLogEntity.toDomain() = AuditLog(
        id = id,
        actorUserId = actorUserId,
        actorName = actorName,
        actionType = actionType,
        entityType = entityType,
        entityId = entityId,
        oldValue = oldValue,
        newValue = newValue,
        metadata = metadata,
        createdAt = createdAtEpochMs.toTs()
    )

    private fun LocalSettingsEntity.toDomain() = AppSettings(
        id = id,
        gracePeriodMinutes = gracePeriodMinutes,
        geofenceRadiusMeters = geofenceRadiusMeters,
        gpsMovingIntervalSec = gpsMovingIntervalSec,
        gpsIdleIntervalSec = gpsIdleIntervalSec,
        notifyShiftEvents = notifyShiftEvents,
        notifyArrival = notifyArrival,
        notifyPickupDelivery = notifyPickupDelivery,
        notifyPayments = notifyPayments,
        notifyDelays = notifyDelays,
        notifyGpsDisconnect = notifyGpsDisconnect,
        updatedAt = updatedAtEpochMs.toTs()
    )
}
