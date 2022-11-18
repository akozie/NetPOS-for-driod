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
    fun findAccount(accountNumber: String) = accountLookUpService.findAccount(AccountNumberLookUpRequest(accountNumber))

    fun confirmOTP(phoneNumber: String, otp:String) = accountLookUpService.confirmOTP(
        ConfirmOTPRequest(otp,phoneNumber)
    )

    fun registerExistingAccount(existingAccountRegisterRequest: ExistingAccountRegisterRequest, bank:String, partnerId: String) =
        accountLookUpService.registerExistingAccount(existingAccountRegisterRequest, bank, partnerId)
}