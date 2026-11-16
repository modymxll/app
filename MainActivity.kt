package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.EventNote
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.local.BubblesLocalDatabase
import com.example.data.local.LocalWorkspaceRepository
import com.example.data.local.OfflineSyncManager
import com.example.data.repository.BubblesRepository
import com.example.data.session.ActiveSession
import com.example.data.session.SessionPreferences
import com.example.notifications.VisitReminderScheduler
import com.example.service.LocationForegroundService
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.authStateFlow
import com.example.ui.auth.onGoogleSignInClicked
import com.example.ui.auth.signOutUser
import com.example.ui.components.BubblesAppHeader
import com.example.ui.components.BubblesAtmosphericBackdrop
import com.example.ui.components.FloatingGlassBottomBar
import com.example.ui.components.GlassNavItem
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.ArabicStrings
import com.example.ui.i18n.EnglishStrings
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.model.PreviewRole
import com.example.ui.model.UiMappers
import com.example.ui.model.UiMappers.toUi
import com.example.ui.screens.AdminDashboardTab
import com.example.ui.screens.BroadcastNotificationDialog
import com.example.ui.screens.CustomersLocationsTab
import com.example.ui.screens.DriverHomeTab
import com.example.ui.screens.DriverMapTab
import com.example.ui.screens.DriversTrackingTab
import com.example.ui.screens.NotificationsTab
import com.example.ui.screens.OperationalSettingsDialog
import com.example.ui.screens.ReportsAuditTab
import com.example.ui.screens.VisitExecutionScreen
import com.example.ui.screens.VisitsScheduleTab
import com.example.ui.theme.BubblesCyan
import com.example.ui.theme.BubblesDeliveryTheme
import com.example.ui.theme.GlassPillFloating
import com.example.ui.viewmodel.BubblesViewModel
import com.example.ui.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BubblesDeliveryTheme {
                BubblesRootApp()
            }
        }
    }
}

@Composable
fun BubblesRootApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sessionPrefs = remember(context) { SessionPreferences(context.applicationContext) }
    val localWorkspace = remember(context) {
        LocalWorkspaceRepository(BubblesLocalDatabase.getInstance(context.applicationContext).workspaceDao())
    }

    val session by sessionPrefs.sessionFlow.collectAsState(initial = ActiveSession())
    val firebaseUser by Firebase.auth.authStateFlow().collectAsState(initial = Firebase.auth.currentUser)

    val language = if (session.languageCode == "EN") AppLanguage.EN else AppLanguage.AR
    val strings = if (language == AppLanguage.AR) ArabicStrings else EnglishStrings
    val layoutDirection = if (language == AppLanguage.AR) LayoutDirection.Rtl else LayoutDirection.Ltr

    val toggleLanguage: () -> Unit = {
        val next = if (language == AppLanguage.AR) "EN" else "AR"
        scope.launch { sessionPrefs.setLanguage(next) }
    }

    CompositionLocalProvider(
        LocalAppStrings provides strings,
        LocalLayoutDirection provides layoutDirection
    ) {
        val activeUser = firebaseUser
        val hasActiveSession = session.isLoggedIn || activeUser != null

        if (!hasActiveSession) {
            AuthScreen(
                onToggleLanguage = toggleLanguage,
                onLoginWithCode = { code ->
                    val cleanCode = code.trim()
                    if (cleanCode == session.adminAccessCode || cleanCode == ActiveSession.DEFAULT_ADMIN_CODE) {
                        sessionPrefs.loginAsAdmin(if (strings.isArabic) "الإدارة" else "Admin")
                        Result.success(Unit)
                    } else {
                        val driver = localWorkspace.findDriverByAccessCode(cleanCode)
                        when {
                            driver == null -> Result.failure(
                                IllegalArgumentException(
                                    if (strings.isArabic) "كود الدخول غير صحيح. أدخل كود الإدارة أو كود المندوب."
                                    else "Invalid access code. Enter Admin or Driver code."
                                )
                            )
                            driver.status == "SUSPENDED" || driver.status == "INACTIVE" -> Result.failure(
                                IllegalStateException(
                                    if (strings.isArabic) "حساب المندوب غير نشط حاليًا. تواصل مع الإدارة."
                                    else "Driver account is currently inactive."
                                )
                            )
                            else -> {
                                sessionPrefs.loginAsDriver(driver.id, driver.name)
                                Result.success(Unit)
                            }
                        }
                    }
                },
                onLoginWithAccount = { username, code ->
                    val cleanUser = username.trim()
                    val cleanCode = code.trim()
                    val isAdminMatch = (cleanUser.equals(session.adminUsername, ignoreCase = true) ||
                        cleanUser.equals(ActiveSession.DEFAULT_ADMIN_USERNAME, ignoreCase = true)) &&
                        (cleanCode == session.adminAccessCode || cleanCode == ActiveSession.DEFAULT_ADMIN_CODE)
                    if (isAdminMatch) {
                        sessionPrefs.loginAsAdmin(cleanUser.ifBlank { "الإدارة" })
                        Result.success(Unit)
                    } else {
                        val driver = localWorkspace.findDriverByCredentials(cleanUser, cleanCode)
                        when {
                            driver == null -> Result.failure(
                                IllegalArgumentException(
                                    if (strings.isArabic) "اسم الحساب أو كود الدخول غير صحيح."
                                    else "Invalid account username or access code."
                                )
                            )
                            driver.status == "SUSPENDED" || driver.status == "INACTIVE" -> Result.failure(
                                IllegalStateException(
                                    if (strings.isArabic) "تم إيقاف هذا الحساب. تواصل مع الإدارة."
                                    else "Driver account is suspended."
                                )
                            )
                            else -> {
                                sessionPrefs.loginAsDriver(driver.id, driver.name)
                                Result.success(Unit)
                            }
                        }
                    }
                }
            )
        } else {
            val isCloudMode = activeUser != null
            val currentUserId = activeUser?.uid ?: (session.driverId ?: "admin_local")
            val sessionKey = if (isCloudMode) "cloud_${activeUser?.uid}" else "local_${session.role}_${session.driverId.orEmpty()}"
            key(sessionKey) {
                BubblesAuthenticatedApp(
                    sessionKey = sessionKey,
                    currentUserId = currentUserId,
                    isCloudMode = isCloudMode,
                    session = session,
                    sessionPrefs = sessionPrefs,
                    localWorkspace = localWorkspace,
                    onToggleLanguage = toggleLanguage
                )
            }
        }
    }
}

@Composable
private fun BubblesAuthenticatedApp(
    sessionKey: String,
    currentUserId: String,
    isCloudMode: Boolean,
    session: ActiveSession,
    sessionPrefs: SessionPreferences,
    localWorkspace: LocalWorkspaceRepository,
    onToggleLanguage: () -> Unit
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val vm: BubblesViewModel = viewModel(
        key = sessionKey,
        factory = viewModelFactory {
            initializer {
                val appCtx = context.applicationContext
                val db = FirebaseFirestore.getInstance(appCtx.getString(R.string.firestore_database_id))
                val repo = BubblesRepository(db, Firebase.auth)
                val localDb = BubblesLocalDatabase.getInstance(appCtx)
                val syncManager = OfflineSyncManager(appCtx, localDb.dao(), repo)
                BubblesViewModel(
                    repository = repo,
                    localWorkspace = localWorkspace,
                    localDao = localDb.dao(),
                    syncManager = syncManager,
                    currentUserId = currentUserId,
                    isCloudMode = isCloudMode,
                    sessionRole = session.role,
                    sessionDriverId = session.driverId,
                    sessionActorName = session.actorName
                )
            }
        }
    )

    val userProfileState by vm.userProfileState.collectAsState()
    val appSettings by vm.appSettings.collectAsState()
    val rawDrivers by vm.drivers.collectAsState()
    val rawCustomers by vm.customers.collectAsState()
    val rawLocations by vm.locations.collectAsState()
    val rawVisits by vm.visits.collectAsState()
    val recurringSchedules by vm.recurringSchedules.collectAsState()
    val attendanceRecords by vm.attendanceRecords.collectAsState()
    val gpsHistory7Days by vm.gpsHistory7Days.collectAsState()
    val paymentRecords by vm.paymentRecords.collectAsState()
    val rawNotifications by vm.notifications.collectAsState()
    val rawAuditLogs by vm.auditLogs.collectAsState()
    val isOnline by vm.isOnline.collectAsState()
    val pendingSyncCount by vm.pendingSyncCount.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(vm) {
        vm.toastEvents.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val showToast: (String) -> Unit = { msg ->
        scope.launch { snackbarHostState.showSnackbar(msg) }
    }

    val profile = (userProfileState as? UiState.Success)?.data
    val isAdminAccount = if (isCloudMode) profile?.role == "ADMIN" else session.role == "ADMIN"

    var role by remember(isAdminAccount) {
        mutableStateOf(if (isAdminAccount) PreviewRole.ADMIN else PreviewRole.DRIVER)
    }
    var adminTabIndex by remember { mutableIntStateOf(0) }
    var driverTabIndex by remember { mutableIntStateOf(0) }
    var activeVisitId by remember { mutableStateOf<String?>(null) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showBroadcastDialog by remember { mutableStateOf(false) }
    var lastAlertedNotificationId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(rawVisits, strings.isArabic) {
        if (rawVisits.isNotEmpty()) {
            VisitReminderScheduler.syncVisitReminders(context, rawVisits, strings.isArabic)
        }
    }

    LaunchedEffect(rawNotifications) {
        val newestUnread = rawNotifications.firstOrNull { !it.isRead }
        if (newestUnread != null && newestUnread.id != lastAlertedNotificationId) {
            lastAlertedNotificationId = newestUnread.id
            VisitReminderScheduler.showNotification(
                context = context,
                title = newestUnread.title,
                body = newestUnread.body
            )
        }
    }

    val recurringMap = remember(recurringSchedules) {
        recurringSchedules.associate { it.id to UiMappers.formatWeekdaysAr(it.weekdaysCsv) }
    }
    val uiDrivers = remember(rawDrivers, rawVisits) {
        rawDrivers.map { it.toUi(rawVisits) }
    }
    val uiCustomers = remember(rawCustomers, rawVisits) {
        rawCustomers.map { it.toUi(rawVisits) }
    }
    val uiLocations = remember(rawLocations) {
        rawLocations.map { it.toUi() }
    }
    val uiVisits = remember(rawVisits, recurringMap) {
        rawVisits.map { it.toUi(recurringMap) }
    }
    val uiNotifications = remember(rawNotifications) {
        rawNotifications.map { it.toUi() }
    }
    val uiAuditLogs = remember(rawAuditLogs) {
        rawAuditLogs.map { it.toUi() }
    }

    val myDriverRaw = remember(rawDrivers, currentUserId, profile, session.driverId) {
        rawDrivers.firstOrNull { it.userId == currentUserId || it.id == session.driverId }
            ?: profile?.driverId?.let { dId -> rawDrivers.firstOrNull { it.id == dId } }
            ?: if (isAdminAccount) rawDrivers.firstOrNull() else null
    }
    val myDriverUi = remember(myDriverRaw, uiDrivers) {
        myDriverRaw?.let { raw -> uiDrivers.firstOrNull { it.id == raw.id } }
    }
    val myDriverVisits = remember(uiVisits, myDriverRaw, isAdminAccount, role) {
        if (myDriverRaw == null) {
            if (isAdminAccount && role == PreviewRole.DRIVER) emptyList() else uiVisits
        } else {
            uiVisits.filter { it.driverId == myDriverRaw.id }
        }
    }
    val myActiveAttendance = remember(attendanceRecords, myDriverRaw) {
        myDriverRaw?.let { d -> attendanceRecords.firstOrNull { it.driverId == d.id && it.isActive } }
    }

    val adminTabs = listOf(
        GlassNavItem(strings.tabDashboard, Icons.Rounded.Dashboard, "nav_admin_dashboard"),
        GlassNavItem(strings.tabVisits, Icons.AutoMirrored.Rounded.EventNote, "nav_admin_visits"),
        GlassNavItem(strings.tabCustomers, Icons.Rounded.Groups, "nav_admin_customers"),
        GlassNavItem(strings.tabDrivers, Icons.Rounded.Badge, "nav_admin_drivers"),
        GlassNavItem(strings.tabReports, Icons.Rounded.Assessment, "nav_admin_reports")
    )

    val driverTabs = listOf(
        GlassNavItem(strings.tabTasks, Icons.Rounded.TaskAlt, "nav_driver_tasks"),
        GlassNavItem(strings.tabMap, Icons.Rounded.Map, "nav_driver_map"),
        GlassNavItem(strings.tabNotifications, Icons.Rounded.Notifications, "nav_driver_notifications")
    )

    Box(modifier = Modifier.fillMaxSize()) {
        BubblesAtmosphericBackdrop()

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                Box(modifier = Modifier.statusBarsPadding()) {
                    BubblesAppHeader(
                        isAdminAccount = isAdminAccount,
                        role = role,
                        onRoleChange = {
                            if (isAdminAccount) {
                                role = it
                                activeVisitId = null
                            }
                        },
                        onToggleLanguage = onToggleLanguage,
                        unreadNotifications = uiNotifications.count { !it.isRead },
                        onOpenNotifications = {
                            if (isAdminAccount) {
                                showBroadcastDialog = true
                            } else {
                                activeVisitId = null
                                driverTabIndex = 2
                            }
                        },
                        onOpenSettings = { showSettingsDialog = true },
                        onSignOut = {
                            LocationForegroundService.stop(context)
                            scope.launch {
                                sessionPrefs.logout()
                            }
                            if (isCloudMode) {
                                signOutUser(credentialManager, scope)
                            }
                        },
                        isOnline = isOnline,
                        pendingSyncCount = pendingSyncCount,
                        onSyncNow = { vm.syncOfflineNow() },
                        isCloudMode = isCloudMode
                    )
                }
            }
        ) { innerPadding ->
            if (isCloudMode && userProfileState is UiState.Loading) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = BubblesCyan)
                }
            } else {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    val isWide = maxWidth >= 600.dp
                    val currentActiveVisit = activeVisitId?.let { id -> uiVisits.firstOrNull { it.id == id } }
                    val currentActiveRaw = activeVisitId?.let { id -> rawVisits.firstOrNull { it.id == id } }

                    Row(modifier = Modifier.fillMaxSize()) {
                        if (isWide && currentActiveVisit == null) {
                            val items = if (role == PreviewRole.ADMIN && isAdminAccount) adminTabs else driverTabs
                            val selectedIdx = if (role == PreviewRole.ADMIN && isAdminAccount) adminTabIndex else driverTabIndex
                            NavigationRail(containerColor = GlassPillFloating) {
                                items.forEachIndexed { idx, item ->
                                    NavigationRailItem(
                                        selected = selectedIdx == idx,
                                        onClick = {
                                            if (role == PreviewRole.ADMIN && isAdminAccount) adminTabIndex = idx else driverTabIndex = idx
                                        },
                                        icon = { Icon(item.icon, contentDescription = item.title) },
                                        label = { Text(item.title) }
                                    )
                                }
                            }
                        }

                        Box(modifier = Modifier.weight(1f)) {
                            AnimatedContent(
                                targetState = Triple(
                                    if (isAdminAccount) role else PreviewRole.DRIVER,
                                    adminTabIndex,
                                    Pair(driverTabIndex, currentActiveVisit?.id)
                                ),
                                transitionSpec = { fadeIn() togetherWith fadeOut() },
                                label = "mainContentTransition"
                            ) { (curRole, curAdminTab, driverState) ->
                                val (curDriverTab, _) = driverState
                                if (curRole == PreviewRole.ADMIN && isAdminAccount) {
                                    when (curAdminTab) {
                                        0 -> AdminDashboardTab(
                                            drivers = uiDrivers,
                                            visits = uiVisits,
                                            auditLogs = uiAuditLogs,
                                            gracePeriodMinutes = appSettings.gracePeriodMinutes,
                                            geofenceRadiusMeters = appSettings.geofenceRadiusMeters,
                                            onQuickNewVisit = { adminTabIndex = 1 },
                                            onOpenTracking = { adminTabIndex = 3 },
                                            onSendBroadcast = { showBroadcastDialog = true }
                                        )
                                        1 -> VisitsScheduleTab(
                                            visits = uiVisits,
                                            rawVisits = rawVisits,
                                            recurringSchedules = recurringSchedules,
                                            locations = rawLocations,
                                            drivers = rawDrivers,
                                            gracePeriodMinutes = appSettings.gracePeriodMinutes,
                                            onCreateVisitOrRecurring = vm::scheduleVisitOrRecurring,
                                            onUpdateVisitAdmin = vm::updateVisitAdmin,
                                            onStopRecurring = vm::stopRecurringSchedule
                                        )
                                        2 -> CustomersLocationsTab(
                                            customers = uiCustomers,
                                            rawCustomers = rawCustomers,
                                            locations = uiLocations,
                                            onCreateCustomerWithBranch = vm::createCustomerWithInitialBranch,
                                            onAddBranch = vm::addBranchLocation,
                                            onShowToast = showToast
                                        )
                                        3 -> DriversTrackingTab(
                                            drivers = uiDrivers,
                                            rawDrivers = rawDrivers,
                                            gpsHistory7Days = gpsHistory7Days,
                                            onCreateDriver = vm::createDriver,
                                            onUpdateDriverStatus = vm::updateDriverStatus,
                                            onSoftDeleteDriver = vm::softDeleteDriver,
                                            onShowToast = showToast
                                        )
                                        4 -> ReportsAuditTab(
                                            visits = uiVisits,
                                            drivers = uiDrivers,
                                            paymentRecords = paymentRecords,
                                            auditLogs = uiAuditLogs,
                                            onShowToast = showToast
                                        )
                                    }
                                } else {
                                    if (currentActiveVisit != null && currentActiveRaw != null && myDriverRaw != null) {
                                        VisitExecutionScreen(
                                            visit = currentActiveVisit,
                                            rawVisit = currentActiveRaw,
                                            driver = myDriverRaw,
                                            geofenceRadiusMeters = appSettings.geofenceRadiusMeters,
                                            onBack = { activeVisitId = null },
                                            onCheckInArrival = vm::checkInArrival,
                                            onCompleteVisit = vm::completeVisit,
                                            onMarkNotExecuted = vm::markVisitNotExecuted,
                                            onShowToast = showToast
                                        )
                                    } else {
                                        when (curDriverTab) {
                                            0 -> DriverHomeTab(
                                                driver = myDriverRaw,
                                                driverUi = myDriverUi,
                                                activeAttendance = myActiveAttendance,
                                                visits = myDriverVisits,
                                                gpsMovingIntervalSec = appSettings.gpsMovingIntervalSec,
                                                gracePeriodMinutes = appSettings.gracePeriodMinutes,
                                                onStartShift = vm::startShift,
                                                onEndShift = vm::endShift,
                                                onSelectVisit = { activeVisitId = it.id },
                                                onShowToast = showToast
                                            )
                                            1 -> DriverMapTab(
                                                driverUi = myDriverUi,
                                                visits = myDriverVisits,
                                                geofenceRadiusMeters = appSettings.geofenceRadiusMeters
                                            )
                                            2 -> NotificationsTab(
                                                notifications = uiNotifications,
                                                rawNotifications = rawNotifications,
                                                onToggleRead = vm::toggleNotificationRead
                                            )
                                        }
                                    }
                                }
                            }

                            if (!isWide && currentActiveVisit == null) {
                                val items = if (role == PreviewRole.ADMIN && isAdminAccount) adminTabs else driverTabs
                                val selectedIdx = if (role == PreviewRole.ADMIN && isAdminAccount) adminTabIndex else driverTabIndex
                                FloatingGlassBottomBar(
                                    items = items,
                                    selectedIndex = selectedIdx,
                                    onSelect = { idx ->
                                        if (role == PreviewRole.ADMIN && isAdminAccount) adminTabIndex = idx else driverTabIndex = idx
                                    },
                                    modifier = Modifier.align(Alignment.BottomCenter)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSettingsDialog && isAdminAccount) {
        OperationalSettingsDialog(
            current = appSettings,
            activeSession = session,
            isCloudMode = isCloudMode,
            onUpdateAdminCredentials = { user, code ->
                scope.launch {
                    sessionPrefs.updateAdminCredentials(user, code)
                }
            },
            onConnectCloudSync = {
                onGoogleSignInClicked(
                    context = context,
                    credentialManager = credentialManager,
                    onAuthSuccess = {
                        showToast(if (strings.isArabic) "تم ربط المزامنة السحابية بنجاح" else "Cloud sync enabled")
                    },
                    onAuthError = { msg -> showToast(msg) },
                    scope = scope
                )
            },
            onDismiss = { showSettingsDialog = false },
            onSave = vm::saveAppSettings
        )
    }

    if (showBroadcastDialog && isAdminAccount) {
        BroadcastNotificationDialog(
            drivers = rawDrivers,
            onDismiss = { showBroadcastDialog = false },
            onSend = { title, body, target -> vm.sendNotification(title, body, target) }
        )
    }
}
