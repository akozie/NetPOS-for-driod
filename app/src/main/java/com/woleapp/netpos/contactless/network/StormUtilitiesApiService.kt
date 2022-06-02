package com.woleapp.netpos.contactless.network

import com.woleapp.netpos.contactless.model.SuccessNetworkResponse
import com.woleapp.netpos.contactless.model.UtilitiesPayload
import com.woleapp.netpos.contactless.model.ValidateBillResponse
import io.reactivex.Single
import retrofit2.http.Body
import retrofit2.http.POST

interface StormUtilitiesApiService {
    @POST("api/utility/pay_bill")
    fun payBills(@Body payload: UtilitiesPayload): Single<SuccessNetworkResponse>

    /*@GET("api/utility/single_provider_fees/{provider_name}")
    fun getServiceFee(@Path("provider_name") providerName: String): Single<ServiceFeeResponse>*/

    @POST("api/utility/validate_bill")
    fun validateBill(@Body payload: UtilitiesPayload): Single<ValidateBillResponse>
}