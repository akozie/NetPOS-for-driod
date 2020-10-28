package com.woleapp.netpos.mqtt

import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.mqtt3.Mqtt3RxClient
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish
import com.pixplicity.easyprefs.library.Prefs
import com.woleapp.netpos.model.MqttEvent
import com.woleapp.netpos.model.User
import com.woleapp.netpos.util.PREF_LAST_LOCATION
import com.woleapp.netpos.util.Singletons
import com.woleapp.netpos.util.Singletons.gson
import com.woleapp.netpos.util.disposeWith

import io.reactivex.Flowable
import io.reactivex.disposables.CompositeDisposable
import timber.log.Timber
import java.nio.charset.Charset


object MqttHelper {
    private var user: User? = Singletons.getCurrentlyLoggedInUser()
    private var client: Mqtt3RxClient? = null
    private var disposables = CompositeDisposable()
    fun init(event: MqttEvent? = null) {
        user?.let { user ->
            val clientBuilder = MqttClient.builder()
                .identifier(user.netplus_id!!)
                .serverHost("139.162.249.69")
                .serverPort(1883)
                .automaticReconnectWithDefaultConfig()
                .addConnectedListener {
                    Timber.e("Connected Successfully")
                }.addDisconnectedListener {
                    Timber.e("Disconnected")
                }
            client = clientBuilder.useMqttVersion3().buildRx().apply {
                connect().subscribe { t1, t2 ->
                    t1?.let {
                        event?.let {
                            sendPayload(it)
                        }
                        Timber.e("Connected:")
                    }
                    t2?.let {
                        Timber.e("Connection Failed")
                        Timber.e(it)
                    }
                }.disposeWith(disposables)
            }
        }
    }

    fun disconnect() {
        client?.disconnect()
    }

    fun sendPayload(event: MqttEvent) {
        event.apply {
            geo = Prefs.getString(PREF_LAST_LOCATION, "lat:6.5244 long:3.3792")
        }
        client?.let { client ->
            val publish = Mqtt3Publish.builder()
                .topic("netpos_event")
                .qos(MqttQos.AT_LEAST_ONCE)
                .payload(gson.toJson(event).toByteArray(Charset.forName("UTF-8")))
                .build()
            val c = client.publish(Flowable.just(publish)).subscribe({
                Timber.e("Published")
            }, { Timber.e(it) }, {}).disposeWith(disposables)
        }
    }
}