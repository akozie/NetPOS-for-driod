package com.woleapp.netpos.contactless.model

data class RegistrationModel(
    var businessName: String,
    var contactInformation: String,
    var businessAddress: String,
    var phoneNumber: String,
    var bank: String,
    var email: String,
    var isRegistered: Boolean = true
) {
    constructor() : this("", "", "", "", "", "")

    fun allFieldsFilled() =
        businessName.isNotEmpty() && contactInformation.isNotEmpty() && businessAddress.isNotEmpty() && phoneNumber.isNotEmpty() && bank.isNotEmpty() && email.isNotEmpty()

}

data class RegistrationError(val message: String)