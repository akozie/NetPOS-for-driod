package com.woleapp.netpos.contactless.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Data(
    val businessName: String,
    val address: String,
    val fullName: String,
    val accountNumber: String,
    val email: String,
    val phone: String
): Parcelable