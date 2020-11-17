package com.woleapp.netpos.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import com.danbamitale.epmslib.entities.TransactionResponse
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.pixplicity.easyprefs.library.Prefs
import com.woleapp.netpos.R
import com.woleapp.netpos.adapter.ServiceAdapter
import com.woleapp.netpos.database.AppDatabase
import com.woleapp.netpos.databinding.FragmentDashboardBinding
import com.woleapp.netpos.databinding.LayoutPrintEndOfDayBinding
import com.woleapp.netpos.model.*
import com.woleapp.netpos.mqtt.MqttHelper
import com.woleapp.netpos.nibss.NetPosTerminalConfig
import com.woleapp.netpos.util.*
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
                3 -> getEndOfDayTransactions()
                else -> {
                    sendPayload()
                }
            }
            //addFragmentWithoutRemove(nextFrag)
        }
        return binding.root
    }

    private fun showEndOfDayBottomSheetDialog(transactions: List<TransactionResponse>) {
        val approvedList = transactions.filter { it.responseCode == "00" }
        val declinedList = transactions.filter { it.responseCode != "00" }
        val endOfDay =
            LayoutPrintEndOfDayBinding.inflate(LayoutInflater.from(requireContext()), null, false)
        endOfDay.apply {
            approvedCount.text = approvedList.size.toString()
            declinedCount.text = declinedList.size.toString()
            totalTransactions.text =
                getString(R.string.total_transaction_count, transactions.size.toString())
            print.setOnClickListener {
                when (chipGroup.checkedChipId) {
                    R.id.print_approved -> approvedList
                    R.id.print_declined -> declinedList
                    else -> transactions
                }.apply {
                    if (isEmpty())
                        Toast.makeText(
                            requireContext(),
                            "No transactions to print",
                            Toast.LENGTH_SHORT
                        ).show()
                }.printAll(true)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ printResp ->
                        Timber.e(printResp.toString())
                    }, { err ->
                        Timber.e(err)
                    }, {
                        Timber.e("On Complete")
                    })
            }
        }
        val bottomSheet = BottomSheetDialog(requireContext(), R.style.SheetDialog)
            .apply {
                dismissWithAnimation = true
                setCancelable(false)
                setContentView(endOfDay.root)
                show()
            }
        endOfDay.closeButton.setOnClickListener {
            bottomSheet.dismiss()
        }
    }

    private fun getEndOfDayTransactions() {
        Toast.makeText(requireContext(), "Please wait", Toast.LENGTH_LONG).show()
        AppDatabase.getDatabaseInstance(requireContext())
            .transactionResponseDao()
            .getEndOfDayTransaction(
                getBeginningOfDay(),
                getEndOfDayTimeStamp()
            )
            .observe(viewLifecycleOwner) {
                showEndOfDayBottomSheetDialog(it)
            }
    }

    private fun sendPayload() {
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
                add(Service(3, "Print End Of Day Transactions", R.drawable.ic_print))
            }
        adapter.submitList(listOfServices)
    }
}
