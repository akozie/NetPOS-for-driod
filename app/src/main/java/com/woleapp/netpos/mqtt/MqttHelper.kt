package com.woleapp.netpos.mqtt

import android.content.Context
import android.os.Build
import com.hivemq.client.internal.mqtt.MqttClientSslConfigImpl
import com.hivemq.client.internal.mqtt.MqttClientSslConfigImplBuilder
import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.mqtt3.Mqtt3RxClient
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish
import com.pixplicity.easyprefs.library.Prefs
import com.woleapp.netpos.BuildConfig
import com.woleapp.netpos.model.MqttEvent
import com.woleapp.netpos.model.MqttTopics
import com.woleapp.netpos.model.User
import com.woleapp.netpos.util.PREF_LAST_LOCATION
import com.woleapp.netpos.util.Singletons
import com.woleapp.netpos.util.Singletons.gson
import com.woleapp.netpos.util.disposeWith
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

object MqttHelper {
    private const val SERVER_HOST = BuildConfig.BASE_URL_NETPOS_MQTT
    private const val PORT = 8883
    private var client: Mqtt3RxClient? = null
    private var disposables = CompositeDisposable()
    private var eventList = arrayListOf<String>()
    fun init(context: Context, event: MqttEvent? = null, topic: MqttTopics? = null) {
        val user: User? = Singletons.getCurrentlyLoggedInUser()
        if (client != null && client!!.state.isConnected)
            return
        user?.let { u ->
            if (u.terminal_id.isNullOrEmpty()) {
                Timber.e("Terminal ID Null")
                return@let
            }
            val clientId = "${Build.MODEL}-${u.terminal_id!!}${(10000..999999999).random()}"
            val clientBuilder = MqttClient.builder()
                .identifier(clientId)
                .sslConfig(getMqttClientSSLConfigImpl(context))
                .serverHost(SERVER_HOST)
                .serverPort(PORT)
                .automaticReconnectWithDefaultConfig()
                .addConnectedListener {
                    Timber.e("Client $clientId Connected Successfully to $SERVER_HOST")
                }.addDisconnectedListener {
                    Timber.e("Disconnected::cause - ${it.cause} ")
                }
            client = clientBuilder.useMqttVersion3().buildRx().apply {
                connect().subscribe { t1, t2 ->
                    t1?.let {
                        event?.let {
                            sendPayload(topic!!, it)
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
        Timber.e("disconnecting")
        if (client == null) {
            Timber.e("Client is null or not connected")
            return
        }

        client?.disconnect()?.subscribeOn(Schedulers.io())!!
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ Timber.e("Disconnected") }, {
                Timber.e(it)
            }).disposeWith(disposables)
        client = null
        disposables.clear()
    }

    private fun getMqttClientSSLConfigImpl(context: Context): MqttClientSslConfigImpl {
        return MqttClientSslConfigImplBuilder.Default()
            .apply {
                //handshakeTimeout(60, TimeUnit.SECONDS)
                hostnameVerifier { _, _ -> true }
                trustManagerFactory(SSLUtil.getTrustManagerFactory(context))
                keyManagerFactory(SSLUtil.getKeyMangerFactory(context))
            }.build()
    }

    fun sendPayload(mqttTopic: MqttTopics, event: MqttEvent? = null, events: List<String>? = null) {
        if (event == null && events.isNullOrEmpty()) {
            Timber.e("Nothing to publish")
            return
        }
        event?.apply {
            geo = Prefs.getString(PREF_LAST_LOCATION, "lat:6.5244 long:3.3792")
            Timber.e("Sending to topic: ${mqttTopic.topic}")
            //Timber.e(gson.toJson(event).toString())
        }
        client?.let { client ->
            Timber.e("client state isConnected ${client.state.isConnected}")
            Timber.e("client state isCorD ${client.state.isConnectedOrReconnect}")
            var flowable: Flowable<Mqtt3Publish>? = null
            event?.let {
                flowable = Flowable.just(
                    Mqtt3Publish.builder()
                        .topic(mqttTopic.topic)
                        .qos(MqttQos.AT_LEAST_ONCE)
                        .payload(gson.toJson(event).toByteArray(Charset.forName("UTF-8")))
                        .build()
                )
            }
            events?.let { listOfEvents ->
                flowable = Flowable.fromIterable(listOfEvents.map {
                    Mqtt3Publish.builder()
                        .topic(mqttTopic.topic)
                        .qos(MqttQos.AT_LEAST_ONCE)
                        .payload(it.toByteArray(Charset.forName("UTF-8")))
                        .build()
                })
            }
            client.publish(flowable!!).subscribe(
                {
                    if (it.error.isPresent) {
                        Timber.e("Error: ${it.error.get().localizedMessage}")
                        Timber.e("There was an error while publishing")
                    }
                    Timber.e("Published")
                    Timber.e(String(it.publish.payloadAsBytes, StandardCharsets.UTF_8))
                },
                {
                    Timber.e(it)
                },
                { Timber.e("Completed") }).disposeWith(disposables)
        }
    }

    fun savePayloadToLocalDatabase() {

    }
}