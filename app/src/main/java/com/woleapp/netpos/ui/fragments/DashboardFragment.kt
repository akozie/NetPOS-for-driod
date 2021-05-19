@file:Suppress("DEPRECATION")

package com.woleapp.netpos.ui.fragments

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.ProgressDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.netpluspay.nibssclient.models.*
import com.netpluspay.nibssclient.service.NibssApiWrapper
import com.netpluspay.nibssclient.util.formatCurrencyAmount
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
import com.woleapp.netpos.viewmodels.TransactionsViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.util.*
import kotlin.collections.ArrayList


class DashboardFragment : BaseFragment() {

    private lateinit var progressDialog: ProgressDialog
    private lateinit var binding: FragmentDashboardBinding
    private lateinit var adapter: ServiceAdapter
    private var compositeDisposable = CompositeDisposable()
    private val transactionViewModel by activityViewModels<TransactionsViewModel>()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDashboardBinding.inflate(inflater, container, false)
        adapter = ServiceAdapter {
            when (it.id) {
                0 -> addFragmentWithoutRemove(TransactionsFragment())
                1 -> getBalance()
                2 -> addFragmentWithoutRemove(NipNotificationFragment.newInstance())
                3 -> addFragmentWithoutRemove(BillsFragment())
                4 -> showCalendarDialog()
                else -> {
                    sendPayload()
                }
            }
            //addFragmentWithoutRemove(nextFrag)
        }
        progressDialog = ProgressDialog(requireContext())
        return binding.root
    }

    private fun getBalance() {
        showCardDialog(
            requireActivity(),
            viewLifecycleOwner,
            1000,
            0L
        ).observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                it.error?.let { error ->
                    Timber.e(error)
                    Toast.makeText(requireContext(), error.localizedMessage, Toast.LENGTH_SHORT)
                        .show()
                }
                it.cardData?.let { cardData ->
                    checkBalance(cardData, it.accountType!!)
                }
            }
        }
    }

    private fun checkBalance(
        cardData: CardData,
        accountType: IsoAccountType = IsoAccountType.DEFAULT_UNSPECIFIED
    ) {
        val checkBalanceParam = CheckBalanceParams(cardData, accountType)
        progressDialog.setMessage("Checking Balance...")
        progressDialog.show()
        val disposable = NibssApiWrapper.checkBalance(requireContext(), checkBalanceParam)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { response, error ->
                if (progressDialog.isShowing)
                    progressDialog.dismiss()
                error?.let {
                    it.printStackTrace()
                    Toast.makeText(
                        requireContext(),
                        "Error ${it.localizedMessage}",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                response?.let {
                    if (it.responseCode == "A3") {
                        Prefs.remove(PREF_CONFIG_DATA)
                        Prefs.remove(PREF_KEYHOLDER)
                        NetPosTerminalConfig.init(
                            requireContext().applicationContext,
                            configureSilently = true
                        )
                    }

                    val messageString = if (it.isApproved) {
                        "Account Balance:\n " + it.accountBalances.joinToString("\n") { accountBalance ->
                            "${accountBalance.accountType}, ${
                                accountBalance.amount.div(100).formatCurrencyAmount()
                            }"
                        }
                    } else {
                        "${it.responseMessage}(${it.responseCode})"
                    }

                    showMessage(if (it.isApproved) "Approved" else "Declined", messageString)
                }
            }
        disposable.disposeWith(compositeDisposable)
    }

    private fun showMessage(s: String, messageString: String) {
        AlertDialog.Builder(requireContext())
            .apply {
                setTitle(s)
                setMessage(messageString)
                setPositiveButton("Ok") { dialog, _ ->
                    dialog.dismiss()
                }
                create().show()
            }
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
                }.printEndOfDay(requireContext())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ printResp ->
                        Timber.e(printResp.toString())
                    }, { err ->
                        Toast.makeText(requireContext(), err.localizedMessage, Toast.LENGTH_LONG)
                            .show()
                        //Timber.e(err.localizedMessage)
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
        endOfDay.view.setOnClickListener {
            transactionViewModel.setEndOfDayList(transactions)
            bottomSheet.dismiss()
            addFragmentWithoutRemove(TransactionHistoryFragment.newInstance(HISTORY_ACTION_EOD))
        }
        endOfDay.closeButton.setOnClickListener {
            bottomSheet.dismiss()
        }
    }

    private fun getEndOfDayTransactions(timestamp: Long? = null) {
        Toast.makeText(requireContext(), "Please wait", Toast.LENGTH_LONG).show()
        val livedata = AppDatabase.getDatabaseInstance(requireContext())
            .transactionResponseDao()
            .getEndOfDayTransaction(
                getBeginningOfDay(timestamp),
                getEndOfDayTimeStamp(timestamp),
                NetPosTerminalConfig.getTerminalId()
            )
        livedata.observe(viewLifecycleOwner) {
            showEndOfDayBottomSheetDialog(it)
            livedata.removeObservers(viewLifecycleOwner)
        }
    }

    private fun showCalendarDialog() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, i, i2, i3 ->
                getEndOfDayTransactions(
                    Calendar.getInstance().apply { set(i, i2, i3) }.timeInMillis
                )
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun sendPayload() {
        //val user = Singletons.gson.fromJson(Prefs.getString(PREF_USER, ""), User::class.java)
        val event = MqttEvent<AuthenticationEventData>()
        val authEventData =
            AuthenticationEventData(event.business_name!!, event.storm_id!!, event.deviceSerial!!)
        event.apply {
            this.event = MqttEvents.AUTHENTICATION.event
            this.status = MqttStatus.SUCCESS.name
            this.code = MqttStatus.SUCCESS.code
            timestamp = System.currentTimeMillis()
            this.geo = "lat:51.507351-long:-0.127758"
            this.data = authEventData
        }
        MqttHelper.sendPayload(MqttTopics.AUTHENTICATION, event)
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
                add(Service(1, "Balance Inquiry", R.drawable.ic_write))
                add(Service(2, "Bank Transfer", R.drawable.ic_lending))
                add(Service(3, "Pay Bills", R.drawable.ic_bill))
                add(Service(4, "View End Of Day Transactions", R.drawable.ic_print))
            }
        adapter.submitList(listOfServices)
    }
}
