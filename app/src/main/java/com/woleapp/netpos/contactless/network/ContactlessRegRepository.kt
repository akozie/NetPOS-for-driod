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
    fun findAccount(accountNumber: String, partnerId: String) = accountLookUpService.findAccount(AccountNumberLookUpRequest(accountNumber), partnerId)

    fun confirmOTP(phoneNumber: String, otp:String) = accountLookUpService.confirmOTP(
        ConfirmOTPRequest(phoneNumber, otp)
    )

    fun registerExistingAccount(existingAccountRegisterRequest: ExistingAccountRegisterRequest, partnerId: String) =
        accountLookUpService.registerExistingAccount(existingAccountRegisterRequest, partnerId)
}