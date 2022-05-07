package com.woleapp.netpos.util

import androidx.room.TypeConverter
import com.danbamitale.epmslib.entities.TransactionType
import com.danbamitale.epmslib.utils.IsoAccountType


class RoomTypeConverters {
    companion object {
        @JvmStatic
        @TypeConverter
        fun convertIsoAccountEnumToString(isoAccountType: IsoAccountType) = isoAccountType.name

        @JvmStatic
        @TypeConverter
        fun convertIsoAccountStringToEnum(value: String) = IsoAccountType.valueOf(value)

        @JvmStatic
        @TypeConverter
        fun convertTransactionResponseEnumToString(transactionType: TransactionType) = transactionType.name

        @JvmStatic
        @TypeConverter
        fun convertTransactionResponseToEnum(value: String)= TransactionType.valueOf(value)
    }
}