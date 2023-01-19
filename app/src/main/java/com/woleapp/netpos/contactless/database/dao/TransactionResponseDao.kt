package com.woleapp.netpos.contactless.database.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.danbamitale.epmslib.entities.TransactionResponse
import com.danbamitale.epmslib.entities.TransactionType
import io.reactivex.Single

@Dao
interface TransactionResponseDao {
    @Insert
    fun insertNewTransaction(transactionResponse: TransactionResponse): Single<Long>

    @Update
    fun updateTransaction(transactionResponse: TransactionResponse): Single<Int>

    @Query("SELECT * FROM transactionresponse WHERE terminalId=:terminalId ORDER BY id DESC")
    fun getTransactions(terminalId: String): LiveData<List<TransactionResponse>>

    @Query("SELECT * FROM transactionresponse WHERE transactionTimeInMillis >= :beginningOfDay and transactionTimeInMillis <= :endOfDay and terminalId=:terminalId ORDER BY transactionTimeInMillis DESC")
    fun getEndOfDayTransaction(
        beginningOfDay: Long,
        endOfDay: Long,
        terminalId: String
    ): LiveData<List<TransactionResponse>>

    @Query("SELECT * FROM transactionresponse WHERE transactionType=:transactionType ORDER BY id DESC")
    fun getTransactionByTransactionType(transactionType: TransactionType): LiveData<List<TransactionResponse>>

    @Query("SELECT * FROM transactionresponse WHERE transactionType LIKE '%PURCHASE%' AND responseCode='00' ORDER BY id DESC")
    fun getRefundableTransactions(): LiveData<List<TransactionResponse>>

}