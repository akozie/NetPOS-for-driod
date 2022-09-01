package com.woleapp.netpos.contactless.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class QrScannedDataModel(
    val card_scheme: String,
    val `data`: String
) : Parcelable
