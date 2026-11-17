package com.manobseba.foundation.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bloodtype
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.manobseba.foundation.ui.components.ThemeBackdropLayer
import com.manobseba.foundation.ui.navigation.AppRoute
import com.manobseba.foundation.ui.screens.AdminScreen
import com.manobseba.foundation.ui.screens.AiHubScreen
import com.manobseba.foundation.ui.screens.AllMenuScreen
import com.manobseba.foundation.ui.screens.AppLockOverlay
import com.manobseba.foundation.ui.screens.AuthScreen
import com.manobseba.foundation.ui.screens.BloodScreen
import com.manobseba.foundation.ui.screens.CommunityScreen
import com.manobseba.foundation.ui.screens.DonateModalDialog
import com.manobseba.foundation.ui.screens.FinanceScreen
import com.manobseba.foundation.ui.screens.HomeScreen
import com.manobseba.foundation.ui.screens.IslamicScreen
import com.manobseba.foundation.ui.screens.MembersScreen
import com.manobseba.foundation.ui.screens.MyDashboardScreen
import com.manobseba.foundation.ui.screens.OverviewScreen
import com.manobseba.foundation.ui.screens.ProjectsWorksMapScreen
import com.manobseba.foundation.ui.screens.WeatherScreen
import com.manobseba.foundation.ui.theme.ManobsebaTheme
import com.manobseba.foundation.ui.viewmodel.AuthUiState
import com.manobseba.foundation.ui.viewmodel.MainViewModel

private data class TopNavItem(
    val label: String,
    val icon: ImageVector,
    val route: AppRoute,
    val tag: String
)

@Composable
fun ManobsebaApp(
    windowSizeClass: WindowSizeClass,
    vm: MainViewModel = viewModel()
) {
    val authState by vm.authState.collectAsStateWithLifecycle()
    val userProfile by vm.userProfile.collectAsStateWithLifecycle()
    val toastMsg by vm.toastMessage.collectAsStateWithLifecycle()
    val isLocked by vm.isLocked.collectAsStateWithLifecycle()

    val paletteKey = userProfile?.themePalette ?: "green"
    val bgMode = userProfile?.themeBg ?: "plain"
    val darkTheme = userProfile?.darkMode ?: false

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(toastMsg) {
        val m = toastMsg
        if (!m.isNullOrBlank()) {
            snackbarHostState.showSnackbar(m)
            vm.clearToast()
        }
    }

    ManobsebaTheme(paletteKey = paletteKey, darkTheme = darkTheme) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (authState) {
                is AuthUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is AuthUiState.SignedOut -> {
                    AuthScreen(onSignedInSuccess = { })
                }
                is AuthUiState.SignedIn -> {
                    AuthenticatedShell(
                        windowSizeClass = windowSizeClass,
                        vm = vm,
                        bgMode = bgMode,
                        snackbarHostState = snackbarHostState
                    )
                    if (isLocked) {
                        AppLockOverlay(
                            lockManager = vm.lockManager,
                            onUnlocked = { vm.unlockApp() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthenticatedShell(
    windowSizeClass: WindowSizeClass,
    vm: MainViewModel,
    bgMode: String,
    snackbarHostState: SnackbarHostState
) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val curDest = backStack?.destination
    var showDonateModal by remember { mutableStateOf(false) }

    val topItems = listOf(
        TopNavItem("হোম", Icons.Default.Home, AppRoute.Home, "nav_home"),
        TopNavItem("আয়-ব্যয়", Icons.Default.AccountBalance, AppRoute.Finance, "nav_finance"),
        TopNavItem("রক্ত SOS", Icons.Default.Bloodtype, AppRoute.Blood, "nav_blood"),
        TopNavItem("ইসলামিক", Icons.Default.Mosque, AppRoute.Islamic, "nav_islamic"),
        TopNavItem("সব মেনু", Icons.Default.Apps, AppRoute.AllMenu, "nav_all_menu")
    )

    fun navigateTo(route: AppRoute) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    val useRail = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact
    val isOnAiHub = curDest?.hasRoute(AppRoute.AiHub::class) == true

    Row(modifier = Modifier.fillMaxSize()) {
        if (useRail) {
            NavigationRail {
                topItems.forEach { item ->
                    val selected = curDest?.hasRoute(item.route::class) == true
                    NavigationRailItem(
                        selected = selected,
                        onClick = { navigateTo(item.route) },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        modifier = Modifier.testTag(item.tag)
                    )
                }
            }
        }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            floatingActionButton = {
                if (!isOnAiHub) {
                    FloatingActionButton(
                        onClick = { navigateTo(AppRoute.AiHub) },
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary,
                        modifier = Modifier.testTag("fab_manobseba_ai")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "মানবসেবা AI")
                    }
                }
            },
            bottomBar = {
                if (!useRail) {
                    NavigationBar {
                        topItems.forEach { item ->
                            val selected = curDest?.hasRoute(item.route::class) == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTo(item.route) },
                                icon = { Icon(item.icon, contentDescription = item.label) },
                                label = { Text(item.label, maxLines = 1) },
                                modifier = Modifier.testTag(item.tag)
                            )
                        }
                    }
                }
            }
        ) { innerPad ->
            Box(modifier = Modifier.fillMaxSize().padding(innerPad)) {
                ThemeBackdropLayer(bgMode = bgMode, primary = MaterialTheme.colorScheme.primary)
                NavHost(
                    navController = navController,
                    startDestination = AppRoute.Home,
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable<AppRoute.Home> {
                        HomeScreen(
                            vm = vm,
                            onOpenDonate = { showDonateModal = true },
                            onOpenPublicForm = { navigateTo(AppRoute.Community) },
                            onGoBlood = { navigateTo(AppRoute.Blood) },
                            onGoFinance = { navigateTo(AppRoute.Finance) },
                            onGoProjects = { navigateTo(AppRoute.ProjectsWorksMap) },
                            onGoIslamic = { navigateTo(AppRoute.Islamic) },
                            onGoWeather = { navigateTo(AppRoute.Weather) },
                            onGoAiHub = { navigateTo(AppRoute.AiHub) },
                            onGoOverview = { navigateTo(AppRoute.Overview) }
                        )
                    }
                    composable<AppRoute.Overview> { OverviewScreen(vm = vm) }
                    composable<AppRoute.AiHub> { AiHubScreen(vm = vm) }
                    composable<AppRoute.Finance> { FinanceScreen(vm = vm) }
                    composable<AppRoute.Blood> { BloodScreen(vm = vm) }
                    composable<AppRoute.Islamic> { IslamicScreen(vm = vm) }
                    composable<AppRoute.Weather> { WeatherScreen(vm = vm) }
                    composable<AppRoute.ProjectsWorksMap> { ProjectsWorksMapScreen(vm = vm) }
                    composable<AppRoute.Members> { MembersScreen(vm = vm) }
                    composable<AppRoute.MyDashboard> {
                        MyDashboardScreen(vm = vm, onSignedOut = { /* authListener transitions automatically */ })
                    }
                    composable<AppRoute.Community> { CommunityScreen(vm = vm) }
                    composable<AppRoute.Admin> { AdminScreen(vm = vm) }
                    composable<AppRoute.AllMenu> {
                        AllMenuScreen(vm = vm, onNavigate = { route -> navigateTo(route) })
                    }
                }
            }
        }
    }

    if (showDonateModal) {
        DonateModalDialog(vm = vm, onDismiss = { showDonateModal = false })
    }
}
