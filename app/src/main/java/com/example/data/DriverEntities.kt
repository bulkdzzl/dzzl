package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "driver_profile")
data class DriverProfile(
    @PrimaryKey val id: Int = 1,
    val name: String = "张晓峰",
    val phone: String = "18629348899",
    val idCard: String = "610124198808123049",
    val idCardVerified: Boolean = false,
    val driverLicense: String = "A2",
    val driverLicenseVerified: Boolean = false,
    val idCardFrontImage: String? = null,
    val idCardBackImage: String? = null,
    val driverLicenseMainImage: String? = null,
    val driverLicenseSubImage: String? = null,
    val plateNumber: String = "陕K·8W912",
    val vehicleSize: String = "13米自卸半挂车",
    val vehicleWeightCapacity: Double = 49.0, // 49 吨
    val vehicleVerified: Boolean = false,
    val safetyMileage: Double = 145200.0,
    val balance: Double = 3450.00,
    val pendingSettlement: Double = 2850.00,
    val vehicleLicenseMainImage: String? = null,
    val vehicleLicenseSubImage: String? = null,
    val roadTransportPermit: String = "陕交规运字610100293482号",
    val roadTransportPermitImage: String? = null,
    val trailerPlateNumber: String = "陕K·8523挂"
) : Serializable

@Entity(tableName = "cargo_listing")
data class CargoListing(
    @PrimaryKey val id: Int,
    val cargoName: String,
    val departure: String,
    val departureCity: String,
    val destination: String,
    val destinationCity: String,
    val distance: Double,
    val pricePerTon: Double,
    val totalTons: Double,
    val remainedTons: Double,
    val publisher: String,
    val publishTime: String,
    val contactPhone: String,
    val vehicleReq: String
) : Serializable

@Entity(tableName = "waybill")
data class Waybill(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val cargoId: Int,
    val cargoName: String,
    val departure: String,
    val destination: String,
    val pricePerTon: Double,
    val targetTons: Double, // Target cargo tons
    val publisher: String,
    val status: String, // "CREATED" (待去装货), "LOADED" (运输中), "UNLOADED" (待确权结算), "AUDITING" (结算审核中), "SETTLED" (已结算结算), "PAID" (已到账)
    val statusText: String, // UI textual description
    val loadWeight: Double = 0.0, // 装入吨数
    val unloadWeight: Double = 0.0, // 卸货吨数
    val loadWeightImage: String? = null,
    val unloadWeightImage: String? = null,
    val freightPayment: Double = 0.0,
    val acceptTime: Long = System.currentTimeMillis(),
    val completeTime: Long? = null
) : Serializable

@Entity(tableName = "wallet_transaction")
data class WalletTransaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val waybillId: Int? = null,
    val title: String,
    val amount: Double, // Input + / Output -
    val timestamp: Long = System.currentTimeMillis(),
    val status: String, // "SUCCESS" (交易成功), "PENDING" (处理中)
    val paymentMethod: String // "余额结算" or "网商银行(8912)"
) : Serializable
