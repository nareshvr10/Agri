package com.example.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.auth.authStateFlow
import com.example.auth.signOutUser
import com.example.core.AgroGuardAppContainer
import com.example.models.CropType
import com.example.models.UiState
import com.example.ui.components.AgroGuardTopBar
import com.example.ui.screens.AddFarmScreen
import com.example.ui.screens.AdminManagementScreen
import com.example.ui.screens.AiDiagnosisResultsScreen
import com.example.ui.screens.AlertsScreen
import com.example.ui.screens.CropImageScannerScreen
import com.example.ui.screens.CropSelectionScreen
import com.example.ui.screens.CropZonesScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ExpertConsultationScreen
import com.example.ui.screens.FarmAnalyticsScreen
import com.example.ui.screens.FarmManagementScreen
import com.example.ui.screens.FarmerRegistrationScreen
import com.example.ui.screens.IrrigationRecommendationsScreen
import com.example.ui.screens.LanguageSelectionScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.MoreHubScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.SensorHistoryChartsScreen
import com.example.ui.screens.SensorMonitoringScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.WeatherAndRiskScreen
import com.example.viewmodel.AppSettingsViewModel
import com.example.viewmodel.ScannerViewModel
import com.example.viewmodel.WorkspaceViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.flowOf

private data class TopNavTab(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector
)

private val PrimaryTabs = listOf(
    TopNavTab(Routes.DASHBOARD, R.string.nav_dashboard, Icons.Default.Dashboard),
    TopNavTab(Routes.FARM_MANAGEMENT, R.string.nav_farms, Icons.Default.Agriculture),
    TopNavTab(Routes.SENSOR_MONITORING, R.string.nav_sensors, Icons.Default.Sensors),
    TopNavTab(Routes.CROP_SCANNER, R.string.nav_scanner, Icons.Default.CameraAlt),
    TopNavTab(Routes.MORE_HUB, R.string.nav_more, Icons.Default.MoreHoriz)
)

@Composable
fun AgroGuardNavHost(
    container: AgroGuardAppContainer,
    settingsViewModel: AppSettingsViewModel
) {
    val authFlow = remember { Firebase.auth.authStateFlow() }
    val currentUser by authFlow.collectAsState(initial = Firebase.auth.currentUser)
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val isOnline by settingsViewModel.isOnline.collectAsStateWithLifecycle()

    if (currentUser == null) {
        UnauthenticatedFlow(
            onboardingCompleted = settings.onboardingCompleted,
            language = settings.language,
            onToggleLanguage = settingsViewModel::toggleLanguage,
            onCompleteOnboarding = settingsViewModel::completeOnboarding
        )
    } else {
        val user = currentUser!!
        key(user.uid) {
            AuthenticatedFlow(
                firebaseUser = user,
                container = container,
                settingsViewModel = settingsViewModel,
                isOnline = isOnline
            )
        }
    }
}

@Composable
private fun UnauthenticatedFlow(
    onboardingCompleted: Boolean,
    language: com.example.models.AppLanguage,
    onToggleLanguage: () -> Unit,
    onCompleteOnboarding: () -> Unit
) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH
    ) {
        composable(Routes.SPLASH) {
            SplashScreen(
                onboardingCompleted = onboardingCompleted,
                onContinueToOnboarding = {
                    navController.navigate(Routes.ONBOARDING) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
                onContinueToLogin = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
                onAutoSignedIn = { /* Auth flow listener swaps root automatically */ }
            )
        }
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                language = language,
                onToggleLanguage = onToggleLanguage,
                onFinishOnboarding = {
                    onCompleteOnboarding()
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.LOGIN) {
            LoginScreen(
                language = language,
                onToggleLanguage = onToggleLanguage,
                onAuthSuccess = { /* Auth flow listener swaps root automatically */ }
            )
        }
    }
}

@Composable
private fun AuthenticatedFlow(
    firebaseUser: FirebaseUser,
    container: AgroGuardAppContainer,
    settingsViewModel: AppSettingsViewModel,
    isOnline: Boolean
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val workspaceViewModel: WorkspaceViewModel = viewModel(
        key = "workspace_${firebaseUser.uid}",
        factory = viewModelFactory {
            initializer {
                WorkspaceViewModel(
                    currentUserId = firebaseUser.uid,
                    authEmail = firebaseUser.email.orEmpty(),
                    authDisplayName = firebaseUser.displayName.orEmpty(),
                    farmRepository = container.farmRepository,
                    sensorRepository = container.sensorRepository,
                    alertRepository = container.alertRepository,
                    scanRepository = container.scanRepository,
                    userAndAdminRepository = container.userAndAdminRepository,
                    consultationRepository = container.consultationRepository,
                    weatherRepository = container.weatherRepository
                )
            }
        }
    )

    val scannerViewModel: ScannerViewModel = viewModel(
        key = "scanner_${firebaseUser.uid}",
        factory = viewModelFactory {
            initializer {
                ScannerViewModel(
                    currentUserId = firebaseUser.uid,
                    aiDiagnosisRepository = container.aiDiagnosisRepository,
                    scanRepository = container.scanRepository,
                    alertRepository = container.alertRepository
                )
            }
        }
    )

    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val userProfileState by workspaceViewModel.userProfileState.collectAsStateWithLifecycle()

    when (val profileState = userProfileState) {
        is UiState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("profile_loading_state"),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.brand_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }

        is UiState.Error -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = profileState.message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                    Button(
                        onClick = { signOutUser(credentialManager, scope) },
                        modifier = Modifier.testTag("error_sign_out_button")
                    ) {
                        Text(stringResource(R.string.action_logout))
                    }
                }
            }
        }

        is UiState.Success -> {
            val userProfile = profileState.data
            if (userProfile == null) {
                FarmerRegistrationScreen(
                    initialName = firebaseUser.displayName.orEmpty(),
                    initialEmail = firebaseUser.email.orEmpty(),
                    language = settings.language,
                    onToggleLanguage = settingsViewModel::toggleLanguage,
                    onSubmitProfile = { name, district, phone, crops ->
                        workspaceViewModel.registerFarmerProfile(
                            displayName = name,
                            district = district,
                            phone = phone,
                            language = settings.language,
                            cropsGrown = crops
                        )
                    },
                    onSignOut = { signOutUser(credentialManager, scope) }
                )
            } else {
                MainWorkspaceShell(
                    userProfile = userProfile,
                    settingsViewModel = settingsViewModel,
                    workspaceViewModel = workspaceViewModel,
                    scannerViewModel = scannerViewModel,
                    isOnline = isOnline,
                    onSignOut = { signOutUser(credentialManager, scope) }
                )
            }
        }
    }
}

@Composable
private fun MainWorkspaceShell(
    userProfile: com.example.models.UserProfile,
    settingsViewModel: AppSettingsViewModel,
    workspaceViewModel: WorkspaceViewModel,
    scannerViewModel: ScannerViewModel,
    isOnline: Boolean,
    onSignOut: () -> Unit
) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val farmsState by workspaceViewModel.farmsState.collectAsStateWithLifecycle()
    val zones by workspaceViewModel.zonesState.collectAsStateWithLifecycle()
    val devices by workspaceViewModel.devicesState.collectAsStateWithLifecycle()
    val readings by workspaceViewModel.readingsState.collectAsStateWithLifecycle()
    val alerts by workspaceViewModel.alertsState.collectAsStateWithLifecycle()
    val scans by workspaceViewModel.scansState.collectAsStateWithLifecycle()
    val experts by workspaceViewModel.expertsState.collectAsStateWithLifecycle()
    val adminUsers by workspaceViewModel.adminUsersState.collectAsStateWithLifecycle()
    val weatherSnapshot by workspaceViewModel.weatherSnapshot.collectAsStateWithLifecycle()
    val actionMessage by workspaceViewModel.actionMessage.collectAsStateWithLifecycle()
    val scannerState by scannerViewModel.uiState.collectAsStateWithLifecycle()

    val farms = (farmsState as? UiState.Success)?.data.orEmpty()
    val selectedFarm = remember(farms, settings.selectedFarmId) {
        farms.firstOrNull { it.id == settings.selectedFarmId } ?: farms.firstOrNull()
    }
    var pendingZoneCrop by remember { mutableStateOf<CropType?>(null) }

    LaunchedEffect(actionMessage) {
        val msg = actionMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            workspaceViewModel.clearActionMessage()
        }
    }

    LaunchedEffect(selectedFarm?.id, settings.weatherProviderMode, settings.customWeatherUrl) {
        if (selectedFarm != null && isOnline) {
            workspaceViewModel.refreshWeather(selectedFarm, settings)
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Routes.DASHBOARD
    val isTopLevelTab = PrimaryTabs.any { it.route == currentRoute }

    val titleRes = when (currentRoute) {
        Routes.DASHBOARD -> R.string.nav_dashboard
        Routes.FARM_MANAGEMENT -> R.string.screen_farm_management
        Routes.ADD_FARM -> R.string.screen_add_farm
        Routes.CROP_SELECTION -> R.string.screen_crop_selection
        Routes.CROP_ZONES -> R.string.screen_crop_zones
        Routes.SENSOR_MONITORING -> R.string.screen_sensor_monitoring
        Routes.SENSOR_CHARTS -> R.string.screen_sensor_charts
        Routes.CROP_SCANNER -> R.string.screen_crop_scanner
        Routes.AI_RESULTS -> R.string.screen_ai_results
        Routes.IRRIGATION -> R.string.screen_irrigation
        Routes.WEATHER_RISK -> R.string.screen_weather_risk
        Routes.ALERTS -> R.string.screen_alerts
        Routes.FARM_ANALYTICS -> R.string.screen_analytics
        Routes.EXPERT_CONSULTATION -> R.string.screen_consultation
        Routes.NOTIFICATIONS -> R.string.screen_notifications
        Routes.PROFILE -> R.string.screen_profile
        Routes.SETTINGS -> R.string.screen_settings
        Routes.LANGUAGE -> R.string.screen_language
        Routes.ADMIN_MANAGEMENT -> R.string.screen_admin
        Routes.MORE_HUB -> R.string.nav_more
        else -> R.string.brand_title
    }

    fun navigateTopTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                AgroGuardTopBar(
                    title = stringResource(titleRes),
                    canNavigateBack = !isTopLevelTab,
                    onBack = { navController.popBackStack() },
                    language = settings.language,
                    onToggleLanguage = settingsViewModel::toggleLanguage
                )
            },
            bottomBar = {
                if (!useRail) {
                    NavigationBar {
                        PrimaryTabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTopTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = stringResource(tab.labelRes)
                                    )
                                },
                                label = { Text(stringResource(tab.labelRes)) },
                                modifier = Modifier.testTag("bottom_nav_${tab.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(modifier = Modifier.fillMaxHeight()) {
                        PrimaryTabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTopTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = stringResource(tab.labelRes)
                                    )
                                },
                                label = { Text(stringResource(tab.labelRes)) },
                                modifier = Modifier.testTag("rail_nav_${tab.route}")
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = Routes.DASHBOARD,
                    modifier = Modifier.weight(1f)
                ) {
                    composable(Routes.DASHBOARD) {
                        val firstZone = selectedFarm?.let { f ->
                            zones.firstOrNull { it.farmId == f.id }
                        }
                        val irrigationPreview = firstZone?.let {
                            workspaceViewModel.computeIrrigationForZone(it)
                        }
                        DashboardScreen(
                            user = userProfile,
                            farms = farms,
                            selectedFarm = selectedFarm,
                            zones = zones,
                            devices = devices,
                            readings = readings,
                            alerts = alerts,
                            scans = scans,
                            irrigationPreview = irrigationPreview,
                            language = settings.language,
                            isOnline = isOnline,
                            lastSyncedEpochMs = settings.lastSyncedEpochMs,
                            onSelectFarm = settingsViewModel::selectFarm,
                            onOpenAddFarm = { navController.navigate(Routes.ADD_FARM) },
                            onOpenScanner = { navigateTopTab(Routes.CROP_SCANNER) },
                            onOpenSensors = { navigateTopTab(Routes.SENSOR_MONITORING) },
                            onOpenIrrigation = { navController.navigate(Routes.IRRIGATION) },
                            onOpenWeather = { navController.navigate(Routes.WEATHER_RISK) },
                            onOpenAlerts = { navController.navigate(Routes.ALERTS) },
                            onOpenConsultation = { navController.navigate(Routes.EXPERT_CONSULTATION) },
                            onOpenCropZones = { navController.navigate(Routes.CROP_ZONES) }
                        )
                    }

                    composable(Routes.FARM_MANAGEMENT) {
                        FarmManagementScreen(
                            farms = farms,
                            selectedFarmId = selectedFarm?.id.orEmpty(),
                            experts = experts,
                            userRole = userProfile.resolvedRole(),
                            language = settings.language,
                            onSelectFarm = settingsViewModel::selectFarm,
                            onOpenAddFarm = { navController.navigate(Routes.ADD_FARM) },
                            onOpenZones = { navController.navigate(Routes.CROP_ZONES) },
                            onShareFarmWithExpert = workspaceViewModel::shareFarmWithExpert
                        )
                    }

                    composable(Routes.ADD_FARM) {
                        AddFarmScreen(
                            language = settings.language,
                            onCreateFarm = { name, location, lat, lon, area, crop ->
                                workspaceViewModel.createFarm(
                                    name = name,
                                    locationName = location,
                                    latitude = lat,
                                    longitude = lon,
                                    areaAcres = area,
                                    primaryCrop = crop
                                ) { createdId ->
                                    settingsViewModel.selectFarm(createdId)
                                    navController.popBackStack()
                                }
                            }
                        )
                    }

                    composable(Routes.CROP_SELECTION) {
                        CropSelectionScreen(
                            language = settings.language,
                            onCreateZoneWithCrop = { crop ->
                                pendingZoneCrop = crop
                                navController.navigate(Routes.CROP_ZONES)
                            }
                        )
                    }

                    composable(Routes.CROP_ZONES) {
                        CropZonesScreen(
                            selectedFarm = selectedFarm,
                            zones = zones,
                            initialCrop = pendingZoneCrop,
                            language = settings.language,
                            userRole = userProfile.resolvedRole(),
                            onCreateZone = { farm, name, crop, variety, plantedMs, stage, area, minM, maxM, maxT ->
                                workspaceViewModel.createCropZone(
                                    farm = farm,
                                    name = name,
                                    cropType = crop,
                                    variety = variety,
                                    plantingDateEpochMs = plantedMs,
                                    growthStage = stage,
                                    areaAcres = area,
                                    minMoisture = minM,
                                    maxMoisture = maxM,
                                    maxAirTemp = maxT
                                ) {
                                    pendingZoneCrop = null
                                }
                            }
                        )
                    }

                    composable(Routes.SENSOR_MONITORING) {
                        SensorMonitoringScreen(
                            selectedFarm = selectedFarm,
                            zones = zones,
                            devices = devices,
                            readings = readings,
                            userRole = userProfile.resolvedRole(),
                            language = settings.language,
                            onRegisterDevice = workspaceViewModel::registerEsp32Device,
                            onRevokeDevice = workspaceViewModel::revokeEsp32Device,
                            onRecordManualReading = workspaceViewModel::recordManualReading,
                            onOpenCharts = { navController.navigate(Routes.SENSOR_CHARTS) }
                        )
                    }

                    composable(Routes.SENSOR_CHARTS) {
                        SensorHistoryChartsScreen(
                            selectedFarm = selectedFarm,
                            readings = readings
                        )
                    }

                    composable(Routes.CROP_SCANNER) {
                        CropImageScannerScreen(
                            selectedFarm = selectedFarm,
                            zones = zones,
                            scannerState = scannerState,
                            language = settings.language,
                            onAnalyze = { crop, bitmap, uri, farm, zone ->
                                scannerViewModel.analyzeAndSave(
                                    cropType = crop,
                                    bitmap = bitmap,
                                    imageUri = uri,
                                    farm = farm,
                                    zone = zone,
                                    settings = settings
                                )
                            },
                            onOpenResults = { navController.navigate(Routes.AI_RESULTS) }
                        )
                    }

                    composable(Routes.AI_RESULTS) {
                        val farmScans = selectedFarm?.let { f ->
                            scans.filter { it.farmId == f.id }
                        } ?: scans
                        AiDiagnosisResultsScreen(
                            scannerState = scannerState,
                            scanHistory = farmScans,
                            language = settings.language,
                            onBackToScanner = { navController.popBackStack() }
                        )
                    }

                    composable(Routes.IRRIGATION) {
                        val farmZones = selectedFarm?.let { f ->
                            zones.filter { it.farmId == f.id }
                        }.orEmpty()
                        val recommendations = remember(farmZones, readings, weatherSnapshot) {
                            farmZones.map { z -> workspaceViewModel.computeIrrigationForZone(z) }
                        }
                        IrrigationRecommendationsScreen(
                            selectedFarm = selectedFarm,
                            zones = zones,
                            recommendations = recommendations,
                            userRole = userProfile.resolvedRole(),
                            language = settings.language,
                            isOnline = isOnline,
                            onUpdateThresholds = workspaceViewModel::updateZoneThresholds
                        )
                    }

                    composable(Routes.WEATHER_RISK) {
                        val assessment = remember(selectedFarm, weatherSnapshot, readings) {
                            workspaceViewModel.evaluateWeatherRisk(selectedFarm)
                        }
                        WeatherAndRiskScreen(
                            selectedFarm = selectedFarm,
                            assessment = assessment,
                            language = settings.language,
                            onRefreshWeather = {
                                workspaceViewModel.refreshWeather(selectedFarm, settings)
                            }
                        )
                    }

                    composable(Routes.ALERTS) {
                        val farmAlerts = selectedFarm?.let { f ->
                            alerts.filter { it.farmId == f.id }
                        } ?: alerts
                        AlertsScreen(
                            alerts = farmAlerts,
                            isOnline = isOnline,
                            onAcknowledge = workspaceViewModel::acknowledgeAlert
                        )
                    }

                    composable(Routes.FARM_ANALYTICS) {
                        FarmAnalyticsScreen(
                            selectedFarm = selectedFarm,
                            readings = readings,
                            alerts = alerts,
                            scans = scans
                        )
                    }

                    composable(Routes.EXPERT_CONSULTATION) {
                        val farmId = selectedFarm?.id.orEmpty()
                        val role = userProfile.resolvedRole()
                        val consultFlow = remember(farmId, role) {
                            if (farmId.isBlank()) flowOf(emptyList())
                            else workspaceViewModel.observeConsultation(farmId, role)
                        }
                        val messages by consultFlow.collectAsStateWithLifecycle(initialValue = emptyList())
                        val farmScans = selectedFarm?.let { f ->
                            scans.filter { it.farmId == f.id }
                        }.orEmpty()
                        ExpertConsultationScreen(
                            selectedFarm = selectedFarm,
                            messages = messages,
                            farmScans = farmScans,
                            userRole = userProfile.resolvedRole(),
                            onSendMessage = { farm, text ->
                                workspaceViewModel.sendConsultationMessage(farm, text)
                            },
                            onVerifyScan = workspaceViewModel::verifyScanAsExpert
                        )
                    }

                    composable(Routes.NOTIFICATIONS) {
                        NotificationsScreen(
                            alerts = alerts,
                            lastSyncedEpochMs = settings.lastSyncedEpochMs,
                            isOnline = isOnline
                        )
                    }

                    composable(Routes.PROFILE) {
                        ProfileScreen(
                            user = userProfile,
                            language = settings.language,
                            onUpdateProfile = { name, district, phone, crops ->
                                workspaceViewModel.registerFarmerProfile(
                                    displayName = name,
                                    district = district,
                                    phone = phone,
                                    language = settings.language,
                                    cropsGrown = crops
                                )
                            },
                            onSignOut = onSignOut
                        )
                    }

                    composable(Routes.SETTINGS) {
                        SettingsScreen(
                            settings = settings,
                            onSaveSettings = settingsViewModel::updateBackendConfig,
                            onOpenLanguage = { navController.navigate(Routes.LANGUAGE) }
                        )
                    }

                    composable(Routes.LANGUAGE) {
                        LanguageSelectionScreen(
                            currentLanguage = settings.language,
                            onSelectLanguage = settingsViewModel::setLanguage
                        )
                    }

                    composable(Routes.ADMIN_MANAGEMENT) {
                        AdminManagementScreen(
                            currentUser = userProfile,
                            allUsers = adminUsers,
                            devices = devices,
                            onUpdateRole = workspaceViewModel::adminUpdateUserRole,
                            onRevokeDevice = workspaceViewModel::revokeEsp32Device
                        )
                    }

                    composable(Routes.MORE_HUB) {
                        MoreHubScreen(
                            userRole = userProfile.resolvedRole(),
                            onNavigate = { targetRoute ->
                                navController.navigate(targetRoute)
                            }
                        )
                    }
                }
            }
        }
    }
}
