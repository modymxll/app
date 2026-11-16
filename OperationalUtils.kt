package com.example.data.validation

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

object SaudiPhoneUtils {
    private val cleanRegex = Regex("[^0-9+]")

    fun normalize(raw: String): String {
        val cleaned = raw.replace(cleanRegex, "").trim()
        return when {
            cleaned.startsWith("+966") -> cleaned
            cleaned.startsWith("00966") -> "+966" + cleaned.removePrefix("00966")
            cleaned.startsWith("966") && cleaned.length >= 11 -> "+$cleaned"
            cleaned.startsWith("05") && cleaned.length == 10 -> "+966" + cleaned.drop(1)
            cleaned.startsWith("5") && cleaned.length == 9 -> "+966$cleaned"
            cleaned.startsWith("01") && cleaned.length >= 9 -> "+966" + cleaned.drop(1)
            else -> cleaned
        }
    }

    fun isValid(raw: String): Boolean {
        val norm = normalize(raw)
        // Mobile: +9665XXXXXXXX (13 chars) or Landline: +9661XXXXXXXX (12 chars)
        return Regex("^\\+9665\\d{8}$").matches(norm) || Regex("^\\+9661\\d{7,8}$").matches(norm)
    }

    fun formatDisplay(raw: String): String {
        val norm = normalize(raw)
        return if (norm.startsWith("+966") && norm.length == 13) {
            "+966 ${norm.substring(4, 6)} ${norm.substring(6, 9)} ${norm.substring(9, 13)}"
        } else {
            raw
        }
    }
}

object GeoUtils {
    private const val EARTH_RADIUS_M = 6371000.0

    // Riyadh default bounding box used to project real Lat/Lng into 0..1 map canvas coordinates
    private const val DEFAULT_MIN_LAT = 24.55
    private const val DEFAULT_MAX_LAT = 24.92
    private const val DEFAULT_MIN_LNG = 46.52
    private const val DEFAULT_MAX_LNG = 46.85

    fun distanceMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Int {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return (EARTH_RADIUS_M * c).roundToInt()
    }

    fun isInsideGeofence(
        driverLat: Double?,
        driverLng: Double?,
        targetLat: Double?,
        targetLng: Double?,
        radiusMeters: Int
    ): Pair<Boolean, Int> {
        if (driverLat == null || driverLng == null || targetLat == null || targetLng == null) {
            return Pair(false, -1)
        }
        val dist = distanceMeters(driverLat, driverLng, targetLat, targetLng)
        return Pair(dist <= radiusMeters, dist)
    }

    fun calculateDelay(
        scheduledAtEpochMs: Long,
        arrivedAtEpochMs: Long,
        gracePeriodMinutes: Long
    ): Pair<Long, Long> {
        val diffSec = ((arrivedAtEpochMs - scheduledAtEpochMs) / 1000L).coerceAtLeast(0L)
        val graceSec = gracePeriodMinutes * 60L
        val delaySec = if (diffSec > graceSec) diffSec else 0L
        val delayMin = if (delaySec > 0L) ((delaySec + 59L) / 60L) else 0L
        return Pair(delaySec, delayMin)
    }

    fun projectToCanvas(
        lat: Double?,
        lng: Double?,
        minLat: Double = DEFAULT_MIN_LAT,
        maxLat: Double = DEFAULT_MAX_LAT,
        minLng: Double = DEFAULT_MIN_LNG,
        maxLng: Double = DEFAULT_MAX_LNG
    ): Pair<Float, Float> {
        if (lat == null || lng == null) return Pair(0.5f, 0.5f)
        val x = ((lng - minLng) / (maxLng - minLng).coerceAtLeast(0.01)).toFloat().coerceIn(0.08f, 0.90f)
        val y = (1.0 - (lat - minLat) / (maxLat - minLat).coerceAtLeast(0.01)).toFloat().coerceIn(0.10f, 0.88f)
        return Pair(x, y)
    }
}

enum class WeekdayToken(val code: String, val calendarDay: Int, val ar: String, val en: String) {
    SUN("SUN", Calendar.SUNDAY, "الأحد", "Sun"),
    MON("MON", Calendar.MONDAY, "الإثنين", "Mon"),
    TUE("TUE", Calendar.TUESDAY, "الثلاثاء", "Tue"),
    WED("WED", Calendar.WEDNESDAY, "الأربعاء", "Wed"),
    THU("THU", Calendar.THURSDAY, "الخميس", "Thu"),
    FRI("FRI", Calendar.FRIDAY, "الجمعة", "Fri"),
    SAT("SAT", Calendar.SATURDAY, "السبت", "Sat");

    companion object {
        fun fromCsv(csv: String): Set<WeekdayToken> {
            val parts = csv.split(",").map { it.trim().uppercase(Locale.US) }.toSet()
            return entries.filter { it.code in parts }.toSet()
        }

        fun formatArabicSummary(csv: String): String {
            val selected = fromCsv(csv)
            return if (selected.isEmpty()) csv else selected.joinToString(" • ") { it.ar }
        }
    }
}

object RecurringVisitGenerator {
    private val keyDateFmt = SimpleDateFormat("yyyyMMdd", Locale.US)

    fun generateOccurrences(
        scheduleId: String,
        weekdaysCsv: String,
        timeHHmm: String,
        startEpochMs: Long,
        endEpochMs: Long?,
        isContinuous: Boolean,
        horizonDays: Int = 14
    ): List<Pair<String, Long>> {
        val selectedDays = WeekdayToken.fromCsv(weekdaysCsv).map { it.calendarDay }.toSet()
        if (selectedDays.isEmpty()) return emptyList()

        val timeParts = timeHHmm.split(":")
        val hour = timeParts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 10
        val minute = timeParts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0

        val startCal = Calendar.getInstance().apply {
            timeInMillis = startEpochMs.coerceAtLeast(System.currentTimeMillis() - 86_400_000L)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val maxEndMs = if (!isContinuous && endEpochMs != null) {
            endEpochMs
        } else {
            startCal.timeInMillis + horizonDays * 86_400_000L
        }

        val results = mutableListOf<Pair<String, Long>>()
        val cursor = startCal.clone() as Calendar
        var guard = 0
        while (cursor.timeInMillis <= maxEndMs && guard < 60) {
            guard++
            if (cursor.get(Calendar.DAY_OF_WEEK) in selectedDays) {
                val occCal = (cursor.clone() as Calendar).apply {
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val ts = occCal.timeInMillis
                if (ts <= maxEndMs) {
                    val key = "${scheduleId}_${keyDateFmt.format(Date(ts))}"
                    results += Pair(key, ts)
                }
            }
            cursor.add(Calendar.DAY_OF_YEAR, 1)
        }
        return results
    }
}

object DateTimeFormatting {
    private val timeFmt = SimpleDateFormat("hh:mm a", Locale.US)
    private val dateTimeFmt = SimpleDateFormat("dd/MM hh:mm a", Locale.US)
    private val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun formatTime(epochMs: Long): String = if (epochMs <= 0L) "--:--" else timeFmt.format(Date(epochMs))
    fun formatDateTime(epochMs: Long): String = if (epochMs <= 0L) "--" else dateTimeFmt.format(Date(epochMs))
    fun formatDate(epochMs: Long): String = if (epochMs <= 0L) "--" else dateFmt.format(Date(epochMs))

    fun isSameLocalDay(epochA: Long, epochB: Long): Boolean {
        if (epochA <= 0L || epochB <= 0L) return false
        val ca = Calendar.getInstance().apply { timeInMillis = epochA }
        val cb = Calendar.getInstance().apply { timeInMillis = epochB }
        return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR) &&
            ca.get(Calendar.DAY_OF_YEAR) == cb.get(Calendar.DAY_OF_YEAR)
    }
}
