package com.sfdnsapp.pro.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.sfdnsapp.pro.data.DnsServer
import com.sfdnsapp.pro.ui.components.ActiveServerCard
import com.sfdnsapp.pro.ui.components.ChangelogModal
import com.sfdnsapp.pro.ui.components.CustomDnsDialog
import com.sfdnsapp.pro.ui.components.CyberConnectButton
import com.sfdnsapp.pro.ui.components.CyberHeader
import com.sfdnsapp.pro.ui.components.GamingHubDialog
import com.sfdnsapp.pro.ui.components.MetricsDashboard
import com.sfdnsapp.pro.ui.components.QuickActionHub
import com.sfdnsapp.pro.ui.components.RadarSpeedTestDialog
import com.sfdnsapp.pro.ui.components.ServerListBottomSheet
import com.sfdnsapp.pro.ui.components.SettingsDialog
import com.sfdnsapp.pro.ui.components.SplitTunnelDialog
import com.sfdnsapp.pro.ui.components.SupportModal
import com.sfdnsapp.pro.ui.components.VipModal
import com.sfdnsapp.pro.ui.theme.CyberBg
import com.sfdnsapp.pro.ui.theme.NeonCyan
import com.sfdnsapp.pro.ui.theme.NeonGreen
import com.sfdnsapp.pro.viewmodel.DnsViewModel
import kotlinx.coroutines.launch
import androidx.compose.material3.ExperimentalMaterial3Api

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: DnsViewModel,
    onToggleConnect: () -> Unit,
    protocolSwitch: @Composable () -> Unit = {}
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val selectedDns by viewModel.selectedDns.collectAsState()
    val dnsList by viewModel.dnsList.collectAsState()
    val pingMap by viewModel.pingMap.collectAsState()
    val gamePingMap by viewModel.gamePingMap.collectAsState()
    val metrics by viewModel.metrics.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val isRadarRunning by viewModel.isRadarRunning.collectAsState()
    val radarProgress by viewModel.radarProgress.collectAsState()
    val radarFastestServer by viewModel.radarFastestServer.collectAsState()
    val installedApps by viewModel.installedApps.collectAsState()
    val installedGamePackages by viewModel.installedGamePackages.collectAsState()
    val bypassPackages by viewModel.bypassPackages.collectAsState()

    val isPersian = settings.language == "fa"
    val layoutDirection = if (isPersian) LayoutDirection.Rtl else LayoutDirection.Ltr

    // Modal dialog visibility states
    var showServerList by remember { mutableStateOf(false) }
    var showRadar by remember { mutableStateOf(false) }
    var showGamingHub by remember { mutableStateOf(false) }
    var showCustomDns by remember { mutableStateOf(false) }
    var showSplitTunnel by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showVip by remember { mutableStateOf(false) }
    var showSupport by remember { mutableStateOf(false) }
    var showChangelog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0)
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Cyber background elements
                CyberBackgroundCanvas()

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header
                    CyberHeader(
                        isPersian = isPersian,
                        onOpenVip = { showVip = true },
                        onOpenChangelog = { showChangelog = true },
                        onOpenSupport = { showSupport = true },
                        onOpenSettings = { showSettings = true }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    protocolSwitch()

                    Spacer(modifier = Modifier.height(6.dp))

                    // Active Server Selector Card
                    ActiveServerCard(
                        server = selectedDns,
                        livePing = pingMap[selectedDns.id],
                        isPersian = isPersian,
                        onClick = { showServerList = true }
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Center Cyber Power Connect Button
                    CyberConnectButton(
                        connectionState = connectionState,
                        isPersian = isPersian,
                        onClick = onToggleConnect
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Metrics Dashboard (Ping, Duration, Download, Upload)
                    MetricsDashboard(
                        metrics = metrics,
                        isPersian = isPersian
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Quick Action Hub (Radar, Gaming, Bypass, Custom DNS)
                    QuickActionHub(
                        isPersian = isPersian,
                        onOpenRadar = {
                            showRadar = true
                            viewModel.runRadarSpeedTest()
                        },
                        onOpenGaming = {
                            showGamingHub = true
                            viewModel.pingAllGames()
                        },
                        onOpenSplitTunnel = { showSplitTunnel = true },
                        onOpenCustomDns = { showCustomDns = true }
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Modal Dialogs
                if (showServerList) {
                    ServerListBottomSheet(
                        servers = dnsList,
                        selectedServer = selectedDns,
                        pingMap = pingMap,
                        isPersian = isPersian,
                        onSelectServer = { s ->
                            viewModel.selectDns(s)
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    if (isPersian) "سرور ${s.faName} انتخاب شد" else "Selected ${s.name}"
                                )
                            }
                        },
                        onPingAll = { viewModel.pingAllServers() },
                        onOpenAddCustom = { showCustomDns = true },
                        onDeleteCustom = { id -> viewModel.deleteCustomDns(id) },
                        onDismiss = { showServerList = false }
                    )
                }

                if (showRadar) {
                    RadarSpeedTestDialog(
                        isScanning = isRadarRunning,
                        progress = radarProgress,
                        fastestServer = radarFastestServer,
                        servers = dnsList,
                        pingMap = pingMap,
                        isPersian = isPersian,
                        onStartScan = { viewModel.runRadarSpeedTest() },
                        onConnectFastest = { s ->
                            viewModel.selectDns(s)
                            if (connectionState != "connected") {
                                onToggleConnect()
                            }
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    if (isPersian) "متصل به سریع‌ترین سرور: ${s.faName} 🚀" else "Connected to fastest server: ${s.name} 🚀"
                                )
                            }
                        },
                        onDismiss = { showRadar = false }
                    )
                }

                if (showGamingHub) {
                    GamingHubDialog(
                        gamePingMap = gamePingMap,
                        installedPackages = installedGamePackages,
                        isPersian = isPersian,
                        onPingAllGames = { viewModel.pingAllGames() },
                        onDismiss = { showGamingHub = false }
                    )
                }

                if (showCustomDns) {
                    CustomDnsDialog(
                        isPersian = isPersian,
                        onSave = { name, p, s, pv6, sv6 ->
                            val success = viewModel.addCustomDns(name, p, s, pv6, sv6)
                            if (success) {
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        if (isPersian) "دی‌ان‌اس اختصاصی با موفقیت افزوده شد" else "Custom DNS added successfully"
                                    )
                                }
                            }
                        },
                        onDismiss = { showCustomDns = false }
                    )
                }

                if (showSplitTunnel) {
                    SplitTunnelDialog(
                        installedApps = installedApps,
                        bypassPackages = bypassPackages,
                        isPersian = isPersian,
                        onToggleApp = { pkg -> viewModel.toggleBypassPackage(pkg) },
                        onDismiss = { showSplitTunnel = false }
                    )
                }

                if (showSettings) {
                    SettingsDialog(
                        settings = settings,
                        isPersian = isPersian,
                        onToggleDoh = { viewModel.toggleDoh(it) },
                        onToggleIpv6 = { viewModel.toggleIpv6(it) },
                        onToggleAntiDpi = { viewModel.toggleAntiDpi(it) },
                        onToggleAutoReconnect = { viewModel.toggleAutoReconnect(it) },
                        onSelectLanguage = { viewModel.updateLanguage(it) },
                        onDismiss = { showSettings = false }
                    )
                }

                if (showVip) {
                    VipModal(isPersian = isPersian, onDismiss = { showVip = false })
                }

                if (showSupport) {
                    SupportModal(isPersian = isPersian, onDismiss = { showSupport = false })
                }

                if (showChangelog) {
                    ChangelogModal(isPersian = isPersian, onDismiss = { showChangelog = false })
                }
            }
        }
    }
}

@Composable
private fun CyberBackgroundCanvas() {
    val infiniteTransition = rememberInfiniteTransition(label = "gridAnim")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bgGlow"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // لایه‌ی گرید: کاملاً ثابته (هیچ‌وقت انیمیت نمی‌شه)، پس با
        // drawWithCache فقط یک‌بار (و هر بار که سایز عوض بشه) محاسبه
        // می‌شه، نه در هر فریم. قبلاً این خط‌ها و Brush.radialGradient
        // در هر فریم (۶۰ بار در ثانیه، برای همیشه) از نو ساخته می‌شدن که
        // باعث لگ می‌شد.
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .drawWithCache {
                    val step = 44.dp.toPx()
                    val gridColor = Color(0xFF161D2E).copy(alpha = 0.35f)
                    val verticalLines = generateSequence(0f) { it + step }
                        .takeWhile { it < size.width }
                        .toList()
                    val horizontalLines = generateSequence(0f) { it + step }
                        .takeWhile { it < size.height }
                        .toList()
                    onDrawBehind {
                        verticalLines.forEach { x ->
                            drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 0.75f)
                        }
                        horizontalLines.forEach { y ->
                            drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 0.75f)
                        }
                    }
                }
        ) {}

        // لایه‌ی درخشش بالا: شعاع/رنگ ثابته، فقط شفافیتش انیمیت می‌شه.
        // با graphicsLayer(alpha) این کار روی GPU و بدون تخصیص حافظه‌ی
        // جدید در هر فریم انجام می‌شه (بر خلاف ساخت Brush جدید در هر فریم).
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = alphaAnim }
        ) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(NeonCyan.copy(alpha = 0.08f), Color.Transparent),
                    center = Offset(size.width * 0.5f, 0f),
                    radius = size.width * 0.9f
                )
            )
        }
    }
}
