@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.data.CargoListing
import com.example.data.DriverProfile
import com.example.data.WalletTransaction
import com.example.data.Waybill

// Professional Polish Theme Colors
object AppThemeState {
    var selectedColorIndex by mutableStateOf(0)
}

private val BrandNavy: Color get() = when (AppThemeState.selectedColorIndex) {
    1 -> Color(0xFFEF4444) // Red
    2 -> Color(0xFF10B981) // Green
    3 -> Color(0xFFF97316) // Orange
    4 -> Color(0xFF6366F1) // Purple/Indigo
    else -> Color(0xFF2563EB) // Professional Blue (Tailwind blue-600)
}

private val BrandBlueLight: Color get() = when (AppThemeState.selectedColorIndex) {
    1 -> Color(0xFFF87171)
    2 -> Color(0xFF34D399)
    3 -> Color(0xFFFB923C)
    4 -> Color(0xFF818CF8)
    else -> Color(0xFF3B82F6) // Medium Blue (Tailwind blue-500)
}

private val BrandBlueBg: Color get() = when (AppThemeState.selectedColorIndex) {
    1 -> Color(0xFFFEF2F2)
    2 -> Color(0xFFECFDF5)
    3 -> Color(0xFFFFF7ED)
    4 -> Color(0xFFEEF2FF)
    else -> Color(0xFFEFF6FF) // Light Highlight Blue (Tailwind blue-50)
}

private val BrandBlueBorder: Color get() = when (AppThemeState.selectedColorIndex) {
    1 -> Color(0xFFFEE2E2)
    2 -> Color(0xFFD1FAE5)
    3 -> Color(0xFFFFEDD5)
    4 -> Color(0xFFE0E7FF)
    else -> Color(0xFFDBEAFE) // Tailwind blue-100
}
private val BrandAmber = Color(0xFFF97316)      // Warning / Alert / Active (Tailwind orange-500)
private val BrandAmberBg = Color(0xFFFFEDD5)    // Tailwind orange-100
private val BrandGreen = Color(0xFF22C55E)      // Done / Verification Green (Tailwind green-500)
private val BrandGreenBg = Color(0xFFDCFCE7)    // Tailwind green-100
private val BrandGreenText = Color(0xFF15803D)  // Tailwind green-700
private val CanvasBg = Color(0xFFF7F9FB)        // Beautiful Clean Light Slate Backdrop
private val CardSurface = Color(0xFFFFFFFF)     // Clean white surface
private val TextPrimary = Color(0xFF0F172A)     // Slate-900 - Rich text
private val TextSecondary = Color(0xFF64748B)   // Slate-500 - Muted text
private val TextMuted = Color(0xFF94A3B8)       // Slate-400 - Very muted text
private val BorderSlate = Color(0xFFF1F5F9)     // Slate-100 - Extra light border

data class PermissionDialogInfo(
    val title: String,
    val description: String,
    val permissions: List<String>,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val rationale: String,
    val onGranted: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContainer(viewModel: DriverViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var permissionDialogState by remember { mutableStateOf<PermissionDialogInfo?>(null) }

    LaunchedEffect(Unit) {
        viewModel.initializeAppRef()
    }

    LaunchedEffect(viewModel.selectedThemeColor) {
        AppThemeState.selectedColorIndex = viewModel.selectedThemeColor
    }

    if (viewModel.showSplashScreen) {
        SplashScreenView(viewModel = viewModel)
        return
    }

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val currentInfo = permissionDialogState
        if (currentInfo != null) {
            val allGranted = results.values.all { it }
            if (allGranted) {
                android.widget.Toast.makeText(context, "✅ 已成功激活 “" + currentInfo.title + "” 安全授信保障", android.widget.Toast.LENGTH_SHORT).show()
                currentInfo.onGranted()
            } else {
                android.widget.Toast.makeText(context, "⚠️ 权限被拒绝，请在系统设置中手动开启“" + currentInfo.title + "”所需权限", android.widget.Toast.LENGTH_LONG).show()
            }
            permissionDialogState = null
        }
    }

    val checkAndRequestPermission = { title: String, desc: String, requiredPermissions: List<String>, icon: androidx.compose.ui.graphics.vector.ImageVector, rationale: String, onGranted: () -> Unit ->
        val misses = requiredPermissions.filter {
            androidx.core.content.ContextCompat.checkSelfPermission(context, it) != android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        if (misses.isEmpty()) {
            onGranted()
        } else {
            permissionDialogState = PermissionDialogInfo(
                title = title,
                description = desc,
                permissions = requiredPermissions,
                icon = icon,
                rationale = rationale,
                onGranted = onGranted
            )
        }
    }

    if (!viewModel.isLoggedIn) {
        LoginScreen(viewModel = viewModel)
        return
    }

    val currentTab = viewModel.currentTab
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val cargoList by viewModel.cargoListings.collectAsStateWithLifecycle()
    val waybillsList by viewModel.waybills.collectAsStateWithLifecycle()
    val transactionList by viewModel.transactions.collectAsStateWithLifecycle()
    val activeWaybillId = viewModel.activeWaybillId

    if (viewModel.showRefuelingScreen) {
        RefuelingScreen(viewModel = viewModel)
        return
    }

    if (viewModel.showBankCardsScreen) {
        BankCardsScreen(viewModel = viewModel)
        return
    }

    if (viewModel.showMessageCenterScreen) {
        MessageCenterScreen(viewModel = viewModel)
        return
    }

    if (viewModel.showReviewsScreen) {
        ReviewsScreen(viewModel = viewModel)
        return
    }

    if (viewModel.showSettingsScreen) {
        SettingsScreen(viewModel = viewModel)
        return
    }

    if (viewModel.showUpdateDialog) {
        VersionUpdateDialog(viewModel = viewModel)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val firstChar = profile?.name?.take(1) ?: "王"
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(BrandNavy),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = firstChar,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = profile?.name ?: "王师傅",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = TextPrimary
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(BrandGreen)
                                    )
                                    Text(
                                        text = "接单中",
                                        fontSize = 10.sp,
                                        color = TextSecondary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        // Right-aligned active title indicator
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(BrandBlueBg)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = when (currentTab) {
                                    0 -> "货源大厅"
                                    1 -> if (activeWaybillId != null) "运单详情" else "我的运单"
                                    2 -> "AI教练"
                                    3 -> "司机钱包"
                                    else -> "个人中心"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = BrandNavy
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = TextPrimary
                ),
                actions = {
                    if (currentTab == 4 && profile?.idCardVerified == false) {
                        AssistChip(
                            onClick = { viewModel.showCertifyDialog = true },
                            label = { Text("一键实名认证", color = Color.White, fontSize = 11.sp) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = BrandAmber,
                                leadingIconContentColor = Color.White
                            ),
                            leadingIcon = {
                                Icon(Icons.Filled.Verified, contentDescription = "认证", modifier = Modifier.size(12.dp))
                            },
                            border = null,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                    } else {
                        IconButton(
                            onClick = { viewModel.showMessageCenterScreen = true },
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "通知中心",
                                tint = TextSecondary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                },
                modifier = Modifier.drawBehind {
                    // Draw bottom border-slate-100 separator line
                    drawLine(
                        color = BorderSlate,
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 0.dp,
                windowInsets = WindowInsets.navigationBars,
                modifier = Modifier.drawBehind {
                    // Draw top border-slate-100 separator line
                    drawLine(
                        color = BorderSlate,
                        start = Offset(0f, 0f),
                        end = Offset(size.width, 0f),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            ) {
                val items = listOf("货源大厅", "我的运单", "AI教练", "司机钱包", "个人中心")
                val selectedIcons = listOf(
                    Icons.Filled.LocalMall,
                    Icons.Filled.LocalShipping,
                    Icons.Filled.SupportAgent,
                    Icons.Filled.AccountBalanceWallet,
                    Icons.Filled.Person
                )
                val unselectedIcons = listOf(
                    Icons.Outlined.LocalMall,
                    Icons.Outlined.LocalShipping,
                    Icons.Outlined.SupportAgent,
                    Icons.Outlined.AccountBalanceWallet,
                    Icons.Outlined.Person
                )

                items.forEachIndexed { index, label ->
                    NavigationBarItem(
                        selected = currentTab == index,
                        onClick = { viewModel.selectTab(index) },
                        icon = {
                            Icon(
                                imageVector = if (currentTab == index) selectedIcons[index] else unselectedIcons[index],
                                contentDescription = label
                            )
                        },
                        label = { Text(label, fontSize = 11.sp, fontWeight = if (currentTab == index) FontWeight.Bold else FontWeight.Medium) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BrandNavy,
                            selectedTextColor = BrandNavy,
                            indicatorColor = BrandBlueBg,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary
                        )
                    )
                }
            }
        },
        floatingActionButton = {
            if (currentTab == 0) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.showScanDialog = true },
                    containerColor = BrandNavy,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(18.dp),
                    elevation = FloatingActionButtonDefaults.elevation(6.dp),
                    icon = { Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = "扫码接单", modifier = Modifier.size(18.dp)) },
                    text = { Text("扫码接单", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
            }
        },
        containerColor = CanvasBg
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // Screen transition animation
            AnimatedContent(
                targetState = when {
                    viewModel.selectedCargoId != null && currentTab == 0 -> 98
                    activeWaybillId != null && currentTab == 1 -> 99
                    else -> currentTab
                },
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "Navigation"
            ) { statusTab ->
                when (statusTab) {
                    0 -> CargoListingScreen(
                        cargoList = cargoList,
                        profile = profile,
                        viewModel = viewModel
                    )
                    1 -> WaybillListScreen(
                        waybills = waybillsList,
                        viewModel = viewModel
                    )
                    2 -> AiCoachScreen(viewModel = viewModel)
                    3 -> WalletScreen(
                        profile = profile,
                        transactions = transactionList,
                        viewModel = viewModel
                    )
                    4 -> when (viewModel.profileSubTab) {
                        1 -> PersonalProfileScreen(profile = profile, viewModel = viewModel)
                        2 -> VehicleProfileScreen(profile = profile, viewModel = viewModel)
                        3 -> PermissionsManagementScreen(viewModel = viewModel)
                        else -> ProfileScreen(
                            profile = profile,
                            viewModel = viewModel,
                            checkAndRequestPermission = checkAndRequestPermission
                        )
                    }
                    98 -> {
                        val currentCargoId = viewModel.selectedCargoId
                        if (currentCargoId != null) {
                            CargoDetailScreen(
                                cargoId = currentCargoId,
                                cargoList = cargoList,
                                profile = profile,
                                viewModel = viewModel
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxSize())
                        }
                    }
                    99 -> {
                        val currentActiveId = activeWaybillId
                        if (currentActiveId != null) {
                            WaybillDetailScreen(
                                waybillId = currentActiveId,
                                waybills = waybillsList,
                                viewModel = viewModel,
                                checkAndRequestPermission = checkAndRequestPermission
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxSize())
                        }
                    }
                }
            }

            // Global Loading Indicator / Toast Simulator
            if (viewModel.isProcessing) {
                Dialog(onDismissRequest = {}) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .padding(16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                color = BrandNavy,
                                strokeWidth = 4.dp,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = viewModel.processMessage,
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // Wallet Withdraw dialog
            if (viewModel.showWithdrawDialog) {
                WithdrawMoneyDialog(viewModel, profile)
            }

            // Form certification dialog
            if (viewModel.showCertifyDialog) {
                CertifyCredentialsDialog(viewModel, profile)
            }

            // Scan QR order dialog
            if (viewModel.showScanDialog) {
                ScanQRCodeDialog(viewModel)
            }

            // Dynamic Permission Dialog
            permissionDialogState?.let { dialogInfo ->
                Dialog(onDismissRequest = { permissionDialogState = null }) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(24.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Title header with badge
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(BrandNavy.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = dialogInfo.icon,
                                    contentDescription = null,
                                    tint = BrandNavy,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text(
                                text = "服务权限与合规授信申请",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary,
                                letterSpacing = 1.sp
                            )
                            
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            Text(
                                text = dialogInfo.title,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandNavy,
                                textAlign = TextAlign.Center
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = CanvasBg),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "💡 卡友服务说明：",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = dialogInfo.description,
                                        fontSize = 11.sp,
                                        color = TextSecondary,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            HorizontalDivider(color = Color(0xFFF1F5F9))
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Text(
                                text = "⚠️ 公路法及多式联运监管须知：",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandAmber,
                                modifier = Modifier.align(Alignment.Start)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = dialogInfo.rationale,
                                fontSize = 11.sp,
                                color = TextSecondary,
                                lineHeight = 15.sp,
                                modifier = Modifier.align(Alignment.Start)
                            )
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { permissionDialogState = null },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                                ) {
                                    Text("暂不授权", color = TextSecondary)
                                }
                                
                                Button(
                                    onClick = {
                                        permissionLauncher.launch(dialogInfo.permissions.toTypedArray())
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandNavy)
                                ) {
                                    Text("允许并授权", color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ======================== TABS IMPLEMENTATION ========================

@Composable
fun DailyStatsGrid(totalRevenue: Double, activeCount: Int, completedCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Card(
            modifier = Modifier.weight(1.5f),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSlate)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "今日结算总收入 (元)",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = String.format("%,.2f", totalRevenue),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2563EB),
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSlate)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "今日运输单数 (单)",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "$completedCount ",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "/ ${completedCount + activeCount}",
                        fontSize = 12.sp,
                        color = TextMuted,
                        fontFamily = FontFamily.SansSerif,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ActiveMissionCard(waybill: Waybill, onClickAction: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(2.dp, BrandBlueBorder)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BrandBlueBg)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(BrandAmber)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "进行中运输任务",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandNavy
                    )
                }
                Text(
                    text = "单号: DZ${waybill.id + 20261000}",
                    fontSize = 11.sp,
                    color = BrandNavy,
                    fontWeight = FontWeight.Medium
                )
            }

            Column(modifier = Modifier.padding(14.dp)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .width(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(2.dp, BrandNavy, CircleShape)
                        )
                        Box(
                            modifier = Modifier
                                .size(width = 2.dp, height = 30.dp)
                                .background(BorderSlate)
                        )
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(BrandAmber)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column {
                            Text(text = "装货地", fontSize = 10.sp, color = TextMuted)
                            Text(text = waybill.departure, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        Column {
                            Text(text = "卸货地", fontSize = 10.sp, color = TextMuted)
                            Text(text = waybill.destination, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Divider(color = BorderSlate, thickness = 1.dp)
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(CanvasBg)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(text = waybill.cargoName, fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(CanvasBg)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(text = "载重 ${waybill.targetTons}t", fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Button(
                        onClick = onClickAction,
                        colors = ButtonDefaults.buttonColors(containerColor = BrandNavy),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text(
                            text = when (waybill.status) {
                                "CREATED" -> "开始启运"
                                "LOADED" -> "在途在轨"
                                "UNLOADED" -> "卸货称重"
                                "AUDITING" -> "审核结算"
                                else -> "查看详情"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CargoListingScreen(
    cargoList: List<CargoListing>,
    profile: DriverProfile?,
    viewModel: DriverViewModel
) {
    var searchQuery by remember { mutableStateOf("") }
    val selectedCategory = remember { mutableStateOf("全部货源") }
    val waybillsList by viewModel.waybills.collectAsStateWithLifecycle()

    // Calculate dynamic values for Stats
    val walletBalance = profile?.balance ?: 3450.0
    val pendingSettlement = profile?.pendingSettlement ?: 2850.0
    val totalRevenue = walletBalance + pendingSettlement
    val completedCount = waybillsList.count { it.status == "PAID" }
    val activeCount = waybillsList.count { it.status != "PAID" }
    val activeWaybill = waybillsList.find { it.status != "PAID" }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "真车真货 · 自助确权",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandNavy
                        )
                        Text(
                            text = "陕煤陕钢网络货运承运平台 • 资金银行清算托管",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(BrandGreenBg)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (profile?.vehicleVerified == true) "卡车已核验" else "等待车辆核验",
                            fontSize = 10.sp,
                            color = if (profile?.vehicleVerified == true) BrandGreenText else BrandAmber,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "证件到期提醒",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "证件到期提醒与合规督办提示",
                            color = Color(0xFF991B1B),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "驾驶证 · A2（暂设到期）",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF374151)
                                )
                                Text(
                                    text = "到期日期: 2026-12-15 (还有 30 天期)",
                                    fontSize = 11.sp,
                                    color = Color(0xFFDC2626),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Text(
                                text = "立即更新 →",
                                fontSize = 11.sp,
                                color = BrandNavy,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable {
                                    viewModel.currentTab = 4
                                    viewModel.profileSubTab = 1
                                }
                            )
                        }
                        Divider(color = Color(0xFFFEE2E2), thickness = 1.dp)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "重型货车道路运输执照",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF374151)
                                )
                                Text(
                                    text = "到期日期: 2026-08-20 (还有 83 天期)",
                                    fontSize = 11.sp,
                                    color = Color(0xFFD97706),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Text(
                                text = "立即更新 →",
                                fontSize = 11.sp,
                                color = BrandNavy,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable {
                                    viewModel.currentTab = 4
                                    viewModel.profileSubTab = 2
                                }
                            )
                        }
                    }
                }
            }
        }

        item {
            DailyStatsGrid(
                totalRevenue = totalRevenue,
                activeCount = activeCount,
                completedCount = completedCount
            )
        }

        if (activeWaybill != null) {
            item {
                ActiveMissionCard(waybill = activeWaybill) {
                    viewModel.selectWaybill(activeWaybill.id)
                    viewModel.currentTab = 1
                }
            }
        }

        item {
            Column(modifier = Modifier.padding(top = 10.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "附近待抢单推荐货源",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "实时刷新",
                        fontSize = 11.sp,
                        color = BrandNavy,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("全部货源", "陕西内煤", "省际重载", "机制骨料", "成品钢材").forEach { cat ->
                        val isSelected = selectedCategory.value == cat
                        Button(
                            onClick = { selectedCategory.value = cat },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) BrandNavy else Color.White,
                                contentColor = if (isSelected) Color.White else TextSecondary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp),
                            border = if (!isSelected) androidx.compose.foundation.BorderStroke(1.dp, BorderSlate) else null
                        ) {
                            Text(text = cat, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        val filteredList = cargoList.filter {
            selectedCategory.value == "全部货源" ||
            (selectedCategory.value == "陕西内煤" && it.cargoName.contains("煤")) ||
            (selectedCategory.value == "省际重载" && it.distance > 300) ||
            (selectedCategory.value == "机制骨料" && it.cargoName.contains("骨料")) ||
            (selectedCategory.value == "成品钢材" && it.cargoName.contains("钢"))
        }

        if (filteredList.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.Inventory,
                            contentDescription = "暂无货物",
                            tint = TextMuted,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "当前分类没有推荐货源\n试试查阅 [全部货源] 发现高价单",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(filteredList) { cargo ->
                CargoItemCard(cargo = cargo, profile = profile, viewModel = viewModel)
            }
        }
    }
}

@Composable
fun CargoItemCard(cargo: CargoListing, profile: DriverProfile?, viewModel: DriverViewModel) {
    var showDialogAlert by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 5.dp)
            .clickable { viewModel.selectedCargoId = cargo.id },
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSlate)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(BrandGreenBg)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "即时",
                            color = BrandGreenText,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "${cargo.departureCity} → ${cargo.destinationCity}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "货物: ${cargo.cargoName} | 合规吨数: ${cargo.remainedTons}吨 | 距离: ${cargo.distance}km",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
                
                Text(
                    text = "运价单结: ¥${cargo.pricePerTon}/吨 • ${cargo.publisher}",
                    fontSize = 10.sp,
                    color = TextMuted
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(start = 10.dp)
            ) {
                IconButton(
                    onClick = {
                        if (profile?.idCardVerified == false || profile?.vehicleVerified == false) {
                            showDialogAlert = true
                        } else {
                            viewModel.grabOrder(cargo) {}
                        }
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .border(1.dp, BrandBlueBorder, CircleShape)
                        .background(BrandBlueBg)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "抢单",
                        tint = BrandNavy,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "抢单",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandNavy
                )
            }
        }
    }

    if (showDialogAlert) {
        AlertDialog(
            onDismissRequest = { showDialogAlert = false },
            title = { Text("平台提示：资质尚未核验", fontWeight = FontWeight.Bold) },
            text = { Text("应交通主管部门道路网络货运相关安全条例：司机接单抢配货款前，必须完成实名、驾照及载重卡车行驶证核验比对。本过程完全免费。") },
            confirmButton = {
                Button(
                    onClick = {
                        showDialogAlert = false
                        viewModel.showCertifyDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandAmber)
                ) {
                    Text("立即一键认证", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialogAlert = false }) {
                    Text("先看看", color = TextSecondary)
                }
            }
        )
    }
}

val BrandLightIcon = Color(0xFF9CA3AF)

// ======================== TABS: MY WAYBILLS ========================

@Composable
fun WaybillListScreen(
    waybills: List<Waybill>,
    viewModel: DriverViewModel
) {
    var selectedStatusTab by remember { mutableStateOf("全部") }
    val statuses = listOf("全部", "去装货", "运输中", "待审核", "结算完结")

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            statuses.forEach { tab ->
                val isSelected = selectedStatusTab == tab
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { selectedStatusTab = tab }
                        .padding(vertical = 8.dp)
                        .weight(1f)
                ) {
                    Text(
                        text = tab,
                        color = if (isSelected) BrandNavy else TextSecondary,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .size(width = 24.dp, height = 3.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) BrandNavy else Color.Transparent)
                    )
                }
            }
        }

        val filteredWaybills = waybills.filter { wb ->
            when (selectedStatusTab) {
                "去装货" -> wb.status == "CREATED"
                "运输中" -> wb.status == "LOADED"
                "待审核" -> wb.status == "UNLOADED" || wb.status == "AUDITING"
                "结算完结" -> wb.status == "PAID"
                else -> true
            }
        }

        if (filteredWaybills.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.DriveEta,
                        contentDescription = "暂无运单",
                        tint = TextMuted,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "暂无该状态下的网络货运电子运单。双向确权中，完成装煤并卸煤首磅即可发起结算！",
                        textAlign = TextAlign.Center,
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Button(
                        onClick = { viewModel.currentTab = 0 },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandNavy),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("去货源大厅抢单", color = Color.White)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp, top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredWaybills) { wb ->
                    WaybillCardItem(waybill = wb, viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun WaybillCardItem(waybill: Waybill, viewModel: DriverViewModel) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp)
            .clickable { viewModel.selectWaybill(waybill.id) },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSlate)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "运单编号: DAZONG${waybill.id + 20261000}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = TextSecondary
                )
                
                val (badgeBg, badgeText, statusLabel) = when (waybill.status) {
                    "CREATED" -> Triple(BrandAmberBg, BrandAmber, "待启运")
                    "LOADED" -> Triple(BrandBlueBg, BrandNavy, "在途运输")
                    "UNLOADED" -> Triple(Color(0xFFEFF6FF), Color(0xFF1E40AF), "卸货称重")
                    "AUDITING" -> Triple(BrandAmberBg, BrandAmber, "待审结算")
                    else -> Triple(BrandGreenBg, BrandGreenText, "结算完结")
                }
                
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(badgeBg)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = statusLabel,
                        fontSize = 10.sp,
                        color = badgeText,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "承运货种: ${waybill.cargoName}",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = TextPrimary
            )
            
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(16.dp).padding(top = 2.dp)
                ) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(BrandNavy))
                    Box(modifier = Modifier.size(width = 1.dp, height = 18.dp).background(BorderSlate))
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(BrandAmber))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "发货工厂/发煤企: ${waybill.departure}",
                        fontSize = 12.sp,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "收货单位/卸煤场: ${waybill.destination}",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Divider(color = BorderSlate, thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "单吨运价: ¥${waybill.pricePerTon}/吨",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = if (waybill.status == "PAID") "金税实结运费: ¥${String.format("%.2f", waybill.freightPayment)}" else "在途承运载重: ${waybill.targetTons} 吨",
                        fontSize = 10.sp,
                        color = if (waybill.status == "PAID") BrandGreenText else TextSecondary,
                        fontWeight = if (waybill.status == "PAID") FontWeight.Bold else FontWeight.Normal
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "货运执行控制",
                        color = BrandNavy,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = null,
                        tint = BrandNavy,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

// ======================== ROUTE: DETAILED WAYBILL PROGRESS ========================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaybillDetailScreen(
    waybillId: Int,
    waybills: List<Waybill>,
    viewModel: DriverViewModel,
    checkAndRequestPermission: (String, String, List<String>, androidx.compose.ui.graphics.vector.ImageVector, String, () -> Unit) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val waybill = waybills.find { it.id == waybillId } ?: return
    var loadedWeightInput by remember { mutableStateOf("38.64") }
    var unloadedWeightInput by remember { mutableStateOf("38.21") }
    var loadingPhotoUri by remember { mutableStateOf<String?>(waybill.loadWeightImage) }
    var unloadingPhotoUri by remember { mutableStateOf<String?>(waybill.unloadWeightImage) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp)
    ) {
        // Back toolbar button
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                onClick = { viewModel.selectWaybill(null) }
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "返回", tint = BrandNavy)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("返回我的运单列表", color = BrandNavy, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }

        // Stepper Process status
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("运单执行监控", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    // Vertical visual progress line
                    val stepsList = listOf(
                        Triple("CREATED", "第①步 · 确认启运", "司机拍照采集车身照片，开始行车派单监控"),
                        Triple("LOADED", "第②步 · 在途运输", "北斗动态轨迹联网，重卡绿色环保平稳行进中"),
                        Triple("UNLOADED", "第③步 · 卸载称重", "到达受货工厂，录入卸载重量并采集收料磅单"),
                        Triple("AUDITING", "第④步 · 结算审核", "双智双磅校核无误，发起快速国家合规结算"),
                        Triple("PAID", "第⑤步 · 运发出账", "金税快速代发到账，资金支持提现本行借记卡")
                    )

                    stepsList.forEachIndexed { index, step ->
                        val currentStatusMatched = waybill.status == step.first
                        // Find current step index
                        val currentStepIndex = when(waybill.status) {
                            "CREATED" -> 0
                            "LOADED" -> 1
                            "UNLOADED" -> 2
                            "AUDITING" -> 3
                            "PAID" -> 4
                            else -> 5
                        }
                        val isDone = index < currentStepIndex
                        val isCurrent = index == currentStepIndex

                        Row(modifier = Modifier.fillMaxWidth()) {
                            // Column representing timeline dot + vertical line spacing
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.width(30.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isCurrent) BrandAmber else if (isDone) BrandNavy else Color.LightGray
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isDone) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                    } else {
                                        Text(text = "${index + 1}", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                                if (index < stepsList.size -1) {
                                    Box(
                                        modifier = Modifier
                                            .size(width = 2.dp, height = 34.dp)
                                            .background(if (isDone) BrandNavy else Color.LightGray)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            // Detail text info
                            Column(modifier = Modifier.padding(bottom = 12.dp)) {
                                Text(
                                    text = step.second,
                                    fontSize = 13.sp,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isCurrent) BrandNavy else if (isDone) TextPrimary else TextSecondary
                                )
                                Text(
                                    text = step.third,
                                    fontSize = 11.sp,
                                    color = TextSecondary.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Waybill detailed specs card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, BorderSlate)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "运单详细信息 (官方存根)",
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 14.sp
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(BrandNavy.copy(alpha = 0.1f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "单号: DAZONG${waybill.id + 20261000}",
                                fontSize = 10.sp,
                                color = BrandNavy,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    HorizontalDivider(color = BorderSlate, thickness = 1.dp, modifier = Modifier.padding(vertical = 10.dp))

                    Text(
                        text = "承运货种: " + waybill.cargoName,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "发货发布方: " + waybill.publisher, fontSize = 11.sp, color = TextSecondary)

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(16.dp).padding(top = 2.dp)
                        ) {
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(BrandGreen))
                            Box(modifier = Modifier.size(width = 1.dp, height = 16.dp).background(Color.LightGray))
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(BrandAmber))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "装货地点: " + waybill.departure,
                                fontSize = 12.sp,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "卸货地点: " + waybill.destination,
                                fontSize = 12.sp,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    HorizontalDivider(color = BorderSlate, thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("合同单吨运价", fontSize = 10.sp, color = TextSecondary)
                            Text("¥" + String.format("%.2f", waybill.pricePerTon) + " /吨", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        Column {
                            Text("派单协议载重", fontSize = 10.sp, color = TextSecondary)
                            Text(waybill.targetTons.toString() + " 吨", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("预计总运金 (税前)", fontSize = 10.sp, color = TextSecondary)
                            Text("¥" + String.format("%.2f", waybill.targetTons * waybill.pricePerTon), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = BrandGreenText)
                        }
                    }
                }
            }
        }

        // Interactive execution area
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("当前操作面板", fontWeight = FontWeight.Bold, color = BrandNavy, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(10.dp))

                    when (waybill.status) {
                        "CREATED" -> {
                            Text("您第一步需要：进行车辆外观与车牌照影像采集，随后便可正式启动车辆，开始承运启运。", color = TextPrimary, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(14.dp))

                            // Custom Camera Upload representation for Start Journey
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(115.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (loadingPhotoUri != null) BrandGreenBg.copy(alpha = 0.15f) else CanvasBg)
                                    .border(
                                        BorderStroke(
                                            1.dp,
                                            if (loadingPhotoUri != null) BrandGreen else Color(0xFFE2E8F0)
                                        ),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        checkAndRequestPermission(
                                            "车辆安全外观及车牌相机授权",
                                            "为了进行车辆装载外观及陕K·6A528车牌影像采集，大宗智联需要使用您的相机进行拍照和扫描识别。该影像将同步上报国家公路网以及煤矿智能化派单备案，保障行车安全合规性。",
                                            listOf(android.Manifest.permission.CAMERA),
                                            androidx.compose.material.icons.Icons.Filled.PhotoCamera,
                                            "大宗商品智联运输，安全资质拍照上传是货源监管的法定合规流程。开启相机可自动为您识别并记录陕K·6A528车牌及厢斗完整度。"
                                        ) {
                                            loadingPhotoUri = "truck_photo_selected"
                                            android.widget.Toast.makeText(context, "车辆装载外观及陕K·6A528车牌影像采集成功", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (loadingPhotoUri != null) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Filled.CheckCircle, contentDescription = "已采集", tint = BrandGreen, modifier = Modifier.size(32.dp))
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text("车辆照片上传成功 (已存)", fontSize = 12.sp, color = BrandGreenText, fontWeight = FontWeight.Bold)
                                        Text("系统联验识别匹配：陕K·6A528", fontSize = 10.sp, color = TextSecondary)
                                    }
                                } else {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Filled.PhotoCamera, contentDescription = "未采集", tint = TextSecondary, modifier = Modifier.size(32.dp))
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text("一键拍照采集［卡车车头车身多维合影照］", fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                        Text("(必须清晰拍到备案车牌号以进行系统校验)", fontSize = 10.sp, color = TextMuted)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = {
                                    if (loadingPhotoUri == null) {
                                        android.widget.Toast.makeText(context, "请先拍照上传车辆车头照！", android.widget.Toast.LENGTH_SHORT).show()
                                    } else {
                                        viewModel.submitLoadingTicket(waybill.id, waybill.targetTons, loadingPhotoUri!!)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = BrandNavy),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("启动发车，开始承运启运", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = {
                                    viewModel.cancelWaybill(waybill.id) {
                                        viewModel.selectWaybill(null)
                                    }
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                                border = BorderStroke(1.dp, Color(0xFFEF4444)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("取消承运运单", fontWeight = FontWeight.Bold)
                            }
                        }

                        "LOADED" -> {
                            Text("您正在路途中。货车北斗轨迹已开启，请各位卡友安全、平稳运输。", color = TextPrimary, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Visual Road map details inside a small container box
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(BrandNavy.copy(alpha = 0.05f))
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.LocationSearching, contentDescription = null, tint = BrandAmber, modifier = Modifier.size(16.dp))
                                        Text(" 北斗在途轨迹动态监管（西禹高速 H5 治超点）", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandNavy)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("司机：安全车速 78km/h | 水温 84°C | 轮压正常", fontSize = 11.sp, color = TextSecondary)
                                    Text("在途协议载重：${waybill.targetTons} 吨", fontSize = 11.sp, color = TextSecondary)
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            
                            Button(
                                onClick = {
                                    viewModel.arriveDestination(waybill.id)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = BrandAmber),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("已安全抵达目的地，开始卸载称重", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }

                        "UNLOADED" -> {
                            Text("您目前已安全抵运。请在收货现场卸煤/卸货地磅房称重，并在此如实填报卸收吨数与上传地磅单照片。", color = TextPrimary, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(14.dp))
                            
                            OutlinedTextField(
                                value = unloadedWeightInput,
                                onValueChange = { unloadedWeightInput = it },
                                label = { Text("输入到达卸货点收料单实测磅重 (吨)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedBorderColor = BrandNavy,
                                    unfocusedBorderColor = BorderSlate,
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = CanvasBg
                                ),
                                shape = RoundedCornerShape(10.dp)
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(115.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (unloadingPhotoUri != null) BrandGreenBg.copy(alpha = 0.15f) else CanvasBg)
                                    .border(
                                        BorderStroke(
                                            1.dp,
                                            if (unloadingPhotoUri != null) BrandGreen else Color(0xFFE2E8F0)
                                        ),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        val mediaPermission = if (android.os.Build.VERSION.SDK_INT >= 33) {
                                            android.Manifest.permission.READ_MEDIA_IMAGES
                                        } else {
                                            android.Manifest.permission.READ_EXTERNAL_STORAGE
                                        }
                                        checkAndRequestPermission(
                                            "收料地磅单相机与相册存储联用授权",
                                            "为了采集并存档卸货收料点地磅单据及签发凭证印章，大宗智联需要使用您的相机和本地存储防伪接口。开启此项，系统智联互通能大大加快运费审批与放款发放时速。",
                                            listOf(android.Manifest.permission.CAMERA, mediaPermission),
                                            androidx.compose.material.icons.Icons.Filled.Image,
                                            "国家货运结算规定，结算前必须上传装卸双方的地磅电子单据作为运输货量确权凭据。保障您的账户结算合规安全。"
                                        ) {
                                            unloadingPhotoUri = "unload_ticket_selected"
                                            android.widget.Toast.makeText(context, "收货卸载地磅票OCR及安全数签采集成功", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (unloadingPhotoUri != null) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Filled.CheckCircle, contentDescription = "已采集", tint = BrandGreen, modifier = Modifier.size(32.dp))
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text("收料地磅单照片上传成功 (已存)", fontSize = 12.sp, color = BrandGreenText, fontWeight = FontWeight.Bold)
                                        Text("地磅房互联检验通过：与首重磅比相符已备案", fontSize = 10.sp, color = TextSecondary)
                                    }
                                } else {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Filled.PhotoCamera, contentDescription = "未采集", tint = TextSecondary, modifier = Modifier.size(32.dp))
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text("扫拍/采集［收料卸货地磅单 / 签收单照片］", fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                        Text("(系统智联验证比对双向地磅证，保障秒级结款)", fontSize = 10.sp, color = TextMuted)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            
                            Button(
                                onClick = {
                                    val weight = unloadedWeightInput.toDoubleOrNull() ?: waybill.targetTons
                                    if (unloadingPhotoUri == null) {
                                        android.widget.Toast.makeText(context, "请先拍卸货票据照或提供收料地磅单！", android.widget.Toast.LENGTH_SHORT).show()
                                    } else {
                                        viewModel.submitUnloadingTicket(waybill.id, weight, unloadingPhotoUri!!)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = BrandNavy),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("提交卸货称重磅单并进入审核", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }

                        "AUDITING" -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                CircularProgressIndicator(color = BrandAmber, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("拼命对接金税合规、道路信息及三方地磅单双向确权中...", fontSize = 13.sp, color = TextSecondary)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("发运载重: ${waybill.loadWeight} 吨 | 收货实承: ${waybill.unloadWeight} 吨", fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                Text("预计可得核算运费：¥" + String.format("%.2f", waybill.unloadWeight * waybill.pricePerTon), fontSize = 12.sp, color = BrandGreenText)
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { viewModel.approveWaybillDirectly(waybill.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandNavy),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("一键测试结算发资 (跳过审核等待)", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        else -> {
                            // Paid State
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = BrandGreen, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("运费打款成功并到账“我的钱包”结算卡！", fontWeight = FontWeight.Bold, color = BrandGreen)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("结算确权详单", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BrandNavy)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text("收料实收吨数：${waybill.unloadWeight} 吨", fontSize = 12.sp, color = TextSecondary)
                                        Text("合同协议单价：¥${waybill.pricePerTon}/吨", fontSize = 12.sp, color = TextSecondary)
                                        Text("应结算本笔运金：¥" + String.format("%.2f", waybill.freightPayment), fontSize = 13.sp, color = BrandGreenText, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ======================== TABS: AI COACH / ASSISTANT ========================

@Composable
fun AiCoachScreen(viewModel: DriverViewModel) {
    val messages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val aiLoading = viewModel.aiLoading
    var listState = remember { mutableStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            colors = CardDefaults.cardColors(containerColor = BrandNavy.copy(alpha = 0.05f))
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(BrandNavy),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.SupportAgent, contentDescription = null, tint = Color.White)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("智能货装AI教练", fontWeight = FontWeight.Bold, color = BrandNavy, fontSize = 14.sp)
                    Text("解答六轴车49吨合规标准、绿色通道政策、省油技巧", fontSize = 11.sp, color = TextSecondary)
                }
            }
        }

        // Suggestions Box
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val suggestions = listOf("中国高速49t限重", "货运省油技巧", "首车接单准备")
            suggestions.forEach { chunk ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White)
                        .clickable {
                            viewModel.chatInput = chunk
                            viewModel.sendChatMessage()
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(text = chunk, fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                }
            }
        }

        // Message board
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 20.dp)
        ) {
            items(messages) { msg ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start
                ) {
                    if (!msg.isUser) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(BrandNavy.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🤖", fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(0.85f),
                        colors = CardDefaults.cardColors(
                            containerColor = if (msg.isUser) BrandNavy else Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = msg.text,
                                color = if (msg.isUser) Color.White else TextPrimary,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }

                    if (msg.isUser) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(BrandAmber),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("👨🏻‍✈️", fontSize = 16.sp)
                        }
                    }
                }
            }

            if (aiLoading) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            color = BrandNavy,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("AI教练正在调取交通海量合规知识库...", fontSize = 11.sp, color = TextSecondary)
                    }
                }
            }
        }

        // Input field
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = viewModel.chatInput,
                onValueChange = { viewModel.chatInput = it },
                placeholder = { Text("向大宗智联AI教练咨询行业规章...", fontSize = 13.sp) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                maxLines = 2,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = BrandNavy,
                    unfocusedBorderColor = Color.LightGray
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = { viewModel.sendChatMessage() },
                colors = IconButtonDefaults.iconButtonColors(containerColor = BrandNavy),
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
            ) {
                Icon(Icons.Filled.Send, contentDescription = "发送", tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
    }
}

// ======================== TABS: MY WALLET ========================

@Composable
fun WalletScreen(
    profile: DriverProfile?,
    transactions: List<WalletTransaction>,
    viewModel: DriverViewModel
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp)
    ) {
        // Gradient account balance card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(BrandNavy, Color(0xFF1E40AF), BrandBlueLight)
                        )
                    )
                    .padding(20.dp)
            ) {
                Column(modifier = Modifier.fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("提现账户可用余额(元)", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                            Text(
                                "¥ " + String.format("%.2f", profile?.balance ?: 0.0),
                                color = Color.White,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(44.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("待确权结算(网签中)", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                            Text("¥ " + String.format("%.2f", profile?.pendingSettlement ?: 0.0), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { viewModel.showWithdrawDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandAmber),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("结余提现", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Subtitle
        item {
            Text(
                text = "资金收支审计流水",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = TextPrimary,
                modifier = Modifier.padding(top = 20.dp, bottom = 10.dp)
            )
        }

        if (transactions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("暂无提现及结款流水记录", color = TextSecondary, fontSize = 13.sp)
                    }
                }
            }
        } else {
            items(transactions) { trans ->
                TransactionListItem(trans = trans)
            }
        }
    }
}

@Composable
fun TransactionListItem(trans: WalletTransaction) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = trans.title, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
                Text(
                    text = "渠道: ${trans.paymentMethod} • " + java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.CHINA).format(trans.timestamp),
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }

            Text(
                text = (if (trans.amount > 0) "+" else "") + String.format("%.2f", trans.amount),
                color = if (trans.amount > 0) BrandGreen else Color.Red,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }
    }
}

// ======================== TABS: MY PROFILE ========================

@Composable
fun ProfileScreen(
    profile: DriverProfile?,
    viewModel: DriverViewModel,
    checkAndRequestPermission: (String, String, List<String>, androidx.compose.ui.graphics.vector.ImageVector, String, () -> Unit) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp)
    ) {
        // Driver header profile
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(BrandNavy),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("👨🏻‍✈️", fontSize = 32.sp)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = profile?.name ?: "载货重型卡车卡友",
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 18.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        if (profile?.idCardVerified == true) BrandGreen.copy(alpha = 0.15f) else Color.Red.copy(alpha = 0.15f)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (profile?.idCardVerified == true) "实名已验" else "资质未验",
                                    color = if (profile?.idCardVerified == true) BrandGreen else Color.Red,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(text = "绑定手机: ${profile?.phone}", fontSize = 12.sp, color = TextSecondary)
                        Text(text = "系统累计安全运输里程: ${profile?.safetyMileage} 公里", fontSize = 11.sp, color = TextSecondary)
                    }
                }
            }
        }

        // Subtitle section
        item {
            Text(
                text = "重卡准入审核备案车辆",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = TextPrimary,
                modifier = Modifier.padding(top = 20.dp, bottom = 10.dp)
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("牌照号码", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextSecondary)
                        Text(profile?.plateNumber ?: "尚未核验", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("厢体规格", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextSecondary)
                        Text(profile?.vehicleSize ?: "尚未核验", fontWeight = FontWeight.Medium, fontSize = 13.sp, color = TextPrimary)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("车轴上限合规载重", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextSecondary)
                        Text("${profile?.vehicleWeightCapacity} 吨内", fontWeight = FontWeight.Medium, fontSize = 13.sp, color = TextPrimary)
                    }
                }
            }
        }

        // Action menu list
        item {
            Text(
                text = "运输设置与保障",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = TextPrimary,
                modifier = Modifier.padding(top = 20.dp, bottom = 10.dp)
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column {
                    ProfileMenuRow("个人资料及证件档案修改", Icons.Default.Person) {
                        viewModel.profileSubTab = 1
                    }
                    Divider(color = CanvasBg, thickness = 1.dp)
                    ProfileMenuRow("重型车辆运输资质资料修改", Icons.Default.LocalShipping) {
                        viewModel.profileSubTab = 2
                    }
                    Divider(color = CanvasBg, thickness = 1.dp)
                    ProfileMenuRow("全天候国家北斗星联位置安全保障校验", Icons.Default.NotificationsActive) {
                        checkAndRequestPermission(
                            "北斗高精度位置安全保障授信",
                            "为了实现运输途中网络车辆全天候智能定位保障、轨迹实时安全合规上传，我们需要获取您的精确定位与模糊定位权限。",
                            listOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION),
                            androidx.compose.material.icons.Icons.Filled.MyLocation,
                            "依照交通公路法及网络大宗物流真实营运转单合规政策，重型货车运输中位置是在途核验与运力结算的硬性指标。开启该项系统将确保卡车全程不漏单不假跑。"
                        ) {
                            android.widget.Toast.makeText(context, "✅ 北斗高精度卫星实时轨迹安全保障中，设备完美绿灯在线", android.widget.Toast.LENGTH_LONG).show()
                        }
                    }
                    Divider(color = CanvasBg, thickness = 1.dp)
                    ProfileMenuRow("智联系统调用及合规隐私授权管理", Icons.Default.Security) {
                        viewModel.profileSubTab = 3
                    }
                    Divider(color = CanvasBg, thickness = 1.dp)
                    ProfileMenuRow("一键完美匹配实名资质比对", Icons.Default.VerifiedUser) {
                        viewModel.showCertifyDialog = true
                    }
                    Divider(color = CanvasBg, thickness = 1.dp)
                    ProfileMenuRow("智联大宗智慧加油门禁管理系统", Icons.Default.LocalGasStation) {
                        viewModel.showRefuelingScreen = true
                        viewModel.refuelingScreenTab = 0
                    }
                    Divider(color = CanvasBg, thickness = 1.dp)
                    ProfileMenuRow("多级网商及农信社储蓄收款卡管理", Icons.Default.CreditCard) {
                        viewModel.showBankCardsScreen = true
                    }
                    Divider(color = CanvasBg, thickness = 1.dp)
                    ProfileMenuRow("我的评价与调度承运满意度考核", Icons.Default.Star) {
                        viewModel.showReviewsScreen = true
                    }
                    Divider(color = CanvasBg, thickness = 1.dp)
                    ProfileMenuRow("司机端系统个性化与合规设置中心", Icons.Default.Settings) {
                        viewModel.showSettingsScreen = true
                        viewModel.settingsScreenSubTab = 0
                    }
                    Divider(color = CanvasBg, thickness = 1.dp)
                    ProfileMenuRow("大宗智联官方24小时客服热线", Icons.Default.HeadsetMic) {
                        android.widget.Toast.makeText(context, "正在呼叫官方24小时客服热线：400-888-9999", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    viewModel.isLoggedIn = false
                    viewModel.profileSubTab = 0
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.ExitToApp, contentDescription = "退出登录", tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("安全退出当前账号", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun ProfileMenuRow(title: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = BrandNavy, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
        }
        Icon(imageVector = Icons.Default.KeyboardArrowRight, contentDescription = null, tint = BrandLightIcon, modifier = Modifier.size(16.dp))
    }
}

// ======================== DIALOGS ========================

@Composable
fun WithdrawMoneyDialog(viewModel: DriverViewModel, profile: DriverProfile?) {
    Dialog(onDismissRequest = { viewModel.showWithdrawDialog = false }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("运费结算提现申请", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = BrandNavy)
                    IconButton(onClick = { viewModel.showWithdrawDialog = false }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "关闭", tint = TextSecondary)
                    }
                }
                
                Spacer(modifier = Modifier.height(14.dp))
                
                Text(
                    text = "账户余额：¥" + String.format("%.2f", profile?.balance ?: 0.0),
                    fontSize = 12.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(10.dp))
                
                OutlinedTextField(
                    value = viewModel.withdrawAmount,
                    onValueChange = { viewModel.withdrawAmount = it },
                    label = { Text("输入提现到银行卡金额") },
                    prefix = { Text("¥ ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = BrandNavy,
                        unfocusedBorderColor = Color.LightGray
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = viewModel.withdrawBank,
                    onValueChange = { viewModel.withdrawBank = it },
                    label = { Text("银行名称") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = BrandNavy,
                        unfocusedBorderColor = Color.LightGray
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = viewModel.withdrawCard,
                    onValueChange = { viewModel.withdrawCard = it },
                    label = { Text("储蓄银联卡号") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = BrandNavy,
                        unfocusedBorderColor = Color.LightGray
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { viewModel.performWithdraw() },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandNavy),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("确认极速提到银行卡", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun CertifyCredentialsDialog(viewModel: DriverViewModel, profile: DriverProfile?) {
    Dialog(onDismissRequest = { viewModel.showCertifyDialog = false }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("一键智能资质核验", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = BrandNavy)
                    IconButton(onClick = { viewModel.showCertifyDialog = false }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "关闭", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "系统将比对交通、车管等官方API，快速通过资质，比对成功后即可在全省货源大厅自由抢接单。",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = viewModel.certName,
                    onValueChange = { viewModel.certName = it },
                    label = { Text("真实姓名") },
                    placeholder = { Text("如：张晓峰") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = BrandNavy,
                        unfocusedBorderColor = Color.LightGray
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = viewModel.certIdCard,
                    onValueChange = { viewModel.certIdCard = it },
                    label = { Text("身份证号码") },
                    placeholder = { Text("例如：610124...") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = BrandNavy,
                        unfocusedBorderColor = Color.LightGray
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = viewModel.certPlate,
                    onValueChange = { viewModel.certPlate = it },
                    label = { Text("重卡特种车牌号码") },
                    placeholder = { Text("如：陕K·8W912") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = BrandNavy,
                        unfocusedBorderColor = Color.LightGray
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = viewModel.certVehicleSize,
                    onValueChange = { viewModel.certVehicleSize = it },
                    label = { Text("厢体及车型规格") },
                    placeholder = { Text("如：13.7米侧翻半挂车") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = BrandNavy,
                        unfocusedBorderColor = Color.LightGray
                    )
                )

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = { viewModel.performCertification() },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandNavy),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("提交核查并对接车管所API", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ScanQRCodeDialog(viewModel: DriverViewModel) {
    val scanOptions = listOf(
        CargoListing(
            id = 201,
            cargoName = "原煤（黄陵集运站特供白资单）",
            departure = "延安黄陵二号煤矿一号装货线",
            departureCity = "延安市",
            destination = "咸阳市大唐北方热电厂上煤仓",
            destinationCity = "咸阳市",
            distance = 185.0,
            pricePerTon = 195.0,
            totalTons = 5000.0,
            remainedTons = 3500.0,
            publisher = "陕煤集团黄陵建庄矿业有限公司",
            publishTime = "刚刚",
            contactPhone = "15899880011",
            vehicleReq = "13米重型翻斗半挂车"
        ),
        CargoListing(
            id = 202,
            cargoName = "热轧卷板（陕钢汉钢成品库直发）",
            departure = "勉县陕钢汉中集收中心成品B库",
            departureCity = "汉中市",
            destination = "西安港务区陕钢联运营业中心堆场",
            destinationCity = "西安市",
            distance = 280.0,
            pricePerTon = 165.0,
            totalTons = 3000.0,
            remainedTons = 1200.0,
            publisher = "陕西钢铁集团销售有限公司",
            publishTime = "刚刚",
            contactPhone = "15899882233",
            vehicleReq = "13米重卡低平板挂车"
        ),
        CargoListing(
            id = 203,
            cargoName = "铁精矿粉（商洛全旺集运特单）",
            departure = "商洛全旺铁矿主井选矿卸货台",
            departureCity = "商洛市",
            destination = "韩城龙门陕钢集团特种原料堆栈",
            destinationCity = "韩城市",
            distance = 220.0,
            pricePerTon = 148.0,
            totalTons = 2000.0,
            remainedTons = 1100.0,
            publisher = "商洛全旺矿业有限责任公司",
            publishTime = "刚刚",
            contactPhone = "15899884455",
            vehicleReq = "重型散料挂车"
        )
    )

    Dialog(onDismissRequest = { viewModel.showScanDialog = false }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header details
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "智能大宗扫码接单",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextPrimary
                    )
                    IconButton(onClick = { viewModel.showScanDialog = false }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "关闭")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Viewfinder scanning animation container
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0F172A)), // dark slate
                    contentAlignment = Alignment.Center
                ) {
                    val infiniteTransition = rememberInfiniteTransition(label = "laser")
                    val laserOffsetY by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 2200, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "laser_offset"
                    )

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val height = size.height
                        val width = size.width
                        val currentY = height * laserOffsetY
                        // Draw scan line with glowing effect
                        drawLine(
                            color = Color(0xFF2563EB),
                            start = Offset(20f, currentY),
                            end = Offset(width - 20f, currentY),
                            strokeWidth = 3.dp.toPx()
                        )
                    }

                    // Green/blue Corner overlay to look like camera scanning area frame boundaries
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .border(width = 1.5.dp, color = Color.White.copy(alpha = 0.3f), shape = RoundedCornerShape(4.dp))
                    ) {
                        // Scan guidelines hint text
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Text(
                                text = "对准发货厂矿出港单二维码",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "📱 模拟对准以下大宗厂矿单据二维码：",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Options list to simulate trigger scan success
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    items(scanOptions) { option ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.grabScanOrder(option) {}
                                },
                            colors = CardDefaults.cardColors(containerColor = BrandBlueBg),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(BrandGreenBg)
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "二维码",
                                                color = BrandGreenText,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = option.cargoName,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${option.departureCity} 装 → ${option.destinationCity} 卸",
                                        fontSize = 10.sp,
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = "运输单价 ¥${option.pricePerTon}/吨",
                                        fontSize = 10.sp,
                                        color = BrandAmber,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(BrandNavy)
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "模拟扫码",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
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

@Composable
fun LoginScreen(viewModel: DriverViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Initial phone defaults to the seeded value or empty
    var phone by remember { mutableStateOf("18790218822") }
    var code by remember { mutableStateOf("") }
    var isCodeSending by remember { mutableStateOf(false) }
    var countdownSeconds by remember { mutableStateOf(0) }
    var agreeChecked by remember { mutableStateOf(true) }
    var isLoggingIn by remember { mutableStateOf(false) }

    // Countdown timer for SMS
    LaunchedEffect(isCodeSending, countdownSeconds) {
        if (isCodeSending && countdownSeconds > 0) {
            delay(1000)
            countdownSeconds--
            if (countdownSeconds == 0) {
                isCodeSending = false
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFEFF6FF), // Light highlight blue
                        Color(0xFFDBEAFE)  // Tailwind blue-100
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App Identity Accent Card
            Card(
                modifier = Modifier
                    .size(80.dp)
                    .padding(8.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = BrandNavy),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "智联",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "大宗商品智能化物流智联系统",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Text(
                text = "网络货运·全国统调·高规备案司机端",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        text = "验证码安全合规登录",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandNavy,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Phone text field
                    Text(
                        text = "手机号码",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { if (it.length <= 11) phone = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("请输入11位中国大陆手机号", fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.PhoneAndroid,
                                contentDescription = "手机",
                                tint = TextMuted,
                                modifier = Modifier.size(18.dp)
                             )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = BrandNavy,
                            unfocusedBorderColor = BorderSlate,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = CanvasBg
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Verification code field
                    Text(
                        text = "短信验证码",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = code,
                            onValueChange = { if (it.length <= 6) code = it },
                            modifier = Modifier.weight(1.3f),
                            placeholder = { Text("请输入6位验证码", fontSize = 13.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.VpnKey,
                                    contentDescription = "验证码",
                                    tint = TextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = BrandNavy,
                                unfocusedBorderColor = BorderSlate,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = CanvasBg
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Button(
                            onClick = {
                                if (phone.length != 11) {
                                    android.widget.Toast.makeText(context, "请输入合规的11位手机号！", android.widget.Toast.LENGTH_SHORT).show()
                                } else {
                                    isCodeSending = true
                                    countdownSeconds = 60
                                    android.widget.Toast.makeText(context, "［大宗智联］模拟验证码已发送至手机，验证码为：8888", android.widget.Toast.LENGTH_LONG).show()
                                }
                            },
                            enabled = !isCodeSending,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isCodeSending) BrandNavy.copy(alpha = 0.5f) else BrandNavy,
                                disabledContainerColor = Color.LightGray
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Text(
                                text = if (isCodeSending) "${countdownSeconds}s后重发" else "获取验证码",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Agree Terms check
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = agreeChecked,
                            onCheckedChange = { agreeChecked = it },
                            colors = CheckboxDefaults.colors(checkedColor = BrandNavy)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "我已同意并签署《网络平台联运协议》、《司机信息保护政策》并授权系统比对运输资质。",
                            fontSize = 10.sp,
                            color = TextSecondary,
                            lineHeight = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Action Button
                    Button(
                        onClick = {
                            if (!agreeChecked) {
                                android.widget.Toast.makeText(context, "请先阅读并勾选下方联运协议！", android.widget.Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (phone.length != 11) {
                                android.widget.Toast.makeText(context, "请输入正确的11位电话号码！", android.widget.Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            if (code != "8888") {
                                android.widget.Toast.makeText(context, "验证码不正确！请点击获取输入「8888」体验。", android.widget.Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            isLoggingIn = true
                            scope.launch {
                                delay(1200) // Simulating checking database
                                if (phone != "18790218822") {
                                    // Seed user profile with the entered custom phone
                                    viewModel.savePersonalProfile("卡友 $phone", phone, "61012419850912" + (1000..9999).random(), "A2", null, null, null, null)
                                }
                                viewModel.isLoggedIn = true
                                isLoggingIn = false
                                android.widget.Toast.makeText(context, "登录成功！欢迎使用大宗智联货运系统。", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        enabled = !isLoggingIn,
                        colors = ButtonDefaults.buttonColors(containerColor = BrandNavy),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isLoggingIn) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "一键登录",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DocumentUploadCardByTitle(
    title: String,
    imageUri: String?,
    onUpload: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(115.dp)
            .clickable { onUpload() },
        border = BorderStroke(1.dp, if (imageUri != null) BrandGreen else Color(0xFFE2E8F0)),
        colors = CardDefaults.cardColors(containerColor = if (imageUri != null) BrandGreenBg.copy(alpha = 0.15f) else CanvasBg)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (imageUri != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "已采集",
                        tint = BrandGreen,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrandGreenText, textAlign = TextAlign.Center)
                    Text(text = "系统联验证核(已存)", fontSize = 9.sp, color = TextSecondary, textAlign = TextAlign.Center)
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.PhotoCamera,
                        contentDescription = "未采集",
                        tint = TextSecondary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = title, fontSize = 11.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                    Text(text = "点按拍照采集文件", fontSize = 9.sp, color = TextMuted, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
fun PermissionsManagementScreen(viewModel: DriverViewModel) {
    val context = LocalContext.current
    
    // Check permission helper for UI drawing
    val hasCamera = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED
    val hasLocation = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
    
    val mediaPermission = if (android.os.Build.VERSION.SDK_INT >= 33) {
        android.Manifest.permission.READ_MEDIA_IMAGES
    } else {
        android.Manifest.permission.READ_EXTERNAL_STORAGE
    }
    val hasStorage = androidx.core.content.ContextCompat.checkSelfPermission(context, mediaPermission) == android.content.pm.PackageManager.PERMISSION_GRANTED

    // Launcher for dynamic permission request from within setting page
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.values.all { it }
        if (allGranted) {
            android.widget.Toast.makeText(context, "✅ 合规服务授信极速接入成功！", android.widget.Toast.LENGTH_SHORT).show()
        } else {
            android.widget.Toast.makeText(context, "⚠️ 权限未开启，请在系统设置中手动开启相关权限。", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(CanvasBg)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.profileSubTab = 0 }
            ) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "返回", tint = BrandNavy)
                Spacer(modifier = Modifier.width(6.dp))
                Text("返回我的设置主页", color = BrandNavy, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = BrandNavy),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Security, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("系统功能调用与安全授信管理", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "依据交通运输部及网信办关于《网络货运经营管理办法》等法规要求，在运单承运履行期间需收集相关的实时轨迹、地磅照片及车牌资质，用以确定运单真实性及秒级安全合规清算。大宗智联卡友端恪守司机隐私保密法案。",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // Location Info
        item {
            PermissionItemCard(
                title = "1. 位置定位权限 (北斗在途动态轨迹)",
                description = "实现承运中运输卡车的实时运行轨迹、治超公路联网比对，是防假运、防止运单漏单的法定要素。",
                statusText = if (hasLocation) "在线 · 北斗高精度位置追踪中" else "离线 · 轨迹监控关闭",
                actionText = if (hasLocation) "运行状态完好" else "一键开启高精轨迹定位",
                isGranted = hasLocation,
                icon = Icons.Filled.MyLocation,
                onClick = {
                    if (!hasLocation) {
                        permissionLauncher.launch(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION))
                    } else {
                        android.widget.Toast.makeText(context, "北斗高精度位置及道路联网在线运行中，状态良好", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        // Camera Info
        item {
            PermissionItemCard(
                title = "2. 相机拍照权限 (地磅OCR/车身核验)",
                description = "在起运阶段和卸货磅房阶段，用于司机离线采集装/卸重卡外观照片与扫描地磅单、签收单实据，实现极速OCR解析确认。",
                statusText = if (hasCamera) "就绪 · 支持地磅与重卡实据扫拍" else "待授权相机硬件支持",
                actionText = if (hasCamera) "相机已完美互联" else "一键开启相机拍照授权",
                isGranted = hasCamera,
                icon = Icons.Filled.PhotoCamera,
                onClick = {
                    if (!hasCamera) {
                        permissionLauncher.launch(arrayOf(android.Manifest.permission.CAMERA))
                    } else {
                        android.widget.Toast.makeText(context, "相机服务完美在线，已接入大宗单据OCR图像处理管道", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        // Album/Storage Info
        item {
            PermissionItemCard(
                title = "3. 相册与存储权限 (单据离线存根与对账)",
                description = "用于在野外大山中、煤矿附近等无信号极弱网络下，暂时安全备份地磅证件原底文件，随后在信号恢复时自动云同步。",
                statusText = if (hasStorage) "就绪 · 开启无网磅单极速存根" else "待授权本地存储对账",
                actionText = if (hasStorage) "离线沙箱数据畅通" else "一键开启相册存储保障",
                isGranted = hasStorage,
                icon = Icons.Filled.Image,
                onClick = {
                    if (!hasStorage) {
                        permissionLauncher.launch(arrayOf(mediaPermission))
                    } else {
                        android.widget.Toast.makeText(context, "本地存储及相册读取服务正常，对账信息可实现100%安全隔离区缓存", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = BrandGreenBg.copy(alpha = 0.1f)),
                border = BorderStroke(1.dp, BrandGreen.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = BrandGreenText)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "国家《网络货运运输合规指数》司机端实测评估：AAA级 极速保障合规。",
                        fontSize = 11.sp,
                        color = BrandGreenText,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun PermissionItemCard(
    title: String,
    description: String,
    statusText: String,
    actionText: String,
    isGranted: Boolean,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, if (isGranted) BrandGreen.copy(alpha = 0.3f) else BrandAmber.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isGranted) BrandGreenBg else BrandAmberBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = icon, contentDescription = null, tint = if (isGranted) BrandGreenText else BrandAmber, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text = title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                }
                
                // Active/Pending Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isGranted) BrandGreenBg else BrandAmberBg)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isGranted) "已安全对接" else "等待授权",
                        color = if (isGranted) BrandGreenText else BrandAmber,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = description, fontSize = 11.sp, color = TextSecondary, lineHeight = 16.sp)
            Spacer(modifier = Modifier.height(12.dp))
            
            HorizontalDivider(color = Color(0xFFF1F5F9))
            
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "系统与宿主校验状态：", fontSize = 10.sp, color = TextMuted)
                    Text(text = statusText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = if (isGranted) BrandGreenText else BrandAmber)
                }
                
                Button(
                    onClick = onClick,
                    colors = ButtonDefaults.buttonColors(containerColor = if (isGranted) BrandGreen else BrandNavy),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(text = actionText, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun PersonalProfileScreen(
    profile: DriverProfile?,
    viewModel: DriverViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var nameInput by remember { mutableStateOf(profile?.name ?: "王建国") }
    var phoneInput by remember { mutableStateOf(profile?.phone ?: "18790218822") }
    var idCardInput by remember { mutableStateOf(profile?.idCard ?: "610124198509123849") }
    var licenseInput by remember { mutableStateOf(profile?.driverLicense ?: "A2") }
    
    var idCardFront by remember { mutableStateOf(profile?.idCardFrontImage) }
    var idCardBack by remember { mutableStateOf(profile?.idCardBackImage) }
    var driverLicenseMain by remember { mutableStateOf(profile?.driverLicenseMainImage) }
    var driverLicenseSub by remember { mutableStateOf(profile?.driverLicenseSubImage) }
    
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(profile) {
        if (profile != null) {
            nameInput = profile.name
            phoneInput = profile.phone
            idCardInput = profile.idCard
            licenseInput = profile.driverLicense
            idCardFront = profile.idCardFrontImage
            idCardBack = profile.idCardBackImage
            driverLicenseMain = profile.driverLicenseMainImage
            driverLicenseSub = profile.driverLicenseSubImage
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CanvasBg)
    ) {
        // Custom Header Navigation Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 14.dp, vertical = 14.dp)
                .drawBehind {
                    drawLine(
                        color = BorderSlate,
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.profileSubTab = 0 },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "返回",
                    tint = TextPrimary
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "修改个人资料档案",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = TextPrimary
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "基本人身档案与行车资质",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = BrandNavy,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // Name field
                        Text(
                            text = "司机真实姓名",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = { nameInput = it },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = BrandNavy,
                                unfocusedBorderColor = BorderSlate,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = CanvasBg
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Phone field
                        Text(
                            text = "绑定安全手机",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        OutlinedTextField(
                            value = phoneInput,
                            onValueChange = { if (it.length <= 11) phoneInput = it },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = BrandNavy,
                                unfocusedBorderColor = BorderSlate,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = CanvasBg
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // ID Card field
                        Text(
                            text = "国家法定身份证号",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        OutlinedTextField(
                            value = idCardInput,
                            onValueChange = { idCardInput = it },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = BrandNavy,
                                unfocusedBorderColor = BorderSlate,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = CanvasBg
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // License Class field
                        Text(
                            text = "驾驶证准驾等级 (比如 A2, B2)",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        OutlinedTextField(
                            value = licenseInput,
                            onValueChange = { licenseInput = it },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = BrandNavy,
                                unfocusedBorderColor = BorderSlate,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = CanvasBg
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "重要证件图像影像采集",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = BrandNavy,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        Text(
                            text = "身份证人像与国徽面影像",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                DocumentUploadCardByTitle("身份证正面 (人像页)", idCardFront) {
                                    idCardFront = "front_selected"
                                    idCardInput = "610124198509123849"
                                    android.widget.Toast.makeText(context, "身份证正面OCR识别：王建国 识别成功", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                DocumentUploadCardByTitle("身份证反面 (国徽页)", idCardBack) {
                                    idCardBack = "back_selected"
                                    android.widget.Toast.makeText(context, "身份证反面安全防伪芯片核签成功", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "驾驶证正页与副页（A2/B2资质）",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                DocumentUploadCardByTitle("驾驶证正本", driverLicenseMain) {
                                    driverLicenseMain = "license_main_selected"
                                    licenseInput = "A2"
                                    android.widget.Toast.makeText(context, "驾驶证正本OCR识别：大型货卡 A2 准驾资质", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                DocumentUploadCardByTitle("驾驶证副本", driverLicenseSub) {
                                    driverLicenseSub = "license_sub_selected"
                                    android.widget.Toast.makeText(context, "驾驶证校核周期和审验无误与国家司法库对接比对成功", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "💼 注意：大宗危化品及重金属钢材运输需要持有对应的道路运输资质上岗。若进行敏感危险行车，请上传从业资格证交由货主端核验。",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            item {
                Button(
                    onClick = {
                        if (nameInput.isBlank() || phoneInput.length != 11 || idCardInput.length != 18) {
                            android.widget.Toast.makeText(context, "请填写真实合规的信息内容！电话11位，身份证18位。", android.widget.Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        saving = true
                        scope.launch {
                            delay(1000)
                            viewModel.savePersonalProfile(
                                nameInput,
                                phoneInput,
                                idCardInput,
                                licenseInput,
                                idCardFront,
                                idCardBack,
                                driverLicenseMain,
                                driverLicenseSub
                            )
                            saving = false
                            android.widget.Toast.makeText(context, "个人资料及全息证照影像保存成功！", android.widget.Toast.LENGTH_SHORT).show()
                            viewModel.profileSubTab = 0
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandNavy),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !saving
                ) {
                    if (saving) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text(
                            text = "保存并提交国家资质审核",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VehicleProfileScreen(
    profile: DriverProfile?,
    viewModel: DriverViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var plateInput by remember { mutableStateOf(profile?.plateNumber ?: "陕K·6A528") }
    var sizeInput by remember { mutableStateOf(profile?.vehicleSize ?: "13米自卸半挂车") }
    var capacityInput by remember { mutableStateOf(profile?.vehicleWeightCapacity?.toString() ?: "49.0") }
    
    var vehicleLicenseMain by remember { mutableStateOf(profile?.vehicleLicenseMainImage) }
    var vehicleLicenseSub by remember { mutableStateOf(profile?.vehicleLicenseSubImage) }
    var permitInput by remember { mutableStateOf(profile?.roadTransportPermit ?: "陕交规运字610100293482号") }
    var permitImage by remember { mutableStateOf(profile?.roadTransportPermitImage) }
    var trailerPlateInput by remember { mutableStateOf(profile?.trailerPlateNumber ?: "陕K·8523挂") }
    
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(profile) {
        if (profile != null) {
            plateInput = profile.plateNumber
            sizeInput = profile.vehicleSize
            capacityInput = profile.vehicleWeightCapacity.toString()
            vehicleLicenseMain = profile.vehicleLicenseMainImage
            vehicleLicenseSub = profile.vehicleLicenseSubImage
            permitInput = profile.roadTransportPermit
            permitImage = profile.roadTransportPermitImage
            trailerPlateInput = profile.trailerPlateNumber
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CanvasBg)
    ) {
        // Navigation Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 14.dp, vertical = 14.dp)
                .drawBehind {
                    drawLine(
                        color = BorderSlate,
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.profileSubTab = 0 },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "返回",
                    tint = TextPrimary
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "修改备案重型卡车车辆信息",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = TextPrimary
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "重型货车物理规格及准入指标",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = BrandNavy,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // Plate number field
                        Text(
                            text = "大国机动车车牌号码 (六轴备案牌照)",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        OutlinedTextField(
                            value = plateInput,
                            onValueChange = { plateInput = it },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = BrandNavy,
                                unfocusedBorderColor = BorderSlate,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = CanvasBg
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Trailer plate number field
                        Text(
                            text = "挂车车牌号",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        OutlinedTextField(
                            value = trailerPlateInput,
                            onValueChange = { trailerPlateInput = it },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = BrandNavy,
                                unfocusedBorderColor = BorderSlate,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = CanvasBg
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Road Transport Permit field
                        Text(
                            text = "道路运输许可证号",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        OutlinedTextField(
                            value = permitInput,
                            onValueChange = { permitInput = it },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = BrandNavy,
                                unfocusedBorderColor = BorderSlate,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = CanvasBg
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Vehicle size field
                        Text(
                            text = "厢体规格及类型",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        OutlinedTextField(
                            value = sizeInput,
                            onValueChange = { sizeInput = it },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = BrandNavy,
                                unfocusedBorderColor = BorderSlate,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = CanvasBg
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Weight capacity field
                        Text(
                            text = "核定总重吨位限值 (依法限制为49.0吨内)",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        OutlinedTextField(
                            value = capacityInput,
                            onValueChange = { capacityInput = it },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = BrandNavy,
                                unfocusedBorderColor = BorderSlate,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = CanvasBg
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "车辆资质证照影像采集",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = BrandNavy,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        Text(
                            text = "行驶证正本与副本（主挂关联）",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                DocumentUploadCardByTitle("行驶证正本", vehicleLicenseMain) {
                                    vehicleLicenseMain = "vehicle_main_selected"
                                    plateInput = "陕K·6A528"
                                    android.widget.Toast.makeText(context, "行驶证正本匹配：陕K·6A528 识别匹配成功", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                DocumentUploadCardByTitle("行驶证副本", vehicleLicenseSub) {
                                    vehicleLicenseSub = "vehicle_sub_selected"
                                    android.widget.Toast.makeText(context, "行驶证副本车辆年度安全检验页比对无误", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "卡车道路运输许可证工作照片影像",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        DocumentUploadCardByTitle("道路运输许可证影像", permitImage) {
                            permitImage = "permit_selected"
                            android.widget.Toast.makeText(context, "运政网车辆营运申报系统核对成功：道路营运资质合规", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            item {
                Text(
                    text = "🚨 安全红绿灯政策倡导：依照《超限超载车辆行驶公路管理规定》，六轴重型货车装载总重量限值最大为49.0吨。严禁非法超限上路！",
                    fontSize = 11.sp,
                    color = BrandAmber,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            item {
                Button(
                    onClick = {
                        val capVal = capacityInput.toDoubleOrNull()
                        if (plateInput.isBlank() || sizeInput.isBlank() || capVal == null || capVal <= 0) {
                            android.widget.Toast.makeText(context, "请填写真实合规的车辆数据信息！", android.widget.Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        saving = true
                        scope.launch {
                            delay(1000)
                            viewModel.saveVehicleProfile(
                                plateInput,
                                sizeInput,
                                capVal,
                                vehicleLicenseMain,
                                vehicleLicenseSub,
                                permitInput,
                                permitImage,
                                trailerPlateInput
                            )
                            saving = false
                            android.widget.Toast.makeText(context, "车辆资料保存并绑定完成！数据已上报国家网络货运备案。", android.widget.Toast.LENGTH_SHORT).show()
                            viewModel.profileSubTab = 0
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandNavy),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !saving
                ) {
                    if (saving) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text(
                            text = "绑定并保存车辆档案",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// ======================== SCREEN: CARGO DETAIL PAGE ========================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CargoDetailScreen(
    cargoId: Int,
    cargoList: List<CargoListing>,
    profile: DriverProfile?,
    viewModel: DriverViewModel
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val cargo = cargoList.find { it.id == cargoId } ?: return
    var showDialogAlert by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(CanvasBg),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Back toolbar button
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                onClick = { viewModel.selectedCargoId = null },
                border = BorderStroke(1.dp, BorderSlate)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "返回",
                        tint = BrandNavy
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "返回货源大厅",
                        color = BrandNavy,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // Main info header card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, BorderSlate)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(BrandGreenBg)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "即时抢单 · 指标有保障",
                                color = BrandGreenText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = cargo.publishTime,
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = cargo.cargoName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("协议单价", fontSize = 11.sp, color = TextSecondary)
                            Text(
                                text = "¥${cargo.pricePerTon}/吨",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = BrandAmber
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("剩余吨数 / 总吨数", fontSize = 11.sp, color = TextSecondary)
                            Text(
                                text = "${cargo.remainedTons} / ${cargo.totalTons} 吨",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandNavy
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Progress bar
                    val progress = if (cargo.totalTons > 0.0) {
                        (cargo.remainedTons / cargo.totalTons).toFloat().coerceIn(0f, 1f)
                    } else {
                        0f
                    }
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                        color = BrandNavy,
                        trackColor = BorderSlate
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "该货源计划调派车次充足，已抢配 ${(progress * 100).toInt()}% 吨数额度",
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                }
            }
        }

        // Transport route schedule details
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, BorderSlate)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Directions,
                            contentDescription = null,
                            tint = BrandNavy,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "承运起止干线路线",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(24.dp).padding(top = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.StopCircle,
                                contentDescription = null,
                                tint = BrandGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .size(width = 2.dp, height = 44.dp)
                                    .background(Color.LightGray)
                            )
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = BrandAmber,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Column {
                            Text(
                                text = "【装货起运地】  " + cargo.departureCity,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = cargo.departure,
                                fontSize = 12.sp,
                                color = TextSecondary,
                                modifier = Modifier.padding(top = 2.dp, bottom = 14.dp)
                            )
                            
                            Text(
                                text = "【卸货目的地】  " + cargo.destinationCity,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = cargo.destination,
                                fontSize = 12.sp,
                                color = TextSecondary,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }

                    Divider(color = BorderSlate, thickness = 1.dp, modifier = Modifier.padding(vertical = 14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("预估运输距离", fontSize = 11.sp, color = TextSecondary)
                            Text(
                                text = "${cargo.distance} 公里 (km)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("预估行驶时间", fontSize = 11.sp, color = TextSecondary)
                            val estimatedHours = String.format("%.1f", cargo.distance / 65.0)
                            Text(
                                text = "约 $estimatedHours 小时",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }
        }

        // Shippers publisher details
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, BorderSlate)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "发货货主资质信誉",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(BrandNavy.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Business,
                                    contentDescription = null,
                                    tint = BrandNavy,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = cargo.publisher,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "平台AAA级发资信誉 • 无运费纠纷账期良好",
                                    fontSize = 10.sp,
                                    color = BrandGreenText,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Button(
                            onClick = {
                                android.widget.Toast.makeText(
                                    context,
                                    "正在安全接通货主热线: ${cargo.contactPhone} ...",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandNavy.copy(alpha = 0.08f), contentColor = BrandNavy),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("拨打电话", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Divider(color = BorderSlate, thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp))

                    Text(
                        text = "承运所需车型要求: ${cargo.vehicleReq}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "注：要求车况良好、尾气符合国六环保标准，配备密封蓬布以防泄漏粉尘。",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }
        }

        // Security regulatory tips
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = BrandAmberBg.copy(alpha = 0.15f)),
                border = BorderStroke(1.dp, BrandAmber.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "⚠️ 道路货运安全与合规守法须知",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandAmber
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "根据《公路安全保护条例》及国家治超网联，本条线货运车辆（含车、挂、货）最大合法限重 49 吨。平台要求运输车次全程上线北斗高精度安全系统，违规超限超载将无法生成电子地磅进行确权以及结算付款，同时会记入司机及车辆信用名单。",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        lineHeight = 15.sp
                    )
                }
            }
        }

        // Action Buttons Grab order
        item {
            Button(
                onClick = {
                    if (profile?.idCardVerified == false || profile?.vehicleVerified == false) {
                        showDialogAlert = true
                    } else {
                        viewModel.grabOrder(cargo) {
                            viewModel.selectedCargoId = null
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BrandNavy),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text(
                    text = "立即申领抢配此单 (保障指标额度)",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 14.sp
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    if (showDialogAlert) {
        AlertDialog(
            onDismissRequest = { showDialogAlert = false },
            title = { Text("平台提示：资质尚未核验", fontWeight = FontWeight.Bold) },
            text = { Text("应交通主管部门道路网络货运相关安全条例：司机接单抢配货款前，必须完成实名、驾照及载重卡车行驶证核验比对。本过程完全免费。") },
            confirmButton = {
                Button(
                    onClick = {
                        showDialogAlert = false
                        viewModel.showCertifyDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandAmber)
                ) {
                    Text("立即一键认证", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialogAlert = false }) {
                    Text("先看看", color = TextSecondary)
                }
            }
        )
    }
}

// ==========================================
// NEW FEATURE USER COMPOSABLES FOR V2.0.0
// ==========================================

@Composable
fun SplashScreenView(viewModel: DriverViewModel) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)), // Deep slate loading bg
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF2563EB)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocalShipping,
                    contentDescription = "App Logo",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "大宗智联 • 司机端",
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "陕煤陕钢网络货运承运平台",
                fontSize = 12.sp,
                color = Color(0xFF94A3B8)
            )
            Spacer(modifier = Modifier.height(60.dp))
            CircularProgressIndicator(
                color = Color(0xFF2563EB),
                strokeWidth = 3.dp,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "正在核对网络货运高精度安全轨信...",
                fontSize = 11.sp,
                color = Color(0xFF64748B)
            )
        }
    }
}

@Composable
fun VersionUpdateDialog(viewModel: DriverViewModel) {
    AlertDialog(
        onDismissRequest = { viewModel.showUpdateDialog = false },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "更新",
                    tint = BrandNavy,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("发现官方全新版本 V2.0.0", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "【大宗智联-司机端 V2.0.0 重磅来袭】",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = TextPrimary
                )
                Text(
                    text = "1. ✨ 新载加油系统：新增「智慧加油模块」，卡车油卡与余额度一键互转！\n" +
                           "2. 💳 银行卡精细管理：支持添加多级收款卡，设定默认极速结算储蓄卡。\n" +
                           "3. 🌟 资信评价公示：承运满意度得分公开，高资信卡友优先分配高价陕煤单。\n" +
                           "4. 💬 消息中心归纳：划分系统公告与卸煤称重结算快讯，掌握一手行盘通告。\n" +
                           "5. 🎨 个性化定制设置：提供红、橙、绿、蓝、靛多重工业承运皮肤色彩极速换装！",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.showUpdateDialog = false
                    android.widget.Toast.makeText(viewModel.getApplication(), "✅ 正在联运后台极速安装增量包性能升级中，承运不影响接单！", android.widget.Toast.LENGTH_LONG).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = BrandNavy)
            ) {
                Text("立即极速更新（增量）", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = { viewModel.showUpdateDialog = false }) {
                Text("稍后再说", color = TextSecondary)
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun RefuelingScreen(viewModel: DriverViewModel) {
    var searchStationQuery by remember { mutableStateOf("") }
    val selectedStationIdVal = viewModel.selectedStationIdForRefuel
    val context = androidx.compose.ui.platform.LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("智能大宗智慧加油系统", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.showRefuelingScreen = false }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
                modifier = Modifier.drawBehind {
                    drawLine(color = BorderSlate, start = Offset(0f, size.height), end = Offset(size.width, size.height), strokeWidth = 1.dp.toPx())
                }
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                NavigationBarItem(
                    selected = viewModel.refuelingScreenTab == 0,
                    onClick = { viewModel.refuelingScreenTab = 0 },
                    icon = { Icon(imageVector = Icons.Default.Map, contentDescription = "附近油站") },
                    label = { Text("附近加油", fontSize = 11.sp) }
                )
                NavigationBarItem(
                    selected = viewModel.refuelingScreenTab == 1,
                    onClick = { viewModel.refuelingScreenTab = 1 },
                    icon = { Icon(imageVector = Icons.Default.Receipt, contentDescription = "加油订单") },
                    label = { Text("订单历史", fontSize = 11.sp) }
                )
                NavigationBarItem(
                    selected = viewModel.refuelingScreenTab == 2,
                    onClick = { viewModel.refuelingScreenTab = 2 },
                    icon = { Icon(imageVector = Icons.Default.CreditCard, contentDescription = "油卡储值") },
                    label = { Text("我的油卡", fontSize = 11.sp) }
                )
            }
        },
        containerColor = CanvasBg
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            when (viewModel.refuelingScreenTab) {
                0 -> { // Station list
                    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
                        item {
                            OutlinedTextField(
                                value = searchStationQuery,
                                onValueChange = { searchStationQuery = it },
                                placeholder = { Text("输入加油站/加气站名称查找...", fontSize = 12.sp) },
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedBorderColor = BrandNavy,
                                    unfocusedBorderColor = BorderSlate,
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White
                                )
                            )
                        }

                        val filteredStations = viewModel.refuelingStations.filter {
                            searchStationQuery.isEmpty() || it.name.contains(searchStationQuery, ignoreCase = true)
                        }

                        items(filteredStations) { station ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, BorderSlate),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(if (station.isGas) Color(0xFF10B981).copy(alpha = 0.1f) else Color(0xFFEF4444).copy(alpha = 0.1f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = if (station.isGas) Icons.Default.PinDrop else Icons.Default.LocalGasStation,
                                                    contentDescription = null,
                                                    tint = if (station.isGas) Color(0xFF10B981) else Color(0xFFEF4444),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(station.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                                                Text(station.address, fontSize = 11.sp, color = TextSecondary)
                                            }
                                        }
                                        Text(station.distance, fontSize = 11.sp, color = BrandNavy, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Divider(color = BorderSlate, thickness = 1.dp)
                                    Spacer(modifier = Modifier.height(10.dp))
                                    
                                    if (station.isGas) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Column {
                                                Text("LNG 天然气智慧价", fontSize = 10.sp, color = TextSecondary)
                                                Row(verticalAlignment = Alignment.Bottom) {
                                                    Text("¥ ${station.gasPrice92}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = BrandGreenText)
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("挂牌价: ¥${station.marketPrice92}", fontSize = 10.sp, color = TextMuted, style = androidx.compose.ui.text.TextStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough))
                                                }
                                            }
                                            Button(
                                                onClick = { viewModel.selectStationForRefuel(station.id) },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.height(30.dp),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                                            ) {
                                                Text("立即加气", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    } else {
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                                    Column {
                                                        Text("0# 柴油", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                                                        Text("¥${station.dieselPrice0} (省¥${String.format("%.2f", station.marketPrice0 - station.dieselPrice0)})", fontSize = 12.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                                                    }
                                                    Column {
                                                        Text("#92 汽油", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                                                        Text("¥${station.gasPrice92} (省¥${String.format("%.2f", station.marketPrice92 - station.gasPrice92)})", fontSize = 11.sp, color = TextPrimary)
                                                    }
                                                    Column {
                                                        Text("#95 汽油", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                                                        Text("¥${station.gasPrice95} (省¥${String.format("%.2f", station.marketPrice95 - station.gasPrice95)})", fontSize = 11.sp, color = TextPrimary)
                                                    }
                                                }
                                                Button(
                                                    onClick = { viewModel.selectStationForRefuel(station.id) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = BrandNavy),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.height(30.dp),
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                                                ) {
                                                    Text("立即加油", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> { // Refueling orders list
                    if (viewModel.refuelingOrders.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("暂无加油/气智慧订单记录", color = TextSecondary, fontSize = 13.sp)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
                            items(viewModel.refuelingOrders) { order ->
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    border = BorderStroke(1.dp, BorderSlate),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Column {
                                                Text(order.stationName, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextPrimary)
                                                Text("油单号: ${order.id}", fontSize = 10.sp, color = TextMuted)
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(BrandGreenBg)
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(order.status, fontSize = 9.sp, color = BrandGreenText, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Divider(color = BorderSlate, thickness = 0.5.dp)
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Column {
                                                Text("车牌: ${order.vehiclePlate} | 枪号: ${order.gunNo}", fontSize = 11.sp, color = TextSecondary)
                                                Text("油品规格 / 单价: ${order.fuelType} | ¥${order.pricePerLitre}/L", fontSize = 11.sp, color = TextSecondary)
                                                Text("加油加气数量: ${String.format("%.2f", order.litres)} 升(公升/公斤)", fontSize = 11.sp, color = TextSecondary)
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text("油卡核销扣款", fontSize = 9.sp, color = TextSecondary)
                                                Text("¥ ${order.amount}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                                                Text(order.date, fontSize = 9.sp, color = TextMuted)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> { // Oil card wallet
                    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp)
                                    .padding(bottom = 16.dp),
                                colors = CardDefaults.cardColors(containerColor = BrandNavy),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Box(modifier = Modifier.fillMaxSize().padding(18.dp)) {
                                    Column {
                                        Text("大宗智联•智慧油卡 (联名互通卡)", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("陕煤 & 延长石油联名专用核销介质", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                        Spacer(modifier = Modifier.weight(1f))
                                        Text("智慧油卡可用金余金 (元)", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                                        Text("¥ ${String.format("%.2f", viewModel.oilCardBalance)}", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Icon(
                                        imageVector = Icons.Default.LocalGasStation,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.15f),
                                        modifier = Modifier.size(80.dp).align(Alignment.BottomEnd).offset(x = 10.dp, y = 10.dp)
                                    )
                                }
                            }
                        }

                        item {
                            Text("运费一键转换充值油卡", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary, modifier = Modifier.padding(bottom = 8.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, BorderSlate),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text("司机提现主钱包可用余额: ¥ ${String.format("%.2f", viewModel.profile.value?.balance ?: 0.0)}", fontSize = 12.sp, color = TextSecondary)
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        listOf(100.0, 500.0, 1000.0).forEach { amt ->
                                            OutlinedButton(
                                                onClick = { viewModel.chargeOilCard(amt) },
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandNavy),
                                                shape = RoundedCornerShape(8.dp),
                                                border = BorderStroke(1.dp, BrandBlueBorder),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("转充¥${amt.toInt()}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("💡 温馨建议：使用卡车主钱包多级资金转换，省去提现审核，直接对接中石化、中石油、延长石油在途核销，加油加气立享双重折扣保障！", fontSize = 10.sp, color = TextSecondary, lineHeight = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (selectedStationIdVal != null) {
        val station = viewModel.refuelingStations.find { it.id == selectedStationIdVal }
        if (station != null) {
            Dialog(onDismissRequest = { viewModel.selectedStationIdForRefuel = null }) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("一键极速扫码智慧加油", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                            IconButton(onClick = { viewModel.selectedStationIdForRefuel = null }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "关闭", modifier = Modifier.size(18.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(station.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BrandNavy)
                        Text(station.address, fontSize = 11.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.height(14.dp))
                        
                        Text("1. 选择加油枪油品", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextPrimary)
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (station.isGas) {
                                FilterChip(
                                    selected = viewModel.refuelSelectedFuel == "LNG",
                                    onClick = { viewModel.refuelSelectedFuel = "LNG" },
                                    label = { Text("LNG 天然气", fontSize = 11.sp) }
                                )
                            } else {
                                listOf("0#", "92#", "95#").forEach { f ->
                                    FilterChip(
                                        selected = viewModel.refuelSelectedFuel == f,
                                        onClick = { viewModel.refuelSelectedFuel = f },
                                        label = { Text("$f 号", fontSize = 11.sp) }
                                    )
                                }
                            }
                        }

                        Text("2. 选择油枪物理编号", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextPrimary, modifier = Modifier.padding(top = 10.dp))
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("1号", "2号", "3号").forEach { g ->
                                FilterChip(
                                    selected = viewModel.refuelSelectedGun == g,
                                    onClick = { viewModel.refuelSelectedGun = g },
                                    label = { Text(g, fontSize = 11.sp) }
                                )
                            }
                        }

                        Text("3. 选择/输入加油核销金额 (元)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextPrimary, modifier = Modifier.padding(top = 10.dp))
                        OutlinedTextField(
                            value = viewModel.refuelAmount,
                            onValueChange = { viewModel.refuelAmount = it },
                            placeholder = { Text("输入加油金额...", fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = BrandNavy,
                                unfocusedBorderColor = BorderSlate,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            )
                        )
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("200", "500", "1000", "2000").forEach { amt ->
                                OutlinedButton(
                                    onClick = { viewModel.refuelAmount = amt },
                                    modifier = Modifier.weight(1f).height(30.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("¥$amt", fontSize = 10.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        val priceText = when (viewModel.refuelSelectedFuel) {
                            "92#" -> station.gasPrice92
                            "95#" -> station.gasPrice95
                            "0#" -> station.dieselPrice0
                            else -> station.gasPrice92 // LNG
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("可用智慧油卡余额: ¥ ${String.format("%.2f", viewModel.oilCardBalance)}", fontSize = 10.sp, color = TextSecondary)
                                Text("当前精算单价: ¥ ${priceText}/L", fontSize = 11.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = {
                                    viewModel.executeRefuel {
                                        android.widget.Toast.makeText(context, "✅ 口卡钱包扣款加油成功！请注意行车安全。", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = BrandNavy),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("确认油卡限时支付", fontSize = 11.sp, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BankCardsScreen(viewModel: DriverViewModel) {
    var showAddDialog by remember { mutableStateOf(false) }
    var holderName by remember { mutableStateOf(viewModel.profile.value?.name ?: "王建国") }
    var cardNumber by remember { mutableStateOf("") }
    var bankName by remember { mutableStateOf("陕西省信用联社(农信社)") }
    val context = androidx.compose.ui.platform.LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("收款人储蓄银行卡管理", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.showBankCardsScreen = false }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(onClick = { showAddDialog = true }) {
                        Text("添加银行卡", color = BrandNavy, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
                modifier = Modifier.drawBehind {
                    drawLine(color = BorderSlate, start = Offset(0f, size.height), end = Offset(size.width, size.height), strokeWidth = 1.dp.toPx())
                }
            )
        },
        containerColor = CanvasBg
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            if (viewModel.bankCards.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("暂无已绑定的结运提现卡，请添加", color = TextSecondary, fontSize = 13.sp)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
                    item {
                        Text("极速结算储蓄银联卡列表", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary, modifier = Modifier.padding(bottom = 12.dp))
                    }
                    items(viewModel.bankCards) { card ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, if (card.isDefault) BrandBlueBorder else BorderSlate),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier.size(32.dp).clip(CircleShape).background(BrandNavy.copy(alpha = 0.1f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.AccountBalance, contentDescription = null, tint = BrandNavy, modifier = Modifier.size(16.dp))
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(card.bankName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                                            Text(card.cardType, fontSize = 10.sp, color = TextSecondary)
                                        }
                                    }
                                    if (card.isDefault) {
                                        Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(BrandBlueBg).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                            Text("默认收款卡", fontSize = 9.sp, color = BrandNavy, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(card.cardNumber, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("收款开户名: ${card.holderName}", fontSize = 11.sp, color = TextSecondary)
                                    TextButton(
                                        onClick = {
                                            viewModel.removeBankCard(card.id)
                                            android.widget.Toast.makeText(context, "✅ 银行储蓄收款卡解绑成功", android.widget.Toast.LENGTH_SHORT).show()
                                        },
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text("解绑销户", fontSize = 10.sp, color = Color(0xFFEF4444))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        Dialog(onDismissRequest = { showAddDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("添加收款人储蓄银联卡", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                        IconButton(onClick = { showAddDialog = false }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "关闭", modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    Text("开户所属银行", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    OutlinedTextField(
                        value = bankName,
                        onValueChange = { bankName = it },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = BrandNavy,
                            unfocusedBorderColor = BorderSlate,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )

                    Text("持卡人真实姓名", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.padding(top = 8.dp))
                    OutlinedTextField(
                        value = holderName,
                        onValueChange = { holderName = it },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = BrandNavy,
                            unfocusedBorderColor = BorderSlate,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )

                    Text("银联储蓄卡16-19位卡号", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.padding(top = 8.dp))
                    OutlinedTextField(
                        value = cardNumber,
                        onValueChange = { cardNumber = it },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        placeholder = { Text("6217...", fontSize = 11.sp) },
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = BrandNavy,
                            unfocusedBorderColor = BorderSlate,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            if (cardNumber.isBlank() || bankName.isBlank()) {
                                android.widget.Toast.makeText(context, "请填入完整的开户所属银行及储蓄卡卡号！", android.widget.Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.addBankCard(holderName, cardNumber, bankName)
                                showAddDialog = false
                                android.widget.Toast.makeText(context, "✅ 收款银行卡绑定核验通过！", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandNavy),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        Text("绑定卡片安全提交", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun MessageCenterScreen(viewModel: DriverViewModel) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("智联消息及理货公告中心", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.showMessageCenterScreen = false }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
                modifier = Modifier.drawBehind {
                    drawLine(color = BorderSlate, start = Offset(0f, size.height), end = Offset(size.width, size.height), strokeWidth = 1.dp.toPx())
                }
            )
        },
        containerColor = CanvasBg
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            if (viewModel.notifications.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("暂无消息通告通告", color = TextSecondary, fontSize = 13.sp)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
                    item {
                        Text("最新发布的联通快盘指示", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary, modifier = Modifier.padding(bottom = 12.dp))
                    }
                    items(viewModel.notifications) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, BorderSlate),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(if (item.category == "系统通知") BrandAmberBg else BrandBlueBg)
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(item.category, fontSize = 9.sp, color = if (item.category == "系统通知") BrandAmber else BrandNavy, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(item.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                                    }
                                    Text(item.date.substringBefore(" "), fontSize = 10.sp, color = TextMuted)
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(item.content, fontSize = 11.sp, color = TextSecondary, lineHeight = 15.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}



@Composable
fun ReviewsScreen(viewModel: DriverViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("司机信誉评价及调度资信库", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.showReviewsScreen = false }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.showReviewsDialog = true }) {
                        Text("模拟评分", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandNavy)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
                modifier = Modifier.drawBehind {
                    drawLine(color = BorderSlate, start = Offset(0f, size.height), end = Offset(size.width, size.height), strokeWidth = 1.dp.toPx())
                }
            )
        },
        containerColor = CanvasBg
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
                // Header rating summary stats
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, BorderSlate)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text("司机综合历史资信评分", fontSize = 12.sp, color = TextSecondary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("${viewModel.reviewsRating}", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Row {
                                                repeat(5) { star ->
                                                    val isGold = star < viewModel.reviewsRating.toInt()
                                                    Icon(
                                                        imageVector = Icons.Default.Star,
                                                        contentDescription = null,
                                                        tint = if (isGold) Color(0xFFFFD700) else Color(0xFFE2E8F0),
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                            Text("共计 ${viewModel.reviewsCount} 条调度/货主考评分单", fontSize = 9.sp, color = TextMuted)
                                        }
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(CircleShape)
                                        .background(BrandGreenBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("极优", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = BrandGreenText)
                                        Text("AAA级", fontSize = 9.sp, color = BrandGreenText)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text("资信等级评定：本考核体系对接行业征信及中国交通物流数据。高分司机专享陕煤集团在途保价保障，免交额外承运保险，货主直付率提升30%！", fontSize = 10.sp, color = TextSecondary, lineHeight = 14.sp)
                        }
                    }
                }

                item {
                    Text("最近来自调度厂矿评价历史", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary, modifier = Modifier.padding(bottom = 10.dp))
                }

                items(viewModel.driverReviews) { r ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, BorderSlate),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier.size(24.dp).clip(CircleShape).background(BrandNavy.copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = BrandNavy, modifier = Modifier.size(12.dp))
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(r.reviewerName, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextPrimary)
                                }
                                Row {
                                    repeat(r.rating) {
                                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(12.dp))
                                    }
                                    repeat(5 - r.rating) {
                                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFE2E8F0), modifier = Modifier.size(12.dp))
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                                r.tags.forEach { tag ->
                                    Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(CanvasBg).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                        Text(tag, fontSize = 10.sp, color = TextSecondary)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                Text(r.date, fontSize = 9.sp, color = TextMuted)
                            }
                        }
                    }
                }
            }
        }
    }

    if (viewModel.showReviewsDialog) {
        Dialog(onDismissRequest = { viewModel.showReviewsDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("模拟厂矿调度评分考核", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("模拟本次陕煤在途承运已顺利送达，请输入收货端大康电厂磅房给本车打的分数值：", fontSize = 12.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        listOf(3, 4, 5).forEach { stars ->
                            FilterChip(
                                selected = viewModel.reviewDialogRating == stars,
                                onClick = { viewModel.reviewDialogRating = stars },
                                label = { Text("$stars 星 (极推)", fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text("选择综合印象标签", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("安全送达", "配合装煤", "车辆极度整洁").forEach { tag ->
                            FilterChip(
                                selected = viewModel.reviewDialogTag == tag,
                                onClick = { viewModel.reviewDialogTag = tag },
                                label = { Text(tag, fontSize = 10.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            viewModel.addSampleReview(viewModel.reviewDialogRating, viewModel.reviewDialogTag)
                            viewModel.showReviewsDialog = false
                            android.widget.Toast.makeText(context, "✅ 口碑资信评分模拟注入成功！", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandNavy),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("立即注入信誉口碑值", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(viewModel: DriverViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (viewModel.settingsScreenSubTab) {
                            1 -> "动态承运主题换装"
                            2 -> "在途多国语言转换"
                            3 -> "关于我们及官方资质"
                            4 -> "智联研发及反馈建言"
                            5 -> "智联一键核销提现密码"
                            else -> "智联系统通用设置中心"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (viewModel.settingsScreenSubTab == 0) {
                                viewModel.showSettingsScreen = false
                            } else {
                                viewModel.settingsScreenSubTab = 0
                            }
                        }
                    ) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
                modifier = Modifier.drawBehind {
                    drawLine(color = BorderSlate, start = Offset(0f, size.height), end = Offset(size.width, size.height), strokeWidth = 1.dp.toPx())
                }
            )
        },
        containerColor = CanvasBg
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            when (viewModel.settingsScreenSubTab) {
                0 -> { // Settings index list
                    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, BorderSlate),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column {
                                    ProfileMenuRow("🎨 个性化大宗承运主题色彩", Icons.Default.Palette)  { viewModel.settingsScreenSubTab = 1 }
                                    Divider(color = CanvasBg, thickness = 1.dp)
                                    ProfileMenuRow("🌐 在途多国语音/语言设置", Icons.Default.Language) { viewModel.settingsScreenSubTab = 2 }
                                    Divider(color = CanvasBg, thickness = 1.dp)
                                    ProfileMenuRow("🔐 司机一键秒提付密密码设置", Icons.Default.Lock) { viewModel.settingsScreenSubTab = 5 }
                                    Divider(color = CanvasBg, thickness = 1.dp)
                                    ProfileMenuRow("📝 新增智联研发反馈与功能建言", Icons.Default.Feedback) { viewModel.settingsScreenSubTab = 4 }
                                    Divider(color = CanvasBg, thickness = 1.dp)
                                    ProfileMenuRow("ℹ️ 关于平台架构与金税备案资质", Icons.Default.Info) { viewModel.settingsScreenSubTab = 3 }
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    viewModel.isLoggedIn = false
                                    viewModel.showSettingsScreen = false
                                    viewModel.profileSubTab = 0
                                    android.widget.Toast.makeText(context, "✅ 安全注销司机端账户，位置授信保障安全同步注销", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("🚪 安全退出当前司机端账号", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                1 -> { // Theme switching
                    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
                        item {
                            Text("点击选取承运工作台主体色调", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary, modifier = Modifier.padding(bottom = 12.dp))
                        }
                        val paletteNames = listOf("科技海蓝 (默认蓝)", "中国红运 (陕钢红)", "丝路陇原 (生态绿)", "陕北煤炭 (能源黄)", "靛紫星空 (高品智)")
                        items(paletteNames.size) { index ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                                    .clickable {
                                        viewModel.selectedThemeColor = index
                                        android.widget.Toast.makeText(context, "✅ 主题色调同步更新！", android.widget.Toast.LENGTH_SHORT).show()
                                    },
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, if (viewModel.selectedThemeColor == index) BrandNavy else BorderSlate),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    val localColor = when (index) {
                                        1 -> Color(0xFFEF4444)
                                        2 -> Color(0xFF10B981)
                                        3 -> Color(0xFFF97316)
                                        4 -> Color(0xFF6366F1)
                                        else -> Color(0xFF2563EB)
                                    }
                                    Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(localColor))
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(paletteNames[index], fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextPrimary)
                                    Spacer(modifier = Modifier.weight(1f))
                                    if (viewModel.selectedThemeColor == index) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = BrandNavy)
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> { // Language selector
                    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
                        item {
                            Text("语言设置", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary, modifier = Modifier.padding(bottom = 12.dp))
                        }
                        val languages = listOf("简体中文", "English", "跟随系统")
                        items(languages.size) { index ->
                            val lang = languages[index]
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                                    .clickable {
                                        viewModel.selectedLanguage = lang
                                        android.widget.Toast.makeText(context, "已切换为: $lang", android.widget.Toast.LENGTH_SHORT).show()
                                    },
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, if (viewModel.selectedLanguage == lang) BrandNavy else BorderSlate),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(lang, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextPrimary)
                                    Spacer(modifier = Modifier.weight(1f))
                                    if (viewModel.selectedLanguage == lang) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = BrandNavy)
                                    }
                                }
                            }
                        }
                    }
                }
                3 -> { // About us
                    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)).background(BrandNavy),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.LocalShipping, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("大宗智联司机端 V2.0.0", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                                    Text("大宗物流网络货运智联承运网络", fontSize = 11.sp, color = TextSecondary)
                                }
                            }
                        }
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, BorderSlate)
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("• 平台名称: 大宗商品智联集成网络货运卡车端", fontSize = 12.sp, color = TextPrimary)
                                    Text("• 监管备案: 国家交通运输部网络货运信息交互系统", fontSize = 12.sp, color = TextPrimary)
                                    Text("• 金税支持: 国家税务总局三级金税多级快捷代开代缴开具系统", fontSize = 12.sp, color = TextPrimary)
                                    Text("• 安全技术: 北斗卫星车载定位轨迹与三方磅单数字确权交互", fontSize = 12.sp, color = TextPrimary)
                                    Text("• 技术权属: 陕煤陕钢网络货运承运理货合规科技服务中心", fontSize = 12.sp, color = TextPrimary)
                                }
                            }
                        }
                    }
                }
                4 -> { // Feedback Form
                    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
                        item {
                            Text("请选择反馈或问题类型", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextPrimary)
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("产品建议", "功能异常", "安全防伪").forEach { type ->
                                    FilterChip(
                                        selected = viewModel.feedbackType == type,
                                        onClick = { viewModel.feedbackType = type },
                                        label = { Text(type, fontSize = 11.sp) }
                                    )
                                }
                            }
                        }
                        item {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("问题/建议详细描述 (不超过100字)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextPrimary)
                            OutlinedTextField(
                                value = viewModel.feedbackText,
                                onValueChange = { viewModel.feedbackText = it },
                                placeholder = { Text("请详细描述您的大宗货运操作建言、或者是治超地磅、加油、银行卡支付问题...", fontSize = 12.sp) },
                                modifier = Modifier.fillMaxWidth().height(120.dp).padding(vertical = 6.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedBorderColor = BrandNavy,
                                    unfocusedBorderColor = BorderSlate,
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White
                                )
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    viewModel.submitDriverFeedback {
                                        android.widget.Toast.makeText(context, "✅ 问题及建言已提交平台后台！", android.widget.Toast.LENGTH_SHORT).show()
                                        viewModel.settingsScreenSubTab = 0
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(44.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BrandNavy),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("确认安全提交智联服务大厅", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                5 -> { // Payment Password
                    var pwdVal by remember { mutableStateOf("") }
                    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
                        item {
                            Text("设定智联大宗直提卡付密密码（6位数字）", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextPrimary, modifier = Modifier.padding(bottom = 12.dp))
                            OutlinedTextField(
                                value = pwdVal,
                                onValueChange = { pwdVal = it },
                                placeholder = { Text("输入 6 位数字支付密码...", fontSize = 12.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                singleLine = true,
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedBorderColor = BrandNavy,
                                    unfocusedBorderColor = BorderSlate,
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White
                                )
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = {
                                    if (pwdVal.length != 6) {
                                        android.widget.Toast.makeText(context, "密码格式不符合，请输入完整6位数字！", android.widget.Toast.LENGTH_SHORT).show()
                                    } else {
                                        android.widget.Toast.makeText(context, "✅ 极速支付密码绑定核对通过，当前设备信用已强化！", android.widget.Toast.LENGTH_SHORT).show()
                                        viewModel.settingsScreenSubTab = 0
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(44.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BrandNavy),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("保存并提交指静脉防伪密码", color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}
