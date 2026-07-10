package com.example.upipaymenttracker

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Insert
    suspend fun insert(transaction: TransactionModel)

    @Delete
    suspend fun delete(transaction: TransactionModel)

    @androidx.room.Update
    suspend fun update(transaction: TransactionModel)

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionModel>>
}