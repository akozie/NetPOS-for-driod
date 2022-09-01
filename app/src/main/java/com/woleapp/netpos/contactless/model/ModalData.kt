package com.woleapp.netpos.contactless.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ModalData(
    var status: Boolean,
    var amount: Double
) : Parcelable
