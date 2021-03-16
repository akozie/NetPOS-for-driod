package com.woleapp.netpos.network

import com.woleapp.netpos.model.SuccessNetworkResponse
import com.woleapp.netpos.model.UtilitiesPayload
import com.woleapp.netpos.model.ValidateBillResponse
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