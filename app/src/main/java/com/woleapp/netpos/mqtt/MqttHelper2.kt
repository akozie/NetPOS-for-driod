package com.woleapp.netpos.mqtt


/*object MqttHelper2 {
    fun init(context: Context, event: MqttEvent? = null) {
        val options = MqttConnectOptions()
        SSLUtil.getSSLFactory(context)?.let {
            Timber.e("able to create ssl socket factory")
            options.socketFactory = it
        }

        val mqttAndroidClient = MqttAndroidClient(
            context,
            "ssl://storm-mqtt.test.netpluspay.com:8883",
            MqttHelper.user!!.netplus_id
        )
        mqttAndroidClient.setCallback(object : MqttCallback {
            override fun connectionLost(cause: Throwable?) {
                Timber.e("Connection lost")
            }

            override fun messageArrived(topic: String?, message: MqttMessage?) {
                Timber.e("Message arrived")
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {
                Timber.e("Message delivery complete")
            }

        })
        try {
            mqttAndroidClient.connect(options, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Timber.e("Connected successfully")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Timber.e(exception)
                    Timber.e("Connection failed because: ${exception?.localizedMessage}")
                }

            })
        } catch (e: MqttException) {
            Timber.e(e.localizedMessage)
        }
    }
}*/