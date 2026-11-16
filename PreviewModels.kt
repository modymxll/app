package com.example.ui.model

import androidx.compose.ui.graphics.Color
import com.example.data.model.AppNotification
import com.example.data.model.AuditLog
import com.example.data.model.Customer
import com.example.data.model.CustomerLocation
import com.example.data.model.Driver
import com.example.data.model.GpsLocationPoint
import com.example.data.model.Visit
import com.example.data.validation.DateTimeFormatting
import com.example.data.validation.GeoUtils
import com.example.data.validation.WeekdayToken
import com.example.ui.theme.BubblesCyan
import com.example.ui.theme.BubblesGray
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusEmerald
import com.example.ui.theme.StatusIndigo
import com.example.ui.theme.StatusRed

enum class PreviewRole { ADMIN, DRIVER }

enum class DriverStatus(val ar: String, val en: String, val color: Color) {
    ON_SHIFT("في الدوام", "On Shift", StatusEmerald),
    ACTIVE("نشط", "Active", BubblesCyan),
    INACTIVE("غير نشط", "Inactive", BubblesGray),
    ON_LEAVE("في إجازة", "On Leave", StatusIndigo),
    SUSPENDED("موقوف", "Suspended", StatusRed),
    OFF_SHIFT("خارج الدوام", "Off Shift", Color(0xFF64748B));

    companion object {
        fun fromCode(code: String): DriverStatus = entries.firstOrNull { it.name == code } ?: OFF_SHIFT
    }
}

enum class OperationType(val ar: String, val en: String) {
    PICKUP("استلام", "Pickup"),
    DELIVERY("تسليم", "Delivery"),
    BOTH("استلام وتسليم", "Pickup & Delivery");

    companion object {
        fun fromCode(code: String): OperationType = entries.firstOrNull { it.name == code } ?: BOTH
    }
}

enum class VisitStatus(val ar: String, val en: String, val color: Color) {
    NOT_STARTED("لم تبدأ", "Not Started", BubblesGray),
    EN_ROUTE("في الطريق", "En Route", BubblesCyan),
    ARRIVED("وصل المندوب", "Arrived", StatusIndigo),
    IN_PROGRESS("بدأ التنفيذ", "In Progress", Color(0xFF0284C7)),
    COMPLETED("مكتملة", "Completed", StatusEmerald),
    LATE("متأخرة", "Late", StatusAmber),
    CANCELLED("ملغاة", "Cancelled", Color(0xFF94A3B8)),
    NOT_EXECUTED("لم تنفذ", "Not Executed", StatusRed);

    companion object {
        fun fromCode(code: String): VisitStatus = entries.firstOrNull { it.name == code } ?: NOT_STARTED
    }
}

enum class PaymentMethod(val ar: String, val en: String) {
    CASH("نقدًا", "Cash"),
    MADA("مدى", "Mada"),
    VISA("فيزا", "Visa"),
    MASTERCARD("ماستركارد", "Mastercard"),
    BANK_TRANSFER("تحويل بنكي", "Bank Transfer"),
    GCC_NET("شبكة خليجية", "GCC Net"),
    UNPAID("لم يتم الدفع", "Unpaid");

    companion object {
        fun fromCode(code: String): PaymentMethod = entries.firstOrNull { it.name == code } ?: UNPAID
    }
}

enum class PaymentStatus(val ar: String, val en: String, val color: Color) {
    PAID("مدفوع بالكامل", "Paid Full", StatusEmerald),
    PARTIAL("مدفوع جزئيًا", "Partial", StatusAmber),
    UNPAID("غير مدفوع", "Unpaid", StatusRed);

    companion object {
        fun fromCode(code: String): PaymentStatus = entries.firstOrNull { it.name == code } ?: UNPAID
    }
}

data class DriverProfile(
    val id: String,
    val userId: String,
    val email: String?,
    val nameAr: String,
    val nameEn: String,
    val username: String,
    val phone: String,
    val status: DriverStatus,
    val gpsActive: Boolean,
    val lastGpsTextAr: String,
    val lastGpsTextEn: String,
    val accuracyMeters: Int,
    val lastLat: Double?,
    val lastLng: Double?,
    val mapX: Float,
    val mapY: Float,
    val completedToday: Int,
    val lateToday: Int,
    val collectedToday: Double,
    val shiftHours: String,
    val accessCode: String = ""
)

data class BranchLocation(
    val id: String,
    val customerId: String,
    val nameAr: String,
    val nameEn: String,
    val cityAr: String,
    val districtAr: String,
    val addressAr: String,
    val primaryPhone: String,
    val secondaryPhone: String?,
    val lat: Double,
    val lng: Double,
    val instructionsAr: String,
    val mapX: Float,
    val mapY: Float
)

data class CustomerRecord(
    val id: String,
    val nameAr: String,
    val nameEn: String,
    val primaryPhone: String,
    val secondaryPhone: String?,
    val notesAr: String,
    val active: Boolean,
    val totalVisits: Int
)

data class VisitItem(
    val id: String,
    val customerId: String,
    val locationId: String,
    val customerNameAr: String,
    val customerNameEn: String,
    val locationNameAr: String,
    val locationNameEn: String,
    val districtAr: String,
    val phone: String,
    val secondaryPhone: String?,
    val driverId: String,
    val driverUserId: String,
    val driverNameAr: String,
    val scheduledAtEpochMs: Long,
    val scheduledTime: String,
    val operationType: OperationType,
    val priority: String,
    val status: VisitStatus,
    val isUrgent: Boolean = false,
    val recurringPatternAr: String? = null,
    val delayMinutes: Int = 0,
    val invoiceNumber: String = "",
    val amountDue: Double = 0.0,
    val amountCollected: Double = 0.0,
    val paymentMethod: PaymentMethod = PaymentMethod.UNPAID,
    val paymentStatus: PaymentStatus = PaymentStatus.UNPAID,
    val instructionsAr: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val mapX: Float = 0.5f,
    val mapY: Float = 0.5f,
    val insideGeofence: Boolean = false,
    val arrivalDistanceMeters: Int = -1,
    val pickupNotes: String = "",
    val pickupPhotoUri: String? = null,
    val nonExecutionReason: String = ""
)

data class NotificationItem(
    val id: String,
    val titleAr: String,
    val titleEn: String,
    val bodyAr: String,
    val bodyEn: String,
    val timeLabel: String,
    val isRead: Boolean,
    val isUrgent: Boolean = false
)

data class AuditEntry(
    val id: String,
    val actorName: String,
    val actionAr: String,
    val oldValue: String,
    val newValue: String,
    val timestamp: String
)

data class GpsStopPoint(
    val label: String,
    val x: Float,
    val y: Float,
    val stopMinutes: Int,
    val timeLabel: String
)

object UiMappers {
    fun Driver.toUi(visits: List<Visit>): DriverProfile {
        val now = System.currentTimeMillis()
        val myTodayVisits = visits.filter {
            it.assignedDriverId == id && DateTimeFormatting.isSameLocalDay(it.scheduledAtEpochMs, now)
        }
        val completed = myTodayVisits.count { it.status == "COMPLETED" }
        val late = myTodayVisits.count { it.status == "LATE" || (it.delayMinutes ?: 0L) > 5L }
        val collected = myTodayVisits.sumOf { it.amountCollected ?: 0.0 }
        val (x, y) = GeoUtils.projectToCanvas(lastLat, lastLng)
        val lastGpsStr = lastGpsAtEpochMs?.let { ts ->
            val sec = ((now - ts) / 1000L).coerceAtLeast(0L)
            when {
                sec < 60L -> "منذ ${sec} ث" to "${sec}s ago"
                sec < 3600L -> "منذ ${sec / 60L} د" to "${sec / 60L}m ago"
                else -> DateTimeFormatting.formatTime(ts) to DateTimeFormatting.formatTime(ts)
            }
        } ?: ("لم يبدأ بعد" to "No GPS yet")

        return DriverProfile(
            id = id,
            userId = userId,
            email = email,
            nameAr = name,
            nameEn = name,
            username = username,
            phone = phone,
            status = DriverStatus.fromCode(status),
            gpsActive = gpsActive,
            lastGpsTextAr = lastGpsStr.first,
            lastGpsTextEn = lastGpsStr.second,
            accuracyMeters = (lastAccuracyMeters ?: 0L).toInt(),
            lastLat = lastLat,
            lastLng = lastLng,
            mapX = x,
            mapY = y,
            completedToday = completed,
            lateToday = late,
            collectedToday = collected,
            shiftHours = if (status == "ON_SHIFT") "LIVE" else "00:00",
            accessCode = accessCode
        )
    }

    fun Customer.toUi(visits: List<Visit>): CustomerRecord {
        val count = visits.count { it.customerId == id }
        val display = name.ifBlank { primaryPhone }
        return CustomerRecord(
            id = id,
            nameAr = display,
            nameEn = display,
            primaryPhone = primaryPhone,
            secondaryPhone = secondaryPhone,
            notesAr = notes.orEmpty(),
            active = isActive,
            totalVisits = count
        )
    }

    fun CustomerLocation.toUi(): BranchLocation {
        val (x, y) = GeoUtils.projectToCanvas(latitude, longitude)
        return BranchLocation(
            id = id,
            customerId = customerId,
            nameAr = branchName,
            nameEn = branchName,
            cityAr = city.orEmpty().ifBlank { "الرياض" },
            districtAr = district.orEmpty(),
            addressAr = address.orEmpty(),
            primaryPhone = primaryPhone,
            secondaryPhone = secondaryPhone,
            lat = latitude,
            lng = longitude,
            instructionsAr = accessInstructions.orEmpty().ifBlank { notes.orEmpty() },
            mapX = x,
            mapY = y
        )
    }

    fun Visit.toUi(recurringSummaryMap: Map<String, String> = emptyMap()): VisitItem {
        val (x, y) = GeoUtils.projectToCanvas(latitude, longitude)
        val custDisplay = customerName?.takeIf { it.isNotBlank() } ?: primaryPhone
        val recLabel = recurringScheduleId?.let { recurringSummaryMap[it] }
        return VisitItem(
            id = id,
            customerId = customerId,
            locationId = locationId,
            customerNameAr = custDisplay,
            customerNameEn = custDisplay,
            locationNameAr = locationName,
            locationNameEn = locationName,
            districtAr = district.orEmpty().ifBlank { address.orEmpty() },
            phone = primaryPhone,
            secondaryPhone = secondaryPhone,
            driverId = assignedDriverId,
            driverUserId = assignedDriverUserId,
            driverNameAr = assignedDriverName.orEmpty().ifBlank { assignedDriverId },
            scheduledAtEpochMs = scheduledAtEpochMs,
            scheduledTime = DateTimeFormatting.formatTime(scheduledAtEpochMs),
            operationType = OperationType.fromCode(operationType),
            priority = priority,
            status = VisitStatus.fromCode(status),
            isUrgent = priority == "URGENT",
            recurringPatternAr = recLabel,
            delayMinutes = (delayMinutes ?: 0L).toInt(),
            invoiceNumber = invoiceNumber.orEmpty(),
            amountDue = amountDue ?: 0.0,
            amountCollected = amountCollected ?: 0.0,
            paymentMethod = PaymentMethod.fromCode(paymentMethod),
            paymentStatus = PaymentStatus.fromCode(paymentStatus),
            instructionsAr = accessInstructions.orEmpty().ifBlank { notes.orEmpty() },
            latitude = latitude,
            longitude = longitude,
            mapX = x,
            mapY = y,
            insideGeofence = insideGeofence == true,
            arrivalDistanceMeters = (arrivalDistanceMeters ?: -1L).toInt(),
            pickupNotes = pickupNotes.orEmpty(),
            pickupPhotoUri = pickupPhotoUri,
            nonExecutionReason = nonExecutionReason.orEmpty()
        )
    }

    fun AppNotification.toUi(): NotificationItem {
        val ts = createdAt?.toDate()?.time ?: System.currentTimeMillis()
        return NotificationItem(
            id = id,
            titleAr = title,
            titleEn = title,
            bodyAr = body,
            bodyEn = body,
            timeLabel = DateTimeFormatting.formatDateTime(ts),
            isRead = isRead,
            isUrgent = type.contains("DELAY", ignoreCase = true) || type.contains("NOT_EXECUTED", ignoreCase = true)
        )
    }

    fun AuditLog.toUi(): AuditEntry {
        val ts = createdAt?.toDate()?.time ?: System.currentTimeMillis()
        return AuditEntry(
            id = id,
            actorName = actorName,
            actionAr = "$actionType ($entityType)",
            oldValue = oldValue,
            newValue = newValue,
            timestamp = DateTimeFormatting.formatDateTime(ts)
        )
    }

    fun List<GpsLocationPoint>.toPlaybackStops(): List<GpsStopPoint> {
        if (isEmpty()) return emptyList()
        return takeLast(40).mapIndexed { idx, pt ->
            val (x, y) = GeoUtils.projectToCanvas(pt.latitude, pt.longitude)
            val label = if (pt.isStopped == true) "نقطة توقف #${idx + 1}" else "نقطة مسار #${idx + 1}"
            GpsStopPoint(
                label = label,
                x = x,
                y = y,
                stopMinutes = if (pt.isStopped == true) 3 else 0,
                timeLabel = DateTimeFormatting.formatTime(pt.recordedAtEpochMs)
            )
        }
    }

    fun formatWeekdaysAr(csv: String): String = WeekdayToken.formatArabicSummary(csv)
}
