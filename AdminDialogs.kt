package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Apartment
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.LocationCity
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Notes
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.AppSettings
import com.example.data.model.Customer
import com.example.data.model.CustomerLocation
import com.example.data.model.Driver
import com.example.data.model.Visit
import com.example.data.session.ActiveSession
import com.example.data.validation.WeekdayToken
import com.example.ui.components.GradientIconBadge
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.model.DriverStatus
import com.example.ui.model.OperationType
import com.example.ui.model.VisitStatus
import com.example.ui.theme.BubblesCyan
import com.example.ui.theme.BubblesCyanBorder
import com.example.ui.theme.BubblesCyanDark
import com.example.ui.theme.BubblesCyanSoft
import com.example.ui.theme.BubblesDivider
import com.example.ui.theme.BubblesGray
import com.example.ui.theme.BubblesSlateDark
import com.example.ui.theme.StatusEmerald
import com.example.ui.theme.StatusIndigo
import java.util.Calendar

private val GlassDialogContainer = Color(0xF5FFFFFF)
private val DialogShape = RoundedCornerShape(26.dp)
private val FieldShape = RoundedCornerShape(16.dp)

@Composable
private fun glassFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = BubblesCyan,
    unfocusedBorderColor = BubblesCyanBorder,
    focusedContainerColor = Color(0xFFF6FCFD),
    unfocusedContainerColor = Color(0xFFF9FEFF)
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateDriverDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, username: String, phone: String, email: String?, accessCode: String?, status: String) -> Unit
) {
    val s = LocalAppStrings.current
    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("+9665") }
    var accessCode by remember { mutableStateOf("") }
    var status by remember { mutableStateOf(DriverStatus.ACTIVE) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GlassDialogContainer,
        shape = DialogShape,
        icon = {
            GradientIconBadge(
                icon = Icons.Rounded.Badge,
                accentColor = BubblesCyanDark,
                size = 46.dp,
                iconSize = 24.dp
            )
        },
        title = {
            Text(s.newDriverBtn, fontWeight = FontWeight.Bold, color = BubblesSlateDark)
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 440.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(if (s.isArabic) "اسم المندوب *" else "Driver Name *") },
                    leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null, tint = BubblesCyanDark) },
                    singleLine = true,
                    shape = FieldShape,
                    colors = glassFieldColors(),
                    modifier = Modifier.fillMaxWidth().testTag("driver_name_input")
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text(if (s.isArabic) "اسم المستخدم *" else "Username *") },
                    leadingIcon = { Icon(Icons.Rounded.Badge, contentDescription = null, tint = BubblesCyanDark) },
                    singleLine = true,
                    shape = FieldShape,
                    colors = glassFieldColors(),
                    modifier = Modifier.fillMaxWidth().testTag("driver_username_input")
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(if (s.isArabic) "رقم الجوال السعودي (+9665...) *" else "Saudi Phone (+9665...) *") },
                    leadingIcon = { Icon(Icons.Rounded.Phone, contentDescription = null, tint = BubblesCyanDark) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    shape = FieldShape,
                    colors = glassFieldColors(),
                    modifier = Modifier.fillMaxWidth().testTag("driver_phone_input")
                )
                OutlinedTextField(
                    value = accessCode,
                    onValueChange = { accessCode = it.filter { c -> c.isDigit() }.take(6) },
                    label = { Text(if (s.isArabic) "كود دخول المندوب (اختياري - يولّد تلقائيًا)" else "Driver Access Code (optional)") },
                    leadingIcon = { Icon(Icons.Rounded.Key, contentDescription = null, tint = BubblesCyanDark) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    shape = FieldShape,
                    colors = glassFieldColors(),
                    modifier = Modifier.fillMaxWidth().testTag("driver_code_input")
                )
                Text(if (s.isArabic) "حالة المندوب" else "Status", style = MaterialTheme.typography.labelLarge, color = BubblesSlateDark)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(DriverStatus.ACTIVE, DriverStatus.OFF_SHIFT, DriverStatus.ON_LEAVE, DriverStatus.INACTIVE).forEach { st ->
                        FilterChip(
                            selected = status == st,
                            onClick = { status = st },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BubblesCyan,
                                selectedLabelColor = Color.White
                            ),
                            label = { Text(if (s.isArabic) st.ar else st.en, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && phone.isNotBlank()) {
                        onCreate(name, username, phone, null, accessCode.takeIf { it.length >= 4 }, status.name)
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BubblesCyan),
                shape = RoundedCornerShape(14.dp)
            ) { Text(if (s.isArabic) "حفظ المندوب" else "Save Driver") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(if (s.isArabic) "إلغاء" else "Cancel") }
        }
    )
}

@Composable
fun CreateCustomerBranchDialog(
    onDismiss: () -> Unit,
    onCreate: (
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
    ) -> Unit
) {
    val s = LocalAppStrings.current
    var customerName by remember { mutableStateOf("") }
    var primaryPhone by remember { mutableStateOf("+9665") }
    var secondaryPhone by remember { mutableStateOf("") }
    var branchName by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("الرياض") }
    var district by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var latText by remember { mutableStateOf("24.7136") }
    var lngText by remember { mutableStateOf("46.6753") }
    var instructions by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GlassDialogContainer,
        shape = DialogShape,
        icon = {
            GradientIconBadge(icon = Icons.Rounded.Apartment, accentColor = BubblesCyanDark, size = 46.dp, iconSize = 24.dp)
        },
        title = { Text(s.newCustomerBtn, fontWeight = FontWeight.Bold, color = BubblesSlateDark) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 440.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                OutlinedTextField(
                    value = customerName,
                    onValueChange = { customerName = it },
                    label = { Text(if (s.isArabic) "اسم العميل (اختياري)" else "Customer Name (optional)") },
                    leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null, tint = BubblesCyanDark) },
                    singleLine = true,
                    shape = FieldShape,
                    colors = glassFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = primaryPhone,
                    onValueChange = { primaryPhone = it },
                    label = { Text(if (s.isArabic) "الجوال الأساسي (+9665...) *" else "Primary Phone *") },
                    leadingIcon = { Icon(Icons.Rounded.Phone, contentDescription = null, tint = BubblesCyanDark) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    shape = FieldShape,
                    colors = glassFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = secondaryPhone,
                    onValueChange = { secondaryPhone = it },
                    label = { Text(if (s.isArabic) "جوال إضافي (اختياري)" else "Secondary Phone") },
                    leadingIcon = { Icon(Icons.Rounded.Phone, contentDescription = null, tint = BubblesGray) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    shape = FieldShape,
                    colors = glassFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = branchName,
                    onValueChange = { branchName = it },
                    label = { Text(if (s.isArabic) "اسم الموقع / الفرع *" else "Branch / Location Name *") },
                    leadingIcon = { Icon(Icons.Rounded.Apartment, contentDescription = null, tint = BubblesCyanDark) },
                    singleLine = true,
                    shape = FieldShape,
                    colors = glassFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = city,
                        onValueChange = { city = it },
                        label = { Text(if (s.isArabic) "المدينة" else "City") },
                        singleLine = true,
                        shape = FieldShape,
                        colors = glassFieldColors(),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = district,
                        onValueChange = { district = it },
                        label = { Text(if (s.isArabic) "الحي" else "District") },
                        singleLine = true,
                        shape = FieldShape,
                        colors = glassFieldColors(),
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text(if (s.isArabic) "العنوان التفصيلي" else "Address") },
                    leadingIcon = { Icon(Icons.Rounded.LocationCity, contentDescription = null, tint = BubblesCyanDark) },
                    singleLine = true,
                    shape = FieldShape,
                    colors = glassFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(value = latText, onValueChange = { latText = it }, label = { Text("Lat") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, shape = FieldShape, colors = glassFieldColors(), modifier = Modifier.weight(1f))
                    OutlinedTextField(value = lngText, onValueChange = { lngText = it }, label = { Text("Lng") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, shape = FieldShape, colors = glassFieldColors(), modifier = Modifier.weight(1f))
                }
                OutlinedTextField(
                    value = instructions,
                    onValueChange = { instructions = it },
                    label = { Text(if (s.isArabic) "تعليمات الوصول" else "Access Instructions") },
                    leadingIcon = { Icon(Icons.Rounded.Notes, contentDescription = null, tint = BubblesGray) },
                    shape = FieldShape,
                    colors = glassFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val lat = latText.toDoubleOrNull() ?: 24.7136
                    val lng = lngText.toDoubleOrNull() ?: 46.6753
                    if (primaryPhone.isNotBlank()) {
                        onCreate(
                            customerName,
                            primaryPhone,
                            secondaryPhone.takeIf { it.isNotBlank() },
                            null,
                            branchName.ifBlank { "الفرع الرئيسي" },
                            city,
                            district,
                            address,
                            lat,
                            lng,
                            instructions.takeIf { it.isNotBlank() }
                        )
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BubblesCyan),
                shape = RoundedCornerShape(14.dp)
            ) { Text(if (s.isArabic) "حفظ" else "Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(if (s.isArabic) "إلغاء" else "Cancel") }
        }
    )
}

@Composable
fun AddBranchDialog(
    customer: Customer,
    onDismiss: () -> Unit,
    onAdd: (branchName: String, phone: String, city: String, district: String, address: String, lat: Double, lng: Double, instructions: String?) -> Unit
) {
    val s = LocalAppStrings.current
    var branchName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf(customer.primaryPhone) }
    var city by remember { mutableStateOf("الرياض") }
    var district by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var latText by remember { mutableStateOf("24.7500") }
    var lngText by remember { mutableStateOf("46.6400") }
    var instructions by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GlassDialogContainer,
        shape = DialogShape,
        icon = {
            GradientIconBadge(icon = Icons.Rounded.LocationOn, accentColor = BubblesCyanDark, size = 46.dp, iconSize = 24.dp)
        },
        title = {
            Text(
                if (s.isArabic) "إضافة فرع لـ ${customer.name.ifBlank { customer.primaryPhone }}" else "Add Branch",
                fontWeight = FontWeight.Bold,
                color = BubblesSlateDark
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(value = branchName, onValueChange = { branchName = it }, label = { Text(if (s.isArabic) "اسم الفرع *" else "Branch Name *") }, singleLine = true, shape = FieldShape, colors = glassFieldColors(), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text(if (s.isArabic) "رقم الجوال" else "Phone") }, singleLine = true, shape = FieldShape, colors = glassFieldColors(), modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(value = city, onValueChange = { city = it }, label = { Text(if (s.isArabic) "المدينة" else "City") }, singleLine = true, shape = FieldShape, colors = glassFieldColors(), modifier = Modifier.weight(1f))
                    OutlinedTextField(value = district, onValueChange = { district = it }, label = { Text(if (s.isArabic) "الحي" else "District") }, singleLine = true, shape = FieldShape, colors = glassFieldColors(), modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text(if (s.isArabic) "العنوان" else "Address") }, singleLine = true, shape = FieldShape, colors = glassFieldColors(), modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(value = latText, onValueChange = { latText = it }, label = { Text("Lat") }, singleLine = true, shape = FieldShape, colors = glassFieldColors(), modifier = Modifier.weight(1f))
                    OutlinedTextField(value = lngText, onValueChange = { lngText = it }, label = { Text("Lng") }, singleLine = true, shape = FieldShape, colors = glassFieldColors(), modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = instructions, onValueChange = { instructions = it }, label = { Text(if (s.isArabic) "تعليمات الوصول" else "Access Instructions") }, shape = FieldShape, colors = glassFieldColors(), modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (branchName.isNotBlank()) {
                        onAdd(
                            branchName,
                            phone,
                            city,
                            district,
                            address,
                            latText.toDoubleOrNull() ?: 24.75,
                            lngText.toDoubleOrNull() ?: 46.64,
                            instructions.takeIf { it.isNotBlank() }
                        )
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BubblesCyan),
                shape = RoundedCornerShape(14.dp)
            ) { Text(if (s.isArabic) "إضافة" else "Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(if (s.isArabic) "إلغاء" else "Cancel") }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateVisitOrRecurringDialog(
    locations: List<CustomerLocation>,
    drivers: List<Driver>,
    onDismiss: () -> Unit,
    onCreate: (
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
    ) -> Unit
) {
    val s = LocalAppStrings.current
    var selectedLocation by remember(locations) { mutableStateOf(locations.firstOrNull()) }
    var selectedDriver by remember(drivers) { mutableStateOf(drivers.firstOrNull()) }
    var opType by remember { mutableStateOf(OperationType.BOTH) }
    var isUrgent by remember { mutableStateOf(false) }
    var timeHHmm by remember { mutableStateOf("10:30") }
    var notes by remember { mutableStateOf("") }
    var isRecurring by remember { mutableStateOf(false) }
    var selectedDays by remember { mutableStateOf(setOf(WeekdayToken.SUN, WeekdayToken.TUE, WeekdayToken.THU)) }
    var isContinuous by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GlassDialogContainer,
        shape = DialogShape,
        icon = {
            GradientIconBadge(icon = Icons.Rounded.Schedule, accentColor = StatusIndigo, size = 46.dp, iconSize = 24.dp)
        },
        title = { Text(s.newVisitBtn, fontWeight = FontWeight.Bold, color = BubblesSlateDark) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (locations.isEmpty() || drivers.isEmpty()) {
                    Text(
                        text = if (s.isArabic) "يرجى إضافة عميل وموقع ومندوب واحد على الأقل قبل جدولة الزيارة." else "Please add at least one customer location and one driver first.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = BubblesGray
                    )
                } else {
                    Text(if (s.isArabic) "اختر الموقع / الفرع" else "Select Location", style = MaterialTheme.typography.labelLarge, color = BubblesSlateDark)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        locations.forEach { loc ->
                            FilterChip(
                                selected = selectedLocation?.id == loc.id,
                                onClick = { selectedLocation = loc },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = BubblesCyan, selectedLabelColor = Color.White),
                                label = { Text("${loc.customerName ?: ""} - ${loc.branchName}", style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    Text(if (s.isArabic) "اختر المندوب" else "Select Driver", style = MaterialTheme.typography.labelLarge, color = BubblesSlateDark)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        drivers.forEach { drv ->
                            FilterChip(
                                selected = selectedDriver?.id == drv.id,
                                onClick = { selectedDriver = drv },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = BubblesCyan, selectedLabelColor = Color.White),
                                label = { Text(drv.name, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    Text(if (s.isArabic) "نوع العملية" else "Operation", style = MaterialTheme.typography.labelLarge, color = BubblesSlateDark)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OperationType.entries.forEach { op ->
                            FilterChip(
                                selected = opType == op,
                                onClick = { opType = op },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = BubblesCyan, selectedLabelColor = Color.White),
                                label = { Text(if (s.isArabic) op.ar else op.en, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = timeHHmm,
                        onValueChange = { timeHHmm = it },
                        label = { Text(if (s.isArabic) "الوقت (HH:mm مثال 14:30)" else "Time (HH:mm)") },
                        leadingIcon = { Icon(Icons.Rounded.Schedule, contentDescription = null, tint = BubblesCyanDark) },
                        singleLine = true,
                        shape = FieldShape,
                        colors = glassFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(s.urgentTag, color = BubblesSlateDark)
                        Switch(checked = isUrgent, onCheckedChange = { isUrgent = it }, colors = SwitchDefaults.colors(checkedTrackColor = BubblesCyan))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (s.isArabic) "جدولة زيارة متكررة" else "Recurring Schedule", color = BubblesSlateDark)
                        Switch(checked = isRecurring, onCheckedChange = { isRecurring = it }, colors = SwitchDefaults.colors(checkedTrackColor = BubblesCyan))
                    }

                    if (isRecurring) {
                        Text(if (s.isArabic) "أيام الأسبوع" else "Weekdays", style = MaterialTheme.typography.labelMedium, color = BubblesCyanDark)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            WeekdayToken.entries.forEach { day ->
                                FilterChip(
                                    selected = day in selectedDays,
                                    onClick = {
                                        selectedDays = if (day in selectedDays) selectedDays - day else selectedDays + day
                                    },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = StatusIndigo, selectedLabelColor = Color.White),
                                    label = { Text(if (s.isArabic) day.ar else day.en, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(if (s.isArabic) "تكرار مستمر (14 يوم متجددة)" else "Continuous (rolling)")
                            Switch(checked = isContinuous, onCheckedChange = { isContinuous = it }, colors = SwitchDefaults.colors(checkedTrackColor = BubblesCyan))
                        }
                    }

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text(if (s.isArabic) "ملاحظات الزيارة" else "Visit Notes") },
                        shape = FieldShape,
                        colors = glassFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = selectedLocation != null && selectedDriver != null,
                onClick = {
                    val loc = selectedLocation ?: return@Button
                    val drv = selectedDriver ?: return@Button
                    val parts = timeHHmm.split(":")
                    val hr = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 10
                    val min = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 30
                    val cal = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, hr)
                        set(Calendar.MINUTE, min)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val normalizedTime = "%02d:%02d".format(hr, min)
                    val csv = selectedDays.ifEmpty { setOf(WeekdayToken.SUN) }.joinToString(",") { it.code }
                    onCreate(
                        loc,
                        drv,
                        opType.name,
                        if (isUrgent) "URGENT" else "NORMAL",
                        cal.timeInMillis,
                        notes.takeIf { it.isNotBlank() },
                        isRecurring,
                        csv,
                        normalizedTime,
                        isContinuous
                    )
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = BubblesCyan),
                shape = RoundedCornerShape(14.dp)
            ) { Text(if (s.isArabic) "حفظ" else "Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(if (s.isArabic) "إلغاء" else "Cancel") }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditVisitDialog(
    visit: Visit,
    drivers: List<Driver>,
    onDismiss: () -> Unit,
    onSave: (newDriver: Driver, newStatus: String, newPriority: String) -> Unit
) {
    val s = LocalAppStrings.current
    var selectedDriver by remember(visit.id) {
        mutableStateOf(drivers.firstOrNull { it.id == visit.assignedDriverId } ?: drivers.firstOrNull())
    }
    var status by remember(visit.id) { mutableStateOf(VisitStatus.fromCode(visit.status)) }
    var isUrgent by remember(visit.id) { mutableStateOf(visit.priority == "URGENT") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GlassDialogContainer,
        shape = DialogShape,
        title = { Text(if (s.isArabic) "تعديل الزيارة: ${visit.locationName}" else "Edit Visit", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 380.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (drivers.isNotEmpty()) {
                    Text(if (s.isArabic) "تعيين المندوب" else "Assign Driver", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        drivers.forEach { drv ->
                            FilterChip(
                                selected = selectedDriver?.id == drv.id,
                                onClick = { selectedDriver = drv },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = BubblesCyan, selectedLabelColor = Color.White),
                                label = { Text(drv.name, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }
                Text(if (s.isArabic) "حالة الزيارة" else "Status", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(VisitStatus.NOT_STARTED, VisitStatus.EN_ROUTE, VisitStatus.COMPLETED, VisitStatus.CANCELLED).forEach { st ->
                        FilterChip(
                            selected = status == st,
                            onClick = { status = st },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = BubblesCyan, selectedLabelColor = Color.White),
                            label = { Text(if (s.isArabic) st.ar else st.en, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(s.urgentTag)
                    Switch(checked = isUrgent, onCheckedChange = { isUrgent = it }, colors = SwitchDefaults.colors(checkedTrackColor = BubblesCyan))
                }
            }
        },
        confirmButton = {
            Button(
                enabled = selectedDriver != null,
                onClick = {
                    selectedDriver?.let { drv ->
                        onSave(drv, status.name, if (isUrgent) "URGENT" else "NORMAL")
                    }
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = BubblesCyan),
                shape = RoundedCornerShape(14.dp)
            ) { Text(if (s.isArabic) "حفظ" else "Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(if (s.isArabic) "إلغاء" else "Cancel") }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BroadcastNotificationDialog(
    drivers: List<Driver>,
    onDismiss: () -> Unit,
    onSend: (title: String, body: String, targetUserId: String) -> Unit
) {
    val s = LocalAppStrings.current
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var targetUserId by remember { mutableStateOf("ALL") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GlassDialogContainer,
        shape = DialogShape,
        icon = {
            GradientIconBadge(icon = Icons.Rounded.Campaign, accentColor = BubblesCyanDark, size = 46.dp, iconSize = 24.dp)
        },
        title = { Text(s.sendBroadcast, fontWeight = FontWeight.Bold, color = BubblesSlateDark) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (s.isArabic) "المستلم" else "Target", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = targetUserId == "ALL",
                        onClick = { targetUserId = "ALL" },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = BubblesCyan, selectedLabelColor = Color.White),
                        label = { Text(if (s.isArabic) "جميع المناديب" else "All Drivers", style = MaterialTheme.typography.labelSmall) }
                    )
                    drivers.filter { it.id.isNotBlank() }.forEach { drv ->
                        val targetId = drv.userId.ifBlank { drv.id }
                        FilterChip(
                            selected = targetUserId == targetId,
                            onClick = { targetUserId = targetId },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = BubblesCyan, selectedLabelColor = Color.White),
                            label = { Text(drv.name, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text(if (s.isArabic) "عنوان التنبيه" else "Title") }, singleLine = true, shape = FieldShape, colors = glassFieldColors(), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = body, onValueChange = { body = it }, label = { Text(if (s.isArabic) "نص الرسالة" else "Message") }, shape = FieldShape, colors = glassFieldColors(), modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && body.isNotBlank()) {
                        onSend(title, body, targetUserId)
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BubblesCyan),
                shape = RoundedCornerShape(14.dp)
            ) { Text(if (s.isArabic) "إرسال" else "Send") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(if (s.isArabic) "إلغاء" else "Cancel") }
        }
    )
}

@Composable
fun OperationalSettingsDialog(
    current: AppSettings,
    activeSession: ActiveSession,
    isCloudMode: Boolean,
    onUpdateAdminCredentials: (username: String, code: String) -> Unit,
    onConnectCloudSync: () -> Unit,
    onDismiss: () -> Unit,
    onSave: (AppSettings) -> Unit
) {
    val s = LocalAppStrings.current
    var adminUsername by remember(activeSession) { mutableStateOf(activeSession.adminUsername) }
    var adminCode by remember(activeSession) { mutableStateOf(activeSession.adminAccessCode) }
    var grace by remember(current) { mutableStateOf(current.gracePeriodMinutes.toString()) }
    var geofence by remember(current) { mutableStateOf(current.geofenceRadiusMeters.toString()) }
    var gpsMoving by remember(current) { mutableStateOf(current.gpsMovingIntervalSec.toString()) }
    var gpsIdle by remember(current) { mutableStateOf(current.gpsIdleIntervalSec.toString()) }
    var nShift by remember(current) { mutableStateOf(current.notifyShiftEvents) }
    var nArrival by remember(current) { mutableStateOf(current.notifyArrival) }
    var nDelivery by remember(current) { mutableStateOf(current.notifyPickupDelivery) }
    var nPay by remember(current) { mutableStateOf(current.notifyPayments) }
    var nDelay by remember(current) { mutableStateOf(current.notifyDelays) }
    var nGps by remember(current) { mutableStateOf(current.notifyGpsDisconnect) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GlassDialogContainer,
        shape = DialogShape,
        icon = {
            GradientIconBadge(icon = Icons.Rounded.Settings, accentColor = BubblesCyanDark, size = 46.dp, iconSize = 24.dp)
        },
        title = { Text(s.settingsTitle, fontWeight = FontWeight.Bold, color = BubblesSlateDark) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = BubblesCyanSoft.copy(alpha = 0.72f),
                    border = BorderStroke(1.dp, BubblesCyanBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Rounded.AdminPanelSettings, contentDescription = null, tint = BubblesCyanDark, modifier = Modifier.size(18.dp))
                            Text(
                                text = if (s.isArabic) "بيانات دخول الإدارة" else "Admin Access Credentials",
                                style = MaterialTheme.typography.titleSmall,
                                color = BubblesSlateDark,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = adminUsername,
                                onValueChange = { adminUsername = it },
                                label = { Text(if (s.isArabic) "اسم الإدارة" else "Admin User") },
                                singleLine = true,
                                shape = FieldShape,
                                colors = glassFieldColors(),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = adminCode,
                                onValueChange = { adminCode = it.filter { c -> c.isDigit() }.take(8) },
                                label = { Text(if (s.isArabic) "كود الإدارة" else "Admin PIN") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                singleLine = true,
                                shape = FieldShape,
                                colors = glassFieldColors(),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = grace, onValueChange = { grace = it }, label = { Text(if (s.isArabic) "سماح التأخير (د)" else "Grace (min)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, shape = FieldShape, colors = glassFieldColors(), modifier = Modifier.weight(1f))
                    OutlinedTextField(value = geofence, onValueChange = { geofence = it }, label = { Text(if (s.isArabic) "نطاق Geofence (م)" else "Geofence (m)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, shape = FieldShape, colors = glassFieldColors(), modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = gpsMoving, onValueChange = { gpsMoving = it }, label = { Text(if (s.isArabic) "تحديث GPS حركة (ث)" else "GPS Move (s)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, shape = FieldShape, colors = glassFieldColors(), modifier = Modifier.weight(1f))
                    OutlinedTextField(value = gpsIdle, onValueChange = { gpsIdle = it }, label = { Text(if (s.isArabic) "تحديث GPS توقف (ث)" else "GPS Idle (s)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, shape = FieldShape, colors = glassFieldColors(), modifier = Modifier.weight(1f))
                }
                SettingSwitchRow(if (s.isArabic) "تنبيه بدء/إنهاء الدوام" else "Shift Start/End Alerts", nShift) { nShift = it }
                SettingSwitchRow(if (s.isArabic) "تنبيه الوصول والموقع" else "Arrival Alerts", nArrival) { nArrival = it }
                SettingSwitchRow(if (s.isArabic) "تنبيه الاستلام والتسليم" else "Pickup/Delivery Alerts", nDelivery) { nDelivery = it }
                SettingSwitchRow(if (s.isArabic) "تنبيه المدفوعات" else "Payment Alerts", nPay) { nPay = it }
                SettingSwitchRow(if (s.isArabic) "تنبيه التأخير" else "Delay Alerts", nDelay) { nDelay = it }
                SettingSwitchRow(if (s.isArabic) "تنبيه انقطاع GPS" else "GPS Disconnect Alerts", nGps) { nGps = it }

                HorizontalDivider(color = BubblesDivider)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isCloudMode) StatusEmerald.copy(alpha = 0.12f) else Color(0xFFF3F8FA),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isCloudMode) {
                                    if (s.isArabic) "المزامنة السحابية مفعّلة" else "Cloud Sync Active"
                                } else {
                                    if (s.isArabic) "المزامنة السحابية بين الأجهزة (اختياري)" else "Multi-device Cloud Sync (Optional)"
                                },
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isCloudMode) StatusEmerald else BubblesSlateDark
                            )
                            Text(
                                text = if (s.isArabic) "البيانات محفوظة محليًا بشكل دائم دون الحاجة لحساب Google." else "All operations persist locally without Google Sign-In.",
                                style = MaterialTheme.typography.labelSmall,
                                color = BubblesGray
                            )
                        }
                        if (!isCloudMode) {
                            OutlinedButton(
                                onClick = {
                                    onDismiss()
                                    onConnectCloudSync()
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Rounded.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(if (s.isArabic) "ربط" else "Link", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (adminUsername.isNotBlank() && adminCode.length >= 4) {
                        onUpdateAdminCredentials(adminUsername, adminCode)
                    }
                    onSave(
                        current.copy(
                            gracePeriodMinutes = (grace.toLongOrNull() ?: 5L).coerceIn(0L, 120L),
                            geofenceRadiusMeters = (geofence.toLongOrNull() ?: 150L).coerceIn(20L, 2000L),
                            gpsMovingIntervalSec = (gpsMoving.toLongOrNull() ?: 30L).coerceIn(10L, 300L),
                            gpsIdleIntervalSec = (gpsIdle.toLongOrNull() ?: 180L).coerceIn(30L, 900L),
                            notifyShiftEvents = nShift,
                            notifyArrival = nArrival,
                            notifyPickupDelivery = nDelivery,
                            notifyPayments = nPay,
                            notifyDelays = nDelay,
                            notifyGpsDisconnect = nGps
                        )
                    )
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = BubblesCyan),
                shape = RoundedCornerShape(14.dp)
            ) { Text(if (s.isArabic) "حفظ الإعدادات" else "Save Settings") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(if (s.isArabic) "إلغاء" else "Cancel") }
        }
    )
}

@Composable
private fun SettingSwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = BubblesSlateDark)
        Switch(checked = checked, onCheckedChange = onCheckedChange, colors = SwitchDefaults.colors(checkedTrackColor = BubblesCyan))
    }
}
