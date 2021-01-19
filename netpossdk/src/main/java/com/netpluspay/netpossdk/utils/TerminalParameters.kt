package com.netpluspay.netpossdk.utils

data class TerminalParameters @JvmOverloads constructor(
    var merchantId: String = "000000000000000",
    var merchantName: String = "Merchant",
    var terminalId: String = "Terminal",
    var terminalCountryCode:String = "0566",
    var terminalCurrencyCode: String = "0566"
)