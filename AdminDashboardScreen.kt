package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.validation.DateTimeFormatting
import com.example.ui.components.CompletionDonutChartCard
import com.example.ui.components.GlassSurface
import com.example.ui.components.GradientIconBadge
import com.example.ui.components.KpiMetricCard
import com.example.ui.components.LiveFleetMapCanvas
import com.example.ui.components.PulsingLiveDot
import com.example.ui.components.StatusChip
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.model.AuditEntry
import com.example.ui.model.DriverProfile
import com.example.ui.model.DriverStatus
import com.example.ui.model.OperationType
import com.example.ui.model.VisitItem
import com.example.ui.model.VisitStatus
import com.example.ui.theme.BubblesCyan
import com.example.ui.theme.BubblesCyanBorder
import com.example.ui.theme.BubblesCyanBright
import com.example.ui.theme.BubblesCyanDark
import com.example.ui.theme.BubblesCyanDeep
import com.example.ui.theme.BubblesCyanSoft
import com.example.ui.theme.BubblesGray
import com.example.ui.theme.BubblesSlateDark
import com.example.ui.theme.BubblesWhite
import com.example.ui.theme.GlassCardTranslucent
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusEmerald
import com.example.ui.theme.StatusIndigo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardTab(
    drivers: List<DriverProfile>,
    visits: List<VisitItem>,
    auditLogs: List<AuditEntry>,
    gracePeriodMinutes: Long,
    geofenceRadiusMeters: Long,
    onQuickNewVisit: () -> Unit,
    onOpenTracking: () -> Unit,
    onSendBroadcast: () -> Unit
) {
    val s = LocalAppStrings.current
    val context = LocalContext.current
    var selectedDriver by remember { mutableStateOf<DriverProfile?>(null) }

    val now = System.currentTimeMillis()
    val todayVisits = remember(visits) {
        visits.filter { DateTimeFormatting.isSameLocalDay(it.scheduledAtEpochMs, now) }.ifEmpty { visits }
    }

    val onShiftCount = drivers.count { it.status == DriverStatus.ON_SHIFT }
    val activeCount = drivers.count { it.status == DriverStatus.ON_SHIFT || it.status == DriverStatus.ACTIVE }
    val offlineCount = drivers.count { !it.gpsActive }
    val pickupCount = todayVisits.count { it.operationType == OperationType.PICKUP || it.operationType == OperationType.BOTH }
    val deliveryCount = todayVisits.count { it.operationType == OperationType.DELIVERY || it.operationType == OperationType.BOTH }
    val completedCount = todayVisits.count { it.status == VisitStatus.COMPLETED }
    val inProgressCount = todayVisits.count {
        it.status == VisitStatus.EN_ROUTE || it.status == VisitStatus.ARRIVED || it.status == VisitStatus.IN_PROGRESS
    }
    val lateCount = todayVisits.count { it.status == VisitStatus.LATE || it.delayMinutes > gracePeriodMinutes }
    val cancelledCount = todayVisits.count { it.status == VisitStatus.CANCELLED }
    val unexecutedCount = todayVisits.count { it.status == VisitStatus.NOT_EXECUTED }
    val collectedSum = todayVisits.sumOf { it.amountCollected }.toInt()
    val uncollectedSum = todayVisits.sumOf { (it.amountDue - it.amountCollected).coerceAtLeast(0.0) }.toInt()
    val completionPct = if (todayVisits.isEmpty()) 0 else ((completedCount * 100f) / todayVisits.size).toInt()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Executive Glass Operations Hero Banner
        item {
            Surface(
                shape = RoundedCornerShape(26.dp),
                shadowElevation = 8.dp,
                border = BorderStroke(1.2.dp, Color.White.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(BubblesCyanDark, BubblesCyanDeep, Color(0xFF0A3D47))
                            )
                        )
                ) {
                    Canvas(modifier = Modifier.matchParentSize()) {
                        drawCircle(
                            brush = Brush.radialGradient(listOf(BubblesCyanBright.copy(alpha = 0.28f), Color.Transparent)),
                            radius = size.height * 0.85f,
                            center = Offset(size.width * 0.90f, size.height * 0.2f)
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = 0.08f),
                            radius = size.height * 0.45f,
                            center = Offset(size.width * 0.14f, size.height * 0.9f),
                            style = Stroke(width = 2.5f)
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color.White.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.30f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    PulsingLiveDot(StatusEmerald)
                                    Text(
                                        text = "${s.liveBadge} • ${if (s.isArabic) "في الدوام $onShiftCount" else "$onShiftCount On Shift"}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                text = s.dashboardTitle,
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (s.isArabic) {
                                    "سماح التأخير $gracePeriodMinutes د • نطاق Geofence ${geofenceRadiusMeters}م"
                                } else {
                                    "Grace ${gracePeriodMinutes}m • Geofence ${geofenceRadiusMeters}m"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.82f)
                            )
                        }

                        // Circular Fleet Readiness Ring
                        Box(modifier = Modifier.size(76.dp), contentAlignment = Alignment.Center) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val stroke = 8.dp.toPx()
                                drawCircle(color = Color.White.copy(alpha = 0.22f), style = Stroke(width = stroke))
                                drawArc(
                                    brush = Brush.sweepGradient(listOf(BubblesCyanBright, StatusEmerald, BubblesCyanBright)),
                                    startAngle = -90f,
                                    sweepAngle = (completionPct / 100f) * 360f,
                                    useCenter = false,
                                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$completionPct%",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (s.isArabic) "إنجاز" else "Done",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.78f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2x2 Frosted Glass KPI Grid with Bezier Sparklines
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    KpiMetricCard(
                        title = "${s.activeDrivers} / ${s.totalDrivers}",
                        value = "$activeCount / ${drivers.size}",
                        accentColor = BubblesCyan,
                        icon = Icons.Rounded.DirectionsCar,
                        sparklinePoints = listOf(0.4f, 0.55f, 0.7f, 0.68f, 0.85f, 0.9f),
                        badgeText = "${s.onShiftDrivers}: $onShiftCount • ${s.offlineDrivers}: $offlineCount",
                        modifier = Modifier.weight(1f)
                    )
                    KpiMetricCard(
                        title = s.todayVisits,
                        value = "${todayVisits.size}",
                        accentColor = StatusIndigo,
                        icon = Icons.Rounded.EventAvailable,
                        sparklinePoints = listOf(0.3f, 0.45f, 0.6f, 0.8f, 0.72f, 0.95f),
                        badgeText = "${s.pickupsCount}: $pickupCount • ${s.deliveriesCount}: $deliveryCount",
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    KpiMetricCard(
                        title = s.collectedAmount,
                        value = "$collectedSum ${s.sar}",
                        accentColor = StatusEmerald,
                        icon = Icons.Rounded.AccountBalanceWallet,
                        sparklinePoints = listOf(0.25f, 0.42f, 0.58f, 0.74f, 0.82f, 0.96f),
                        badgeText = "${s.uncollectedAmount}: $uncollectedSum ${s.sar}",
                        modifier = Modifier.weight(1f)
                    )
                    KpiMetricCard(
                        title = "${s.completedCount} / ${s.lateCount}",
                        value = "$completedCount / $lateCount",
                        accentColor = if (lateCount > 0) StatusAmber else StatusEmerald,
                        icon = Icons.Rounded.TaskAlt,
                        sparklinePoints = listOf(0.75f, 0.6f, 0.7f, 0.55f, 0.65f, 0.85f),
                        badgeText = "${s.cancelledCount}: $cancelledCount • ${s.unexecutedCount}: $unexecutedCount",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Donut Completion Analytics Ring
        item {
            CompletionDonutChartCard(
                completed = completedCount,
                inProgress = inProgressCount,
                late = lateCount,
                unexecuted = unexecutedCount
            )
        }

        // Live Fleet Map
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GradientIconBadge(icon = Icons.Rounded.Radar, accentColor = BubblesCyanDark, size = 32.dp, iconSize = 18.dp)
                        Text(
                            text = s.liveFleetMap,
                            style = MaterialTheme.typography.titleMedium,
                            color = BubblesSlateDark
                        )
                    }
                    Surface(shape = RoundedCornerShape(12.dp), color = BubblesCyanSoft) {
                        Text(
                            text = if (s.isArabic) "اضغط على المندوب للتفاصيل" else "Tap driver pin",
                            style = MaterialTheme.typography.labelSmall,
                            color = BubblesCyanDark,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                LiveFleetMapCanvas(
                    drivers = drivers,
                    visits = todayVisits,
                    selectedDriverId = selectedDriver?.id,
                    geofenceRadiusMeters = geofenceRadiusMeters,
                    onSelectDriver = { selectedDriver = it }
                )
            }
        }

        // Quick Actions Pills
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Rounded.Bolt, contentDescription = null, tint = BubblesCyanDark, modifier = Modifier.size(18.dp))
                    Text(
                        text = if (s.isArabic) "إجراءات سريعة" else "Quick Actions",
                        style = MaterialTheme.typography.titleSmall,
                        color = BubblesSlateDark
                    )
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        Button(
                            onClick = onQuickNewVisit,
                            colors = ButtonDefaults.buttonColors(containerColor = BubblesCyan),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.testTag("quick_new_visit_button")
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(6.dp))
                            Text(s.newVisitBtn)
                        }
                    }
                    item {
                        FilledTonalButton(
                            onClick = onOpenTracking,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = GlassCardTranslucent,
                                contentColor = BubblesCyanDark
                            ),
                            border = BorderStroke(1.dp, BubblesCyanBorder),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.testTag("quick_open_tracking_button")
                        ) {
                            Icon(Icons.Rounded.GpsFixed, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(6.dp))
                            Text(s.routePlaybackTitle)
                        }
                    }
                    item {
                        FilledTonalButton(
                            onClick = onSendBroadcast,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = GlassCardTranslucent,
                                contentColor = BubblesSlateDark
                            ),
                            border = BorderStroke(1.dp, Color.White),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.testTag("quick_broadcast_button")
                        ) {
                            Icon(Icons.Rounded.Campaign, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(6.dp))
                            Text(s.sendBroadcast)
                        }
                    }
                }
            }
        }

        // Recent Live Activity Feed
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GradientIconBadge(icon = Icons.Rounded.Timeline, accentColor = StatusIndigo, size = 32.dp, iconSize = 18.dp)
                Text(
                    text = s.recentActivity,
                    style = MaterialTheme.typography.titleMedium,
                    color = BubblesSlateDark
                )
            }
        }

        if (auditLogs.isEmpty()) {
            item {
                GlassSurface(cornerRadius = 20.dp) {
                    Text(
                        text = if (s.isArabic) "لا توجد عمليات مسجلة حتى الآن. ابدأ بإضافة المناديب والعملاء والزيارات." else "No activity recorded yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = BubblesGray,
                        modifier = Modifier.padding(18.dp)
                    )
                }
            }
        } else {
            items(auditLogs.take(15), key = { it.id }) { log ->
                GlassSurface(cornerRadius = 18.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(BubblesCyan)
                        )
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = log.actionAr,
                                style = MaterialTheme.typography.titleSmall,
                                color = BubblesSlateDark
                            )
                            Text(
                                text = "${log.actorName} • ${log.newValue}",
                                style = MaterialTheme.typography.bodySmall,
                                color = BubblesGray
                            )
                        }
                        Surface(shape = RoundedCornerShape(10.dp), color = BubblesCyanSoft) {
                            Text(
                                text = log.timestamp,
                                style = MaterialTheme.typography.labelSmall,
                                color = BubblesCyanDark,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    selectedDriver?.let { driver ->
        ModalBottomSheet(
            onDismissRequest = { selectedDriver = null },
            containerColor = BubblesWhite
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (s.isArabic) driver.nameAr else driver.nameEn,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (driver.accessCode.isNotBlank()) "${driver.phone} • كود: ${driver.accessCode}" else driver.phone,
                            style = MaterialTheme.typography.bodyMedium,
                            color = BubblesGray
                        )
                    }
                    StatusChip(
                        label = if (s.isArabic) driver.status.ar else driver.status.en,
                        color = driver.status.color
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    KpiMetricCard(
                        title = s.completedCount,
                        value = driver.completedToday.toString(),
                        accentColor = StatusEmerald,
                        icon = Icons.Rounded.TaskAlt,
                        modifier = Modifier.weight(1f)
                    )
                    KpiMetricCard(
                        title = s.collectedAmount,
                        value = "${driver.collectedToday.toInt()} ${s.sar}",
                        accentColor = BubblesCyan,
                        icon = Icons.Rounded.AccountBalanceWallet,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = BubblesCyan, modifier = Modifier.size(18.dp))
                    Text(
                        text = if (s.isArabic) "آخر تحديث GPS: ${driver.lastGpsTextAr} • الدقة: ${driver.accuracyMeters}م"
                        else "Last GPS: ${driver.lastGpsTextEn} • Accuracy: ${driver.accuracyMeters}m",
                        style = MaterialTheme.typography.bodyMedium,
                        color = BubblesGray
                    )
                }
                Button(
                    onClick = {
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${driver.phone}")))
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BubblesCyan),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("${s.callCustomer}: ${driver.phone}")
                }
                Spacer(Modifier.height(14.dp))
            }
        }
    }
}
