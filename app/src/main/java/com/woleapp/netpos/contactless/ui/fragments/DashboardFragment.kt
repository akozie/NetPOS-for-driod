@file:Suppress("DEPRECATION")

package com.woleapp.netpos.contactless.ui.fragments

import android.app.DatePickerDialog
import android.app.ProgressDialog
import android.content.DialogInterface
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.danbamitale.epmslib.entities.*
import com.danbamitale.epmslib.entities.TransactionResponse
import com.danbamitale.epmslib.extensions.formatCurrencyAmount
import com.danbamitale.epmslib.processors.TransactionProcessor
import com.danbamitale.epmslib.utils.IsoAccountType
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import com.google.gson.JsonObject
import com.pixplicity.easyprefs.library.Prefs
import com.woleapp.netpos.contactless.R
import com.woleapp.netpos.contactless.adapter.ServiceAdapter
import com.woleapp.netpos.contactless.database.AppDatabase
import com.woleapp.netpos.contactless.databinding.DialogPrintTypeBinding
import com.woleapp.netpos.contactless.databinding.FragmentDashboardBinding
import com.woleapp.netpos.contactless.databinding.LayoutPrintEndOfDayBinding
import com.woleapp.netpos.contactless.model.*
import com.woleapp.netpos.contactless.mqtt.MqttHelper
import com.woleapp.netpos.contactless.nibss.NetPosTerminalConfig
import com.woleapp.netpos.contactless.util.*
import com.woleapp.netpos.contactless.viewmodels.NfcCardReaderViewModel
import com.woleapp.netpos.contactless.viewmodels.SalesViewModel
import com.woleapp.netpos.contactless.viewmodels.TransactionsViewModel
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.util.*
import java.util.concurrent.TimeUnit

class DashboardFragment : BaseFragment() {

    private lateinit var progressDialog: ProgressDialog
    private lateinit var binding: FragmentDashboardBinding
    private lateinit var adapter: ServiceAdapter
    private var compositeDisposable = CompositeDisposable()
    private val transactionViewModel by activityViewModels<TransactionsViewModel>()
    private val nfcCardReaderViewModel by activityViewModels<NfcCardReaderViewModel>()
    private var observer: ((Event<ICCCardHelper>) -> Unit)? = null
    private val viewModel by activityViewModels<SalesViewModel>()
    private var isVend: Boolean = false
    private lateinit var transactionType: TransactionType
    private lateinit var dialogPrintTypeBinding: DialogPrintTypeBinding
    private lateinit var printTypeDialog: AlertDialog
    private lateinit var printerErrorDialog: AlertDialog


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
     //   Log.d("MID", "------->${Singletons.getCurrentlyLoggedInUser()?.netplus_id}")
        binding = FragmentDashboardBinding.inflate(inflater, container, false)
        transactionType = TransactionType.PURCHASE
        isVend = arguments?.getBoolean("IS_VEND", false) ?: false
        viewModel.isVend(isVend)

        dialogPrintTypeBinding = DialogPrintTypeBinding.inflate(layoutInflater, null, false).apply {
            executePendingBindings()
        }
        printerErrorDialog = AlertDialog.Builder(requireContext())
            .apply {
                setTitle("Printer Error")
                setIcon(R.drawable.ic_warning)
                setPositiveButton("Send Receipt") { d, _ ->
                    d.cancel()
                    viewModel.showReceiptDialog()
                }
                setNegativeButton("Dismiss") { d, _ ->
                    d.cancel()
                    viewModel.finish()
                }
            }.create()
        binding.apply {
            viewmodel = viewModel
            lifecycleOwner = viewLifecycleOwner
            executePendingBindings()
            type = transactionType.name
        }
        viewModel.message.observe(viewLifecycleOwner) {
            it.getContentIfNotHandled()?.let { s ->
                showSnackBar(s)
            }
        }
        viewModel.getCardData.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { shouldGetCardData ->
                if (shouldGetCardData) {
                    showCardDialog(
                        requireActivity(),
                        viewLifecycleOwner
                    ).observe(viewLifecycleOwner) { event ->
                        event.getContentIfNotHandled()?.let {
                            Timber.e(it.toString())
                            nfcCardReaderViewModel.initiateNfcPayment(
                                viewModel.amountLong,
                                viewModel.cashbackLong,
                                it
                            )
                        }
                    }
                }
            }
        }
        viewModel.showReceiptType.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                printTypeDialog.show()
            }
        }

        viewModel.toastMessage.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
            }
        }
        viewModel.finish.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                if (it) {
                    requireActivity().onBackPressed()
                }
            }
        }
        viewModel.showPrinterError.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                if (printTypeDialog.isShowing) {
                    printTypeDialog.cancel()
                }
                if (printerErrorDialog.isShowing) {
                    printerErrorDialog.cancel()
                }
                printerErrorDialog.apply {
                    setMessage(it)
                }.show()
            }
        }

        viewModel.showPrintDialog.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                nfcCardReaderViewModel.setLastPosTransactionResponse(viewModel.lastPosTransaction.value!!)
                nfcCardReaderViewModel.prepareSMS(it)
            }
        }

        viewModel.shouldRefreshNibssKeys.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                if (it) {
                    NetPosTerminalConfig.init(
                        requireContext().applicationContext,
                        configureSilently = true
                    )
                }
            }
        }
        binding.process.setOnClickListener {
            viewModel.validateField()
        }

        nfcCardReaderViewModel.iccCardHelperLiveData.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                it.error?.let { error ->
                    Timber.e(error)
                    Toast.makeText(
                        requireContext(),
                        error.message,
                        Toast.LENGTH_LONG
                    )
                        .show()
                }
                it.cardData?.let { _ ->
                    viewModel.setCardScheme(it.cardScheme!!)
                    viewModel.setCustomerName(it.customerName ?: "Customer")
                    viewModel.setAccountType(it.accountType!!)
                    viewModel.cardData = it.cardData
                    viewModel.makePayment(requireContext(), transactionType)
                }
            }
        }

        adapter = ServiceAdapter {
            when (it.id) {
                0 -> showFragment(TransactionsFragment())
                //1 -> getBalance()
                2 -> showFragment(NipNotificationFragment.newInstance())
                3 -> showFragment(BillsFragment())
               // 4 -> showCalendarDialog()
                5 -> {
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.container_main, SettingsFragment())
                        .addToBackStack(null)
                        .commit()
                }
                else -> {
                    sendPayload()
                }
            }
            // showFragment(nextFrag)
        }

        progressDialog = ProgressDialog(requireContext())

//        observer = { event ->
//            event.getContentIfNotHandled()?.let {
//                it.error?.let { error ->
//                    Timber.e(error)
//                    Toast.makeText(requireContext(), error.localizedMessage, Toast.LENGTH_SHORT)
//                        .show()
//                }
//                it.cardData?.let { cardData ->
//                    checkBalance(cardData, it.accountType!!)
//                }
//            }
//        }
        return binding.root
    }

    override fun onDetach() {
        super.onDetach()
        Timber.e("dashboard detached")
    }

//    override fun onStop() {
//        super.onStop()
//        Timber.e("on stop")
//        nfcCardReaderViewModel.iccCardHelperLiveData.removeObservers(viewLifecycleOwner)
//    }
//
//    override fun onStart() {
//        super.onStart()
//        Timber.e("on start")
//        observer?.let {
//            Timber.e("add obs")
//            nfcCardReaderViewModel.iccCardHelperLiveData.observe(viewLifecycleOwner, it)
//        }
//    }
//
//    private fun getBalance() {
//        showCardDialog(
//            requireActivity(),
//            viewLifecycleOwner
//        ).observe(viewLifecycleOwner) { event ->
//            event.getContentIfNotHandled()?.let {
//                nfcCardReaderViewModel.initiateNfcPayment(10, 0, it)
//            }
//        }
//    }
//
//    private fun checkBalance(
//        cardData: CardData,
//        accountType: IsoAccountType = IsoAccountType.DEFAULT_UNSPECIFIED
//    ) {
//        if (NetPosTerminalConfig.getKeyHolder() == null) {
//            Toast.makeText(requireContext(), "Terminal not configured", Toast.LENGTH_LONG).show()
//            return
//        }
//
//        val hostConfig = HostConfig(
//            NetPosTerminalConfig.getTerminalId(),
//            NetPosTerminalConfig.connectionData,
//            NetPosTerminalConfig.getKeyHolder()!!,
//            NetPosTerminalConfig.getConfigData()!!
//        )
//        val requestData =
//            TransactionRequestData(TransactionType.BALANCE, 0L, accountType = accountType)
//        progressDialog.setMessage("Checking Balance...")
//        progressDialog.show()
//        val processor = TransactionProcessor(hostConfig)
//        // processor.
//        val disposable = processor.processTransaction(requireContext(), requestData, cardData)
//            .subscribeOn(Schedulers.io())
//            .observeOn(AndroidSchedulers.mainThread())
//            .subscribe { response, error ->
//                if (progressDialog.isShowing) {
//                    progressDialog.dismiss()
//                }
//                error?.let {
//                    it.printStackTrace()
//                    Toast.makeText(
//                        requireContext(),
//                        "Error ${it.localizedMessage}",
//                        Toast.LENGTH_SHORT
//                    ).show()
//                }
//
//                response?.let {
//                    if (it.responseCode == "A3") {
//                        Prefs.remove(PREF_CONFIG_DATA)
//                        Prefs.remove(PREF_KEYHOLDER)
//                        NetPosTerminalConfig.init(
//                            requireContext().applicationContext,
//                            configureSilently = true
//                        )
//                    }
//
//                    val me = it.buildSMSText("Account Balance Check")
//
//                    val messageString = if (it.isApproved) {
//                        "Account Balance:\n " + it.accountBalances.joinToString("\n") { accountBalance ->
//                            "${accountBalance.accountType}, ${
//                            accountBalance.amount.div(100).formatCurrencyAmount()
//                            }"
//                        }
//                    } else {
//                        "${it.responseMessage}(${it.responseCode})"
//                    }
//
//                    showMessage(
//                        if (it.isApproved) "Approved" else "Declined",
//                        messageString,
//                        me.toString()
//                    )
//                }
//            }
//
//        // compositeDisposable.add(disposable)
//    }
//
//    private fun showMessage(s: String, vararg messageString: String) {
//        AlertDialog.Builder(requireContext())
//            .apply {
//                setTitle(s)
//                setMessage(messageString.first())
//                setPositiveButton("Ok") { dialog, _ ->
//                    dialog.dismiss()
//                    nfcCardReaderViewModel.prepareSMS(messageString.reversed().joinToString("\n"))
//                }
//                create().show()
//            }
//    }
//
//    private fun showEndOfDayBottomSheetDialog(transactions: List<TransactionResponse>) {
//        val approvedList = transactions.filter { it.responseCode == "00" }
//        val declinedList = transactions.filter { it.responseCode != "00" }
//        val endOfDay =
//            LayoutPrintEndOfDayBinding.inflate(LayoutInflater.from(requireContext()), null, false)
//        endOfDay.apply {
//            approvedCount.text = approvedList.size.toString()
//            declinedCount.text = declinedList.size.toString()
//            totalTransactions.text =
//                getString(R.string.total_transaction_count, transactions.size.toString())
//            print.setOnClickListener {
//                when (chipGroup.checkedChipId) {
//                    R.id.print_approved -> approvedList
//                    R.id.print_declined -> declinedList
//                    else -> transactions
//                }.apply {
//                    if (isEmpty()) {
//                        Toast.makeText(
//                            requireContext(),
//                            "No transactions to print",
//                            Toast.LENGTH_SHORT
//                        ).show()
//                    }
//                }
//            }
//        }
//        val bottomSheet = BottomSheetDialog(requireContext(), R.style.SheetDialog)
//            .apply {
//                dismissWithAnimation = true
//                setCancelable(false)
//                setContentView(endOfDay.root)
//                show()
//            }
//        endOfDay.view.setOnClickListener {
//            transactionViewModel.setEndOfDayList(transactions)
//            bottomSheet.dismiss()
//            showFragment(TransactionHistoryFragment.newInstance(HISTORY_ACTION_EOD))
//        }
//        endOfDay.closeButton.setOnClickListener {
//            bottomSheet.dismiss()
//        }
//    }
//
//    private fun getEndOfDayTransactions(timestamp: Long? = null) {
//        Toast.makeText(requireContext(), "Please wait", Toast.LENGTH_LONG).show()
//        val livedata = AppDatabase.getDatabaseInstance(requireContext())
//            .transactionResponseDao()
//            .getEndOfDayTransaction(
//                getBeginningOfDay(timestamp),
//                getEndOfDayTimeStamp(timestamp),
//                NetPosTerminalConfig.getTerminalId()
//            )
//        livedata.observe(viewLifecycleOwner) {
//            showEndOfDayBottomSheetDialog(it)
//            livedata.removeObservers(viewLifecycleOwner)
//        }
//    }
//
//    private fun showCalendarDialog() {
//        val calendar = Calendar.getInstance()
//        DatePickerDialog(
//            requireContext(),
//            { _, i, i2, i3 ->
//                getEndOfDayTransactions(
//                    Calendar.getInstance().apply { set(i, i2, i3) }.timeInMillis
//                )
//            },
//            calendar.get(Calendar.YEAR),
//            calendar.get(Calendar.MONTH),
//            calendar.get(Calendar.DAY_OF_MONTH)
//        ).show()
//    }

    private fun sendPayload() {
        // val user = Singletons.gson.fromJson(Prefs.getString(PREF_USER, ""), User::class.java)
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
        // Timber.e(Singletons.gson.toJson(event))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setServices()
        vend()
        observer = { event ->
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
       // binding.rvDashboard.layoutManager = GridLayoutManager(context, 2)
       // binding.rvDashboard.adapter = adapter
    }

    private fun setServices() {
        val listOfServices = ArrayList<Service>()
            .apply {
                add(Service(0, "Transaction", R.drawable.ic_trans))
                add(Service(1, "Balance Inquiry", R.drawable.ic_write))
                // add(Service(2, "Bank Transfer", R.drawable.ic_lending))
                // add(Service(3, "Pay Bills", R.drawable.ic_bill))
                add(Service(4, "View End Of Day Transactions", R.drawable.ic_print))
                add(Service(5, "Settings", R.drawable.ic_baseline_settings))
            }
        adapter.submitList(listOfServices)
    }

    private fun vend() {
        if (isVend) {
            var count = 0
            val progressBar = ProgressDialog(context).apply {
                this.setCancelable(false)
                this.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel") { dialog, _ ->
                    dialog.cancel()
                    compositeDisposable.clear()
                    requireActivity().onBackPressed()
                }
                this.setMessage("Waiting for amount.")
                show()
            }
            val socket = Socket()
            var printWriter: PrintWriter? = null
            var reader: BufferedReader? = null
            Observable.fromCallable {
                socket.soTimeout = 120_000
                socket.connect(InetSocketAddress(VEND_IP, VEND_PORT))
                printWriter = PrintWriter(socket.getOutputStream(), true)
                reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                val firstData = reader?.readLine()
                Timber.e(firstData)
            }.flatMap {
                Observable.interval(0, 5, TimeUnit.SECONDS)
            }.flatMap {
                val out = JsonObject().apply {
                    addProperty("serial_number", Build.ID)
                    addProperty("status", "")
                }.toString()
                printWriter?.println(out)
                try {
                    val s = reader?.readLine()
                    Timber.e(s)
                    val vend = Singletons.gson.fromJson(s, Vend::class.java)
                    // socket.close()
                    Observable.just(vend)
                } catch (e: Exception) {
                    Observable.just(Vend(0.0))
                }
            }.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    Timber.e("vend: $it")
                    count++
                    if (it.amount > 0.0) {
                        progressBar.dismiss()
                        Toast.makeText(context, "received", Toast.LENGTH_SHORT).show()
                        Toast.makeText(context, it.amount.toString(), Toast.LENGTH_LONG).show()
                        binding.priceTextbox.setText(it.amount.toLong().toString())
                        compositeDisposable.clear()
                    } else if (count > 12) {
                        progressBar.dismiss()
                        Toast.makeText(
                            context,
                            "Did not receive amount after waiting",
                            Toast.LENGTH_LONG
                        ).show()
                        compositeDisposable.clear()
                        requireActivity().onBackPressed()
                    }
                }, {
                    progressBar.dismiss()
                    Toast.makeText(
                        requireContext(),
                        "Error ${it.localizedMessage}",
                        Toast.LENGTH_SHORT
                    ).show()
                    Timber.e("Error: ${it.localizedMessage}")
                    requireActivity().onBackPressed()
                }).disposeWith(compositeDisposable)
        }
    }

    private fun showSnackBar(message: String) {
        if (message == "Transaction not approved") {
            AlertDialog.Builder(requireContext())
                .apply {
                    setTitle("Response")
                    setMessage(message)
                    show()
                }
        }

        Snackbar.make(
            requireActivity().findViewById(
                R.id.container_main
            ),
            message,
            Snackbar.LENGTH_LONG
        ).show()
    }



    override fun onResume() {
        super.onResume()
        val pref = Prefs.getString(PREF_PRINTER_SETTINGS, "nothing comes")
        Timber.d("THE_PREFS_GOTTEN======>%s", pref)
    }



    private fun checkBalance(
        cardData: CardData,
        accountType: IsoAccountType = IsoAccountType.DEFAULT_UNSPECIFIED
    ) {
        if (NetPosTerminalConfig.getKeyHolder() == null) {
            Toast.makeText(requireContext(), "Terminal not configured", Toast.LENGTH_LONG).show()
            return
        }

        val hostConfig = HostConfig(
            NetPosTerminalConfig.getTerminalId(),
            NetPosTerminalConfig.connectionData,
            NetPosTerminalConfig.getKeyHolder()!!,
            NetPosTerminalConfig.getConfigData()!!
        )
        val requestData =
            TransactionRequestData(TransactionType.BALANCE, 0L, accountType = accountType)
        progressDialog!!.setMessage("Checking Balance...")
        progressDialog.show()
        val processor = TransactionProcessor(hostConfig)
        // processor.
        val disposable = processor.processTransaction(requireContext(), requestData, cardData)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { response, error ->
                if (progressDialog!!.isShowing) {
                    progressDialog!!.dismiss()
                }
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

                    val me = it.buildSMSText("Account Balance Check")

                    val messageString = if (it.isApproved) {
                        "Account Balance:\n " + it.accountBalances.joinToString("\n") { accountBalance ->
                            "${accountBalance.accountType}, ${
                                accountBalance.amount.div(100).formatCurrencyAmount()
                            }"
                        }
                    } else {
                        "${it.responseMessage}(${it.responseCode})"
                    }

                    showMessage(
                        if (it.isApproved) "Approved" else "Declined",
                        messageString,
                        me.toString()
                    )
                }
            }

        // compositeDisposable.add(disposable)
    }

    private fun showMessage(s: String, vararg messageString: String) {
        AlertDialog.Builder(requireContext())
            .apply {
                setTitle(s)
                setMessage(messageString.first())
                setPositiveButton("Ok") { dialog, _ ->
                    dialog.dismiss()
                    nfcCardReaderViewModel.prepareSMS(messageString.reversed().joinToString("\n"))
                }
                create().show()
            }
    }


}
