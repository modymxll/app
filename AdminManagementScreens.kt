package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Business
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.EventNote
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PriorityHigh
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.StopCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.Customer
import com.example.data.model.CustomerLocation
import com.example.data.model.Driver
import com.example.data.model.GpsLocationPoint
import com.example.data.model.PaymentRecord
import com.example.data.model.RecurringSchedule
import com.example.data.model.Visit
import com.example.data.validation.DateTimeFormatting
import com.example.export.ReportExporter
import com.example.ui.components.GlassSurface
import com.example.ui.components.GradientIconBadge
import com.example.ui.components.RoutePlaybackMapCanvas
import com.example.ui.components.StatusChip
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.model.AuditEntry
import com.example.ui.model.BranchLocation
import com.example.ui.model.CustomerRecord
import com.example.ui.model.DriverProfile
import com.example.ui.model.DriverStatus
import com.example.ui.model.PaymentMethod
import com.example.ui.model.UiMappers
import com.example.ui.model.UiMappers.toPlaybackStops
import com.example.ui.model.VisitItem
import com.example.ui.model.VisitStatus
import com.example.ui.theme.BubblesCyan
import com.example.ui.theme.BubblesCyanBorder
import com.example.ui.theme.BubblesCyanBright
import com.example.ui.theme.BubblesCyanDark
import com.example.ui.theme.BubblesCyanSoft
import com.example.ui.theme.BubblesGray
import com.example.ui.theme.BubblesSlateDark
import com.example.ui.theme.BubblesWhite
import com.example.ui.theme.GlassCardTranslucent
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusEmerald
import com.example.ui.theme.StatusIndigo
import com.example.ui.theme.StatusRed
import kotlinx.coroutines.delay
import java.util.Calendar

@Composable
fun VisitsScheduleTab(
    visits: List<VisitItem>,
    rawVisits: List<Visit>,
    recurringSchedules: List<RecurringSchedule>,
    locations: List<CustomerLocation>,
    drivers: List<Driver>,
    gracePeriodMinutes: Long,
    onCreateVisitOrRecurring: (
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
    ) -> Unit,
    onUpdateVisitAdmin: (visit: Visit, newDriver: Driver, newStatus: String, newPriority: String) -> Unit,
    onStopRecurring: (RecurringSchedule) -> Unit
) {
    val s = LocalAppStrings.current
    var filterStatus by remember { mutableStateOf<VisitStatus?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingVisit by remember { mutableStateOf<Visit?>(null) }

    val activeRecurring = remember(recurringSchedules) {
        recurringSchedules.filter { it.isActive }
    }

    val filtered = remember(visits, filterStatus) {
        if (filterStatus == null) visits else visits.filter { it.status == filterStatus }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    GradientIconBadge(icon = Icons.Rounded.EventNote, accentColor = BubblesCyanDark, size = 38.dp, iconSize = 20.dp)
                    Text(
                        text = if (s.isArabic) "جدولة الزيارات والدورية" else "Visits & Recurring",
                        style = MaterialTheme.typography.titleLarge,
                        color = BubblesSlateDark
                    )
                }
                Button(
                    onClick = { showCreateDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = BubblesCyan),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("add_visit_btn")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(s.newVisitBtn)
                }
            }
        }

        if (activeRecurring.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = if (s.isArabic) "الجداول المتكررة النشطة (${activeRecurring.size})" else "Active Recurring Schedules (${activeRecurring.size})",
                        style = MaterialTheme.typography.titleSmall,
                        color = StatusIndigo
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(activeRecurring, key = { it.id }) { sched ->
                            GlassSurface(cornerRadius = 16.dp) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    GradientIconBadge(icon = Icons.Rounded.EventRepeat, accentColor = StatusIndigo, size = 32.dp, iconSize = 17.dp)
                                    Column {
                                        Text(
                                            text = "${sched.locationName ?: sched.locationId} (${sched.timeHHmm})",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = BubblesSlateDark
                                        )
                                        Text(
                                            text = UiMappers.formatWeekdaysAr(sched.weekdaysCsv),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = BubblesGray
                                        )
                                    }
                                    OutlinedButton(
                                        onClick = { onStopRecurring(sched) },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Rounded.StopCircle, contentDescription = null, tint = StatusRed, modifier = Modifier.size(15.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text(if (s.isArabic) "إيقاف" else "Stop", color = StatusRed, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = filterStatus == null,
                        onClick = { filterStatus = null },
                        label = { Text(if (s.isArabic) "الكل (${visits.size})" else "All (${visits.size})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BubblesCyan,
                            selectedLabelColor = Color.White,
                            containerColor = GlassCardTranslucent
                        )
                    )
                }
                items(
                    listOf(
                        VisitStatus.NOT_STARTED,
                        VisitStatus.EN_ROUTE,
                        VisitStatus.ARRIVED,
                        VisitStatus.LATE,
                        VisitStatus.COMPLETED,
                        VisitStatus.NOT_EXECUTED
                    )
                ) { st ->
                    FilterChip(
                        selected = filterStatus == st,
                        onClick = { filterStatus = if (filterStatus == st) null else st },
                        label = { Text(if (s.isArabic) st.ar else st.en) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BubblesCyan,
                            selectedLabelColor = Color.White,
                            containerColor = GlassCardTranslucent
                        )
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                GlassSurface(cornerRadius = 20.dp) {
                    Text(
                        text = if (s.isArabic) "لا توجد زيارات مطابقة حاليًا. اضغط على «زيارة جديدة» لجدولة زيارة." else "No visits found. Tap New Visit to schedule one.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = BubblesGray,
                        modifier = Modifier.padding(18.dp)
                    )
                }
            }
        } else {
            items(filtered, key = { it.id }) { visit ->
                GlassSurface(
                    cornerRadius = 22.dp,
                    onClick = { editingVisit = rawVisits.firstOrNull { it.id == visit.id } }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(5.dp)
                                .height(78.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    Brush.verticalGradient(listOf(visit.status.color, visit.status.color.copy(alpha = 0.45f)))
                                )
                        )
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
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
                                    if (visit.isUrgent) {
                                        Surface(shape = RoundedCornerShape(10.dp), color = StatusRed.copy(alpha = 0.14f)) {
                                            Icon(Icons.Rounded.PriorityHigh, contentDescription = s.urgentTag, tint = StatusRed, modifier = Modifier.padding(3.dp).size(14.dp))
                                        }
                                    }
                                }
                                StatusChip(
                                    label = if (s.isArabic) visit.status.ar else visit.status.en,
                                    color = visit.status.color
                                )
                            }
                            Text(
                                text = if (s.isArabic) "${visit.customerNameAr} — ${visit.locationNameAr}" else "${visit.customerNameEn} — ${visit.locationNameEn}",
                                style = MaterialTheme.typography.titleMedium,
                                color = BubblesSlateDark
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Rounded.LocalShipping, contentDescription = null, tint = BubblesGray, modifier = Modifier.size(14.dp))
                                    Text(visit.driverNameAr, style = MaterialTheme.typography.bodySmall, color = BubblesGray)
                                }
                                if (visit.recurringPatternAr != null) {
                                    Surface(shape = RoundedCornerShape(10.dp), color = StatusIndigo.copy(alpha = 0.1f)) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(Icons.Rounded.EventRepeat, contentDescription = null, tint = StatusIndigo, modifier = Modifier.size(13.dp))
                                            Text(visit.recurringPatternAr, style = MaterialTheme.typography.labelSmall, color = StatusIndigo)
                                        }
                                    }
                                }
                            }
                            if (visit.delayMinutes > gracePeriodMinutes) {
                                Text(
                                    text = if (s.isArabic) "تأخير ${visit.delayMinutes} د (تجاوز سماح ${gracePeriodMinutes}د)" else "Delayed ${visit.delayMinutes}m",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = StatusAmber
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateVisitOrRecurringDialog(
            locations = locations,
            drivers = drivers,
            onDismiss = { showCreateDialog = false },
            onCreate = onCreateVisitOrRecurring
        )
    }

    editingVisit?.let { raw ->
        EditVisitDialog(
            visit = raw,
            drivers = drivers,
            onDismiss = { editingVisit = null },
            onSave = { newDriver, newStatus, newPriority ->
                onUpdateVisitAdmin(raw, newDriver, newStatus, newPriority)
            }
        )
    }
}

@Composable
fun CustomersLocationsTab(
    customers: List<CustomerRecord>,
    rawCustomers: List<Customer>,
    locations: List<BranchLocation>,
    onCreateCustomerWithBranch: (
        customerName: String,
        primaryPhone: String,
        secondaryPhone: String?,
        customerNotes: String?,
        branchName: String,
        city: String,
        district: String,
        address: String,
        lat: Double,
        lng: Double,
        instructions: String?
    ) -> Unit,
    onAddBranch: (
        customer: Customer,
        branchName: String,
        primaryPhone: String,
        city: String,
        district: String,
        address: String,
        lat: Double,
        lng: Double,
        instructions: String?
    ) -> Unit,
    onShowToast: (String) -> Unit
) {
    val s = LocalAppStrings.current
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var showCreateCustomer by remember { mutableStateOf(false) }
    var branchTargetCustomer by remember { mutableStateOf<Customer?>(null) }

    val filteredCustomers = remember(customers, query) {
        if (query.isBlank()) customers
        else customers.filter {
            it.nameAr.contains(query, ignoreCase = true) ||
                it.nameEn.contains(query, ignoreCase = true) ||
                it.primaryPhone.contains(query)
        }
    }

    fun copyPhone(phone: String) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("phone", phone))
        onShowToast("${s.copiedMsg}: $phone")
    }

    fun dialPhone(phone: String) {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    GradientIconBadge(icon = Icons.Rounded.Groups, accentColor = BubblesCyanDark, size = 38.dp, iconSize = 20.dp)
                    Text(
                        text = if (s.isArabic) "العملاء والمواقع المتعددة" else "Customers & Locations",
                        style = MaterialTheme.typography.titleLarge,
                        color = BubblesSlateDark
                    )
                }
                Button(
                    onClick = { showCreateCustomer = true },
                    colors = ButtonDefaults.buttonColors(containerColor = BubblesCyan),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("add_customer_btn")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(s.newCustomerBtn)
                }
            }
        }

        item {
            GlassSurface(cornerRadius = 20.dp) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(if (s.isArabic) "بحث بالاسم أو جوال +966..." else "Search name or +966 phone...") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = BubblesCyanDark) },
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (filteredCustomers.isEmpty()) {
            item {
                GlassSurface(cornerRadius = 20.dp) {
                    Text(
                        text = if (s.isArabic) "لا يوجد عملاء مسجلون بعد. أضف عميلاً وموقعه الأول من الزر أعلاه." else "No customers yet. Add your first customer above.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = BubblesGray,
                        modifier = Modifier.padding(18.dp)
                    )
                }
            }
        } else {
            items(filteredCustomers, key = { it.id }) { customer ->
                val customerBranches = locations.filter { it.customerId == customer.id }
                val rawCust = rawCustomers.firstOrNull { it.id == customer.id }
                GlassSurface(cornerRadius = 22.dp) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(Brush.linearGradient(listOf(BubblesCyanBright, BubblesCyan, BubblesCyanDark))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = customer.nameAr.take(1),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Column {
                                    Text(
                                        text = if (s.isArabic) customer.nameAr else customer.nameEn,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = BubblesSlateDark
                                    )
                                    Text(
                                        text = customer.primaryPhone,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = BubblesCyanDark
                                    )
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                Surface(shape = CircleShape, color = Color(0xFFEDF5F7)) {
                                    IconButton(onClick = { copyPhone(customer.primaryPhone) }) {
                                        Icon(Icons.Rounded.ContentCopy, contentDescription = s.copyPhone, tint = BubblesGray, modifier = Modifier.size(18.dp))
                                    }
                                }
                                Surface(shape = CircleShape, color = BubblesCyanSoft) {
                                    IconButton(onClick = { dialPhone(customer.primaryPhone) }) {
                                        Icon(Icons.Rounded.Call, contentDescription = s.callCustomer, tint = BubblesCyanDark, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (s.isArabic) "المواقع المرتبطة (${customerBranches.size}):" else "Linked Locations (${customerBranches.size}):",
                                style = MaterialTheme.typography.labelLarge,
                                color = BubblesGray
                            )
                            if (rawCust != null) {
                                TextButton(onClick = { branchTargetCustomer = rawCust }) {
                                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(3.dp))
                                    Text(if (s.isArabic) "إضافة فرع" else "Add Branch", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }

                        customerBranches.forEach { branch ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = BubblesCyanSoft.copy(alpha = 0.72f),
                                border = BorderStroke(1.dp, BubblesCyanBorder)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(Icons.Rounded.Business, contentDescription = null, tint = BubblesCyanDark, modifier = Modifier.size(20.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (s.isArabic) "${branch.nameAr} (${branch.districtAr.ifBlank { branch.cityAr }})" else branch.nameEn,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = BubblesSlateDark
                                        )
                                        Text(
                                            text = "${branch.primaryPhone} • ${branch.instructionsAr.ifBlank { branch.addressAr }}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = BubblesGray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateCustomer) {
        CreateCustomerBranchDialog(
            onDismiss = { showCreateCustomer = false },
            onCreate = onCreateCustomerWithBranch
        )
    }

    branchTargetCustomer?.let { target ->
        AddBranchDialog(
            customer = target,
            onDismiss = { branchTargetCustomer = null },
            onAdd = { branchName, phone, city, district, address, lat, lng, instructions ->
                onAddBranch(target, branchName, phone, city, district, address, lat, lng, instructions)
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DriversTrackingTab(
    drivers: List<DriverProfile>,
    rawDrivers: List<Driver>,
    gpsHistory7Days: List<GpsLocationPoint>,
    onCreateDriver: (name: String, username: String, phone: String, email: String?, accessCode: String?, status: String) -> Unit,
    onUpdateDriverStatus: (Driver, String) -> Unit,
    onSoftDeleteDriver: (Driver) -> Unit,
    onShowToast: (String) -> Unit
) {
    val s = LocalAppStrings.current
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0.45f) }
    var playbackSpeed by remember { mutableIntStateOf(1) }
    var selectedPlaybackDriverId by remember(drivers) { mutableStateOf(drivers.firstOrNull()?.id) }
    var showCreateDriverDialog by remember { mutableStateOf(false) }
    var statusDialogDriver by remember { mutableStateOf<Driver?>(null) }
    var deleteConfirmDriver by remember { mutableStateOf<Driver?>(null) }

    val playbackPoints = remember(gpsHistory7Days, selectedPlaybackDriverId) {
        val filtered = if (selectedPlaybackDriverId == null) gpsHistory7Days
        else gpsHistory7Days.filter { it.driverId == selectedPlaybackDriverId }
        filtered.toPlaybackStops()
    }

    LaunchedEffect(isPlaying, playbackSpeed, playbackPoints.size) {
        while (isPlaying && playbackPoints.size >= 2) {
            delay(120L / playbackSpeed)
            progress = if (progress >= 1f) 0f else (progress + 0.02f).coerceAtMost(1f)
        }
    }

    fun copyAccessCode(code: String) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("driver_code", code))
        onShowToast(if (s.isArabic) "تم نسخ كود الدخول: $code" else "Copied access code: $code")
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    GradientIconBadge(icon = Icons.Rounded.Route, accentColor = BubblesCyanDark, size = 38.dp, iconSize = 20.dp)
                    Text(s.routePlaybackTitle, style = MaterialTheme.typography.titleLarge, color = BubblesSlateDark)
                }
                Button(
                    onClick = { showCreateDriverDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = BubblesCyan),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("add_driver_btn")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(s.newDriverBtn)
                }
            }
        }

        if (drivers.isNotEmpty()) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(drivers, key = { it.id }) { drv ->
                        FilterChip(
                            selected = selectedPlaybackDriverId == drv.id,
                            onClick = {
                                selectedPlaybackDriverId = drv.id
                                progress = 0f
                                isPlaying = false
                            },
                            label = { Text(drv.nameAr) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BubblesCyan,
                                selectedLabelColor = Color.White,
                                containerColor = GlassCardTranslucent
                            )
                        )
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                RoutePlaybackMapCanvas(points = playbackPoints, progress = progress)
                GlassSurface(cornerRadius = 18.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(shape = CircleShape, color = BubblesCyanSoft) {
                            IconButton(
                                onClick = { if (playbackPoints.size >= 2) isPlaying = !isPlaying },
                                modifier = Modifier.testTag("playback_toggle_button")
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = "Playback",
                                    tint = BubblesCyanDark
                                )
                            }
                        }
                        Slider(
                            value = progress,
                            onValueChange = { progress = it },
                            colors = SliderDefaults.colors(thumbColor = BubblesCyan, activeTrackColor = BubblesCyan),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedButton(
                            onClick = { playbackSpeed = if (playbackSpeed == 1) 2 else 1 },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("${playbackSpeed}x", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GradientIconBadge(icon = Icons.Rounded.Badge, accentColor = StatusEmerald, size = 32.dp, iconSize = 18.dp)
                Text(
                    text = if (s.isArabic) "إدارة المناديب وأكواد الدخول" else "Drivers & Access Codes",
                    style = MaterialTheme.typography.titleMedium,
                    color = BubblesSlateDark
                )
            }
        }

        if (drivers.isEmpty()) {
            item {
                GlassSurface(cornerRadius = 20.dp) {
                    Text(
                        text = if (s.isArabic) "لا يوجد مناديب مضافون بعد. أضف المندوب الأول لتوليد كود دخول فوري له." else "No drivers added yet. Add a driver above to generate an access code.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = BubblesGray,
                        modifier = Modifier.padding(18.dp)
                    )
                }
            }
        } else {
            items(drivers, key = { it.id }) { driver ->
                val rawDriver = rawDrivers.firstOrNull { it.id == driver.id }
                GlassSurface(
                    cornerRadius = 22.dp,
                    onClick = { if (rawDriver != null) statusDialogDriver = rawDriver }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_driver_avatar_1794774063821),
                                contentDescription = driver.nameAr,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                            )
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = if (s.isArabic) driver.nameAr else driver.nameEn,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = BubblesSlateDark
                                    )
                                    StatusChip(
                                        label = if (s.isArabic) driver.status.ar else driver.status.en,
                                        color = driver.status.color
                                    )
                                }
                                Text(
                                    text = "${driver.phone} • @${driver.username}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = BubblesGray
                                )
                                Text(
                                    text = "GPS: ${if (s.isArabic) driver.lastGpsTextAr else driver.lastGpsTextEn} (${driver.accuracyMeters}m)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BubblesCyanDark
                                )
                            }
                            if (rawDriver != null) {
                                Surface(shape = CircleShape, color = StatusRed.copy(alpha = 0.10f)) {
                                    IconButton(onClick = { deleteConfirmDriver = rawDriver }) {
                                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Soft delete", tint = StatusRed, modifier = Modifier.size(19.dp))
                                    }
                                }
                            }
                        }

                        if (driver.accessCode.isNotBlank()) {
                            Surface(
                                onClick = { copyAccessCode(driver.accessCode) },
                                shape = RoundedCornerShape(14.dp),
                                color = BubblesCyanSoft.copy(alpha = 0.85f),
                                border = BorderStroke(1.dp, BubblesCyanBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(Icons.Rounded.Key, contentDescription = null, tint = BubblesCyanDark, modifier = Modifier.size(16.dp))
                                        Text(
                                            text = if (s.isArabic) "كود دخول المندوب: ${driver.accessCode}" else "Access Code: ${driver.accessCode}",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = BubblesCyanDark,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy", tint = BubblesCyanDark, modifier = Modifier.size(15.dp))
                                        Text(
                                            text = if (s.isArabic) "نسخ" else "Copy",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = BubblesCyanDark
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDriverDialog) {
        CreateDriverDialog(
            onDismiss = { showCreateDriverDialog = false },
            onCreate = onCreateDriver
        )
    }

    statusDialogDriver?.let { raw ->
        AlertDialog(
            onDismissRequest = { statusDialogDriver = null },
            containerColor = BubblesWhite,
            title = { Text(if (s.isArabic) "تغيير حالة: ${raw.name}" else "Driver Status: ${raw.name}") },
            text = {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    DriverStatus.entries.forEach { st ->
                        FilterChip(
                            selected = raw.status == st.name,
                            onClick = {
                                onUpdateDriverStatus(raw, st.name)
                                statusDialogDriver = null
                            },
                            label = { Text(if (s.isArabic) st.ar else st.en) }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { statusDialogDriver = null }) { Text(if (s.isArabic) "إغلاق" else "Close") }
            }
        )
    }

    deleteConfirmDriver?.let { raw ->
        AlertDialog(
            onDismissRequest = { deleteConfirmDriver = null },
            containerColor = BubblesWhite,
            title = { Text(if (s.isArabic) "إيقاف وحفظ سجل المندوب" else "Soft-Delete Driver") },
            text = {
                Text(
                    text = if (s.isArabic) "سيتم تعطيل حساب ${raw.name} وإخفاؤه من التعيين الجديد مع الاحتفاظ بكامل زياراته وسجلات الدفع والتتبع التاريخية." else "Disables ${raw.name} while preserving all historical visits, payments, and GPS records."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSoftDeleteDriver(raw)
                        deleteConfirmDriver = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusRed)
                ) { Text(if (s.isArabic) "تأكيد الإيقاف" else "Disable Driver") }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmDriver = null }) { Text(if (s.isArabic) "إلغاء" else "Cancel") }
            }
        )
    }
}

@Composable
fun ReportsAuditTab(
    visits: List<VisitItem>,
    drivers: List<DriverProfile>,
    paymentRecords: List<PaymentRecord>,
    auditLogs: List<AuditEntry>,
    onShowToast: (String) -> Unit
) {
    val s = LocalAppStrings.current
    val context = LocalContext.current
    var showAudit by remember { mutableStateOf(false) }

    val breakdown = remember(visits, paymentRecords) {
        val methodColor: (PaymentMethod) -> Color = { m ->
            when (m) {
                PaymentMethod.MADA -> BubblesCyan
                PaymentMethod.CASH -> StatusEmerald
                PaymentMethod.BANK_TRANSFER -> StatusIndigo
                PaymentMethod.VISA -> Color(0xFF0284C7)
                PaymentMethod.MASTERCARD -> Color(0xFF0EA5E9)
                PaymentMethod.GCC_NET -> Color(0xFF14B8A6)
                PaymentMethod.UNPAID -> StatusRed
            }
        }
        PaymentMethod.entries.map { method ->
            val sum = if (method == PaymentMethod.UNPAID) {
                visits.sumOf { (it.amountDue - it.amountCollected).coerceAtLeast(0.0) }
            } else if (paymentRecords.isNotEmpty()) {
                paymentRecords.filter { it.paymentMethod == method.name }.sumOf { it.amountCollected }
            } else {
                visits.filter { it.paymentMethod == method }.sumOf { it.amountCollected }
            }
            Triple(method, sum.toInt(), methodColor(method))
        }
    }

    val maxBreakdown = remember(breakdown) {
        (breakdown.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1).toFloat()
    }

    val weeklyRevenue = remember(visits, paymentRecords) {
        val cal = Calendar.getInstance()
        val totals = FloatArray(7)
        val now = System.currentTimeMillis()
        for (i in 0..6) {
            val dayStart = now - (6 - i) * 86_400_000L
            cal.timeInMillis = dayStart
            val dayVal = if (paymentRecords.isNotEmpty()) {
                paymentRecords.filter { DateTimeFormatting.isSameLocalDay(it.recordedAtEpochMs, dayStart) }
                    .sumOf { it.amountCollected }.toFloat()
            } else {
                visits.filter { DateTimeFormatting.isSameLocalDay(it.scheduledAtEpochMs, dayStart) }
                    .sumOf { it.amountCollected }.toFloat()
            }
            totals[i] = dayVal
        }
        totals.toList()
    }
    val dayLabels = if (s.isArabic) listOf("6-", "5-", "4-", "3-", "2-", "أمس", "اليوم") else listOf("-6d", "-5d", "-4d", "-3d", "-2d", "Yest", "Today")

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            GlassSurface(cornerRadius = 18.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(5.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        onClick = { showAudit = false },
                        color = if (!showAudit) BubblesCyan else Color.Transparent,
                        shape = RoundedCornerShape(13.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(modifier = Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                            Text(
                                s.financialReport,
                                color = if (!showAudit) BubblesWhite else BubblesGray,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (!showAudit) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                    Surface(
                        onClick = { showAudit = true },
                        color = if (showAudit) BubblesCyan else Color.Transparent,
                        shape = RoundedCornerShape(13.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(modifier = Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                            Text(
                                s.auditLogTitle,
                                color = if (showAudit) BubblesWhite else BubblesGray,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (showAudit) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        if (!showAudit) {
            item {
                GlassSurface(cornerRadius = 24.dp) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            GradientIconBadge(icon = Icons.Rounded.Assessment, accentColor = BubblesCyanDark, size = 34.dp, iconSize = 18.dp)
                            Text(
                                text = if (s.isArabic) "منحنى التحصيل خلال 7 أيام (ر.س)" else "7-Day Collection Trend (SAR)",
                                style = MaterialTheme.typography.titleMedium,
                                color = BubblesSlateDark
                            )
                        }
                        val maxVal = (weeklyRevenue.maxOrNull() ?: 1f).coerceAtLeast(1f)
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(116.dp)
                        ) {
                            val barCount = weeklyRevenue.size
                            val spacing = size.width / barCount
                            val barWidth = spacing * 0.48f
                            weeklyRevenue.forEachIndexed { idx, v ->
                                val barH = ((v / maxVal) * (size.height * 0.88f)).coerceAtLeast(8f)
                                val left = idx * spacing + (spacing - barWidth) / 2f
                                val top = size.height - barH
                                drawRoundRect(
                                    color = BubblesCyanSoft,
                                    topLeft = Offset(left, 0f),
                                    size = Size(barWidth, size.height),
                                    cornerRadius = CornerRadius(14f, 14f)
                                )
                                drawRoundRect(
                                    brush = Brush.verticalGradient(listOf(BubblesCyanBright, BubblesCyan, BubblesCyanDark)),
                                    topLeft = Offset(left, top),
                                    size = Size(barWidth, barH),
                                    cornerRadius = CornerRadius(14f, 14f)
                                )
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                            dayLabels.forEach { d ->
                                Text(d, style = MaterialTheme.typography.labelSmall, color = BubblesGray)
                            }
                        }
                    }
                }
            }

            if (drivers.isNotEmpty()) {
                item {
                    GlassSurface(cornerRadius = 24.dp) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = if (s.isArabic) "تقرير التزام وأداء المناديب" else "Driver Performance & Punctuality",
                                style = MaterialTheme.typography.titleMedium,
                                color = BubblesSlateDark
                            )
                            drivers.forEach { d ->
                                val totalD = (d.completedToday + d.lateToday).coerceAtLeast(1)
                                val onTimePct = (((d.completedToday - d.lateToday).coerceAtLeast(0)) * 100) / totalD
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(d.nameAr, style = MaterialTheme.typography.bodyMedium, color = BubblesSlateDark, fontWeight = FontWeight.SemiBold)
                                        Text(
                                            text = if (s.isArabic) "${d.completedToday} مكتمل • التزام $onTimePct% • ${d.collectedToday.toInt()} ر.س"
                                            else "${d.completedToday} done • $onTimePct% • ${d.collectedToday.toInt()} SAR",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = BubblesCyanDark
                                        )
                                    }
                                    LinearProgressIndicator(
                                        progress = { (onTimePct / 100f).coerceIn(0.05f, 1f) },
                                        color = if (onTimePct >= 80) StatusEmerald else StatusAmber,
                                        trackColor = Color(0xFFE7F5F8),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(7.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                GlassSurface(cornerRadius = 24.dp) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = if (s.isArabic) "توزيع المدفوعات حسب الطريقة" else "Collections by Payment Method",
                            style = MaterialTheme.typography.titleMedium,
                            color = BubblesSlateDark
                        )
                        breakdown.forEach { (method, amount, color) ->
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(if (s.isArabic) method.ar else method.en, style = MaterialTheme.typography.bodyMedium)
                                    Text("$amount ${s.sar}", style = MaterialTheme.typography.titleSmall, color = color)
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(10.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFFEDF5F7))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth((amount / maxBreakdown).coerceIn(if (amount > 0) 0.08f else 0.02f, 1f))
                                            .height(10.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(color)
                                    )
                                }
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val headers = listOf("Visit", "Customer", "Branch", "Driver", "Status", "Invoice", "Due SAR", "Collected SAR", "Payment Method")
                                    val rows = visits.map { v ->
                                        listOf(
                                            v.id,
                                            v.customerNameAr,
                                            v.locationNameAr,
                                            v.driverNameAr,
                                            v.status.name,
                                            v.invoiceNumber,
                                            v.amountDue.toInt().toString(),
                                            v.amountCollected.toInt().toString(),
                                            v.paymentMethod.name
                                        )
                                    }
                                    val ok = ReportExporter.shareExcelCsv(
                                        context = context,
                                        fileNamePrefix = "bubbles_financial_report",
                                        headers = headers,
                                        rows = rows,
                                        chooserTitle = s.exportExcel
                                    )
                                    if (!ok) onShowToast(if (s.isArabic) "تعذر تصدير ملف CSV" else "CSV export failed")
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(s.exportExcel)
                            }
                            Button(
                                onClick = {
                                    val totalCollected = visits.sumOf { it.amountCollected }.toInt()
                                    val totalRemaining = visits.sumOf { (it.amountDue - it.amountCollected).coerceAtLeast(0.0) }.toInt()
                                    val lines = buildList {
                                        add("Total Visits: ${visits.size} | Collected: $totalCollected SAR | Uncollected: $totalRemaining SAR")
                                        breakdown.forEach { (m, amt, _) -> add("${m.en}: $amt SAR") }
                                        visits.take(40).forEach { v ->
                                            add("${v.scheduledTime} | ${v.locationNameEn} | ${v.status.name} | INV:${v.invoiceNumber.ifBlank { "-" }} | ${v.amountCollected.toInt()} SAR")
                                        }
                                    }
                                    val ok = ReportExporter.sharePdfReport(
                                        context = context,
                                        fileNamePrefix = "bubbles_report",
                                        title = "Operations & Financial Report",
                                        subtitle = "Generated ${DateTimeFormatting.formatDateTime(System.currentTimeMillis())}",
                                        lines = lines,
                                        chooserTitle = s.exportPdf
                                    )
                                    if (!ok) onShowToast(if (s.isArabic) "تعذر تصدير ملف PDF" else "PDF export failed")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = BubblesCyan),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(s.exportPdf)
                            }
                        }
                    }
                }
            }
        } else {
            if (auditLogs.isEmpty()) {
                item {
                    GlassSurface(cornerRadius = 18.dp) {
                        Text(
                            text = if (s.isArabic) "سجل العمليات التاريخي فارغ حاليًا." else "Audit log is currently empty.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = BubblesGray,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(auditLogs, key = { it.id }) { entry ->
                    GlassSurface(cornerRadius = 18.dp) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            GradientIconBadge(icon = Icons.Rounded.History, accentColor = BubblesCyanDark, size = 36.dp, iconSize = 18.dp)
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(entry.actionAr, style = MaterialTheme.typography.titleSmall, color = BubblesSlateDark)
                                    Text(entry.timestamp, style = MaterialTheme.typography.labelSmall, color = BubblesCyanDark)
                                }
                                Text(entry.actorName, style = MaterialTheme.typography.labelMedium, color = BubblesGray)
                                Text(
                                    text = "${entry.oldValue}  ←  ${entry.newValue}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = BubblesCyanDark
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
