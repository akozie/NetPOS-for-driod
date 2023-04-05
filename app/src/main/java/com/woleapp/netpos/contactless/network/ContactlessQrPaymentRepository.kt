package com.woleapp.netpos.contactless.network

import com.woleapp.netpos.contactless.model.PayWithQrRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactlessQrPaymentRepository @Inject constructor(
    private val qrPaymentService: QrPaymentService
) {
    fun payWithQr(payWithQrRequest: PayWithQrRequest
    ) = qrPaymentService.payWithQr(payWithQrRequest
    )

}