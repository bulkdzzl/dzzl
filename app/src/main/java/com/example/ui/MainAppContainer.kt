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
private val BrandNavy = Color(0xFF2563EB)      // Professional Blue (Tailwind blue-600)
private val BrandBlueLight = Color(0xFF3B82F6)  // Medium Blue (Tailwind blue-500)
private val BrandBlueBg = Color(0xFFEFF6FF)     // Light Highlight Blue (Tailwind blue-50)
private val BrandBlueBorder = Color(0xFFDBEAFE) // Tailwind blue-100
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContainer(viewModel: DriverViewModel) {
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
                            onClick = { /* Simple notify view */ },
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
                targetState = if (activeWaybillId != null && currentTab == 1) 99 else currentTab,
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
                        else -> ProfileScreen(
                            profile = profile,
                            viewModel = viewModel
                        )
                    }
                    99 -> {
                        val currentActiveId = activeWaybillId
                        if (currentActiveId != null) {
                            WaybillDetailScreen(
                                waybillId = currentActiveId,
                                waybills = waybillsList,
                                viewModel = viewModel
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
                                "CREATED" -> "开始执行"
                                "LOADED" -> "在途确权"
                                "UNLOADED" -> "发起结算"
                                "AUDITING" -> "审核中"
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
            .padding(horizontal = 14.dp, vertical = 5.dp),
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
                    "CREATED" -> Triple(BrandAmberBg, BrandAmber, "去装货")
                    "LOADED" -> Triple(BrandBlueBg, BrandNavy, "运输中")
                    "UNLOADED" -> Triple(Color(0xFFF1F5F9), TextSecondary, "待首磅确权")
                    "AUDITING" -> Triple(BrandAmberBg, BrandAmber, "审核对接中")
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
    viewModel: DriverViewModel
) {
    val waybill = waybills.find { it.id == waybillId } ?: return
    var loadedWeightInput by remember { mutableStateOf("38.64") }
    var unloadedWeightInput by remember { mutableStateOf("38.21") }

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
                        Triple("CREATED", "承运成功·待到达装煤厂", "司机已接受运单合同，调度派发指标就位"),
                        Triple("LOADED", "装货磅单确权中", "上传发煤厂首车电子磅单，重量确权进行中"),
                        Triple("UNLOADED", "重度路段行驶中", "重卡在途跟迹定位中。安全行驶无红线违章记录"),
                        Triple("AUDITING", "卸载称重·等待最后结算核发", "到达受货地点，双向卸载磅单差额比对合格"),
                        Triple("PAID", "运费到账结资", "金税打款通过，款项随时可提现到银联卡")
                    )

                    stepsList.forEachIndexed { index, step ->
                        val currentStatusMatched = waybill.status == step.first
                        // Find current step index
                        val currentStepIndex = when(waybill.status) {
                            "CREATED" -> 0
                            "LOADED" -> 1
                            "UNLOADED" -> 3
                            "AUDITING" -> 4
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
                            Text("您第一步需要：到达发煤厂进行称重装料，并将［装煤厂地磅单］进行数据采集。", color = TextPrimary, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(14.dp))
                            
                            OutlinedTextField(
                                value = loadedWeightInput,
                                onValueChange = { loadedWeightInput = it },
                                label = { Text("装货实称地磅重 (吨)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Fake Camera Upload representation
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(CanvasBg)
                                    .clickable { /* Simulate capturing ticket picture */ },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Filled.QrCodeScanner, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(32.dp))
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("一键拍照OCR扫描煤矿地磅单", fontSize = 12.sp, color = TextSecondary)
                                    Text("(系统自动解析核实，无需人工填写)", fontSize = 10.sp, color = TextSecondary.copy(alpha = 0.7f))
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = {
                                    val weight = loadedWeightInput.toDoubleOrNull() ?: 38.0
                                    viewModel.submitLoadingTicket(waybill.id, weight, "temp_uri_loading")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = BrandNavy),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("确认首磅开始启运", color = Color.White, fontWeight = FontWeight.Bold)
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
                            Text("您正在路途中。货车北斗轨迹已开启。请勿长期超高档大油门、疲劳驾驶。", color = TextPrimary, fontSize = 13.sp)
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
                                    Text("装货磅重确权额度：${waybill.loadWeight} 吨", fontSize = 11.sp, color = TextSecondary)
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            
                            OutlinedTextField(
                                value = unloadedWeightInput,
                                onValueChange = { unloadedWeightInput = it },
                                label = { Text("输入到达卸货点收料单磅重 (吨)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(CanvasBg)
                                    .clickable { /* Simulate capturing ticket picture */ },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Filled.PhotoCamera, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(32.dp))
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("上传收料卸货地磅单 / 签收单照片", fontSize = 12.sp, color = TextSecondary)
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            
                            Button(
                                onClick = {
                                    val weight = unloadedWeightInput.toDoubleOrNull() ?: 38.0
                                    viewModel.submitUnloadingTicket(waybill.id, weight, "temp_uri_unloading")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = BrandAmber),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("到达卸货完毕，提交双向地磅证", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }

                        "UNLOADED" -> {
                            Text("已成功提交卸载。已产生代结算账款清单：", color = TextSecondary, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("发煤厂重磅: ${waybill.loadWeight} 吨", fontSize = 12.sp, color = TextPrimary)
                            Text("卸煤厂重磅: ${waybill.unloadWeight} 吨 (合理磅差: -0.42 吨)", fontSize = 12.sp, color = TextPrimary)
                            Text("网络货运单价: ¥${waybill.pricePerTon}/吨", fontSize = 12.sp, color = TextPrimary)
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "核实应发运费：¥" + String.format("%.2f", waybill.freightPayment),
                                fontWeight = FontWeight.Bold,
                                color = BrandGreen,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = { viewModel.requestAudit(waybill.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = BrandNavy),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("发起网络货运合规性快速结算审核", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }

                        "AUDITING" -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = BrandAmber, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("拼命对接金税合规端口，请稍等...", fontSize = 13.sp, color = TextSecondary)
                                Spacer(modifier = Modifier.height(14.dp))
                                Button(
                                    onClick = { viewModel.approveWaybillDirectly(waybill.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandNavy)
                                ) {
                                    Text("一键测试审批(跳过等待)", color = Color.White)
                                }
                            }
                        }

                        else -> {
                            // Paid State
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = BrandGreen, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("合同结款已到账提现钱包! ¥" + String.format("%.2f", waybill.freightPayment), fontWeight = FontWeight.Bold, color = BrandGreen)
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
    viewModel: DriverViewModel
) {
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
                    ProfileMenuRow("一键完美匹配实名资质比对", Icons.Default.VerifiedUser) {
                        viewModel.showCertifyDialog = true
                    }
                    Divider(color = CanvasBg, thickness = 1.dp)
                    ProfileMenuRow("全天候国家北斗星联位置安全保障", Icons.Default.NotificationsActive) {}
                    Divider(color = CanvasBg, thickness = 1.dp)
                    ProfileMenuRow("大宗智联官方24小时客服热线", Icons.Default.HeadsetMic) {}
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
                        focusedBorderColor = BrandNavy,
                        unfocusedBorderColor = Color.LightGray
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = viewModel.withdrawBank,
                    onValueChange = { viewModel.withdrawBank = it },
                    label = { Text("银行名称") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = viewModel.withdrawCard,
                    onValueChange = { viewModel.withdrawCard = it },
                    label = { Text("储蓄银联卡号") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
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
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = viewModel.certIdCard,
                    onValueChange = { viewModel.certIdCard = it },
                    label = { Text("身份证号码") },
                    placeholder = { Text("例如：610124...") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = viewModel.certPlate,
                    onValueChange = { viewModel.certPlate = it },
                    label = { Text("重卡特种车牌号码") },
                    placeholder = { Text("如：陕K·8W912") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = viewModel.certVehicleSize,
                    onValueChange = { viewModel.certVehicleSize = it },
                    label = { Text("厢体及车型规格") },
                    placeholder = { Text("如：13.7米侧翻半挂车") },
                    modifier = Modifier.fillMaxWidth()
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
                                text = "身份校验并安全开锁登录",
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
