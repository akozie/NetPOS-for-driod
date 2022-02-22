package com.woleapp.netpos.network

import com.pixplicity.easyprefs.library.Prefs
import com.woleapp.netpos.model.User
import com.woleapp.netpos.util.PREF_USER
import com.woleapp.netpos.util.PREF_USER_TOKEN
import com.woleapp.netpos.util.Singletons
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber

class StormApiClient {

    companion object {

        private fun getBaseOkhttpClientBuilder(): OkHttpClient.Builder {
            val okHttpClientBuilder = OkHttpClient.Builder()

            val loggingInterceptor = HttpLoggingInterceptor()
            loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
            okHttpClientBuilder.addInterceptor(loggingInterceptor)

            return okHttpClientBuilder
        }

        private fun getOkHttpClient() =
            getBaseOkhttpClientBuilder()
                .addInterceptor(TokenInterceptor())
                .build()

        private fun getNipOkHttpClient() = getBaseOkhttpClientBuilder()
            .addInterceptor(NipInterceptor())
            .build()

        private const val BASE_URL = "https://netpos.netpluspay.com/"
        private var INSTANCE: StormApiService? = null
        fun getInstance(): StormApiService = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(getOkHttpClient())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(StormApiService::class.java)
                .also {
                    INSTANCE = it
                }
        }

        private const val BASE_URL_BILLS = "https://storm.netpluspay.com/"
        private var BILLS_INSTANCE: StormApiService? = null
        fun getBillsInstance(): StormApiService = BILLS_INSTANCE ?: synchronized(this) {
            BILLS_INSTANCE ?: Retrofit.Builder()
                .baseUrl(BASE_URL_BILLS)
                .client(getOkHttpClient())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(StormApiService::class.java)
                .also {
                    BILLS_INSTANCE = it
                }
        }

        private const val NIP_BASE_URL = "https://netpos.netpluspay.com/"
        private var NIPINSTANCE: NipService? = null
        fun getNipInstance(): NipService = NIPINSTANCE ?: synchronized(this) {
            NIPINSTANCE ?: Retrofit.Builder()
                .baseUrl(NIP_BASE_URL)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .client(getNipOkHttpClient())
                .build()
                .create(NipService::class.java)
                .also {
                    NIPINSTANCE = it
                }
        }

        private var smsServiceInstance: SmsService? = null
        fun getSmsServiceInstance(): SmsService = smsServiceInstance ?: synchronized(this) {
            smsServiceInstance ?: Retrofit.Builder()
                .baseUrl("https://sms.netpluspay.com")
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build().create(SmsService::class.java)
                .also {
                    smsServiceInstance = it
                }
        }

        private var masterPassQRServiceInstance: MasterPassQRService? = null
        fun getMasterPassQrServiceInstance(): MasterPassQRService =
            masterPassQRServiceInstance ?: synchronized(this) {
                masterPassQRServiceInstance ?: Retrofit.Builder()
                    .baseUrl("https://masterpassqr.netpluspay.com/api/v1/")
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(MasterPassQRService::class.java)
                    .also {
                        masterPassQRServiceInstance = it
                    }
            }

        private var nibssQRServiceInstance: NibssQRService? = null
        fun getNibssQRServiceInstance(): NibssQRService =
            nibssQRServiceInstance ?: synchronized(this) {
                nibssQRServiceInstance ?: Retrofit.Builder()
                    .baseUrl("https://masterpassqr.netpluspay.com/api/v1/")
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(NibssQRService::class.java)
                    .also {
                        nibssQRServiceInstance = it
                    }
            }

        private var zenithQrServiceInstance: ZenithQrService? = null
        fun getZenithQRServiceInstance(): ZenithQrService =
            zenithQrServiceInstance ?: synchronized(this) {
                zenithQrServiceInstance ?: Retrofit.Builder()
                    .baseUrl("https://api.zenith.netpluspay.com/qr/")
                    .client(getOkHttpClient())
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(ZenithQrService::class.java)
                    .also {
                        zenithQrServiceInstance = it
                    }
            }
    }
}

class TokenInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        val headersInReq = request.headers
        if (headersInReq["Authorization"].isNullOrEmpty()) {
            Prefs.getString(PREF_USER_TOKEN, null)?.let {
                request = request.newBuilder().addHeader("Authorization", "Bearer $it").build()
            }
        }
        val response = chain.proceed(request)
        val body = response.body
        val bodyString = body?.string()
        Timber.e("resp: $bodyString")
        return response.newBuilder().body(bodyString!!.toResponseBody(body.contentType()))
            .build()
    }
}


class BlueCodeInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val requestUri = chain.request().url.toUri().toString()
        Timber.e(requestUri)
        Thread.sleep(3000)
        return getResponse(chain, getResponseCode(requestUri), getResponseString(requestUri))
    }

    private fun getResponseCode(requestUrl: String) = when {
        requestUrl.contains("getQr") -> {
            if (Prefs.contains("hasQr")) 200 else 400
        }
        else -> 200
    }

    private fun getResponse(chain: Interceptor.Chain, code: Int, responseString: String) =
        chain.proceed(chain.request())
            .newBuilder()
            .code(code)
            .protocol(Protocol.HTTP_2)
            .message(responseString)
            .body(
                ResponseBody.create(
                    "application/json".toMediaTypeOrNull(),
                    responseString.toByteArray()
                )
            )
            .addHeader("content-type", "application/json")
            .build()


    private fun getResponseString(requestUrl: String): String {
        return when {
            requestUrl.contains("registerMerchant") -> {
                Prefs.putBoolean("hasQr", true)
                "{ \"message\": \"Merchant created successfully\", \"data\": { \"address\": { \"city\": \"LAGOS\", \"country\": \"NG\", \"line_1\": \"Kabiru str\", \"line_2\": null, \"zip\": \"100001\" }, \"booking_reference_prefix\": \"Payment to dapo@webmallng.com completed\", \"contact\": { \"emails\": [], \"gender\": null, \"name\": null, \"phone\": null }, \"ext_id\": \"2058XU23\", \"inserted_at\": \"2021-12-31T07:37:46.485082Z\", \"merchant_branch_id\": \"2058XU23\", \"merchant_ext_id\": \"c2306f02-6a92-21ea-957c-f23c929b0057\", \"name\": \"dapo@webmallng.com\", \"state\": \"ACTIVE\", \"updated_at\": \"2021-12-31T07:37:46.485082Z\", \"virtual_terminal\": null } }"
            }
            requestUrl.contains("getPostal") -> "{ \"ABIA\": \"440001\", \"ABUJA\": \"900001\", \"ADAMAWA\": \"640001\", \"AKWA-IBOM\": \"520001\", \"ANAMBRA\": \"420001\", \"BAUCHI\": \"740001\", \"BAYELSA\": \"561001\", \"BENUE\": \"970001\", \"BORNO\": \"600001\", \"CROSS-RIVER\": \"540001\", \"DELTA\": \"320001\", \"EBONYI\": \"840001\", \"EDO\": \"300001\", \"EKITI\": \"360001\", \"ENUGU\": \"400001\", \"GOMBE\": \"760001\", \"IMO\": \"460001\", \"JIGAWA\": \"720001\", \"KADUNA\": \"700001\", \"KANO\": \"800001\", \"KATSINA\": \"820001\", \"KEBBI\": \"860001\", \"KOGI\": \"260001\", \"KWARA\": \"240001\", \"LAGOS\": { \"MAINLAND\": \"100001\", \"ISLAND\": \"101001\" }, \"NASARAWA\": \"962001\", \"NIGER\": \"920001\", \"OGUN\": \"110001\", \"ONDO\": \"340001\", \"OSUN\": \"230001\", \"OYO\": \"200001\", \"PLATEAU\": \"930001\", \"RIVERS\": \"500001\", \"SOKOTO\": \"840001\", \"TARABA\": \"660001\", \"YOBE\": \"320001\", \"ZAMFARA\": \"860001\" }"
            requestUrl.contains("getQr") -> {
                if (Prefs.contains("hasQr")
                        .not()
                ) "{\"status\":\"failed\", \"message\":\"merchant not registered\"}"
                else "{ \"qr\": \"data:image/png;base64,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\", \"transactionId\": \"BC04012022131608438097\" }"
            }
            requestUrl.contains("getMCC") -> "{ \"aero servicio carabobo\": \"6236\", \"insurance sales, underwriting and premiums\": \"6300\", \"insurance–premiums\": \"6381\", \"insurance, not elsewhere classified ( no longer valid for first presentment work)\": \"6399\", \"real estate agents and managers\": \"6513\", \"remote stored value load — member financial institution\": \"6529\", \"remove stored value load — merchant\": \"6530\", \"payment transaction—customer financial institution\": \"6532\", \"payment transaction—merchant\": \"6533\", \"value purchase–member financial institution\": \"6535\", \"moneysend intracountry\": \"6536\", \"moneysend intercountry\": \"6537\", \"moneysend funding\": \"6538\", \"funding transaction (excluding moneysend)\": \"6539\", \"non-financial institutions – stored value card purchase/load\": \"6540\", \"overpayments\": \"6611\", \"savings bonds\": \"6760\", \"lodging — hotels, motels and resorts\": \"7011\", \"timeshares\": \"7012\", \"sporting and recreational camps\": \"7032\", \"trailer parks and camp-sites\": \"7033\", \"laundry, cleaning and garment services\": \"7210\", \"laundry services — family and commercial\": \"7211\", \"dry cleaners\": \"7216\", \"carpet and upholstery cleaning\": \"7217\", \"photographic studios\": \"7221\", \"barber and beauty shops\": \"7230\", \"shoe repair shops, shoe shine parlours and hat cleaning shops\": \"7251\", \"funeral services and crematoriums\": \"7261\", \"dating and escort services\": \"7273\", \"tax preparation services\": \"7276\", \"counselling services — debt, marriage and personal\": \"7277\", \"buying and shopping services and clubs\": \"7278\", \"hospital patient-personal funds withdrawal\": \"7280\", \"babysitting services\": \"7295\", \"clothing rentals — costumes, uniforms and formal wear\": \"7296\", \"massage parlours\": \"7297\", \"health and beauty spas\": \"7298\", \"miscellaneous personal services — not elsewhere classified\": \"7299\", \"advertising services\": \"7311\", \"consumer credit reporting agencies\": \"7321\", \"debt collection agencies\": \"7322\", \"blueprinting and photocopying services\": \"7332\", \"commercial photography, art and graphics\": \"7333\", \"quick copy, reproduction and blueprinting services\": \"7338\", \"stenographic and secretarial support services\": \"7339\", \"exterminating and disinfecting services\": \"7342\", \"cleaning, maintenance and janitorial services\": \"7349\", \"employment agencies and temporary help services\": \"7361\", \"computer programming, data processing and integrated systems design services\": \"7372\", \"information retrieval services\": \"7375\", \"computer maintenance and repair services — not elsewhere classified\": \"7379\", \"management, consulting and public relations services\": \"7392\", \"detective agencies, protective agencies and security services, including armoured cars and guard dogs\": \"7393\", \"equipment, tool, furniture and appliance rentals and leasing\": \"7394\", \"photofinishing laboratories and photo developing\": \"7395\", \"business services — not elsewhere classified\": \"7399\", \"truck stop\": \"7511\", \"automobile rental agency—not elsewhere classified\": \"7512\", \"truck and utility trailer rentals\": \"7513\", \"motor home and recreational vehicle rentals\": \"7519\", \"parking lots and garages\": \"7523\", \"express payment service merchants–parking lots and garages\": \"7524\", \"automotive body repair shops\": \"7531\", \"tyre retreading and repair shops\": \"7534\", \"automotive paint shops\": \"7535\", \"automotive service shops (non-dealer)\": \"7538\", \"automotive service shops (spain) - other merchant categories\": \"7539\", \"car washes\": \"7542\", \"towing services\": \"7549\", \"electronics repair shops\": \"7622\", \"air conditioning and refrigeration repair shops\": \"7623\", \"electrical and small appliance repair shops\": \"7629\", \"watch, clock and jewellery repair shops\": \"7631\", \"furniture reupholstery, repair and refinishing\": \"7641\", \"welding services\": \"7692\", \"miscellaneous repair shops and related services\": \"7699\", \"government-owned lotteries (us region only)\": \"7800\", \"government licensed on-line casinos (on-line gambling) (us region only)\": \"7801\", \"government-licensed horse/dog racing (us region only)\": \"7802\", \"motion picture and video tape production and distribution\": \"7829\", \"motion picture theatres\": \"7832\", \"express payment service — motion picture theater\": \"7833\", \"video tape rentals\": \"7841\", \"dance halls, studios and schools\": \"7911\", \"theatrical producers (except motion pictures) and ticket agencies\": \"7922\", \"bands, orchestras, and miscellaneous entertainers (not elsewhere classified)\": \"7929\", \"billiard and pool establishments\": \"7932\", \"bowling alleys\": \"7933\", \"commercial sports, professional sports clubs, athletic fields and sports promoters\": \"7941\", \"tourist attractions and exhibits\": \"7991\", \"public golf courses\": \"7992\", \"video amusement game supplies\": \"7993\", \"video game arcades and establishments\": \"7994\", \"betting, including lottery tickets, casino gaming chips, off-track betting, and wagers at race tracks\": \"7995\", \"amusement parks, circuses, carnivals, and fortune tellers\": \"7996\", \"membership clubs (sports, recreation, athletic), country clubs and private golf courses\": \"7997\", \"aquariums, seaquariums, dolphinariums, and zoos\": \"7998\", \"recreation services — not elsewhere classified\": \"7999\", \"doctors and physicians — not elsewhere classified\": \"8011\", \"dentists and orthodontists\": \"8021\", \"osteopaths\": \"8031\", \"chiropractors\": \"8041\", \"optometrists and ophthalmologists\": \"8042\", \"opticians, optical goods and eyeglasses\": \"8043\", \"optical goods and eyeglasses\": \"8044\", \"podiatrists and chiropodists\": \"8049\", \"nursing and personal care facilities\": \"8050\", \"hospitals\": \"8062\", \"medical and dental laboratories\": \"8071\", \"medical services and health practitioners — not elsewhere classified\": \"8099\", \"legal services and attorneys\": \"8111\", \"elementary and secondary schools\": \"8211\", \"colleges, universities, professional schools and junior colleges\": \"8220\", \"correspondence schools\": \"8241\", \"business and secretarial schools\": \"8244\", \"trade and vocational schools\": \"8249\", \"schools and educational services — not elsewhere classified\": \"8299\", \"child care services\": \"8351\", \"charitable and social service organizations\": \"8398\", \"civic, social and fraternal associations\": \"8641\", \"political organizations\": \"8651\", \"religious organizations\": \"8661\", \"automobile associations\": \"8675\", \"membership organization — not elsewhere classified\": \"8699\", \"testing laboratories (non-medical)\": \"8734\", \"architectural, engineering, and surveying services\": \"8911\", \"accounting, auditing, and bookkeeping services\": \"8931\", \"professional services — not elsewhere classified\": \"8999\", \"i-purchasing pilot\": \"9034\", \"court costs, including alimony and child support\": \"9211\", \"fines\": \"9222\", \"bail and bond payments\": \"9223\", \"tax payments\": \"9311\", \"government services — not elsewhere classified\": \"9399\", \"postal services — government only\": \"9402\", \"u.s. federal government agencies or departments\": \"9405\", \"government-owned lotteries (non-u.s. region)\": \"9406\", \"automated referral service\": \"9700\", \"visa credential server\": \"9701\", \"emergency services (gcas) (visa use only)\": \"9702\", \"uk supermarkets, electronic hot file\": \"9751\", \"uk petrol stations, electronic hot file\": \"9752\", \"gambling-horse, dog racing, state lottery\": \"9754\", \"intra-company purchases\": \"9950\", \"veterinary services\": \"0742\", \"wine producers\": \"0743\", \"champagne producers\": \"0744\", \"agricultural cooperatives\": \"0763\", \"landscaping and horticultural services\": \"0780\", \"airlines (codes between 3000 and 3350)\": \"G300\", \"car rentals (codes between 3351 and 3500)\": \"G335\", \"hotels (codes between 3501 and 3999)\": \"G350\" }"
            else -> "{}"
        }
    }
}


class NipInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val builder = request.newBuilder()
        Singletons.gson.fromJson(Prefs.getString(PREF_USER, ""), User::class.java).netplus_id
        Prefs.getString(PREF_USER_TOKEN, "")
        builder.addHeader("X-CLIENT-ID", "85522f45-e459-4548-8b20-3a922196c515")
        builder.addHeader(
            "X-ACCESSCODE",
            "a14014e18e2cffc4d74e150ed68a472bd94189db82d374306d5b307dc7620f20"

        )
        return chain.proceed(builder.build())
    }
}