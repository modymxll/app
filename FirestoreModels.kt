package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Suppress("UNCHECKED_CAST")
private fun Map<String, Any?>.withoutNulls(): Map<String, Any> =
    filterValues { it != null } as Map<String, Any>

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String? = null,
    val phone: String? = null,
    val role: String = "DRIVER",
    val driverId: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "displayName" to displayName.ifBlank { "Bubbles User" },
        "email" to email,
        "phone" to phone,
        "role" to role,
        "driverId" to driverId,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    ).withoutNulls()
}

@IgnoreExtraProperties
data class AdminMarker(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "email" to email,
        "createdAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class Driver(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String? = null,
    val name: String = "",
    val username: String = "",
    val phone: String = "",
    val status: String = "OFF_SHIFT",
    val gpsActive: Boolean = false,
    val lastLat: Double? = null,
    val lastLng: Double? = null,
    val lastAccuracyMeters: Long? = null,
    val lastGpsAtEpochMs: Long? = null,
    val isDeleted: Boolean = false,
    val accessCode: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "email" to email?.takeIf { it.isNotBlank() },
        "name" to name,
        "username" to username,
        "phone" to phone,
        "status" to status,
        "gpsActive" to gpsActive,
        "lastLat" to lastLat,
        "lastLng" to lastLng,
        "lastAccuracyMeters" to lastAccuracyMeters,
        "lastGpsAtEpochMs" to lastGpsAtEpochMs,
        "isDeleted" to isDeleted,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    ).withoutNulls()

    fun toUpdateMap(preserveCreatedAt: Timestamp): Map<String, Any> = mapOf(
        "userId" to userId,
        "email" to email?.takeIf { it.isNotBlank() },
        "name" to name,
        "username" to username,
        "phone" to phone,
        "status" to status,
        "gpsActive" to gpsActive,
        "lastLat" to lastLat,
        "lastLng" to lastLng,
        "lastAccuracyMeters" to lastAccuracyMeters,
        "lastGpsAtEpochMs" to lastGpsAtEpochMs,
        "isDeleted" to isDeleted,
        "createdAt" to preserveCreatedAt,
        "updatedAt" to FieldValue.serverTimestamp()
    ).withoutNulls()
}

@IgnoreExtraProperties
data class Customer(
    @DocumentId val id: String = "",
    val name: String = "",
    val primaryPhone: String = "",
    val secondaryPhone: String? = null,
    val notes: String? = null,
    val isActive: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "name" to name,
        "primaryPhone" to primaryPhone,
        "secondaryPhone" to secondaryPhone?.takeIf { it.isNotBlank() },
        "notes" to notes?.takeIf { it.isNotBlank() },
        "isActive" to isActive,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    ).withoutNulls()

    fun toUpdateMap(preserveCreatedAt: Timestamp): Map<String, Any> = mapOf(
        "name" to name,
        "primaryPhone" to primaryPhone,
        "secondaryPhone" to secondaryPhone?.takeIf { it.isNotBlank() },
        "notes" to notes?.takeIf { it.isNotBlank() },
        "isActive" to isActive,
        "createdAt" to preserveCreatedAt,
        "updatedAt" to FieldValue.serverTimestamp()
    ).withoutNulls()
}

@IgnoreExtraProperties
data class CustomerLocation(
    @DocumentId val id: String = "",
    val customerId: String = "",
    val customerName: String? = null,
    val branchName: String = "",
    val invoiceNumber: String? = null,
    val primaryPhone: String = "",
    val secondaryPhone: String? = null,
    val address: String? = null,
    val city: String? = null,
    val district: String? = null,
    val latitude: Double = 24.7136,
    val longitude: Double = 46.6753,
    val accessInstructions: String? = null,
    val notes: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "customerId" to customerId,
        "customerName" to customerName?.takeIf { it.isNotBlank() },
        "branchName" to branchName,
        "invoiceNumber" to invoiceNumber?.takeIf { it.isNotBlank() },
        "primaryPhone" to primaryPhone,
        "secondaryPhone" to secondaryPhone?.takeIf { it.isNotBlank() },
        "address" to address?.takeIf { it.isNotBlank() },
        "city" to city?.takeIf { it.isNotBlank() },
        "district" to district?.takeIf { it.isNotBlank() },
        "latitude" to latitude,
        "longitude" to longitude,
        "accessInstructions" to accessInstructions?.takeIf { it.isNotBlank() },
        "notes" to notes?.takeIf { it.isNotBlank() },
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    ).withoutNulls()

    fun toUpdateMap(preserveCreatedAt: Timestamp): Map<String, Any> = mapOf(
        "customerId" to customerId,
        "customerName" to customerName?.takeIf { it.isNotBlank() },
        "branchName" to branchName,
        "invoiceNumber" to invoiceNumber?.takeIf { it.isNotBlank() },
        "primaryPhone" to primaryPhone,
        "secondaryPhone" to secondaryPhone?.takeIf { it.isNotBlank() },
        "address" to address?.takeIf { it.isNotBlank() },
        "city" to city?.takeIf { it.isNotBlank() },
        "district" to district?.takeIf { it.isNotBlank() },
        "latitude" to latitude,
        "longitude" to longitude,
        "accessInstructions" to accessInstructions?.takeIf { it.isNotBlank() },
        "notes" to notes?.takeIf { it.isNotBlank() },
        "createdAt" to preserveCreatedAt,
        "updatedAt" to FieldValue.serverTimestamp()
    ).withoutNulls()
}

@IgnoreExtraProperties
data class Visit(
    @DocumentId val id: String = "",
    val customerId: String = "",
    val locationId: String = "",
    val customerName: String? = null,
    val locationName: String = "",
    val district: String? = null,
    val address: String? = null,
    val primaryPhone: String = "",
    val secondaryPhone: String? = null,
    val accessInstructions: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val assignedDriverId: String = "",
    val assignedDriverUserId: String = "",
    val assignedDriverName: String? = null,
    val operationType: String = "BOTH",
    val priority: String = "NORMAL",
    val status: String = "NOT_STARTED",
    val scheduledAtEpochMs: Long = 0L,
    val arrivedAtEpochMs: Long? = null,
    val startedAtEpochMs: Long? = null,
    val completedAtEpochMs: Long? = null,
    val delaySeconds: Long? = null,
    val delayMinutes: Long? = null,
    val insideGeofence: Boolean? = null,
    val arrivalDistanceMeters: Long? = null,
    val invoiceNumber: String? = null,
    val posInvoiceRef: String? = null,
    val amountDue: Double? = null,
    val amountCollected: Double? = null,
    val paymentMethod: String = "UNPAID",
    val paymentStatus: String = "UNPAID",
    val pickupNotes: String? = null,
    val pickupPhotoUri: String? = null,
    val nonExecutionReason: String? = null,
    val recurringScheduleId: String? = null,
    val occurrenceKey: String? = null,
    val notes: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "customerId" to customerId,
        "locationId" to locationId,
        "customerName" to customerName?.takeIf { it.isNotBlank() },
        "locationName" to locationName,
        "district" to district?.takeIf { it.isNotBlank() },
        "address" to address?.takeIf { it.isNotBlank() },
        "primaryPhone" to primaryPhone,
        "secondaryPhone" to secondaryPhone?.takeIf { it.isNotBlank() },
        "accessInstructions" to accessInstructions?.takeIf { it.isNotBlank() },
        "latitude" to latitude,
        "longitude" to longitude,
        "assignedDriverId" to assignedDriverId,
        "assignedDriverUserId" to assignedDriverUserId,
        "assignedDriverName" to assignedDriverName?.takeIf { it.isNotBlank() },
        "operationType" to operationType,
        "priority" to priority,
        "status" to status,
        "scheduledAtEpochMs" to scheduledAtEpochMs,
        "arrivedAtEpochMs" to arrivedAtEpochMs,
        "startedAtEpochMs" to startedAtEpochMs,
        "completedAtEpochMs" to completedAtEpochMs,
        "delaySeconds" to delaySeconds,
        "delayMinutes" to delayMinutes,
        "insideGeofence" to insideGeofence,
        "arrivalDistanceMeters" to arrivalDistanceMeters,
        "invoiceNumber" to invoiceNumber?.takeIf { it.isNotBlank() },
        "posInvoiceRef" to posInvoiceRef?.takeIf { it.isNotBlank() },
        "amountDue" to amountDue,
        "amountCollected" to amountCollected,
        "paymentMethod" to paymentMethod,
        "paymentStatus" to paymentStatus,
        "pickupNotes" to pickupNotes?.takeIf { it.isNotBlank() },
        "pickupPhotoUri" to pickupPhotoUri?.takeIf { it.isNotBlank() },
        "nonExecutionReason" to nonExecutionReason?.takeIf { it.isNotBlank() },
        "recurringScheduleId" to recurringScheduleId?.takeIf { it.isNotBlank() },
        "occurrenceKey" to occurrenceKey?.takeIf { it.isNotBlank() },
        "notes" to notes?.takeIf { it.isNotBlank() },
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    ).withoutNulls()
}

@IgnoreExtraProperties
data class RecurringSchedule(
    @DocumentId val id: String = "",
    val customerId: String = "",
    val locationId: String = "",
    val customerName: String? = null,
    val locationName: String? = null,
    val assignedDriverId: String = "",
    val assignedDriverUserId: String = "",
    val assignedDriverName: String? = null,
    val operationType: String = "BOTH",
    val weekdaysCsv: String = "SUN,TUE,THU",
    val timeHHmm: String = "10:30",
    val startEpochMs: Long = 0L,
    val endEpochMs: Long? = null,
    val isContinuous: Boolean = true,
    val isActive: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "customerId" to customerId,
        "locationId" to locationId,
        "customerName" to customerName?.takeIf { it.isNotBlank() },
        "locationName" to locationName?.takeIf { it.isNotBlank() },
        "assignedDriverId" to assignedDriverId,
        "assignedDriverUserId" to assignedDriverUserId,
        "assignedDriverName" to assignedDriverName?.takeIf { it.isNotBlank() },
        "operationType" to operationType,
        "weekdaysCsv" to weekdaysCsv,
        "timeHHmm" to timeHHmm,
        "startEpochMs" to startEpochMs,
        "endEpochMs" to endEpochMs,
        "isContinuous" to isContinuous,
        "isActive" to isActive,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    ).withoutNulls()
}

@IgnoreExtraProperties
data class DriverAttendance(
    @DocumentId val id: String = "",
    val driverId: String = "",
    val userId: String = "",
    val driverName: String = "",
    val startedAtEpochMs: Long = 0L,
    val endedAtEpochMs: Long? = null,
    val durationMinutes: Long? = null,
    val isActive: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "driverId" to driverId,
        "userId" to userId,
        "driverName" to driverName,
        "startedAtEpochMs" to startedAtEpochMs,
        "endedAtEpochMs" to endedAtEpochMs,
        "durationMinutes" to durationMinutes,
        "isActive" to isActive,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    ).withoutNulls()
}

@IgnoreExtraProperties
data class GpsLocationPoint(
    @DocumentId val id: String = "",
    val driverId: String = "",
    val userId: String = "",
    val attendanceId: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val accuracyMeters: Long = 0L,
    val speedMps: Double? = null,
    val isStopped: Boolean? = null,
    val recordedAtEpochMs: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "driverId" to driverId,
        "userId" to userId,
        "attendanceId" to attendanceId,
        "latitude" to latitude,
        "longitude" to longitude,
        "accuracyMeters" to accuracyMeters,
        "speedMps" to speedMps,
        "isStopped" to isStopped,
        "recordedAtEpochMs" to recordedAtEpochMs,
        "createdAt" to FieldValue.serverTimestamp()
    ).withoutNulls()
}

@IgnoreExtraProperties
data class PaymentRecord(
    @DocumentId val id: String = "",
    val visitId: String = "",
    val driverId: String = "",
    val driverUserId: String = "",
    val driverName: String? = null,
    val customerId: String? = null,
    val locationId: String? = null,
    val invoiceNumber: String = "",
    val amountDue: Double = 0.0,
    val amountCollected: Double = 0.0,
    val amountRemaining: Double = 0.0,
    val paymentMethod: String = "UNPAID",
    val paymentStatus: String = "UNPAID",
    val deliveryStatus: String = "COMPLETED",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val notes: String? = null,
    val recordedAtEpochMs: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "visitId" to visitId,
        "driverId" to driverId,
        "driverUserId" to driverUserId,
        "driverName" to driverName?.takeIf { it.isNotBlank() },
        "customerId" to customerId?.takeIf { it.isNotBlank() },
        "locationId" to locationId?.takeIf { it.isNotBlank() },
        "invoiceNumber" to invoiceNumber,
        "amountDue" to amountDue.coerceAtLeast(0.0),
        "amountCollected" to amountCollected.coerceAtLeast(0.0),
        "amountRemaining" to amountRemaining.coerceAtLeast(0.0),
        "paymentMethod" to paymentMethod,
        "paymentStatus" to paymentStatus,
        "deliveryStatus" to deliveryStatus,
        "latitude" to latitude,
        "longitude" to longitude,
        "notes" to notes?.takeIf { it.isNotBlank() },
        "recordedAtEpochMs" to recordedAtEpochMs,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    ).withoutNulls()
}

@IgnoreExtraProperties
data class AppNotification(
    @DocumentId val id: String = "",
    val targetUserId: String = "",
    val senderUserId: String = "",
    val title: String = "",
    val body: String = "",
    val type: String = "GENERAL",
    val relatedVisitId: String? = null,
    val isRead: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "targetUserId" to targetUserId,
        "senderUserId" to senderUserId,
        "title" to title.take(120),
        "body" to body.take(500),
        "type" to type,
        "relatedVisitId" to relatedVisitId?.takeIf { it.isNotBlank() },
        "isRead" to isRead,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    ).withoutNulls()
}

@IgnoreExtraProperties
data class AuditLog(
    @DocumentId val id: String = "",
    val actorUserId: String = "",
    val actorName: String = "",
    val actionType: String = "",
    val entityType: String = "",
    val entityId: String = "",
    val oldValue: String = "",
    val newValue: String = "",
    val metadata: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "actorUserId" to actorUserId,
        "actorName" to actorName.take(100),
        "actionType" to actionType.take(60),
        "entityType" to entityType.take(60),
        "entityId" to entityId.take(128),
        "oldValue" to oldValue.take(400),
        "newValue" to newValue.take(400),
        "metadata" to metadata?.takeIf { it.isNotBlank() }?.take(500),
        "createdAt" to FieldValue.serverTimestamp()
    ).withoutNulls()
}

@IgnoreExtraProperties
data class AppSettings(
    @DocumentId val id: String = "default",
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
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toSaveMap(): Map<String, Any> = mapOf(
        "gracePeriodMinutes" to gracePeriodMinutes.coerceIn(0L, 120L),
        "geofenceRadiusMeters" to geofenceRadiusMeters.coerceIn(20L, 2000L),
        "gpsMovingIntervalSec" to gpsMovingIntervalSec.coerceIn(10L, 300L),
        "gpsIdleIntervalSec" to gpsIdleIntervalSec.coerceIn(30L, 900L),
        "notifyShiftEvents" to notifyShiftEvents,
        "notifyArrival" to notifyArrival,
        "notifyPickupDelivery" to notifyPickupDelivery,
        "notifyPayments" to notifyPayments,
        "notifyDelays" to notifyDelays,
        "notifyGpsDisconnect" to notifyGpsDisconnect,
        "updatedAt" to FieldValue.serverTimestamp()
    )
}
