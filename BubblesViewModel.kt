package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.error.unwrapFirestoreException
import com.example.data.local.BubblesLocalDao
import com.example.data.local.LocalWorkspaceRepository
import com.example.data.local.OfflineSyncManager
import com.example.data.local.PendingSyncActionEntity
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
import com.example.data.repository.BubblesRepository
import com.example.data.validation.GeoUtils
import com.example.data.validation.SaudiPhoneUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

@OptIn(ExperimentalCoroutinesApi::class)
class BubblesViewModel(
    private val repository: BubblesRepository,
    private val localWorkspace: LocalWorkspaceRepository,
    private val localDao: BubblesLocalDao,
    private val syncManager: OfflineSyncManager,
    val currentUserId: String,
    val isCloudMode: Boolean,
    private val sessionRole: String = "ADMIN",
    private val sessionDriverId: String? = null,
    private val sessionActorName: String = "الإدارة"
) : ViewModel() {

    private val _toastEvents = MutableSharedFlow<String>(extraBufferCapacity = 10)
    val toastEvents: SharedFlow<String> = _toastEvents.asSharedFlow()

    private val _selectedPreviewDriverId = MutableStateFlow<String?>(null)
    val selectedPreviewDriverId: StateFlow<String?> = _selectedPreviewDriverId.asStateFlow()

    val isOnline: StateFlow<Boolean> = syncManager.isOnline
    val pendingSyncCount: StateFlow<Int> = syncManager.pendingSyncCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), 0)

    private fun <T> Flow<T>.toSafeUiState(tagLabel: String): StateFlow<UiState<T>> =
        map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val fe = error.unwrapFirestoreException()
                if (fe == null && error is CancellationException) throw error
                val root = fe ?: error
                Log.w(TAG, "Stream error ($tagLabel)", root)
                emit(UiState.Error(root.localizedMessage ?: "Failed to load $tagLabel"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = UiState.Loading
            )

    val userProfileState: StateFlow<UiState<UserProfile?>> = if (isCloudMode) {
        repository.observeUserProfile(currentUserId).toSafeUiState("userProfile")
    } else {
        flowOf<UiState<UserProfile?>>(
            UiState.Success(
                UserProfile(
                    id = currentUserId,
                    userId = currentUserId,
                    displayName = sessionActorName,
                    role = sessionRole,
                    driverId = sessionDriverId
                )
            )
        ).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = UiState.Success(
                UserProfile(
                    id = currentUserId,
                    userId = currentUserId,
                    displayName = sessionActorName,
                    role = sessionRole,
                    driverId = sessionDriverId
                )
            )
        )
    }

    private val roleInfoFlow: Flow<Pair<Boolean, String?>> = userProfileState
        .map { st ->
            val p = (st as? UiState.Success)?.data
            Pair(p?.role == "ADMIN", p?.driverId)
        }
        .distinctUntilChanged()

    val appSettings: StateFlow<AppSettings> = if (isCloudMode) {
        repository.observeAppSettings(currentUserId)
            .catch { emit(AppSettings()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), AppSettings())
    } else {
        localWorkspace.observeAppSettings()
            .catch { emit(AppSettings()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), AppSettings())
    }

    val drivers: StateFlow<List<Driver>> = roleInfoFlow.flatMapLatest { (isAdmin, driverId) ->
        if (isCloudMode) {
            if (isAdmin) repository.observeAllDrivers(currentUserId)
            else repository.observeMyDriver(currentUserId).map { listOfNotNull(it) }
        } else {
            if (isAdmin) localWorkspace.observeAllDrivers()
            else if (!driverId.isNullOrBlank()) localWorkspace.observeDriver(driverId).map { listOfNotNull(it) }
            else flowOf(emptyList())
        }
    }.catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val customers: StateFlow<List<Customer>> = roleInfoFlow.flatMapLatest { (isAdmin, _) ->
        if (!isAdmin) flowOf(emptyList())
        else if (isCloudMode) repository.observeCustomers(currentUserId)
        else localWorkspace.observeCustomers()
    }.catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val locations: StateFlow<List<CustomerLocation>> = roleInfoFlow.flatMapLatest { (isAdmin, _) ->
        if (!isAdmin) flowOf(emptyList())
        else if (isCloudMode) repository.observeLocations(currentUserId)
        else localWorkspace.observeLocations()
    }.catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val visits: StateFlow<List<Visit>> = roleInfoFlow.flatMapLatest { (isAdmin, driverId) ->
        if (isCloudMode) {
            if (isAdmin) repository.observeAllVisits(currentUserId)
            else repository.observeVisitsForDriver(currentUserId, driverId)
        } else {
            if (isAdmin) localWorkspace.observeAllVisits()
            else if (!driverId.isNullOrBlank()) localWorkspace.observeVisitsForDriver(driverId)
            else flowOf(emptyList())
        }
    }.catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val recurringSchedules: StateFlow<List<RecurringSchedule>> = roleInfoFlow.flatMapLatest { (isAdmin, _) ->
        if (!isAdmin) flowOf(emptyList())
        else if (isCloudMode) repository.observeRecurringSchedules(currentUserId)
        else localWorkspace.observeRecurringSchedules()
    }.catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val attendanceRecords: StateFlow<List<DriverAttendance>> = roleInfoFlow.flatMapLatest { (isAdmin, driverId) ->
        if (isCloudMode) repository.observeAttendance(currentUserId, isAdmin)
        else localWorkspace.observeAttendance(isAdmin, driverId)
    }.catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val gpsHistory7Days: StateFlow<List<GpsLocationPoint>> = roleInfoFlow.flatMapLatest { (isAdmin, driverId) ->
        if (isCloudMode) repository.observeGpsHistory7Days(currentUserId, isAdmin)
        else localWorkspace.observeGpsHistory7Days(isAdmin, driverId)
    }.catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val paymentRecords: StateFlow<List<PaymentRecord>> = roleInfoFlow.flatMapLatest { (isAdmin, driverId) ->
        if (isCloudMode) repository.observePaymentRecords(currentUserId, isAdmin)
        else localWorkspace.observePaymentRecords(isAdmin, driverId)
    }.catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val notifications: StateFlow<List<AppNotification>> = roleInfoFlow.flatMapLatest { (isAdmin, driverId) ->
        if (isCloudMode) repository.observeNotifications(currentUserId, isAdmin)
        else localWorkspace.observeNotifications(isAdmin, driverId)
    }.catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val auditLogs: StateFlow<List<AuditLog>> = roleInfoFlow.flatMapLatest { (isAdmin, _) ->
        if (!isAdmin) flowOf(emptyList())
        else if (isCloudMode) repository.observeAuditLogs(currentUserId)
        else localWorkspace.observeAuditLogs()
    }.catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    init {
        if (isCloudMode) {
            viewModelScope.launch {
                val res = repository.ensureUserProfileAndBootstrap()
                if (res.isFailure) {
                    emitToast(res.exceptionOrNull()?.localizedMessage ?: "Account setup failed")
                }
                syncManager.flushPendingQueue()
            }
        }
    }

    fun selectDriverForPreview(driverId: String) {
        _selectedPreviewDriverId.value = driverId
    }

    fun syncOfflineNow() {
        if (isCloudMode) {
            syncManager.flushPendingQueue()
            emitToast("جاري مزامنة البيانات السحابية...")
        } else {
            emitToast("جميع البيانات محفوظة ومحدثة محليًا")
        }
    }

    fun createDriver(
        name: String,
        username: String,
        phone: String,
        email: String?,
        accessCode: String?,
        status: String
    ) = viewModelScope.launch {
        val normPhone = SaudiPhoneUtils.normalize(phone)
        if (!SaudiPhoneUtils.isValid(normPhone)) {
            emitToast("يرجى إدخال رقم جوال سعودي صحيح (+9665...)")
            return@launch
        }
        val cleanUser = username.trim().ifBlank { "drv_${System.currentTimeMillis() % 10000}" }
        if (isCloudMode) {
            val res = repository.createDriver(
                Driver(
                    userId = "",
                    email = email?.trim()?.lowercase()?.takeIf { it.isNotBlank() },
                    name = name.trim(),
                    username = cleanUser,
                    phone = normPhone,
                    status = status,
                    gpsActive = false
                )
            )
            emitToast(if (res.isSuccess) "تمت إضافة المندوب بنجاح" else (res.exceptionOrNull()?.localizedMessage ?: "فشل إضافة المندوب"))
        } else {
            val res = localWorkspace.createDriver(
                actorId = currentUserId,
                actorName = sessionActorName,
                name = name,
                username = cleanUser,
                phone = normPhone,
                email = email,
                requestedCode = accessCode,
                status = status
            )
            val createdCode = res.getOrNull()?.second
            emitToast(
                if (createdCode != null) "تمت إضافة المندوب • كود الدخول: $createdCode"
                else (res.exceptionOrNull()?.localizedMessage ?: "فشل إضافة المندوب")
            )
        }
    }

    fun updateDriverStatus(driver: Driver, newStatus: String) = viewModelScope.launch {
        val res = if (isCloudMode) {
            repository.updateDriverByAdmin(driver, newStatus = newStatus)
        } else {
            localWorkspace.updateDriverStatus(currentUserId, sessionActorName, driver, newStatus)
        }
        emitToast(if (res.isSuccess) "تم تحديث حالة المندوب" else "تعذر التحديث")
    }

    fun softDeleteDriver(driver: Driver) = viewModelScope.launch {
        val res = if (isCloudMode) {
            repository.softDeleteDriver(driver)
        } else {
            localWorkspace.softDeleteDriver(currentUserId, sessionActorName, driver)
        }
        emitToast(if (res.isSuccess) "تم إيقاف وحفظ سجل المندوب التاريخي" else "تعذر الحذف")
    }

    fun createCustomerWithInitialBranch(
        customerName: String,
        primaryPhone: String,
        secondaryPhone: String?,
        customerNotes: String?,
        branchName: String,
        city: String,
        district: String,
        address: String,
        latitude: Double,
        longitude: Double,
        accessInstructions: String?
    ) = viewModelScope.launch {
        val normPrimary = SaudiPhoneUtils.normalize(primaryPhone)
        if (!SaudiPhoneUtils.isValid(normPrimary)) {
            emitToast("رقم الجوال الرئيسي غير صالح (+9665...)")
            return@launch
        }
        val normSecondary = secondaryPhone?.takeIf { it.isNotBlank() }?.let { SaudiPhoneUtils.normalize(it) }
        val custModel = Customer(
            name = customerName.trim(),
            primaryPhone = normPrimary,
            secondaryPhone = normSecondary,
            notes = customerNotes?.trim()
        )
        val custRes = if (isCloudMode) {
            repository.createCustomer(custModel)
        } else {
            localWorkspace.createCustomer(currentUserId, sessionActorName, custModel)
        }
        val custId = custRes.getOrNull()
        if (custId == null) {
            emitToast(custRes.exceptionOrNull()?.localizedMessage ?: "فشل حفظ العميل")
            return@launch
        }
        val locModel = CustomerLocation(
            customerId = custId,
            customerName = customerName.trim().ifBlank { normPrimary },
            branchName = branchName.trim().ifBlank { "الفرع الرئيسي" },
            primaryPhone = normPrimary,
            secondaryPhone = normSecondary,
            city = city.trim().ifBlank { "الرياض" },
            district = district.trim(),
            address = address.trim(),
            latitude = latitude,
            longitude = longitude,
            accessInstructions = accessInstructions?.trim()
        )
        val locRes = if (isCloudMode) {
            repository.createLocation(locModel)
        } else {
            localWorkspace.createLocation(currentUserId, sessionActorName, locModel)
        }
        emitToast(if (locRes.isSuccess) "تم حفظ العميل والموقع بنجاح" else "تم حفظ العميل وفشل حفظ الموقع")
    }

    fun addBranchLocation(
        customer: Customer,
        branchName: String,
        primaryPhone: String,
        city: String,
        district: String,
        address: String,
        latitude: Double,
        longitude: Double,
        instructions: String?
    ) = viewModelScope.launch {
        val normPhone = SaudiPhoneUtils.normalize(primaryPhone.ifBlank { customer.primaryPhone })
        val loc = CustomerLocation(
            customerId = customer.id,
            customerName = customer.name.ifBlank { customer.primaryPhone },
            branchName = branchName.trim().ifBlank { "فرع إضافي" },
            primaryPhone = normPhone,
            city = city.trim().ifBlank { "الرياض" },
            district = district.trim(),
            address = address.trim(),
            latitude = latitude,
            longitude = longitude,
            accessInstructions = instructions?.trim()
        )
        val res = if (isCloudMode) {
            repository.createLocation(loc)
        } else {
            localWorkspace.createLocation(currentUserId, sessionActorName, loc)
        }
        emitToast(if (res.isSuccess) "تمت إضافة الفرع للعميل" else "فشل إضافة الفرع")
    }

    fun scheduleVisitOrRecurring(
        location: CustomerLocation,
        driver: Driver,
        operationType: String,
        priority: String,
        scheduledAtEpochMs: Long,
        notes: String?,
        isRecurring: Boolean,
        weekdaysCsv: String,
        timeHHmm: String,
        isContinuous: Boolean
    ) = viewModelScope.launch {
        val custName = location.customerName ?: location.primaryPhone
        val res = if (isRecurring) {
            val schedule = RecurringSchedule(
                customerId = location.customerId,
                locationId = location.id,
                customerName = custName,
                locationName = location.branchName,
                assignedDriverId = driver.id,
                assignedDriverUserId = driver.userId,
                assignedDriverName = driver.name,
                operationType = operationType,
                weekdaysCsv = weekdaysCsv,
                timeHHmm = timeHHmm,
                startEpochMs = scheduledAtEpochMs,
                endEpochMs = if (isContinuous) null else (scheduledAtEpochMs + 30L * 86_400_000L),
                isContinuous = isContinuous,
                isActive = true
            )
            if (isCloudMode) {
                repository.createRecurringScheduleAndGenerateVisits(schedule, location, custName, priority, notes)
            } else {
                localWorkspace.createRecurringScheduleAndGenerateVisits(currentUserId, sessionActorName, schedule, location, custName, priority, notes)
            }
        } else {
            val visit = Visit(
                customerId = location.customerId,
                locationId = location.id,
                customerName = custName,
                locationName = location.branchName,
                district = location.district,
                address = location.address,
                primaryPhone = location.primaryPhone,
                secondaryPhone = location.secondaryPhone,
                accessInstructions = location.accessInstructions,
                latitude = location.latitude,
                longitude = location.longitude,
                assignedDriverId = driver.id,
                assignedDriverUserId = driver.userId,
                assignedDriverName = driver.name,
                operationType = operationType,
                priority = priority,
                status = "NOT_STARTED",
                scheduledAtEpochMs = scheduledAtEpochMs,
                notes = notes
            )
            if (isCloudMode) repository.createVisit(visit)
            else localWorkspace.createVisit(currentUserId, sessionActorName, visit)
        }
        emitToast(if (res.isSuccess) "تم حفظ الزيارة بنجاح" else (res.exceptionOrNull()?.localizedMessage ?: "فشل إنشاء الزيارة"))
    }

    fun updateVisitAdmin(
        visit: Visit,
        newDriver: Driver,
        newStatus: String,
        newPriority: String
    ) = viewModelScope.launch {
        val res = if (isCloudMode) {
            repository.updateVisitByAdmin(
                visit = visit,
                newDriverId = newDriver.id,
                newDriverUserId = newDriver.userId,
                newDriverName = newDriver.name,
                newScheduledEpochMs = visit.scheduledAtEpochMs,
                newStatus = newStatus,
                newPriority = newPriority
            )
        } else {
            localWorkspace.updateVisitByAdmin(currentUserId, sessionActorName, visit, newDriver, newStatus, newPriority)
        }
        emitToast(if (res.isSuccess) "تم تحديث الزيارة" else "فشل تحديث الزيارة")
    }

    fun stopRecurringSchedule(schedule: RecurringSchedule) = viewModelScope.launch {
        val res = if (isCloudMode) {
            repository.stopRecurringScheduleFutureOccurrences(schedule)
        } else {
            localWorkspace.stopRecurringSchedule(currentUserId, sessionActorName, schedule)
        }
        emitToast(if (res.isSuccess) "تم إيقاف التكرار وحذف المواعيد المستقبلية فقط" else "فشل إيقاف التكرار")
    }

    suspend fun startShift(driver: Driver): String? {
        val settings = appSettings.value
        val res = if (isCloudMode) {
            repository.startDriverShift(driver, settings.notifyShiftEvents)
        } else {
            localWorkspace.startDriverShift(driver, settings.notifyShiftEvents)
        }
        val attId = res.getOrNull()
        emitToast(if (attId != null) "تم بدء الدوام وتفعيل GPS" else "تعذر بدء الدوام")
        return attId
    }

    fun endShift(driver: Driver) = viewModelScope.launch {
        val settings = appSettings.value
        val activeAtt = attendanceRecords.value.firstOrNull { it.driverId == driver.id && it.isActive }
        val res = if (isCloudMode) {
            repository.endDriverShift(driver, activeAtt, settings.notifyShiftEvents)
        } else {
            localWorkspace.endDriverShift(driver, activeAtt, settings.notifyShiftEvents)
        }
        emitToast(if (res.isSuccess) "تم إنهاء الدوام وإيقاف GPS" else "تعذر إنهاء الدوام")
    }

    fun checkInArrival(visit: Visit, driverLat: Double?, driverLng: Double?) = viewModelScope.launch {
        val settings = appSettings.value
        val now = System.currentTimeMillis()
        if (!isCloudMode) {
            val res = localWorkspace.recordCheckInArrival(currentUserId, sessionActorName, visit, driverLat, driverLng, settings, now)
            emitToast(if (res.isSuccess) "تم تسجيل الوصول والتحقق الجغرافي" else "تعذر تسجيل الوصول")
            return@launch
        }
        if (!isOnline.value) {
            val (inside, dist) = GeoUtils.isInsideGeofence(driverLat, driverLng, visit.latitude, visit.longitude, settings.geofenceRadiusMeters.toInt())
            val (dSec, dMin) = GeoUtils.calculateDelay(visit.scheduledAtEpochMs, now, settings.gracePeriodMinutes)
            localDao.insertPendingAction(
                PendingSyncActionEntity(
                    actionType = "ARRIVAL",
                    visitId = visit.id,
                    driverId = visit.assignedDriverId,
                    driverUserId = currentUserId,
                    driverName = visit.assignedDriverName.orEmpty(),
                    invoiceNumber = "",
                    amountDue = 0.0,
                    amountCollected = 0.0,
                    paymentMethod = "UNPAID",
                    pickupNotes = "",
                    pickupPhotoUri = "",
                    nonExecutionReason = "",
                    latitude = driverLat,
                    longitude = driverLng,
                    insideGeofence = inside,
                    distanceMeters = dist.toLong(),
                    delaySeconds = dSec,
                    delayMinutes = dMin,
                    recordedAtEpochMs = now
                )
            )
            emitToast("تم تسجيل الوصول محليًا وسيتم الرفع عند الاتصال")
            return@launch
        }
        val res = repository.recordCheckInArrival(visit, driverLat, driverLng, settings, now)
        emitToast(if (res.isSuccess) "تم تسجيل الوصول والتحقق الجغرافي" else "تعذر تسجيل الوصول")
    }

    fun completeVisit(
        visit: Visit,
        driver: Driver,
        invoiceNumber: String,
        amountDue: Double,
        amountCollected: Double,
        paymentMethod: String,
        pickupNotes: String,
        pickupPhotoUri: String?,
        driverLat: Double?,
        driverLng: Double?
    ) = viewModelScope.launch {
        val settings = appSettings.value
        val now = System.currentTimeMillis()
        if (!isCloudMode) {
            val res = localWorkspace.completeVisitExecution(
                actorId = currentUserId,
                visit = visit,
                driverId = driver.id,
                driverName = driver.name,
                invoiceNumber = invoiceNumber,
                amountDue = amountDue,
                amountCollected = amountCollected,
                paymentMethod = paymentMethod,
                pickupNotes = pickupNotes,
                pickupPhotoUri = pickupPhotoUri,
                driverLat = driverLat,
                driverLng = driverLng,
                settings = settings,
                nowMs = now
            )
            emitToast(if (res.isSuccess) "تم تأكيد التنفيذ وحفظ السجل المالي" else (res.exceptionOrNull()?.localizedMessage ?: "فشل الحفظ"))
            return@launch
        }
        if (!isOnline.value) {
            localDao.insertPendingAction(
                PendingSyncActionEntity(
                    actionType = "COMPLETE_VISIT",
                    visitId = visit.id,
                    driverId = driver.id,
                    driverUserId = currentUserId,
                    driverName = driver.name,
                    invoiceNumber = invoiceNumber,
                    amountDue = amountDue,
                    amountCollected = amountCollected,
                    paymentMethod = paymentMethod,
                    pickupNotes = pickupNotes,
                    pickupPhotoUri = pickupPhotoUri.orEmpty(),
                    nonExecutionReason = "",
                    latitude = driverLat,
                    longitude = driverLng,
                    insideGeofence = false,
                    distanceMeters = 0L,
                    delaySeconds = 0L,
                    delayMinutes = 0L,
                    recordedAtEpochMs = now
                )
            )
            emitToast("تم حفظ التنفيذ والتحصيل محليًا للمزامنة")
            return@launch
        }
        val res = repository.completeVisitExecution(
            visit = visit,
            driverId = driver.id,
            driverName = driver.name,
            invoiceNumber = invoiceNumber,
            amountDue = amountDue,
            amountCollected = amountCollected,
            paymentMethod = paymentMethod,
            pickupNotes = pickupNotes,
            pickupPhotoUri = pickupPhotoUri,
            driverLat = driverLat,
            driverLng = driverLng,
            settings = settings,
            nowMs = now
        )
        emitToast(if (res.isSuccess) "تم تأكيد التنفيذ وحفظ السجل المالي" else (res.exceptionOrNull()?.localizedMessage ?: "فشل الحفظ"))
    }

    fun markVisitNotExecuted(visit: Visit, reason: String) = viewModelScope.launch {
        val now = System.currentTimeMillis()
        if (!isCloudMode) {
            val res = localWorkspace.markVisitNotExecuted(currentUserId, sessionActorName, visit, reason, now)
            emitToast(if (res.isSuccess) "تم تسجيل تعذر التنفيذ وإشعار الإدارة" else "فشل التحديث")
            return@launch
        }
        if (!isOnline.value) {
            localDao.insertPendingAction(
                PendingSyncActionEntity(
                    actionType = "NOT_EXECUTED",
                    visitId = visit.id,
                    driverId = visit.assignedDriverId,
                    driverUserId = currentUserId,
                    driverName = visit.assignedDriverName.orEmpty(),
                    invoiceNumber = "",
                    amountDue = 0.0,
                    amountCollected = 0.0,
                    paymentMethod = "UNPAID",
                    pickupNotes = "",
                    pickupPhotoUri = "",
                    nonExecutionReason = reason,
                    latitude = null,
                    longitude = null,
                    insideGeofence = false,
                    distanceMeters = 0L,
                    delaySeconds = 0L,
                    delayMinutes = 0L,
                    recordedAtEpochMs = now
                )
            )
            emitToast("تم حفظ سبب عدم التنفيذ محليًا للمزامنة")
            return@launch
        }
        val res = repository.markVisitNotExecuted(visit, reason, now)
        emitToast(if (res.isSuccess) "تم تسجيل تعذر التنفيذ وإشعار الإدارة" else "فشل التحديث")
    }

    fun sendNotification(title: String, body: String, targetUserId: String = "ALL") = viewModelScope.launch {
        val ntf = AppNotification(
            targetUserId = targetUserId,
            senderUserId = currentUserId,
            title = title,
            body = body,
            type = "ADMIN_BROADCAST"
        )
        val res = if (isCloudMode) repository.sendNotification(ntf) else localWorkspace.sendNotification(ntf)
        emitToast(if (res.isSuccess) "تم إرسال الإشعار بنجاح" else "فشل إرسال الإشعار")
    }

    fun toggleNotificationRead(notification: AppNotification) = viewModelScope.launch {
        if (isCloudMode) repository.markNotificationRead(notification.id, !notification.isRead)
        else localWorkspace.markNotificationRead(notification.id, !notification.isRead)
    }

    fun saveAppSettings(settings: AppSettings) = viewModelScope.launch {
        val res = if (isCloudMode) repository.updateAppSettings(settings)
        else localWorkspace.updateAppSettings(currentUserId, sessionActorName, settings)
        emitToast(if (res.isSuccess) "تم حفظ إعدادات التشغيل" else "فشل حفظ الإعدادات")
    }

    private fun emitToast(msg: String) {
        _toastEvents.tryEmit(msg)
    }

    private companion object {
        const val TAG = "BubblesVM"
        const val STOP_TIMEOUT_MS = 5000L
    }
}
