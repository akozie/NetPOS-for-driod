package com.woleapp.netpos.util

import java.time.*
import java.util.*

fun getBeginningOfDay(timeStamp: Long? = null): Long =
    (if (timeStamp == null) ZonedDateTime.now() else ZonedDateTime.of(
        Date(timeStamp).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime(),
        ZoneId.systemDefault()
    )).with(LocalTime.MIN).toEpochSecond() * 1000

fun getEndOfDayTimeStamp(timeStamp: Long? = null): Long =
    (if (timeStamp == null) ZonedDateTime.now() else ZonedDateTime.of(
        Date(timeStamp).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime(),
        ZoneId.systemDefault()
    )).with(LocalTime.MAX).toEpochSecond() * 1000

fun playAround(): String {
    val localDateTime =
        Date(1604664119161).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
    val zonedDate = ZonedDateTime.of(localDateTime, ZoneId.systemDefault())
    return zonedDate.with(LocalTime.MAX).toEpochSecond().toString()
}