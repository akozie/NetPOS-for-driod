package com.woleapp.netpos.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import com.danbamitale.epmslib.extensions.formatCurrencyAmount
import com.netpluspay.kozenlib.ReceiptBuilder
import com.pixplicity.easyprefs.library.Prefs
import com.woleapp.netpos.BuildConfig
import com.woleapp.netpos.R
import com.woleapp.netpos.adapter.ServiceAdapter
import com.woleapp.netpos.databinding.FragmentDashboardBinding
import com.woleapp.netpos.model.*
import com.woleapp.netpos.mqtt.MqttHelper
import com.woleapp.netpos.nibss.NetPosTerminalConfig
import com.woleapp.netpos.util.PREF_USER
import com.woleapp.netpos.util.Singletons
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber


class DashboardFragment : BaseFragment() {

    private lateinit var binding: FragmentDashboardBinding
    private lateinit var adapter: ServiceAdapter
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentDashboardBinding.inflate(inflater, container, false)
        adapter = ServiceAdapter {
            when (it.id) {
                0 -> addFragmentWithoutRemove(TransactionsFragment())
                1 -> addFragmentWithoutRemove(TransactionHistoryFragment.NewInstance())
                2 -> addFragmentWithoutRemove(NipNotificationFragment.newInstance())
                else -> {
                    sendPayload()
                }
            }
            //addFragmentWithoutRemove(nextFrag)
        }
        return binding.root
    }

    fun sendPayload() {
        val user = Singletons.gson.fromJson(Prefs.getString(PREF_USER, ""), User::class.java)
        val event = MqttEvent(
            user.netplus_id!!,
            user.business_name!!,
            NetPosTerminalConfig.getTerminalId(),
            "JKEWUBUBIBSBBWUBUWBYUB89243"
        )
        val authEventData =
            AuthenticationEventData(event.business_name, event.storm_id, event.deviceSerial)
        event.apply {
            this.event = MqttEvents.AUTHENTICATION.event
            this.status = MqttStatus.SUCCESS.name
            this.code = MqttStatus.SUCCESS.code
            timestamp = System.currentTimeMillis()
            this.geo = "lat:51.507351-long:-0.127758"
            this.data = authEventData
        }
        MqttHelper.sendPayload(event)
        //Timber.e(Singletons.gson.toJson(event))
    }

    companion object {
        fun printSampleReceipt() {
            val c = ReceiptBuilder()
                .apply {
                    appendAID("AID")
                    appendAddress("NETPOS")
                    appendAmount(
                        100.formatCurrencyAmount("\u20A6")
                    )
                    appendAppName("NetPOS")
                    appendAppVersion(BuildConfig.VERSION_NAME)
                    appendAuthorizationCode("Auth code")
                    appendCardHolderName("Card Holder")
                    appendCardNumber("Masked Pan")
                    appendCardScheme("card scheme")
                    appendDateTime("")
                    appendRRN("RRN")
                    appendStan("STAN")
                    appendTerminalId(NetPosTerminalConfig.getTerminalId())
                    appendTransactionType("PURCHASEE")
                    appendTransactionStatus("APPROVED")
                    appendResponseCode("00")
                }.print().subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { t1, t2 ->
                    t1?.let {

                    }
                    t2?.let {
                        Timber.e("Error: ${it.localizedMessage}")
                    }
                }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setServices()
        binding.rvDashboard.layoutManager = GridLayoutManager(context, 2)
        binding.rvDashboard.adapter = adapter
    }

    private fun setServices() {
        val listOfServices = ArrayList<Service>()
            .apply {
                add(Service(0, "Transaction", R.drawable.ic_trans))
                add(Service(1, "Inquiry", R.drawable.ic_write))
                add(Service(2, "Bank Transfer", R.drawable.ic_lending))
                add(Service(3, "Print Sample Receipt", R.drawable.ic_print))
            }
        adapter.submitList(listOfServices)
    }
}
