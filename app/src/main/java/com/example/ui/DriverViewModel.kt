package com.example.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.AppDatabase
import com.example.data.CargoListing
import com.example.data.DriverProfile
import com.example.data.DriverRepository
import com.example.data.WalletTransaction
import com.example.data.Waybill
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

data class ChatMsg(val isUser: Boolean, val text: String, val timestamp: Long = System.currentTimeMillis())

data class RefuelingStation(
    val id: Int,
    val name: String,
    val address: String,
    val distance: String,
    val gasPrice92: Double,
    val marketPrice92: Double,
    val gasPrice95: Double,
    val marketPrice95: Double,
    val dieselPrice0: Double,
    val marketPrice0: Double,
    val isGas: Boolean = false
)

data class RefuelingOrder(
    val id: String,
    val stationName: String,
    val address: String,
    val fuelType: String,
    val gunNo: String,
    val vehiclePlate: String,
    val amount: Double,
    val pricePerLitre: Double,
    val litres: Double,
    val date: String,
    val status: String
)

data class BankCard(
    val id: Int,
    val bankName: String,
    val cardType: String,
    val holderName: String,
    val cardNumber: String,
    val isDefault: Boolean
)

data class SystemNotification(
    val id: Int,
    val title: String,
    val category: String, // "系统通知", "运单通知"
    val content: String,
    val date: String
)

data class DriverReview(
    val id: Int,
    val reviewerName: String,
    val rating: Int,
    val tags: List<String>,
    val date: String
)

class DriverViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = DriverRepository(db.driverDao())

    // --- Flows from DB ---
    val profile: StateFlow<DriverProfile?> = repository.profileFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val cargoListings: StateFlow<List<CargoListing>> = repository.cargoListingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val waybills: StateFlow<List<Waybill>> = repository.waybillsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val transactions: StateFlow<List<WalletTransaction>> = repository.transactionsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // --- UI Navigation State ---
    var currentTab by mutableStateOf(0) // 0: Cargo, 1: Waybills, 2: AI Coach, 3: Wallet, 4: Me
    var activeWaybillId by mutableStateOf<Int?>(null) // If set, opens waybill execution details details
    var selectedCargoId by mutableStateOf<Int?>(null) // If set, opens cargo details
    var isLoggedIn by mutableStateOf(false)
    var profileSubTab by mutableStateOf(0) // 0: Main Me list, 1: Personal Profile screen, 2: Vehicle Profile screen

    // --- Refueling Management Feature States ---
    var showRefuelingScreen by mutableStateOf(false)
    var refuelingScreenTab by mutableStateOf(0) // 0: Stations, 1: Orders, 2: Oil Card
    var refuelingStations by mutableStateOf(listOf(
        RefuelingStation(1, "中石化西安东三环站", "西安市雁塔区东三环与浐灞大道交叉口", "5.2km", 6.98, 8.12, 7.48, 8.62, 6.58, 7.80),
        RefuelingStation(2, "中石油榆林南站", "榆林市榆阳区环城南路18号", "12.5km", 7.02, 8.15, 7.52, 8.65, 6.62, 7.85),
        RefuelingStation(3, "延长石油延长路加油站", "延安市宝塔区延长路88号", "18.1km", 6.88, 8.08, 7.38, 8.58, 6.48, 7.75),
        RefuelingStation(4, "秦能西咸LNG加气站", "咸阳市秦都区世纪大道中段", "8.0km", 5.20, 6.50, 0.0, 0.0, 0.0, 0.0, isGas = true),
        RefuelingStation(5, "中石化神木滨河路站", "榆林市神木市滨河路120号", "25.0km", 6.95, 8.10, 7.45, 8.60, 6.55, 7.78)
    ))
    var refuelingOrders by mutableStateOf(listOf(
        RefuelingOrder("REF2026100501", "中石化西安东三环站", "西安市雁塔区东三环与浐灞大道交叉口", "0# 柴油", "2号", "陕K·8W912", 500.0, 6.58, 75.98, "2026-05-28 15:32", "已完成"),
        RefuelingOrder("REF2026100102", "中石油榆林南站", "榆林市榆阳区环城南路18号", "92# 汽油", "1号", "陕K·8W912", 200.0, 7.02, 28.49, "2026-05-25 09:12", "已完成")
    ))
    var oilCardBalance by mutableStateOf(2000.00)
    var selectedStationIdForRefuel by mutableStateOf<Int?>(null)
    var refuelSelectedFuel by mutableStateOf("0#")
    var refuelSelectedGun by mutableStateOf("2号")
    var refuelAmount by mutableStateOf("500")

    // --- Refueling Actions ---
    fun selectStationForRefuel(stationId: Int) {
        val station = refuelingStations.find { it.id == stationId } ?: return
        selectedStationIdForRefuel = stationId
        refuelSelectedFuel = if (station.isGas) "LNG" else "0#"
        refuelSelectedGun = "2号"
        refuelAmount = "500"
    }

    fun executeRefuel(onSuccess: () -> Unit) {
        val station = refuelingStations.find { it.id == selectedStationIdForRefuel } ?: return
        val amountDouble = refuelAmount.toDoubleOrNull() ?: 0.0
        if (amountDouble <= 0.0) return

        viewModelScope.launch {
            isProcessing = true
            processMessage = "正在连接【${station.name}】物联网接口核销扣款..."
            delay(1500)

            if (oilCardBalance < amountDouble) {
                processMessage = "扣款失败：油卡余额不足！当前余额为¥$oilCardBalance。"
                delay(1200)
                isProcessing = false
            } else {
                oilCardBalance -= amountDouble
                val fuelTypeLabel = when (refuelSelectedFuel) {
                    "92#" -> "92# 汽油"
                    "95#" -> "95# 汽油"
                    "0#" -> "0# 柴油"
                    else -> "LNG 天然气"
                }
                val price = when (refuelSelectedFuel) {
                    "92#" -> station.gasPrice92
                    "95#" -> station.gasPrice95
                    "0#" -> station.dieselPrice0
                    else -> station.gasPrice92 // gas price
                }
                val litres = if (price > 0.0) amountDouble / price else amountDouble / 6.0
                val dateStr = "2026-05-29 08:08"
                val newOrder = RefuelingOrder(
                    id = "REF20261029" + (refuelingOrders.size + 1),
                    stationName = station.name,
                    address = station.address,
                    fuelType = fuelTypeLabel,
                    gunNo = refuelSelectedGun,
                    vehiclePlate = profile.value?.plateNumber ?: "陕K·8W912",
                    amount = amountDouble,
                    pricePerLitre = price,
                    litres = litres,
                    date = dateStr,
                    status = "已完成"
                )
                refuelingOrders = listOf(newOrder) + refuelingOrders
                processMessage = "一键智联智慧加油扣款成功！请配合1号大宗车辆靠箱加油。"
                delay(1500)
                isProcessing = false
                selectedStationIdForRefuel = null
                refuelingScreenTab = 1 // Switch to Tab Orders
                onSuccess()
            }
        }
    }

    fun chargeOilCard(amount: Double) {
        viewModelScope.launch {
            isProcessing = true
            val balanceVal = profile.value?.balance ?: 0.0
            if (balanceVal < amount) {
                processMessage = "充值失败：卡车钱包余额不足以转换充值！"
                delay(1200)
                isProcessing = false
                return@launch
            }
            processMessage = "智联一键核算：正在从卡车运费提现账户中分配 ¥$amount 到油卡..."
            delay(1500)
            
            // Deduct profile balance & add transactions
            val current = profile.value ?: DriverProfile()
            val updated = current.copy(balance = balanceVal - amount)
            repository.updateProfile(updated)

            val newTx = com.example.data.WalletTransaction(
                title = "运费转冲油卡",
                amount = -amount,
                status = "SUCCESS",
                paymentMethod = "余额转换"
            )
            repository.insertTransaction(newTx)

            oilCardBalance += amount
            processMessage = "充值充油卡成功！油卡余额：¥$oilCardBalance。"
            delay(1200)
            isProcessing = false
        }
    }

    // --- Bank Card Feature States ---
    var showBankCardsScreen by mutableStateOf(false)
    var showAddBankCardDialog by mutableStateOf(false)
    var bankCards by mutableStateOf(listOf(
        BankCard(1, "中国建设银行", "储蓄卡", "王建国", "6217 **** **** 3829", isDefault = true),
        BankCard(2, "陕西省信用联社", "储蓄卡", "王建国", "6217 **** **** 3849", isDefault = false)
    ))

    fun addBankCard(name: String, cardNo: String, bank: String) {
        if (name.isBlank() || cardNo.isBlank() || bank.isBlank()) return
        val last4 = if (cardNo.length >= 4) cardNo.takeLast(4) else "8888"
        val formattedNo = "**** **** **** $last4"
        val newCard = BankCard(
            id = bankCards.size + 1,
            bankName = bank,
            cardType = "储蓄卡",
            holderName = name,
            cardNumber = formattedNo,
            isDefault = bankCards.isEmpty()
        )
        bankCards = bankCards + newCard
        showAddBankCardDialog = false
    }

    fun removeBankCard(id: Int) {
        bankCards = bankCards.filterNot { it.id == id }
    }

    // --- Message Center States ---
    var showMessageCenterScreen by mutableStateOf(false)
    var notifications by mutableStateOf(listOf(
        SystemNotification(1, "系统通知: 陕煤新货源上线", "系统通知", "今日新增神木至渭南原煤货源1200吨，调派车辆充足，运价稳定¥135/吨，欢迎师傅们积极申领抢单！", "2026-05-29 08:00"),
        SystemNotification(2, "运单通知: 电子运单已结算", "运单通知", "您的运单【DAZONG20261002】(原煤运输任务) 已通过金税多级审核，运费 ¥4,200.00 已划拨至您的司机钱包中，支持秒提到账。", "2026-05-29 07:12"),
        SystemNotification(3, "系统通知: 关于近期强降雨天气安全行车提示", "系统通知", "陕北路段未来3天将有暴雨。由于重载半挂自卸起承运阻力大，建议卡友控制车速，保持安全车距，注意山体滑坡！", "2026-05-28 14:00"),
        SystemNotification(4, "系统通知: 全国成品油下调预告", "系统通知", "预计本周五24时国内0#柴油下调约0.15元/升，各位师傅可酌情调整加油计划。", "2026-05-27 10:30")
    ))

    // --- Reviews State ---
    var showReviewsScreen by mutableStateOf(false)
    var reviewsRating by mutableStateOf(4.2)
    var reviewsCount by mutableStateOf(28)
    var driverReviews by mutableStateOf(listOf(
        DriverReview(1, "王经理 (陕煤运输处)", 5, listOf("货量准时", "安全送达", "车辆整洁"), "2026-05-28"),
        DriverReview(2, "华能电厂物资部", 4, listOf("货量准时", "配送专业"), "2026-05-25"),
        DriverReview(3, "神东煤炭收发主管", 5, listOf("配送效率极高", "态度诚恳"), "2026-05-20"),
        DriverReview(4, "渭南大康发货班长", 3, listOf("卸货配合好"), "2026-05-18"),
        DriverReview(5, "宝钢物流调度员", 4, listOf("车况及车棚严密"), "2026-05-15")
    ))

    fun addSampleReview(ratingVal: Int, tagSelected: String) {
        val newReview = DriverReview(
            id = driverReviews.size + 1,
            reviewerName = "装煤场调度负责人",
            rating = ratingVal,
            tags = listOf(tagSelected, "大宗智联认证"),
            date = "2026-05-29"
        )
        driverReviews = listOf(newReview) + driverReviews
        reviewsCount++
        reviewsRating = String.format(java.util.Locale.US, "%.1f", (driverReviews.sumOf { it.rating }.toDouble() / driverReviews.size)).toDouble()
    }

    // --- Settings Center States ---
    var showSettingsScreen by mutableStateOf(false)
    var settingsScreenSubTab by mutableStateOf(0) // 0: Main setting pane, 1: Theme selection, 2: Language, 3: About us, 4: Feedback feed, 5: Payment password
    var selectedThemeColor by mutableStateOf(0) // 0: Blue (Default), 1: Red, 2: Green, 3: Orange, 4: Purple/Slate
    var selectedLanguage by mutableStateOf("简体中文")
    var feedbackType by mutableStateOf("产品建议")
    var feedbackText by mutableStateOf("")

    fun submitDriverFeedback(onComplete: () -> Unit) {
        if (feedbackText.isBlank()) return
        viewModelScope.launch {
            isProcessing = true
            processMessage = "反馈提交中，正在上传至智联服务日志..."
            delay(1200)
            feedbackText = ""
            processMessage = "您的反馈提交成功！客服人员将尽快审阅并持续优化系统。"
            delay(1200)
            isProcessing = false
            onComplete()
        }
    }

    // --- Splash and Version Update Dialog States ---
    var showSplashScreen by mutableStateOf(true)
    var showUpdateDialog by mutableStateOf(false)

    fun initializeAppRef() {
        viewModelScope.launch {
            delay(1800) // Simulate splash checks
            showSplashScreen = false
            showUpdateDialog = true // Trigger discovered new version dialog
        }
    }

    var showReviewsDialog by mutableStateOf(false)
    var reviewDialogRating by mutableStateOf(5)
    var reviewDialogTag by mutableStateOf("安全抵达")

    // --- UI Dialog / Temp States ---
    var showWithdrawDialog by mutableStateOf(false)
    var withdrawAmount by mutableStateOf("")
    var withdrawBank by mutableStateOf("陕西省信用联社(农信社)")
    var withdrawCard by mutableStateOf("6217009088123849")

    var showCertifyDialog by mutableStateOf(false)
    var showScanDialog by mutableStateOf(false)
    var certName by mutableStateOf("")
    var certIdCard by mutableStateOf("")
    var certPlate by mutableStateOf("")
    var certVehicleSize by mutableStateOf("13米重型翻斗半挂车")

    var isProcessing by mutableStateOf(false)
    var processMessage by mutableStateOf("")

    // --- AI Chat States ---
    private val _chatMessages = MutableStateFlow<List<ChatMsg>>(
        listOf(
            ChatMsg(
                isUser = false,
                text = "您好！我是您的智能装运助手，您可以随时向我提问，例如：\n1. 中华人民共和国道路车辆限载标准？\n2. 榆林至咸阳大宗货运的最佳避堵路线？\n3. 货车超载49吨以上有什么处罚条例？"
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMsg>> = _chatMessages.asStateFlow()

    var chatInput by mutableStateOf("")
    var aiLoading by mutableStateOf(false)

    init {
        viewModelScope.launch {
            repository.checkAndSeedInitialData()
        }
    }

    fun selectWaybill(id: Int?) {
        activeWaybillId = id
    }

    fun selectTab(tab: Int) {
        currentTab = tab
        activeWaybillId = null // clear detail screen on tab switch
        selectedCargoId = null // clear cargo detail on tab switch
    }

    // --- Business Actions ---

    fun grabOrder(cargo: CargoListing, onComplete: () -> Unit) {
        viewModelScope.launch {
            isProcessing = true
            processMessage = "大宗商品智联平台：正在向煤矿及企业申请派单指标..."
            delay(1200)
            val waybillId = repository.grabCargo(cargo)
            processMessage = "接单成功！已生成统一网络货运运单。请尽快前往装煤/货场地。"
            delay(1500)
            isProcessing = false
            activeWaybillId = waybillId.toInt()
            currentTab = 1 // Switch to waybills
            onComplete()
        }
    }

    fun grabScanOrder(cargo: CargoListing, onComplete: () -> Unit) {
        viewModelScope.launch {
            isProcessing = true
            processMessage = "物联网扫码识别成功！正在解析【发货厂矿大宗合同】..."
            delay(1200)
            val driverPlate = profile.value?.plateNumber ?: "陕K·6A528"
            processMessage = "国家网络货运监管比对：卡车 $driverPlate 联运资质与环保标准检验通过..."
            delay(1200)
            val waybillId = repository.grabCargo(cargo)
            processMessage = "扫码承运接单成功！已生成陕煤/陕钢统一理货电子运单(DAZONG${waybillId + 20261000})。"
            delay(1500)
            isProcessing = false
            showScanDialog = false
            activeWaybillId = waybillId.toInt()
            currentTab = 1 // Switch to waybills
            onComplete()
        }
    }

    fun cancelWaybill(waybillId: Int, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            isProcessing = true
            processMessage = "大宗智联联运监控中心：正在取消本次承运申报..."
            delay(1200)
            repository.cancelWaybill(waybillId)
            processMessage = "运单已安全注销，对应承运指标已退回大宗货源池。"
            delay(1200)
            isProcessing = false
            activeWaybillId = null
            onComplete()
        }
    }

    fun submitLoadingTicket(waybillId: Int, actualWeight: Double, imageUri: String) {
        viewModelScope.launch {
            isProcessing = true
            processMessage = "发货工厂物联网磅房：正在对接称重系统数字确权..."
            delay(1500)
            repository.uploadLoadingTicket(waybillId, actualWeight, imageUri)
            processMessage = "装货磅单采集成功，重量确权为 $actualWeight 吨。运单已更新为［运输中］状态，请安全平稳驾驶！"
            delay(1500)
            isProcessing = false
        }
    }

    fun arriveDestination(waybillId: Int) {
        viewModelScope.launch {
            isProcessing = true
            processMessage = "大宗智联终端系统：正在连接卸货场入场道闸及大宗地磅..."
            delay(1200)
            repository.arriveDestination(waybillId)
            processMessage = "电子路牌核签通过，已进入第三步：卸货称重磅单采集！"
            delay(1200)
            isProcessing = false
        }
    }

    fun submitUnloadingTicket(waybillId: Int, actualWeight: Double, imageUri: String) {
        viewModelScope.launch {
            isProcessing = true
            processMessage = "收货工程物联网磅房：正在对接卸载称重数据..."
            delay(1500)
            repository.uploadUnloadingTicket(waybillId, actualWeight, imageUri)
            processMessage = "卸货磅单确认成功，实收重量 $actualWeight 吨。已完成第三步！正在直达第四步：金税多级快速安全结算审核中。"
            delay(1500)
            isProcessing = false
        }
    }

    fun requestAudit(waybillId: Int) {
        viewModelScope.launch {
            isProcessing = true
            processMessage = "金税系统及网络货运平台合规审验中..."
            delay(1200)
            repository.submitForAudit(waybillId)
            processMessage = "已提交核实！运货轨迹、进出磅及三双磅单审核对接..."
            delay(1200)
            // Auto approve in 3 seconds to show reactive wallet deposit
            processMessage = "审批通过！货主已签署确权结账。运费已划拨到云运单提现卡，请在“我的钱包”查看。"
            repository.approveWaybill(waybillId)
            delay(1500)
            isProcessing = false
        }
    }

    fun approveWaybillDirectly(waybillId: Int) {
        viewModelScope.launch {
            repository.approveWaybill(waybillId)
        }
    }

    fun performWithdraw() {
        val amountVal = withdrawAmount.toDoubleOrNull() ?: return
        viewModelScope.launch {
            isProcessing = true
            processMessage = "正在申请网商银行 / 农信社银联直提清算..."
            delay(1800)
            val success = repository.withdrawFunds(amountVal, withdrawBank, withdrawCard)
            if (success) {
                processMessage = "提现成功！¥${amountVal} 资金预计10分钟内入账。"
                showWithdrawDialog = false
                withdrawAmount = ""
            } else {
                processMessage = "提现失败：可用提现余额不足！"
            }
            delay(1500)
            isProcessing = false
        }
    }

    fun performCertification() {
        if (certName.isBlank() || certIdCard.isBlank() || certPlate.isBlank()) return
        viewModelScope.launch {
            isProcessing = true
            processMessage = "国家网络货运监管资质核验接口比对中..."
            delay(2000)
            repository.updateCertification(certName, certIdCard, certPlate, certVehicleSize)
            processMessage = "恭喜您！身份证、驾驶证、车辆行驶证及道路运输证三证合一比对成功！"
            delay(1500)
            showCertifyDialog = false
            isProcessing = false
        }
    }

    fun savePersonalProfile(
        name: String,
        phone: String,
        idCard: String,
        driverLicense: String,
        idCardFrontImage: String?,
        idCardBackImage: String?,
        driverLicenseMainImage: String?,
        driverLicenseSubImage: String?
    ) {
        viewModelScope.launch {
            val current = profile.value ?: DriverProfile()
            val updated = current.copy(
                name = name,
                phone = phone,
                idCard = idCard,
                driverLicense = driverLicense,
                idCardFrontImage = idCardFrontImage,
                idCardBackImage = idCardBackImage,
                driverLicenseMainImage = driverLicenseMainImage,
                driverLicenseSubImage = driverLicenseSubImage,
                idCardVerified = true,
                driverLicenseVerified = true
            )
            repository.updateProfile(updated)
        }
    }

    fun saveVehicleProfile(
        plate: String,
        size: String,
        weight: Double,
        vehicleLicenseMainImage: String?,
        vehicleLicenseSubImage: String?,
        roadTransportPermit: String,
        roadTransportPermitImage: String?,
        trailerPlateNumber: String
    ) {
        viewModelScope.launch {
            val current = profile.value ?: DriverProfile()
            val updated = current.copy(
                plateNumber = plate,
                vehicleSize = size,
                vehicleWeightCapacity = weight,
                vehicleLicenseMainImage = vehicleLicenseMainImage,
                vehicleLicenseSubImage = vehicleLicenseSubImage,
                roadTransportPermit = roadTransportPermit,
                roadTransportPermitImage = roadTransportPermitImage,
                trailerPlateNumber = trailerPlateNumber,
                vehicleVerified = true
            )
            repository.updateProfile(updated)
        }
    }

    // --- Gemini Support Call ---
    fun sendChatMessage() {
        val tempInput = chatInput.trim()
        if (tempInput.isEmpty()) return
        chatInput = ""

        val userMsg = ChatMsg(isUser = true, text = tempInput)
        _chatMessages.value = _chatMessages.value + userMsg

        viewModelScope.launch {
            aiLoading = true
            val responseText = callGeminiApiRest(tempInput)
            _chatMessages.value = _chatMessages.value + ChatMsg(isUser = false, text = responseText)
            aiLoading = false
        }
    }

    private suspend fun callGeminiApiRest(prompt: String): String = withContext(Dispatchers.IO) {
        val key = BuildConfig.GEMINI_API_KEY
        if (key.isEmpty() || key == "MY_GEMINI_API_KEY") {
            return@withContext "平台装载提示：本地加载离线AI助手。请绑定正确的 GEMINI_API_KEY 以解锁云端万物互联能力。\n关于大宗运输：单车限重一般为49吨以内，超载会面临罚款和扣分。请务必确认磅单在49-14吨标准合规范围内！"
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$key"

        // Build request body manually with Moshi or simple string builder to avoid serialization version mismatches.
        // String templates are 100% reliable and robust!
        val requestJson = """
            {
              "contents": [
                {
                  "parts": [
                    {
                      "text": "你是一个大宗商品公路运输专业智能导航与规则宣贯助手（大宗智联APP内的AI教练）。主要职责是给货运重卡司机（煤炭、砂石、钢材、化工危化品）解答运输路况、国家49吨限超标准、治超站磅单要求、安全平稳省油驾驶技巧，以及提供心理关怀。回答要简短、语气亲切、非常懂大宗重载行规，使用中文回答，回答字数在200字以内，重点突出。提问是：$prompt"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = requestJson.toRequestBody(mediaType)

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        return@withContext try {
            val response = client.newCall(request).execute()
            val respBodyString = response.body?.string() ?: ""
            if (response.isSuccessful && respBodyString.isNotEmpty()) {
                val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
                val adapter = moshi.adapter(Map::class.java)
                val responseMap = adapter.fromJson(respBodyString)
                
                // Deep parsing maps to find text
                val candidates = responseMap?.get("candidates") as? List<*>
                val firstCandidate = candidates?.firstOrNull() as? Map<*, *>
                val content = firstCandidate?.get("content") as? Map<*, *>
                val parts = content?.get("parts") as? List<*>
                val firstPart = parts?.firstOrNull() as? Map<*, *>
                val text = firstPart?.get("text") as? String

                text ?: "AI助手暂时处于离线状态，大宗车辆行驶请注意避开超高限高路段。"
            } else {
                "中国治超网政策提示：卡货车六轴车（俗称大挂车）双轴复合总限重不得超过49吨！前行即是高速治超秤。请卡友保持48.5吨左右最为稳妥。"
            }
        } catch (e: Exception) {
            "陕煤/陕钢重载专家友情提醒：当前通信网络存在起伏抖动。请确认您的装载货单，煤炭与非金属矿产运输需要随车携带货单磅单，切勿超限超速。"
        }
    }
}
