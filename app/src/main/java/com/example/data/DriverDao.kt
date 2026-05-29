package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DriverDao {

    // --- Driver Profile ---
    @Query("SELECT * FROM driver_profile WHERE id = 1 LIMIT 1")
    fun getProfileFlow(): Flow<DriverProfile?>

    @Query("SELECT * FROM driver_profile WHERE id = 1 LIMIT 1")
    suspend fun getProfileDirect(): DriverProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: DriverProfile)

    @Update
    suspend fun updateProfile(profile: DriverProfile)

    // --- Cargo Listings ---
    @Query("SELECT * FROM cargo_listing ORDER BY id DESC")
    fun getCargoListingsFlow(): Flow<List<CargoListing>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCargoListings(listings: List<CargoListing>)

    @Query("DELETE FROM cargo_listing")
    suspend fun clearCargoListings()

    @Query("SELECT * FROM cargo_listing WHERE id = :id LIMIT 1")
    suspend fun getCargoListingById(id: Int): CargoListing?

    // --- Waybills ---
    @Query("SELECT * FROM waybill ORDER BY acceptTime DESC")
    fun getWaybillsFlow(): Flow<List<Waybill>>

    @Query("SELECT * FROM waybill WHERE id = :id LIMIT 1")
    suspend fun getWaybillById(id: Int): Waybill?

    @Query("DELETE FROM waybill WHERE id = :id")
    suspend fun deleteWaybillById(id: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWaybill(waybill: Waybill): Long

    @Update
    suspend fun updateWaybill(waybill: Waybill)

    // --- Transactions ---
    @Query("SELECT * FROM wallet_transaction ORDER BY timestamp DESC")
    fun getTransactionsFlow(): Flow<List<WalletTransaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: WalletTransaction)
}
