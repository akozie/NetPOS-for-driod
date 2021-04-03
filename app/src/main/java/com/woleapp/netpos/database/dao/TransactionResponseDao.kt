package com.woleapp.netpos.database.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.netpluspay.nibssclient.models.TransactionResponse
import com.netpluspay.nibssclient.models.TransactionType
import io.reactivex.Single

@Dao
interface TransactionResponseDao {
    @Insert
    fun insertNewTransaction(transactionResponse: TransactionResponse): Single<Long>

    @Update
    fun updateTransaction(transactionResponse: TransactionResponse): Single<Int>

    @Query("SELECT * FROM transactionresponse ORDER BY id DESC")
    fun getTransactions(): LiveData<List<TransactionResponse>>

    @Query("SELECT * FROM transactionresponse WHERE transactionTimeInMillis >= :beginningOfDay and transactionTimeInMillis <= :endOfDay")
    fun getEndOfDayTransaction(
        beginningOfDay: Long,
        endOfDay: Long
    ): LiveData<List<TransactionResponse>>

    @Query("SELECT * FROM transactionresponse WHERE transactionType=:transactionType ORDER BY id DESC")
    fun getTransactionByTransactionType(transactionType: TransactionType): LiveData<List<TransactionResponse>>

    @Query("SELECT * FROM transactionresponse WHERE transactionType='PURCHASE' AND responseCode='00'")
    fun getRefundableTransactions(): LiveData<List<TransactionResponse>>

}