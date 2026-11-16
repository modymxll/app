package com.example.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.MoneyOff
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.AppNotification
import com.example.data.model.Driver
import com.example.data.model.DriverAttendance
import com.example.data.model.Visit
import com.example.data.validation.GeoUtils
import com.example.service.LocationForegroundService
import com.example.ui.components.GlassSurface
import com.example.ui.components.GradientIconBadge
import com.example.ui.components.LiveFleetMapCanvas
import com.example.ui.components.PulsingLiveDot
import com.example.ui.components.StatusChip
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.model.DriverProfile
import com.example.ui.model.NotificationItem
import com.example.ui.model.OperationType
import com.example.ui.model.PaymentMethod
import com.example.ui.model.PaymentStatus
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
import com.example.ui.theme.StatusRed
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun DriverHomeTab(
    driver: Driver?,
    driverUi: DriverProfile?,
    activeAttendance: DriverAttendance?,
    visits: List<VisitItem>,
    gpsMovingIntervalSec: Long,
    gracePeriodMinutes: Long,
    onStartShift: suspend (Driver) -> String?,
    onEndShift: (Driver) -> Unit,
    onSelectVisit: (VisitItem) -> Unit,
    onShowToast: (String) -> Unit
) {
    val s = LocalAppStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val isServiceTracking by LocationForegroundService.isTracking.collectAsState()
    val onShift = driver?.status == "ON_SHIFT" || activeAttendance?.isActive == true

    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(onShift) {
        while (onShift) {
            nowMs = System.currentTimeMillis()
            delay(1000L)
        }
    }

    val shiftSeconds = remember(nowMs, activeAttendance?.startedAtEpochMs, onShift) {
        if (!onShift) 0L
        else {
            val start = activeAttendance?.startedAtEpochMs ?: nowMs
            ((nowMs - start) / 1000L).coerceAtLeast(0L)
        }
    }
    val shiftClock = "%02d:%02d:%02d".format(shiftSeconds / 3600L, (shiftSeconds % 3600L) / 60L, shiftSeconds % 60L)

    val startForegroundShift: () -> Unit = {
        val d = driver
        if (d != null) {
            scope.launch {
                val attId = onStartShift(d)
                if (attId != null) {
                    LocationForegroundService.start(
                        context = context,
                        driverId = d.id,
                        userId = d.userId.ifBlank { d.id },
                        attendanceId = attId,
                        intervalSec = gpsMovingIntervalSec
                    )
                }
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val locOk = (grants[Manifest.permission.ACCESS_FINE_LOCATION] == true) ||
            (grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true)
        if (locOk) {
            startForegroundShift()
        } else {
            onShowToast(if (s.isArabic) "صلاحية الموقع مطلوبة لبدء الدوام وتفعيل GPS" else "Location permission required for shift GPS tracking")
        }
    }

    val completedCount = visits.count { it.status == VisitStatus.COMPLETED }
    val lateCount = visits.count { it.status == VisitStatus.LATE || it.delayMinutes > gracePeriodMinutes }
    val remainingCount = visits.count {
        it.status != VisitStatus.COMPLETED && it.status != VisitStatus.CANCELLED && it.status != VisitStatus.NOT_EXECUTED
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (driver == null || driverUi == null) {
            item {
                GlassSurface(cornerRadius = 22.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        GradientIconBadge(icon = Icons.Rounded.Key, accentColor = BubblesCyanDark, size = 44.dp, iconSize = 22.dp)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = if (s.isArabic) "بانتظار ربط ملف المندوب" else "Pending Driver Profile Link",
                                style = MaterialTheme.typography.titleMedium,
                                color = BubblesSlateDark
                            )
                            Text(
                                text = if (s.isArabic) "اطلب من الإدارة إنشاء حساب مندوب في تبويب المناديب وتزويدك بكود الدخول السداسي الخاص بك." else "Ask Admin to create your Driver profile and share your 6-digit Access Code.",
                                style = MaterialTheme.typography.bodySmall,
                                color = BubblesGray
                            )
                        }
                    }
                }
            }
        } else {
            item {
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    shadowElevation = 6.dp,
                    border = BorderStroke(1.2.dp, Color.White.copy(alpha = 0.55f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    if (onShift) listOf(BubblesCyanBright, BubblesCyan, BubblesCyanDark, BubblesCyanDeep)
                                    else listOf(Color(0xFF475569), Color(0xFF334155))
                                )
                            )
                    ) {
                        Canvas(modifier = Modifier.matchParentSize()) {
                            drawCircle(Color.White.copy(alpha = 0.12f), radius = size.height * 0.55f, center = Offset(size.width * 0.9f, size.height * 0.15f))
                            drawCircle(Color.White.copy(alpha = 0.08f), radius = size.height * 0.32f, center = Offset(size.width * 0.12f, size.height * 0.85f))
                        }
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Image(
                                        painter = painterResource(id = R.drawable.img_driver_avatar_1794774063821),
                                        contentDescription = driverUi.nameAr,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(CircleShape)
                                    )
                                    Column {
                                        Text(
                                            text = if (s.isArabic) driverUi.nameAr else driverUi.nameEn,
                                            style = MaterialTheme.typography.titleLarge,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                            if (onShift) PulsingLiveDot(Color.White)
                                            Text(
                                                text = if (onShift) "${s.onShift} • $shiftClock" else s.offShift,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Color.White.copy(alpha = 0.92f)
                                            )
                                        }
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = Color.White.copy(alpha = 0.2f),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Icon(Icons.Rounded.GpsFixed, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                        Text(
                                            text = if (onShift && isServiceTracking) "${gpsMovingIntervalSec}s" else if (onShift) "ACTIVE" else "OFF",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            Button(
                                onClick = {
                                    if (onShift) {
                                        LocationForegroundService.stop(context)
                                        onEndShift(driver)
                                    } else {
                                        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                        if (hasFine || hasCoarse) {
                                            startForegroundShift()
                                        } else {
                                            val perms = buildList {
                                                add(Manifest.permission.ACCESS_FINE_LOCATION)
                                                add(Manifest.permission.ACCESS_COARSE_LOCATION)
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                    add(Manifest.permission.POST_NOTIFICATIONS)
                                                }
                                            }.toTypedArray()
                                            permissionLauncher.launch(perms)
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White.copy(alpha = 0.95f),
                                    contentColor = if (onShift) StatusRed else BubblesCyanDark
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("driver_shift_toggle_button")
                            ) {
                                Icon(
                                    imageVector = if (onShift) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                                    contentDescription = null
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = if (onShift) s.endShift else s.startShift,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DriverStatPill(s.tabTasks, visits.size, BubblesCyan, Modifier.weight(1f))
                DriverStatPill(s.completedCount, completedCount, StatusEmerald, Modifier.weight(1f))
                DriverStatPill(if (s.isArabic) "متبقية" else "Left", remainingCount, BubblesGray, Modifier.weight(1f))
                DriverStatPill(s.lateCount, lateCount, StatusAmber, Modifier.weight(1f))
            }
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GradientIconBadge(icon = Icons.Rounded.LocalShipping, accentColor = BubblesCyanDark, size = 32.dp, iconSize = 18.dp)
                Text(s.todaySchedule, style = MaterialTheme.typography.titleMedium, color = BubblesSlateDark)
            }
        }

        if (visits.isEmpty()) {
            item {
                GlassSurface(cornerRadius = 20.dp) {
                    Text(
                        text = if (s.isArabic) "لا توجد زيارات مسندة حاليًا." else "No assigned visits yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = BubblesGray,
                        modifier = Modifier.padding(18.dp)
                    )
                }
            }
        } else {
            items(visits, key = { it.id }) { visit ->
                GlassSurface(
                    onClick = { onSelectVisit(visit) },
                    cornerRadius = 22.dp,
                    modifier = Modifier.testTag("driver_visit_card_${visit.id}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(5.dp)
                                .height(96.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    Brush.verticalGradient(listOf(visit.status.color, visit.status.color.copy(alpha = 0.45f)))
                                )
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(visit.scheduledTime, style = MaterialTheme.typography.titleSmall, color = BubblesCyanDark, fontWeight = FontWeight.Bold)
                                    StatusChip(
                                        label = if (s.isArabic) visit.operationType.ar else visit.operationType.en,
                                        color = BubblesCyan
                                    )
                                }
                                StatusChip(
                                    label = if (s.isArabic) visit.status.ar else visit.status.en,
                                    color = visit.status.color
                                )
                            }
                            Text(
                                text = if (s.isArabic) "${visit.customerNameAr} - ${visit.locationNameAr}" else "${visit.customerNameEn} - ${visit.locationNameEn}",
                                style = MaterialTheme.typography.titleMedium,
                                color = BubblesSlateDark
                            )
                            Text(
                                text = "${visit.districtAr} • ${visit.phone}",
                                style = MaterialTheme.typography.bodySmall,
                                color = BubblesGray
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        runCatching {
                                            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${visit.phone}")))
                                        }
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Rounded.Call, contentDescription = s.callCustomer, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(s.callCustomer)
                                }
                                Button(
                                    onClick = {
                                        val lat = visit.latitude ?: 24.7136
                                        val lng = visit.longitude ?: 46.6753
                                        val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(${Uri.encode(visit.locationNameAr)})")
                                        runCatching {
                                            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                        }.onFailure {
                                            onShowToast(if (s.isArabic) "لا يوجد تطبيق خرائط مثبت" else "No maps app installed")
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = BubblesCyanSoft, contentColor = BubblesCyanDark),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Rounded.Map, contentDescription = s.openMaps, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(s.openMaps)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DriverStatPill(label: String, count: Int, color: Color, modifier: Modifier = Modifier) {
    GlassSurface(
        modifier = modifier,
        cornerRadius = 18.dp,
        containerColor = GlassCardTranslucent
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(count.toString(), style = MaterialTheme.typography.titleLarge, color = color, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = BubblesSlateDark, maxLines = 1)
        }
    }
}

private fun PaymentMethod.icon(): ImageVector = when (this) {
    PaymentMethod.CASH -> Icons.Rounded.Payments
    PaymentMethod.MADA -> Icons.Rounded.CreditCard
    PaymentMethod.VISA -> Icons.Rounded.CreditCard
    PaymentMethod.MASTERCARD -> Icons.Rounded.CreditCard
    PaymentMethod.BANK_TRANSFER -> Icons.Rounded.AccountBalance
    PaymentMethod.GCC_NET -> Icons.Rounded.Public
    PaymentMethod.UNPAID -> Icons.Rounded.MoneyOff
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VisitExecutionScreen(
    visit: VisitItem,
    rawVisit: Visit,
    driver: Driver,
    geofenceRadiusMeters: Long,
    onBack: () -> Unit,
    onCheckInArrival: (visit: Visit, driverLat: Double?, driverLng: Double?) -> Unit,
    onCompleteVisit: (
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
    ) -> Unit,
    onMarkNotExecuted: (visit: Visit, reason: String) -> Unit,
    onShowToast: (String) -> Unit
) {
    val s = LocalAppStrings.current
    val context = LocalContext.current
    BackHandler { onBack() }

    val latestLocation by LocationForegroundService.latestLocation.collectAsState()
    val driverLat = latestLocation?.latitude ?: driver.lastLat
    val driverLng = latestLocation?.longitude ?: driver.lastLng

    val (insideGeo, distanceMeters) = remember(driverLat, driverLng, visit.latitude, visit.longitude, geofenceRadiusMeters) {
        GeoUtils.isInsideGeofence(driverLat, driverLng, visit.latitude, visit.longitude, geofenceRadiusMeters.toInt())
    }

    var invoiceNumber by remember(visit.id) { mutableStateOf(visit.invoiceNumber) }
    var amountDueText by remember(visit.id) { mutableStateOf(if (visit.amountDue > 0) visit.amountDue.toInt().toString() else "") }
    var amountCollectedText by remember(visit.id) { mutableStateOf(if (visit.amountCollected > 0) visit.amountCollected.toInt().toString() else "") }
    var selectedMethod by remember(visit.id) { mutableStateOf(visit.paymentMethod) }
    var pickupNotes by remember(visit.id) { mutableStateOf(visit.pickupNotes) }
    var selectedPhotoUri by remember(visit.id) { mutableStateOf(visit.pickupPhotoUri) }
    var showNotExecutedDialog by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedPhotoUri = uri.toString()
        }
    }

    val due = amountDueText.toDoubleOrNull() ?: 0.0
    val collected = if (selectedMethod == PaymentMethod.UNPAID) 0.0 else (amountCollectedText.toDoubleOrNull() ?: 0.0)
    val remaining = (due - collected).coerceAtLeast(0.0)
    val ratio = if (due > 0) (collected / due).toFloat().coerceIn(0f, 1f) else 0f
    val derivedPaymentStatus = when {
        collected <= 0.0 || selectedMethod == PaymentMethod.UNPAID -> PaymentStatus.UNPAID
        remaining <= 0.0 -> PaymentStatus.PAID
        else -> PaymentStatus.PARTIAL
    }

    val currentStep = when (visit.status) {
        VisitStatus.COMPLETED -> 4
        VisitStatus.ARRIVED, VisitStatus.IN_PROGRESS -> 3
        VisitStatus.EN_ROUTE -> 2
        else -> 1
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(shape = CircleShape, color = GlassCardTranslucent, border = BorderStroke(1.dp, BubblesCyanBorder)) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("visit_back_button")) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = BubblesSlateDark)
                    }
                }
                Text("${s.visitDetails} • ${visit.scheduledTime}", style = MaterialTheme.typography.titleLarge, color = BubblesSlateDark)
            }
        }

        // 4-Step Visual Progress Stepper
        item {
            GlassSurface(cornerRadius = 20.dp) {
                val labels = if (s.isArabic) listOf("وصول", "فحص الموقع", "استلام/تسليم", "إتمام")
                else listOf("Arrive", "Geofence", "Service", "Complete")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    labels.forEachIndexed { idx, label ->
                        val stepNum = idx + 1
                        val done = stepNum <= currentStep
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(if (done) BubblesCyan else Color(0xFFE2EFF2))
                                    .border(1.dp, if (done) BubblesCyanDark else Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stepNum.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (done) Color.White else BubblesGray,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (done) BubblesCyanDark else BubblesGray,
                                fontWeight = if (done) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        item {
            GlassSurface(cornerRadius = 22.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (s.isArabic) "${visit.customerNameAr} — ${visit.locationNameAr}" else "${visit.customerNameEn} — ${visit.locationNameEn}",
                        style = MaterialTheme.typography.titleMedium,
                        color = BubblesSlateDark
                    )
                    if (visit.instructionsAr.isNotBlank()) {
                        Text(visit.instructionsAr, style = MaterialTheme.typography.bodyMedium, color = BubblesGray)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        val geoText = when {
                            driverLat == null || driverLng == null -> if (s.isArabic) "بانتظار GPS" else "Waiting GPS"
                            insideGeo -> "${s.geofenceInside} (${distanceMeters}m)"
                            else -> if (s.isArabic) "خارج النطاق ($distanceMeters م)" else "Outside ($distanceMeters m)"
                        }
                        val geoColor = if (insideGeo) StatusEmerald else StatusAmber
                        StatusChip(label = geoText, color = geoColor)

                        Surface(shape = CircleShape, color = Color(0xFFEDF5F7)) {
                            IconButton(onClick = {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("phone", visit.phone))
                                onShowToast("${s.copiedMsg}: ${visit.phone}")
                            }) {
                                Icon(Icons.Rounded.ContentCopy, contentDescription = s.copyPhone, tint = BubblesGray, modifier = Modifier.size(18.dp))
                            }
                        }
                        Surface(shape = CircleShape, color = BubblesCyanSoft) {
                            IconButton(onClick = {
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${visit.phone}")))
                                }
                            }) {
                                Icon(Icons.Rounded.Call, contentDescription = s.callCustomer, tint = BubblesCyanDark, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    if (visit.status == VisitStatus.NOT_STARTED || visit.status == VisitStatus.EN_ROUTE) {
                        OutlinedButton(
                            onClick = { onCheckInArrival(rawVisit, driverLat, driverLng) },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().testTag("check_in_arrival_btn")
                        ) {
                            Icon(Icons.Rounded.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(s.checkInArrival)
                        }
                    }
                }
            }
        }

        if (visit.operationType == OperationType.PICKUP || visit.operationType == OperationType.BOTH) {
            item {
                GlassSurface(cornerRadius = 22.dp) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            GradientIconBadge(icon = Icons.Rounded.Inventory2, accentColor = BubblesCyanDark, size = 34.dp, iconSize = 18.dp)
                            Text(s.recordPickup, style = MaterialTheme.typography.titleMedium, color = BubblesSlateDark)
                        }
                        OutlinedTextField(
                            value = pickupNotes,
                            onValueChange = { pickupNotes = it },
                            label = { Text(s.pickupNotesLabel) },
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BubblesCyan, unfocusedBorderColor = BubblesCyanBorder),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Rounded.AddAPhoto, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(s.attachPhotoBtn)
                            }
                            if (!selectedPhotoUri.isNullOrBlank()) {
                                Box(contentAlignment = Alignment.TopEnd) {
                                    AsyncImage(
                                        model = selectedPhotoUri,
                                        contentDescription = s.attachPhotoBtn,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                    )
                                    Surface(
                                        onClick = { selectedPhotoUri = null },
                                        shape = CircleShape,
                                        color = StatusRed,
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(Icons.Rounded.Close, contentDescription = "Remove photo", tint = Color.White, modifier = Modifier.padding(2.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (visit.operationType == OperationType.DELIVERY || visit.operationType == OperationType.BOTH) {
            item {
                GlassSurface(cornerRadius = 22.dp) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            GradientIconBadge(icon = Icons.Rounded.ReceiptLong, accentColor = StatusEmerald, size = 34.dp, iconSize = 18.dp)
                            Text(s.recordDelivery, style = MaterialTheme.typography.titleMedium, color = BubblesSlateDark)
                        }
                        OutlinedTextField(
                            value = invoiceNumber,
                            onValueChange = { invoiceNumber = it },
                            label = { Text(s.invoiceManualLabel) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BubblesCyan, unfocusedBorderColor = BubblesCyanBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("invoice_input")
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = amountDueText,
                                onValueChange = { amountDueText = it },
                                label = { Text(s.amountDueLabel) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = amountCollectedText,
                                onValueChange = { amountCollectedText = it },
                                label = { Text(s.amountCollectedLabel) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = BubblesCyanSoft
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${s.remainingLabel}: ${remaining.toInt()} ${s.sar}",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = BubblesCyanDark,
                                        fontWeight = FontWeight.Bold
                                    )
                                    StatusChip(
                                        label = if (s.isArabic) derivedPaymentStatus.ar else derivedPaymentStatus.en,
                                        color = derivedPaymentStatus.color
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { ratio },
                                    color = derivedPaymentStatus.color,
                                    trackColor = Color.White,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(7.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                )
                            }
                        }

                        Text(s.paymentMethodLabel, style = MaterialTheme.typography.labelLarge, color = BubblesGray)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PaymentMethod.entries.forEach { method ->
                                val selected = selectedMethod == method
                                Surface(
                                    onClick = { selectedMethod = method },
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (selected) BubblesCyan else Color(0xFFF3F8FA),
                                    border = BorderStroke(1.dp, if (selected) BubblesCyanDark else Color(0xFFDCE8EB))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Icon(
                                            imageVector = method.icon(),
                                            contentDescription = null,
                                            tint = if (selected) Color.White else BubblesGray,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = if (s.isArabic) method.ar else method.en,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (selected) Color.White else BubblesSlateDark
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        onCompleteVisit(
                            rawVisit,
                            driver,
                            invoiceNumber.trim(),
                            due,
                            collected,
                            selectedMethod.name,
                            pickupNotes.trim(),
                            selectedPhotoUri,
                            driverLat,
                            driverLng
                        )
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BubblesCyan),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("confirm_visit_execution_btn")
                ) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (visit.operationType == OperationType.PICKUP) s.confirmPickupBtn else s.confirmDeliveryBtn,
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                OutlinedButton(
                    onClick = { showNotExecutedDialog = true },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(s.markNotExecuted, color = StatusRed)
                }
            }
        }
    }

    if (showNotExecutedDialog) {
        var reason by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNotExecutedDialog = false },
            containerColor = BubblesWhite,
            title = { Text(s.markNotExecuted) },
            text = {
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text(if (s.isArabic) "سبب عدم التنفيذ" else "Reason") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onMarkNotExecuted(rawVisit, reason.ifBlank { "العميل غير متواجد" })
                        showNotExecutedDialog = false
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusRed)
                ) { Text(if (s.isArabic) "تأكيد" else "Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { showNotExecutedDialog = false }) { Text(if (s.isArabic) "إلغاء" else "Cancel") }
            }
        )
    }
}

@Composable
fun DriverMapTab(
    driverUi: DriverProfile?,
    visits: List<VisitItem>,
    geofenceRadiusMeters: Long
) {
    val s = LocalAppStrings.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GradientIconBadge(icon = Icons.Rounded.Map, accentColor = BubblesCyanDark, size = 38.dp, iconSize = 20.dp)
            Text(
                text = if (s.isArabic) "خريطة زياراتي ونطاقات الوصول" else "My Route & Geofences",
                style = MaterialTheme.typography.titleLarge,
                color = BubblesSlateDark
            )
        }
        LiveFleetMapCanvas(
            drivers = listOfNotNull(driverUi),
            visits = visits,
            selectedDriverId = driverUi?.id,
            geofenceRadiusMeters = geofenceRadiusMeters,
            onSelectDriver = {}
        )
        GlassSurface(cornerRadius = 18.dp, containerColor = GlassCardTranslucent) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Rounded.VerifiedUser, contentDescription = null, tint = BubblesCyanDark)
                Text(
                    text = if (s.isArabic) "التحقق الجغرافي التلقائي مفعل ضمن $geofenceRadiusMeters متر من موقع العميل" else "Geofencing auto-check active within ${geofenceRadiusMeters}m",
                    style = MaterialTheme.typography.bodyMedium,
                    color = BubblesCyanDark
                )
            }
        }
    }
}

@Composable
fun NotificationsTab(
    notifications: List<NotificationItem>,
    rawNotifications: List<AppNotification>,
    onToggleRead: (AppNotification) -> Unit
) {
    val s = LocalAppStrings.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GradientIconBadge(icon = Icons.Rounded.NotificationsActive, accentColor = BubblesCyanDark, size = 38.dp, iconSize = 20.dp)
                Text(s.tabNotifications, style = MaterialTheme.typography.titleLarge, color = BubblesSlateDark)
            }
        }
        if (notifications.isEmpty()) {
            item {
                GlassSurface(cornerRadius = 18.dp) {
                    Text(
                        text = if (s.isArabic) "لا توجد تنبيهات جديدة." else "No notifications yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = BubblesGray,
                        modifier = Modifier.padding(18.dp)
                    )
                }
            }
        } else {
            items(notifications, key = { it.id }) { item ->
                val raw = rawNotifications.firstOrNull { it.id == item.id }
                GlassSurface(
                    onClick = { if (raw != null) onToggleRead(raw) },
                    cornerRadius = 18.dp,
                    containerColor = if (item.isRead) GlassCardTranslucent else BubblesCyanSoft.copy(alpha = 0.85f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (item.isRead) Color.LightGray else BubblesCyan)
                        )
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (s.isArabic) item.titleAr else item.titleEn,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = BubblesSlateDark
                                )
                                Text(item.timeLabel, style = MaterialTheme.typography.labelSmall, color = BubblesGray)
                            }
                            Text(
                                text = if (s.isArabic) item.bodyAr else item.bodyEn,
                                style = MaterialTheme.typography.bodyMedium,
                                color = BubblesGray
                            )
                        }
                        Icon(Icons.Rounded.NotificationsActive, contentDescription = null, tint = BubblesCyanDark, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
