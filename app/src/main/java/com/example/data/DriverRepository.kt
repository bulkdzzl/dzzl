package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlin.random.Random

class DriverRepository(private val driverDao: DriverDao) {

    val profileFlow: Flow<DriverProfile?> = driverDao.getProfileFlow()
    val cargoListingsFlow: Flow<List<CargoListing>> = driverDao.getCargoListingsFlow()
    val waybillsFlow: Flow<List<Waybill>> = driverDao.getWaybillsFlow()
    val transactionsFlow: Flow<List<WalletTransaction>> = driverDao.getTransactionsFlow()

    // Seeds default profile if empty
    suspend fun checkAndSeedInitialData() {
        var profile = driverDao.getProfileDirect()
        if (profile == null) {
            profile = DriverProfile(
                id = 1,
                name = "王建国",
                phone = "18790218822",
                idCard = "610124198509123849",
                idCardVerified = true,
                driverLicense = "A2",
                driverLicenseVerified = true,
                plateNumber = "陕K·6A528",
                vehicleSize = "13.7米栏板半挂车",
                vehicleWeightCapacity = 49.0,
                vehicleVerified = true,
                safetyMileage = 186400.0,
                balance = 8600.00,
                pendingSettlement = 0.0
            )
            driverDao.insertProfile(profile)
        }

        // Seed some cargo listings if empty
        val currentCargos = driverDao.getCargoListingsFlow().firstOrNull() ?: emptyList()
        if (currentCargos.isEmpty()) {
            val seedCargos = listOf(
                CargoListing(
                    id = 101,
                    cargoName = "原煤（电煤特供）",
                    departure = "神木市大柳塔煤矿集货厂",
                    departureCity = "榆林市",
                    destination = "渭南市大康燃煤发电厂",
                    destinationCity = "渭南市",
                    distance = 520.0,
                    pricePerTon = 135.0,
                    totalTons = 1200.0,
                    remainedTons = 480.0,
                    publisher = "陕西神东电力燃料输运公司",
                    publishTime = "刚刚",
                    contactPhone = "13511112222",
                    vehicleReq = "重型自卸半挂车（侧翻/后翻）"
                ),
                CargoListing(
                    id = 102,
                    cargoName = "精选高品位铁矿粉",
                    departure = "商洛市柞水铁矿选矿库",
                    departureCity = "商洛市",
                    destination = "韩城陕钢集团中门堆场",
                    destinationCity = "韩城市",
                    distance = 310.0,
                    pricePerTon = 88.0,
                    totalTons = 3000.0,
                    remainedTons = 1850.0,
                    publisher = "宝武集团西北矿产事业部",
                    publishTime = "3分钟前",
                    contactPhone = "13622223333",
                    vehicleReq = "重型低平板半挂车"
                ),
                CargoListing(
                    id = 103,
                    cargoName = "机制高强度砂石",
                    departure = "蓝田县灞河建材机制砂石厂",
                    departureCity = "西安市",
                    destination = "西咸新区中建三局高铁新城工地",
                    destinationCity = "咸阳市",
                    distance = 85.0,
                    pricePerTon = 45.0,
                    totalTons = 5000.0,
                    remainedTons = 4100.0,
                    publisher = "蓝田华新优质骨料供应公司",
                    publishTime = "12分钟前",
                    contactPhone = "13733334444",
                    vehicleReq = "中重型箱式货车 / 自卸车"
                ),
                CargoListing(
                    id = 104,
                    cargoName = "螺纹钢筋（HRB400E）",
                    departure = "韩城陕钢成品B3仓库",
                    departureCity = "韩城市",
                    destination = "安康市高新区汉江大桥施工一分部",
                    destinationCity = "安康市",
                    distance = 490.0,
                    pricePerTon = 110.0,
                    totalTons = 500.0,
                    remainedTons = 240.0,
                    publisher = "陕西钢铁集团销售部",
                    publishTime = "25分钟前",
                    contactPhone = "13844445555",
                    vehicleReq = "13米高低板挂车"
                ),
                CargoListing(
                    id = 105,
                    cargoName = "散装水泥（P.O 42.5）",
                    departure = "铜川市尧柏水泥有限公司熟料厂",
                    departureCity = "铜川市",
                    destination = "西安市地铁15号线航天基地站工地",
                    destinationCity = "西安市",
                    distance = 115.0,
                    pricePerTon = 38.0,
                    totalTons = 1500.0,
                    remainedTons = 980.0,
                    publisher = "铜川尧柏特种水泥集团",
                    publishTime = "40分钟前",
                    contactPhone = "13955556666",
                    vehicleReq = "散装罐式水泥车"
                )
            )
            driverDao.insertCargoListings(seedCargos)
        }
    }

    suspend fun updateProfile(profile: DriverProfile) {
        driverDao.insertProfile(profile)
    }

    suspend fun grabCargo(cargo: CargoListing): Long {
        // Create an active waybill
        val waybill = Waybill(
            cargoId = cargo.id,
            cargoName = cargo.cargoName,
            departure = cargo.departure,
            destination = cargo.destination,
            pricePerTon = cargo.pricePerTon,
            targetTons = 35.0 + Random.nextInt(0, 10), // Default payload weight for heavy truck is around 35~45 tonnes
            publisher = cargo.publisher,
            status = "CREATED",
            statusText = "待装货已接单"
        )
        val id = driverDao.insertWaybill(waybill)

        // Decrement some cargo listings remained size
        val updatedCargo = cargo.copy(remainedTons = (cargo.remainedTons - waybill.targetTons).coerceAtLeast(0.0))
        driverDao.insertCargoListings(listOf(updatedCargo))

        return id
    }

    suspend fun cancelWaybill(waybillId: Int) {
        val waybill = driverDao.getWaybillById(waybillId) ?: return
        if (waybill.status == "CREATED") {
            val cargo = driverDao.getCargoListingById(waybill.cargoId)
            if (cargo != null) {
                val updatedCargo = cargo.copy(remainedTons = cargo.remainedTons + waybill.targetTons)
                driverDao.insertCargoListings(listOf(updatedCargo))
            }
            driverDao.deleteWaybillById(waybillId)
        }
    }

    suspend fun uploadLoadingTicket(waybillId: Int, actualLoadedWeight: Double, imageUri: String) {
        val waybill = driverDao.getWaybillById(waybillId) ?: return
        val updated = waybill.copy(
            loadWeight = actualLoadedWeight,
            loadWeightImage = imageUri,
            status = "LOADED",
            statusText = "运输中(已装车)"
        )
        driverDao.updateWaybill(updated)
    }

    suspend fun arriveDestination(waybillId: Int) {
        val waybill = driverDao.getWaybillById(waybillId) ?: return
        val updated = waybill.copy(
            status = "UNLOADED",
            statusText = "已抵达(卸货称重中)"
        )
        driverDao.updateWaybill(updated)
    }

    suspend fun uploadUnloadingTicket(waybillId: Int, actualUnloadedWeight: Double, imageUri: String) {
        val waybill = driverDao.getWaybillById(waybillId) ?: return
        val freightVal = actualUnloadedWeight * waybill.pricePerTon
        val updated = waybill.copy(
            unloadWeight = actualUnloadedWeight,
            unloadWeightImage = imageUri,
            freightPayment = freightVal,
            status = "AUDITING",
            statusText = "合规快速结算审核中"
        )
        driverDao.updateWaybill(updated)

        // Add to pending balance in profile
        val profile = driverDao.getProfileDirect() ?: return
        val updatedProfile = profile.copy(
            pendingSettlement = profile.pendingSettlement + freightVal
        )
        driverDao.updateProfile(updatedProfile)
    }

    suspend fun submitForAudit(waybillId: Int) {
        val waybill = driverDao.getWaybillById(waybillId) ?: return
        val updated = waybill.copy(
            status = "AUDITING",
            statusText = "资金自动确权核实中"
        )
        driverDao.updateWaybill(updated)
    }

    // Simmons immediate or manual approval by broker
    suspend fun approveWaybill(waybillId: Int) {
        val waybill = driverDao.getWaybillById(waybillId) ?: return
        if (waybill.status == "UNLOADED" || waybill.status == "AUDITING") {
            val freight = waybill.freightPayment
            val updated = waybill.copy(
                status = "PAID",
                statusText = "运费结算成功(已到账)",
                completeTime = System.currentTimeMillis()
            )
            driverDao.updateWaybill(updated)

            val profile = driverDao.getProfileDirect() ?: return
            val updatedProfile = profile.copy(
                balance = profile.balance + freight,
                pendingSettlement = (profile.pendingSettlement - freight).coerceAtLeast(0.0),
                safetyMileage = profile.safetyMileage + 350 // increase safety mileage randomly
            )
            driverDao.updateProfile(updatedProfile)

            // Add cash transaction
            val trans = WalletTransaction(
                waybillId = waybillId,
                title = "单号 #${waybillId} 运费结算到账",
                amount = freight,
                status = "SUCCESS",
                paymentMethod = "余额账户"
            )
            driverDao.insertTransaction(trans)
        }
    }

    suspend fun withdrawFunds(amount: Double, bankName: String, cardNumber: String): Boolean {
        val profile = driverDao.getProfileDirect() ?: return false
        if (profile.balance >= amount && amount > 0) {
            val updatedProfile = profile.copy(
                balance = profile.balance - amount
            )
            driverDao.updateProfile(updatedProfile)

            // Add withdraw transaction record
            val trans = WalletTransaction(
                title = "运费提现到银行卡",
                amount = -amount,
                status = "SUCCESS",
                paymentMethod = "$bankName (${cardNumber.takeLast(4)})"
            )
            driverDao.insertTransaction(trans)
            return true
        }
        return false
    }

    suspend fun updateCertification(
        name: String,
        idCard: String,
        plateNumber: String,
        vehicleSize: String
    ) {
        val profile = driverDao.getProfileDirect() ?: return
        val updated = profile.copy(
            name = name,
            idCard = idCard,
            plateNumber = plateNumber,
            vehicleSize = vehicleSize,
            idCardVerified = true,
            driverLicenseVerified = true,
            vehicleVerified = true
        )
        driverDao.updateProfile(updated)
    }
}
