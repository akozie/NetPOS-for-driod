package com.woleapp.netpos.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.netpluspay.kozenlib.KozenLib
import com.pixplicity.easyprefs.library.Prefs
import com.woleapp.netpos.databinding.FragmentNipNotificationSearchBinding
import com.woleapp.netpos.model.MqttEvent
import com.woleapp.netpos.model.MqttEvents
import com.woleapp.netpos.model.MqttStatus
import com.woleapp.netpos.mqtt.MqttHelper
import com.woleapp.netpos.network.StormApiClient
import com.woleapp.netpos.nibss.NetPosTerminalConfig
import com.woleapp.netpos.util.PREF_USER_TOKEN
import com.woleapp.netpos.util.Singletons
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber

class NipNotificationSearch : BaseFragment() {
    private lateinit var binding: FragmentNipNotificationSearchBinding
    private lateinit var event: MqttEvent

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentNipNotificationSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.nip.root.visibility = View.GONE
        val userToken = Prefs.getString(PREF_USER_TOKEN, "")
        val user = Singletons.getCurrentlyLoggedInUser()
        event = MqttEvent(
            user!!.netplus_id!!,
            user.business_name!!,
            NetPosTerminalConfig.getTerminalId(),
            KozenLib.getDeviceSerial()
        )
            .apply {
                this.geo = "lat:51.507351-long:-0.127758"
            }
        binding.searchButton.setOnClickListener {
            if (binding.sessionCode.text.toString().isEmpty())
                return@setOnClickListener
            StormApiClient.getNipInstance().getNotificationByReference(
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
                        MqttHelper.sendPayload(event)
                        binding.nip.nip = it
                        binding.nip.root.visibility = View.VISIBLE
                    }
                    throwable?.let {
                        event.apply {
                            this.event = MqttEvents.NIP_SEARCH.event
                            this.code = "99"
                            this.timestamp = System.currentTimeMillis()
                            this.status = MqttStatus.ERROR.name
                        }
                        MqttHelper.sendPayload(event)
                        Timber.e("Nip Error: ${it.localizedMessage}")
                        Toast.makeText(requireContext(), "Error: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }
}