package com.example.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.BubblesLocalDatabase
import com.example.data.local.OfflineGpsPointEntity
import com.example.data.model.GpsLocationPoint
import com.example.data.repository.BubblesRepository
import com.example.data.validation.GeoUtils
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class LocationForegroundService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var fusedClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null

    private var driverId: String = ""
    private var userId: String = ""
    private var attendanceId: String = ""
    private var movingIntervalSec: Long = 30L
    private var lastLat: Double? = null
    private var lastLng: Double? = null

    override fun onCreate() {
        super.onCreate()
        fusedClient = LocationServices.getFusedLocationProviderClient(this)
        ensureChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopTracking()
            stopSelf()
            return START_NOT_STICKY
        }

        driverId = intent?.getStringExtra(EXTRA_DRIVER_ID).orEmpty()
        userId = intent?.getStringExtra(EXTRA_USER_ID).orEmpty()
        attendanceId = intent?.getStringExtra(EXTRA_ATTENDANCE_ID).orEmpty()
        movingIntervalSec = (intent?.getLongExtra(EXTRA_INTERVAL_SEC, 30L) ?: 30L).coerceIn(10L, 300L)

        if (driverId.isBlank() || userId.isBlank() || attendanceId.isBlank()) {
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        _isTracking.value = true
        startLocationUpdates()
        return START_STICKY
    }

    private fun startLocationUpdates() {
        val hasFine = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) {
            Log.w(TAG, "Location permission not granted; stopping LocationForegroundService")
            stopSelf()
            return
        }

        stopTracking()

        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            movingIntervalSec * 1000L
        )
            .setMinUpdateIntervalMillis((movingIntervalSec / 2).coerceAtLeast(10L) * 1000L)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                _latestLocation.value = LocationSnapshot(
                    latitude = loc.latitude,
                    longitude = loc.longitude,
                    accuracyMeters = loc.accuracy.toInt().coerceAtLeast(1),
                    speedMps = loc.speed.toDouble(),
                    recordedAtEpochMs = System.currentTimeMillis()
                )

                val distMoved = if (lastLat != null && lastLng != null) {
                    GeoUtils.distanceMeters(lastLat!!, lastLng!!, loc.latitude, loc.longitude)
                } else {
                    100
                }
                val isStopped = distMoved < 20 && loc.speed < 0.8f
                lastLat = loc.latitude
                lastLng = loc.longitude

                val pointId = "gps_${UUID.randomUUID().toString().take(12)}"
                val now = System.currentTimeMillis()
                val point = GpsLocationPoint(
                    id = pointId,
                    driverId = driverId,
                    userId = userId,
                    attendanceId = attendanceId,
                    latitude = loc.latitude,
                    longitude = loc.longitude,
                    accuracyMeters = loc.accuracy.toInt().coerceAtLeast(1).toLong(),
                    speedMps = loc.speed.toDouble(),
                    isStopped = isStopped,
                    recordedAtEpochMs = now
                )

                serviceScope.launch {
                    val hasCloudUser = Firebase.auth.currentUser != null
                    val localDb = BubblesLocalDatabase.getInstance(applicationContext)
                    if (hasCloudUser) {
                        val db = FirebaseFirestore.getInstance(getString(R.string.firestore_database_id))
                        val repo = BubblesRepository(db)
                        val res = repo.recordGpsPoint(point)
                        if (res.isFailure) {
                            localDb.dao().insertGpsPoint(
                                OfflineGpsPointEntity(
                                    pointId = pointId,
                                    driverId = driverId,
                                    userId = userId,
                                    attendanceId = attendanceId,
                                    latitude = loc.latitude,
                                    longitude = loc.longitude,
                                    accuracyMeters = point.accuracyMeters,
                                    speedMps = loc.speed.toDouble(),
                                    isStopped = isStopped,
                                    recordedAtEpochMs = now
                                )
                            )
                        }
                    } else {
                        com.example.data.local.LocalWorkspaceRepository(localDb.workspaceDao())
                            .recordGpsPoint(point)
                    }
                }
            }
        }
        locationCallback = callback
        try {
            fusedClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
        } catch (se: SecurityException) {
            Log.e(TAG, "SecurityException requesting location updates", se)
            stopSelf()
        }
    }

    private fun stopTracking() {
        locationCallback?.let { fusedClient.removeLocationUpdates(it) }
        locationCallback = null
        _isTracking.value = false
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Bubbles GPS Shift Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Active delivery shift location tracking"
            }
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification() = run {
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Bubbles Laundry • تتبع الدوام نشط")
            .setContentText("يتم تحديث الموقع كل ${movingIntervalSec}ث أثناء الدوام فقط")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setContentIntent(openIntent)
            .build()
    }

    override fun onDestroy() {
        stopTracking()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    data class LocationSnapshot(
        val latitude: Double,
        val longitude: Double,
        val accuracyMeters: Int,
        val speedMps: Double,
        val recordedAtEpochMs: Long
    )

    companion object {
        private const val TAG = "LocationFgService"
        private const val CHANNEL_ID = "bubbles_tracking"
        private const val NOTIFICATION_ID = 4011
        const val ACTION_START = "com.example.action.START_GPS"
        const val ACTION_STOP = "com.example.action.STOP_GPS"
        const val EXTRA_DRIVER_ID = "extra_driver_id"
        const val EXTRA_USER_ID = "extra_user_id"
        const val EXTRA_ATTENDANCE_ID = "extra_attendance_id"
        const val EXTRA_INTERVAL_SEC = "extra_interval_sec"

        private val _latestLocation = MutableStateFlow<LocationSnapshot?>(null)
        val latestLocation: StateFlow<LocationSnapshot?> = _latestLocation.asStateFlow()

        private val _isTracking = MutableStateFlow(false)
        val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()

        fun start(
            context: Context,
            driverId: String,
            userId: String,
            attendanceId: String,
            intervalSec: Long
        ) {
            val intent = Intent(context, LocationForegroundService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_DRIVER_ID, driverId)
                putExtra(EXTRA_USER_ID, userId)
                putExtra(EXTRA_ATTENDANCE_ID, attendanceId)
                putExtra(EXTRA_INTERVAL_SEC, intervalSec)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, LocationForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
