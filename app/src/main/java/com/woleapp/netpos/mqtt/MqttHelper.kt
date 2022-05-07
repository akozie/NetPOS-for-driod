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
import com.woleapp.netpos.database.AppDatabase
import com.woleapp.netpos.database.dao.MqttLocalDao
import com.woleapp.netpos.model.*
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
    private var mqttLocalDao: MqttLocalDao? = null

    fun <T> init(context: Context, event: MqttEvent<T>? = null, topic: MqttTopics? = null) {
        if (mqttLocalDao == null)
            mqttLocalDao = AppDatabase.getDatabaseInstance(context).mqttLocalDao()
        if (client != null && client!!.state.isConnected) {
            checkDatabaseForFailedEvents(context)
            return
        }
        val user: User? = Singletons.getCurrentlyLoggedInUser()
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
                    checkDatabaseForFailedEvents(context)
                    Timber.e("Client $clientId Connected Successfully to $SERVER_HOST")
                }.addDisconnectedListener {
                    Timber.e("Disconnected::cause - ${it.cause} ")
                }
            client = clientBuilder.useMqttVersion3().buildRx().apply {
//                connect().subscribe { t1, t2 ->
//                    t1?.let {
//                        event?.let {
//                            sendPayload(topic!!, it)
//                        }
//                        Timber.e("Connected:")
//                    }
//                    t2?.let {
//                        Timber.e("Connection Failed")
//                        Timber.e(it)
//                    }
//                }.disposeWith(disposables)
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

    fun <T> sendPayload(
        mqttTopic: MqttTopics,
        event: MqttEvent<T>? = null,
        failedEvent: MqttEventsLocal? = null
    ) {
        if (event == null && failedEvent == null) {
            Timber.e("Nothing to publish")
            return
        }

        event?.apply {
            Timber.e(event.toString())
            geo = Prefs.getString(PREF_LAST_LOCATION, "lat:6.5244 long:3.3792")
            Timber.e("Sending to topic: ${mqttTopic.topic}")
            //Timber.e(gson.toJson(event).toString())
        }
        client?.let { client ->
            Timber.e("client state isConnected ${client.state.isConnected}")
            Timber.e("client state isCorD ${client.state.isConnectedOrReconnect}")
            if (client.state.isConnected.not()) {
                Timber.e("not connected, save")
                val local: MqttEventsLocal? =
                    event?.toLocal(mqttTopic.topic, "client not connected") ?: failedEvent
                local?.let {
                    savePayloadToLocalDatabase(local)
                }
                return@let
            }

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
            failedEvent?.let { e ->
                flowable = Flowable.just(
                    Mqtt3Publish.builder()
                        .topic(e.topic)
                        .qos(MqttQos.AT_LEAST_ONCE)
                        .payload(e.data.toByteArray(Charset.forName("UTF-8")))
                        .build()
                )
            }
            client.publish(flowable!!).subscribe(
                {
                    if (it.error.isPresent) {
                        Timber.e("Error: ${it.error.get().localizedMessage}")
                        Timber.e("There was an error while publishing to topic: ${it.publish.topic}; save")
                        val failedPublish =
                            String(it.publish.payloadAsBytes, StandardCharsets.UTF_8)
                        savePayloadToLocalDatabase(
                            MqttEventsLocal(
                                it.publish.topic.toString(),
                                failedPublish,
                                "error during publishing"
                            )
                        )
                    }
                    Timber.e("Published")
                    Timber.e(String(it.publish.payloadAsBytes, StandardCharsets.UTF_8))
                },
                {
                    Timber.e("throwable; save")
                    val local: MqttEventsLocal? =
                        event?.toLocal(mqttTopic.topic, it.localizedMessage) ?: failedEvent
                    local?.let {
                        savePayloadToLocalDatabase(local)
                    }
                    Timber.e(it)
                },
                { Timber.e("Completed") }).disposeWith(disposables)
            return@let
        }
        if (client == null) {
            Timber.e("null, save")
            val local: MqttEventsLocal? =
                event?.toLocal(mqttTopic.topic, "client is null") ?: failedEvent
            local?.let {
                savePayloadToLocalDatabase(it)
            }
        }
    }

    private fun savePayloadToLocalDatabase(mqttEventsLocal: MqttEventsLocal) {
        Timber.e("saving into local")
//        if (mqttLocalDao == null)
//            mqttLocalDao = AppDatabase.getDatabaseInstance(context).mqttLocalDao()
        mqttLocalDao?.insertNewTransaction(mqttEventsLocal)?.subscribeOn(Schedulers.io())
            ?.observeOn(AndroidSchedulers.mainThread())
            ?.subscribe { t1, t2 ->
                t1?.let {
                    Timber.e("inserted into local: $it")
                }
                t2?.let {
                    Timber.e("did not insert into local: $it")
                }
            }?.disposeWith(compositeDisposable = disposables)
    }

    private fun checkDatabaseForFailedEvents(context: Context) {
        if (mqttLocalDao == null) mqttLocalDao =
            AppDatabase.getDatabaseInstance(context).mqttLocalDao()
        mqttLocalDao?.apply {
            getLocalEvents().flatMap {
                //NetPosWork.createNotification(context, "Failed Events", "Found ${it.size} failed events", null)
                deleteAllEvents().toSingleDefault(it)
            }.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { t1, t2 ->
                    t1?.let {
                        it.forEach { localEvent ->
                            Timber.e(getTopic(localEvent.topic).name)
                            sendPayload<Nothing>(
                                getTopic(localEvent.topic),
                                failedEvent = localEvent
                            )
                        }
                    }
                    t2?.let {
                        Timber.e(it)
                    }
                }
        }
    }

    private fun getTopic(string: String): MqttTopics {
        MqttTopics.values().forEach {
            if (it.topic == string)
                return it
        }
        return MqttTopics.TRANSACTIONS
    }
}