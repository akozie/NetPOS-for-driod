package com.woleapp.netpos.contactless.network

import com.woleapp.netpos.contactless.model.AccountNumberLookUpRequest
import com.woleapp.netpos.contactless.model.ConfirmOTPRequest
import com.woleapp.netpos.contactless.model.ExistingAccountRegisterRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactlessRegRepository @Inject constructor(
    private val accountLookUpService: AccountLookUpService
) {
    fun findAccount(accountNumber: String, partnerId: String, deviceSerialId: String) = accountLookUpService.findAccount(AccountNumberLookUpRequest(accountNumber), partnerId, deviceSerialId)

    fun confirmOTP(phoneNumber: String, accountNumber: String, otp:String, partnerId: String) = accountLookUpService.
    confirmOTP(
        ConfirmOTPRequest(phoneNumber, accountNumber, otp), partnerId
    )

    fun registerExistingAccount(existingAccountRegisterRequest: ExistingAccountRegisterRequest, partnerId: String, deviceSerialId: String) =
        accountLookUpService.registerExistingAccount(existingAccountRegisterRequest, partnerId, deviceSerialId)
}