package com.woleapp.netpos.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.woleapp.netpos.model.MqttEventsLocal
import io.reactivex.Completable
import io.reactivex.Single

@Dao
interface MqttLocalDao {
    @Insert
    fun insertNewTransaction(mqttEventsLocal: MqttEventsLocal): Single<Long>

    @Query("DELETE FROM mqttEvents")
    fun deleteAllEvents(): Completable

    @Query("SELECT * FROM mqttEvents")
    fun getLocalEvents(): Single<List<MqttEventsLocal>>

    @Query("SELECT COUNT(*) from mqttEvents")
    fun getNumberOfFailedTransactions(): Single<Long>
}