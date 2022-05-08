@file:Suppress("DEPRECATION")

package com.woleapp.netpos.util

import android.app.Activity
import android.app.ProgressDialog
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.danbamitale.epmslib.entities.CardData
import com.danbamitale.epmslib.utils.IsoAccountType
import com.woleapp.netpos.databinding.DialogSelectCardschemeBinding
import com.woleapp.netpos.nibss.NetPosTerminalConfig
import com.woleapp.netpos.taponphone.visa.NfcPaymentType
import timber.log.Timber


data class ICCCardHelper(
    var customerName: String? = null,
    var cardScheme: String? = null,
    var accountType: IsoAccountType? = null,
    var cardData: CardData? = null,
    var error: Throwable? = null
)


fun showCardDialog(
    context: Activity,
    lifecycleOwner: LifecycleOwner,
): LiveData<Event<NfcPaymentType?>> {
    var configurationFinished = false
    val liveData: MutableLiveData<Event<NfcPaymentType?>> = MutableLiveData()
    if (NetPosTerminalConfig.liveData.hasActiveObservers())
        NetPosTerminalConfig.liveData.removeObservers(lifecycleOwner)
    val progressDialog = ProgressDialog(context)
    progressDialog.setMessage("connecting, please wait...")
    var observer: Observer<Event<Int>>? = null
    observer = Observer<Event<Int>> {
        it.getContentIfNotHandled()?.let { int ->
            Timber.e("picked up: $int")
            when (int) {
                0 -> progressDialog.show()
                1 -> {
                    configurationFinished = true
                    if (progressDialog.isShowing)
                        progressDialog.dismiss()
                    showSelectCardDialog(context, liveData)
                    //getFakeCardLiveData(liveData)
                    NetPosTerminalConfig.liveData.removeObserver(observer!!)
                }
                -1 -> {
                    configurationFinished = true
                    if (progressDialog.isShowing)
                        progressDialog.dismiss()
                    if (NetPosTerminalConfig.getTerminalId().isEmpty()) {
                        Toast.makeText(context, "No TID found on account", Toast.LENGTH_SHORT)
                            .show()
                    } else
                        Toast.makeText(context, "Connection Failed", Toast.LENGTH_SHORT).show()
                }
                else -> {
                }
            }
        }
    }
    NetPosTerminalConfig.liveData.observe(lifecycleOwner, observer)
    when {
        NetPosTerminalConfig.configurationStatus != 1 && NetPosTerminalConfig.isConfigurationInProcess.not() -> NetPosTerminalConfig.init(
            context.applicationContext
        )
        NetPosTerminalConfig.isConfigurationInProcess -> if (configurationFinished.not()) progressDialog.show()
        NetPosTerminalConfig.configurationStatus == 1 ->
            showSelectCardDialog(context, liveData)
    }
    return liveData
}

fun showSelectCardDialog(context: Activity, liveData: MutableLiveData<Event<NfcPaymentType?>>) {
    val binding = DialogSelectCardschemeBinding.inflate(LayoutInflater.from(context))
    val selectCardDialog = AlertDialog.Builder(context).apply {
        setView(binding.root)
    }.create()
    var nfcPaymentType: NfcPaymentType? = null
    binding.mastercardIcon.setOnClickListener {
        binding.mastercardSelector.isVisible = true
        nfcPaymentType = NfcPaymentType.MASTERCARD
        binding.visaSelector.isVisible = false
        binding.proceedButton.isEnabled = true
    }
    binding.visaIcon.setOnClickListener {
        nfcPaymentType = NfcPaymentType.VISA
        binding.mastercardSelector.isVisible = false
        binding.visaSelector.isVisible = true
        binding.proceedButton.isEnabled = true
    }
    binding.proceedButton.setOnClickListener {
        selectCardDialog.cancel()
        liveData.postValue(Event(nfcPaymentType))
    }
    //selectCardDialog.show()
    liveData.postValue(Event(NfcPaymentType.VISA))
}
