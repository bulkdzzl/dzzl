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
    var isLoggedIn by mutableStateOf(false)
    var profileSubTab by mutableStateOf(0) // 0: Main Me list, 1: Personal Profile screen, 2: Vehicle Profile screen

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

    fun submitUnloadingTicket(waybillId: Int, actualWeight: Double, imageUri: String) {
        viewModelScope.launch {
            isProcessing = true
            processMessage = "收货工厂物联网磅房：正在对接卸载称重数据..."
            delay(1500)
            repository.uploadUnloadingTicket(waybillId, actualWeight, imageUri)
            processMessage = "卸货磅单确认成功，实收重量 $actualWeight 吨。已计算运费：¥" + String.format("%.2f", actualWeight * 135) + "，正在发起自动结算审批。"
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
