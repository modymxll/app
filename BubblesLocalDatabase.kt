package com.example.data.local

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.GpsLocationPoint
import com.example.data.repository.BubblesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Entity(tableName = "offline_gps_points")
data class OfflineGpsPointEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0L,
    val pointId: String,
    val driverId: String,
    val userId: String,
    val attendanceId: String,
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Long,
    val speedMps: Double,
    val isStopped: Boolean,
    val recordedAtEpochMs: Long
)

@Entity(tableName = "pending_sync_actions")
data class PendingSyncActionEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0L,
    val actionType: String, // ARRIVAL, START_VISIT, COMPLETE_VISIT, NOT_EXECUTED
    val visitId: String,
    val driverId: String,
    val driverUserId: String,
    val driverName: String,
    val invoiceNumber: String,
    val amountDue: Double,
    val amountCollected: Double,
    val paymentMethod: String,
    val pickupNotes: String,
    val pickupPhotoUri: String,
    val nonExecutionReason: String,
    val latitude: Double?,
    val longitude: Double?,
    val insideGeofence: Boolean,
    val distanceMeters: Long,
    val delaySeconds: Long,
    val delayMinutes: Long,
    val recordedAtEpochMs: Long
)

@Entity(tableName = "local_drivers")
data class LocalDriverEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val email: String?,
    val name: String,
    val username: String,
    val phone: String,
    val accessCode: String,
    val status: String,
    val gpsActive: Boolean,
    val lastLat: Double?,
    val lastLng: Double?,
    val lastAccuracyMeters: Long?,
    val lastGpsAtEpochMs: Long?,
    val isDeleted: Boolean,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
)

@Entity(tableName = "local_customers")
data class LocalCustomerEntity(
    @PrimaryKey val id: String,
    val name: String,
    val primaryPhone: String,
    val secondaryPhone: String?,
    val notes: String?,
    val isActive: Boolean,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
)

@Entity(tableName = "local_locations")
data class LocalLocationEntity(
    @PrimaryKey val id: String,
    val customerId: String,
    val customerName: String?,
    val branchName: String,
    val invoiceNumber: String?,
    val primaryPhone: String,
    val secondaryPhone: String?,
    val address: String?,
    val city: String?,
    val district: String?,
    val latitude: Double,
    val longitude: Double,
    val accessInstructions: String?,
    val notes: String?,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
)

@Entity(tableName = "local_visits")
data class LocalVisitEntity(
    @PrimaryKey val id: String,
    val customerId: String,
    val locationId: String,
    val customerName: String?,
    val locationName: String,
    val district: String?,
    val address: String?,
    val primaryPhone: String,
    val secondaryPhone: String?,
    val accessInstructions: String?,
    val latitude: Double?,
    val longitude: Double?,
    val assignedDriverId: String,
    val assignedDriverUserId: String,
    val assignedDriverName: String?,
    val operationType: String,
    val priority: String,
    val status: String,
    val scheduledAtEpochMs: Long,
    val arrivedAtEpochMs: Long?,
    val startedAtEpochMs: Long?,
    val completedAtEpochMs: Long?,
    val delaySeconds: Long?,
    val delayMinutes: Long?,
    val insideGeofence: Boolean?,
    val arrivalDistanceMeters: Long?,
    val invoiceNumber: String?,
    val posInvoiceRef: String?,
    val amountDue: Double?,
    val amountCollected: Double?,
    val paymentMethod: String,
    val paymentStatus: String,
    val pickupNotes: String?,
    val pickupPhotoUri: String?,
    val nonExecutionReason: String?,
    val recurringScheduleId: String?,
    val occurrenceKey: String?,
    val notes: String?,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
)

@Entity(tableName = "local_recurring_schedules")
data class LocalRecurringScheduleEntity(
    @PrimaryKey val id: String,
    val customerId: String,
    val locationId: String,
    val customerName: String?,
    val locationName: String?,
    val assignedDriverId: String,
    val assignedDriverUserId: String,
    val assignedDriverName: String?,
    val operationType: String,
    val weekdaysCsv: String,
    val timeHHmm: String,
    val startEpochMs: Long,
    val endEpochMs: Long?,
    val isContinuous: Boolean,
    val isActive: Boolean,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
)

@Entity(tableName = "local_attendance")
data class LocalAttendanceEntity(
    @PrimaryKey val id: String,
    val driverId: String,
    val userId: String,
    val driverName: String,
    val startedAtEpochMs: Long,
    val endedAtEpochMs: Long?,
    val durationMinutes: Long?,
    val isActive: Boolean,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
)

@Entity(tableName = "local_gps_points")
data class LocalGpsPointEntity(
    @PrimaryKey val id: String,
    val driverId: String,
    val userId: String,
    val attendanceId: String,
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Long,
    val speedMps: Double?,
    val isStopped: Boolean?,
    val recordedAtEpochMs: Long
)

@Entity(tableName = "local_payment_records")
data class LocalPaymentRecordEntity(
    @PrimaryKey val id: String,
    val visitId: String,
    val driverId: String,
    val driverUserId: String,
    val driverName: String?,
    val customerId: String?,
    val locationId: String?,
    val invoiceNumber: String,
    val amountDue: Double,
    val amountCollected: Double,
    val amountRemaining: Double,
    val paymentMethod: String,
    val paymentStatus: String,
    val deliveryStatus: String,
    val latitude: Double?,
    val longitude: Double?,
    val notes: String?,
    val recordedAtEpochMs: Long
)

@Entity(tableName = "local_notifications")
data class LocalNotificationEntity(
    @PrimaryKey val id: String,
    val targetUserId: String,
    val senderUserId: String,
    val title: String,
    val body: String,
    val type: String,
    val relatedVisitId: String?,
    val isRead: Boolean,
    val createdAtEpochMs: Long
)

@Entity(tableName = "local_audit_logs")
data class LocalAuditLogEntity(
    @PrimaryKey val id: String,
    val actorUserId: String,
    val actorName: String,
    val actionType: String,
    val entityType: String,
    val entityId: String,
    val oldValue: String,
    val newValue: String,
    val metadata: String?,
    val createdAtEpochMs: Long
)

@Entity(tableName = "local_settings")
data class LocalSettingsEntity(
    @PrimaryKey val id: String = "default",
    val gracePeriodMinutes: Long = 5L,
    val geofenceRadiusMeters: Long = 150L,
    val gpsMovingIntervalSec: Long = 30L,
    val gpsIdleIntervalSec: Long = 180L,
    val notifyShiftEvents: Boolean = true,
    val notifyArrival: Boolean = true,
    val notifyPickupDelivery: Boolean = true,
    val notifyPayments: Boolean = true,
    val notifyDelays: Boolean = true,
    val notifyGpsDisconnect: Boolean = true,
    val updatedAtEpochMs: Long = 0L
)

@Dao
interface BubblesLocalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGpsPoint(point: OfflineGpsPointEntity)

    @Query("SELECT * FROM offline_gps_points ORDER BY recordedAtEpochMs ASC LIMIT 100")
    suspend fun getPendingGpsPoints(): List<OfflineGpsPointEntity>

    @Query("DELETE FROM offline_gps_points WHERE localId = :localId")
    suspend fun deleteGpsPoint(localId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPendingAction(action: PendingSyncActionEntity)

    @Query("SELECT * FROM pending_sync_actions ORDER BY recordedAtEpochMs ASC")
    suspend fun getPendingActions(): List<PendingSyncActionEntity>

    @Query("DELETE FROM pending_sync_actions WHERE localId = :localId")
    suspend fun deletePendingAction(localId: Long)

    @Query("SELECT (SELECT COUNT(*) FROM offline_gps_points) + (SELECT COUNT(*) FROM pending_sync_actions)")
    fun observeTotalPendingCount(): Flow<Int>
}

@Dao
interface WorkspaceDao {
    // Drivers
    @Query("SELECT * FROM local_drivers WHERE isDeleted = 0 ORDER BY name ASC")
    fun observeActiveDrivers(): Flow<List<LocalDriverEntity>>

    @Query("SELECT * FROM local_drivers WHERE id = :driverId AND isDeleted = 0 LIMIT 1")
    fun observeDriverById(driverId: String): Flow<LocalDriverEntity?>

    @Query("SELECT * FROM local_drivers WHERE id = :driverId LIMIT 1")
    suspend fun getDriverById(driverId: String): LocalDriverEntity?

    @Query("SELECT * FROM local_drivers WHERE isDeleted = 0 AND accessCode = :code LIMIT 1")
    suspend fun findDriverByAccessCode(code: String): LocalDriverEntity?

    @Query("SELECT * FROM local_drivers WHERE isDeleted = 0 AND LOWER(username) = LOWER(:username) AND accessCode = :code LIMIT 1")
    suspend fun findDriverByCredentials(username: String, code: String): LocalDriverEntity?

    @Query("SELECT accessCode FROM local_drivers WHERE isDeleted = 0")
    suspend fun getAllDriverCodes(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDriver(driver: LocalDriverEntity)

    // Customers
    @Query("SELECT * FROM local_customers ORDER BY name ASC")
    fun observeCustomers(): Flow<List<LocalCustomerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCustomer(customer: LocalCustomerEntity)

    // Locations
    @Query("SELECT * FROM local_locations ORDER BY branchName ASC")
    fun observeLocations(): Flow<List<LocalLocationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLocation(location: LocalLocationEntity)

    // Visits
    @Query("SELECT * FROM local_visits ORDER BY scheduledAtEpochMs ASC")
    fun observeAllVisits(): Flow<List<LocalVisitEntity>>

    @Query("SELECT * FROM local_visits WHERE assignedDriverId = :driverId ORDER BY scheduledAtEpochMs ASC")
    fun observeVisitsForDriver(driverId: String): Flow<List<LocalVisitEntity>>

    @Query("SELECT * FROM local_visits WHERE recurringScheduleId = :scheduleId")
    suspend fun getVisitsByRecurringId(scheduleId: String): List<LocalVisitEntity>

    @Query("SELECT * FROM local_visits WHERE id = :visitId LIMIT 1")
    suspend fun getVisitById(visitId: String): LocalVisitEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertVisit(visit: LocalVisitEntity)

    // Recurring Schedules
    @Query("SELECT * FROM local_recurring_schedules ORDER BY createdAtEpochMs DESC")
    fun observeRecurringSchedules(): Flow<List<LocalRecurringScheduleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRecurringSchedule(schedule: LocalRecurringScheduleEntity)

    // Attendance
    @Query("SELECT * FROM local_attendance ORDER BY startedAtEpochMs DESC")
    fun observeAllAttendance(): Flow<List<LocalAttendanceEntity>>

    @Query("SELECT * FROM local_attendance WHERE driverId = :driverId ORDER BY startedAtEpochMs DESC")
    fun observeDriverAttendance(driverId: String): Flow<List<LocalAttendanceEntity>>

    @Query("SELECT * FROM local_attendance WHERE id = :attId LIMIT 1")
    suspend fun getAttendanceById(attId: String): LocalAttendanceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAttendance(attendance: LocalAttendanceEntity)

    // GPS Points
    @Query("SELECT * FROM local_gps_points WHERE recordedAtEpochMs >= :sinceEpochMs ORDER BY recordedAtEpochMs ASC")
    fun observeGpsPointsSince(sinceEpochMs: Long): Flow<List<LocalGpsPointEntity>>

    @Query("SELECT * FROM local_gps_points WHERE driverId = :driverId AND recordedAtEpochMs >= :sinceEpochMs ORDER BY recordedAtEpochMs ASC")
    fun observeDriverGpsPointsSince(driverId: String, sinceEpochMs: Long): Flow<List<LocalGpsPointEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGpsPoint(point: LocalGpsPointEntity)

    // Payments
    @Query("SELECT * FROM local_payment_records ORDER BY recordedAtEpochMs DESC")
    fun observeAllPayments(): Flow<List<LocalPaymentRecordEntity>>

    @Query("SELECT * FROM local_payment_records WHERE driverId = :driverId ORDER BY recordedAtEpochMs DESC")
    fun observeDriverPayments(driverId: String): Flow<List<LocalPaymentRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: LocalPaymentRecordEntity)

    // Notifications
    @Query("SELECT * FROM local_notifications ORDER BY createdAtEpochMs DESC")
    fun observeAllNotifications(): Flow<List<LocalNotificationEntity>>

    @Query("SELECT * FROM local_notifications WHERE targetUserId IN (:driverId, 'ALL') ORDER BY createdAtEpochMs DESC")
    fun observeDriverNotifications(driverId: String): Flow<List<LocalNotificationEntity>>

    @Query("UPDATE local_notifications SET isRead = :isRead WHERE id = :id")
    suspend fun markNotificationRead(id: String, isRead: Boolean)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: LocalNotificationEntity)

    // Audit Logs
    @Query("SELECT * FROM local_audit_logs ORDER BY createdAtEpochMs DESC LIMIT 250")
    fun observeAuditLogs(): Flow<List<LocalAuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: LocalAuditLogEntity)

    // Settings
    @Query("SELECT * FROM local_settings WHERE id = 'default' LIMIT 1")
    fun observeSettings(): Flow<LocalSettingsEntity?>

    @Query("SELECT * FROM local_settings WHERE id = 'default' LIMIT 1")
    suspend fun getSettingsOnce(): LocalSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSettings(settings: LocalSettingsEntity)
}

@Database(
    entities = [
        OfflineGpsPointEntity::class,
        PendingSyncActionEntity::class,
        LocalDriverEntity::class,
        LocalCustomerEntity::class,
        LocalLocationEntity::class,
        LocalVisitEntity::class,
        LocalRecurringScheduleEntity::class,
        LocalAttendanceEntity::class,
        LocalGpsPointEntity::class,
        LocalPaymentRecordEntity::class,
        LocalNotificationEntity::class,
        LocalAuditLogEntity::class,
        LocalSettingsEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class BubblesLocalDatabase : RoomDatabase() {
    abstract fun dao(): BubblesLocalDao
    abstract fun workspaceDao(): WorkspaceDao

    companion object {
        @Volatile
        private var INSTANCE: BubblesLocalDatabase? = null

        fun getInstance(context: Context): BubblesLocalDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    BubblesLocalDatabase::class.java,
                    "bubbles_offline_queue.db"
                ).fallbackToDestructiveMigration(dropAllTables = true).build().also { INSTANCE = it }
            }
        }
    }
}

class OfflineSyncManager(
    context: Context,
    private val dao: BubblesLocalDao,
    private val repository: BubblesRepository
) {
    private val appContext = context.applicationContext
    private val connectivityManager =
        appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _isOnline = MutableStateFlow(checkCurrentlyOnline())
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    val pendingSyncCount: Flow<Int> = dao.observeTotalPendingCount()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _isOnline.value = true
            flushPendingQueue()
        }

        override fun onLost(network: Network) {
            _isOnline.value = checkCurrentlyOnline()
        }
    }

    init {
        runCatching {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager.registerNetworkCallback(request, networkCallback)
        }
    }

    private fun checkCurrentlyOnline(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val caps = connectivityManager.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun flushPendingQueue() {
        if (!_isOnline.value) return
        scope.launch {
            runCatching {
                val gpsPoints = dao.getPendingGpsPoints()
                for (pt in gpsPoints) {
                    val model = GpsLocationPoint(
                        id = pt.pointId,
                        driverId = pt.driverId,
                        userId = pt.userId,
                        attendanceId = pt.attendanceId,
                        latitude = pt.latitude,
                        longitude = pt.longitude,
                        accuracyMeters = pt.accuracyMeters,
                        speedMps = pt.speedMps,
                        isStopped = pt.isStopped,
                        recordedAtEpochMs = pt.recordedAtEpochMs
                    )
                    if (repository.recordGpsPoint(model).isSuccess) {
                        dao.deleteGpsPoint(pt.localId)
                    } else {
                        break
                    }
                }
                val actions = dao.getPendingActions()
                for (act in actions) {
                    val res = repository.replayOfflineAction(act)
                    if (res.isSuccess) {
                        dao.deletePendingAction(act.localId)
                    } else {
                        break
                    }
                }
            }
        }
    }

    fun unregister() {
        runCatching { connectivityManager.unregisterNetworkCallback(networkCallback) }
    }
}
