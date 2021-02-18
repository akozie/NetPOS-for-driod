package com.netpluspay.netpossdk.utils

data class TerminalParameters @JvmOverloads constructor(
    var merchantId: String = "000000000000000",
    var merchantName: String = "Merchant",
    var merchantCode: String = "3900",
    var terminalId: String = "12345678",
    var terminalCountryCode:String = "0566",
    var terminalCurrencyCode: String = "0566",
    var transCurrExp: String = "02",
    var referCurrCode:String = "0566",
    var referCurrExp:String = "02",
    var terminalType:String = "22",
    //var terminalCapability: String = "E068C8",
    var terminalCapability: String = "E0F8C8",
    var terminalExCapability: String = "F000F0F001",
    var terminalEntryMode:String = "051"
){
    init {
        if (merchantCode.length != 4)
            throw POSException(-9, "Merchant code length should be equal to 4")
        if (terminalId.length != 8)
            throw POSException(-9, "Length of Terminal Id should be 8")
        if (terminalCurrencyCode.length != 4)
            throw POSException(-9, "Length of Terminal currency code should be 4")
        if (terminalCountryCode.length != 4)
            throw POSException(-9, "Length of Terminal country code should be 4")
    }
}