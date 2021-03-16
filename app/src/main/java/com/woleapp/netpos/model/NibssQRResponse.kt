package com.woleapp.netpos.model

import com.google.gson.annotations.SerializedName

data class NibssQRResponse(
    @SerializedName("ReturnCode") val returnCode: String?,
    @SerializedName("OrderSn", alternate = ["order_sn"]) val orderSn: String?,
    @SerializedName("CodeUrl") val codeUrl: String?,
    @SerializedName("order_no") val orderNo: String?,
    @SerializedName("ReturnMsg") val returnMessage: String?
)