package com.woleapp.netpos.model

data class ConfigurationData(
    var ip: String,
    var port: String,
    var terminalId: String,
    var key1: String,
    var key2: String
)