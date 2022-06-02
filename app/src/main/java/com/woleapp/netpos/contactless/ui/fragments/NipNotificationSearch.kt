package com.woleapp.netpos.contactless.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.woleapp.netpos.contactless.databinding.FragmentNipNotificationSearchBinding
import com.woleapp.netpos.contactless.model.*
import com.woleapp.netpos.contactless.mqtt.MqttHelper
import com.woleapp.netpos.contactless.network.StormApiClient

import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber

class NipNotificationSearch : BaseFragment() {
    private lateinit var binding: FragmentNipNotificationSearchBinding
    private lateinit var event: MqttEvent<NipEvent>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentNipNotificationSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.nip.root.visibility = View.GONE
        //val userToken = Prefs.getString(PREF_USER_TOKEN, "")
        //val user = Singletons.getCurrentlyLoggedInUser()
        event = MqttEvent<NipEvent>()
            .apply {
                this.geo = "lat:51.507351-long:-0.127758"
            }
        binding.searchButton.setOnClickListener {
            if (binding.sessionCode.text.toString().isEmpty())
                return@setOnClickListener
            val subscribe = StormApiClient.getNipInstance().getNotificationByReference(
                binding.sessionCode.text.toString()
            ).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { binding.progressCircular.visibility = View.VISIBLE }
                .doFinally {
                    binding.progressCircular.visibility = View.GONE
                }
                .subscribe { notification, throwable ->
                    notification?.let {
                        event.apply {
                            this.event = MqttEvents.NIP_SEARCH.event
                            this.code = "00"
                            this.timestamp = System.currentTimeMillis()
                            this.status = MqttStatus.SUCCESS.name
                        }
                        MqttHelper.sendPayload(MqttTopics.NIP_SEARCH, event)
                        if (it.isNotEmpty()) {
                            binding.nip.nip = it.first()
                            binding.nip.root.visibility = View.VISIBLE
                            binding.nip.print.setOnClickListener { _ ->
                                Timber.e("print nip")
//                                it.first().print(
//                                    requireContext(),
//                                    object : POIPrinterManage.IPrinterListener {
//                                        override fun onError(p0: Int, p1: String?) {
//                                            Timber.e("printer error")
//                                            Toast.makeText(
//                                                requireContext(),
//                                                "Printer Error",
//                                                Toast.LENGTH_SHORT
//                                            ).show()
//                                        }
//
//                                        override fun onFinish() {
//                                            Timber.e("on finish")
//                                        }
//
//                                        override fun onStart() {
//                                            Timber.e("on start")
//                                        }
//
//                                    })
                            }
                        } else
                            Toast.makeText(
                                requireContext(),
                                "Bank transfer with ref: ${binding.sessionCode.text.toString()} not found",
                                Toast.LENGTH_SHORT
                            ).show()
                    }
                    val let = throwable?.let {
                        event.apply {
                            this.event = MqttEvents.NIP_SEARCH.event
                            this.code = "99"
                            this.timestamp = System.currentTimeMillis()
                            this.status = MqttStatus.ERROR.name
                        }
                        MqttHelper.sendPayload(MqttTopics.NIP_SEARCH, event)
                        Timber.e("Nip Error: ${it.localizedMessage}")
                        Toast.makeText(
                            requireContext(),
                            "Error: ${it.localizedMessage}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }
    }
}