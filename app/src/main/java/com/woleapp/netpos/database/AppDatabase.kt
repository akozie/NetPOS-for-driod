package com.woleapp.netpos.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.netpluspay.nibssclient.models.TransactionResponse
import com.woleapp.netpos.database.dao.TransactionResponseDao
import com.woleapp.netpos.util.RoomTypeConverters


@Database(entities = [TransactionResponse::class], version = 1, exportSchema = false)
@TypeConverters(
   RoomTypeConverters::class
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionResponseDao(): TransactionResponseDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabaseInstance(context: Context) = INSTANCE ?: synchronized(this) {
            INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
        }

        private fun buildDatabase(context: Context) =
            Room.databaseBuilder(context, AppDatabase::class.java, "netpos-db")
                .fallbackToDestructiveMigration()
                .build()
    }
}