@file:Suppress("DEPRECATION")

package com.woleapp.netpos.ui.fragments

import android.app.ProgressDialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import com.google.android.material.snackbar.Snackbar
import com.google.gson.JsonObject
import com.netpluspay.netpossdk.NetPosSdk
import com.netpluspay.nibssclient.models.TransactionType
import com.woleapp.netpos.R
import com.woleapp.netpos.databinding.DialogPrintTypeBinding
import com.woleapp.netpos.databinding.DialogTransactionResultBinding
import com.woleapp.netpos.databinding.FragmentSalesBinding
import com.woleapp.netpos.nibss.NetPosTerminalConfig
import com.woleapp.netpos.util.TRANSACTION_TYPE
import com.woleapp.netpos.util.disposeWith
import com.woleapp.netpos.util.showCardDialog
import com.woleapp.netpos.viewmodels.SalesViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import com.woleapp.netpos.model.Vend
import com.woleapp.netpos.util.Singletons
import io.reactivex.Observable
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.lang.Exception
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.TimeUnit


class SalesFragment : BaseFragment() {
    companion object {
        fun newInstance(
            transactionType: TransactionType = TransactionType.PURCHASE,
            isVend: Boolean = false
        ): SalesFragment =
            SalesFragment().apply {
                arguments = Bundle().apply {
                    putString(TRANSACTION_TYPE, transactionType.name)
                    putBoolean("IS_VEND", isVend)
                }
            }
    }

    private val viewModel by viewModels<SalesViewModel>()
    private lateinit var transactionType: TransactionType
    private lateinit var alertDialog: AlertDialog
    private lateinit var receiptDialogBinding: DialogTransactionResultBinding
    private lateinit var dialogPrintTypeBinding: DialogPrintTypeBinding
    private lateinit var printTypeDialog: AlertDialog
    private lateinit var printerErrorDialog: AlertDialog
    private val compositeDisposable = CompositeDisposable()
    private var isVend: Boolean = false
    private lateinit var binding: FragmentSalesBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSalesBinding.inflate(inflater, container, false)
        transactionType = TransactionType.valueOf(
            arguments?.getString(
                TRANSACTION_TYPE,
                TransactionType.PURCHASE.name
            )!!
        )
        isVend = arguments?.getBoolean("IS_VEND", false) ?: false
        viewModel.isVend(isVend)
        receiptDialogBinding = DialogTransactionResultBinding.inflate(inflater, null, false)
            .apply { executePendingBindings() }
        dialogPrintTypeBinding = DialogPrintTypeBinding.inflate(layoutInflater, null, false).apply {
            executePendingBindings()
        }
        printTypeDialog = AlertDialog.Builder(requireContext()).setCancelable(false)
            .apply {
                setView(dialogPrintTypeBinding.root)
                dialogPrintTypeBinding.apply {
                    cancel.setOnClickListener {
                        printTypeDialog.dismiss()
                        viewModel.finish()
                    }
                    customer.setOnClickListener {
                        viewModel.printReceipt(
                            requireContext(),
                            isMerchantCopy = false,
                            selected = true
                        )
                    }
                    merchant.setOnClickListener {
                        viewModel.printReceipt(
                            requireContext(),
                            isMerchantCopy = true,
                            selected = true
                        )
                    }
                }
            }.create()
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
        /*viewModel.getCardData.observe(viewLifecycleOwner){event ->
            event.getContentIfNotHandled()?.let {
                quickPay()
            }
        }*/
        viewModel.getCardData.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { shouldGetCardData ->
                if (shouldGetCardData)
                    showCardDialog(
                        requireActivity(),
                        viewLifecycleOwner,
                        viewModel.amountLong,
                        0L,
                        compositeDisposable
                    ).observe(viewLifecycleOwner) { event ->
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
            }
        }
        viewModel.showReceiptType.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                printTypeDialog.show()
            }
        }
        viewModel.smsSent.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                receiptDialogBinding.progress.visibility = View.GONE
                receiptDialogBinding.sendButton.isEnabled = true
                if (it) {
                    Toast.makeText(requireContext(), "Sent Receipt", Toast.LENGTH_LONG).show()
                    alertDialog.dismiss()
                    viewModel.finish()
                }
            }
        }
        viewModel.toastMessage.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
            }
        }
        viewModel.finish.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                if (it)
                    requireActivity().onBackPressed()
            }
        }
        viewModel.showPrinterError.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                if (printTypeDialog.isShowing)
                    printTypeDialog.cancel()
                if (printerErrorDialog.isShowing)
                    printerErrorDialog.cancel()
                printerErrorDialog.apply {
                    setMessage(it)
                }.show()
            }
        }
        alertDialog = AlertDialog.Builder(requireContext()).setCancelable(false).apply {
            setView(receiptDialogBinding.root)
            receiptDialogBinding.apply {
                closeBtn.setOnClickListener {
                    alertDialog.dismiss()
                    viewModel.finish()
                }
                sendButton.setOnClickListener {
                    if (receiptDialogBinding.telephone.text.toString().length != 11) {
                        Toast.makeText(
                            requireContext(),
                            "Please enter a valid phone number",
                            Toast.LENGTH_LONG
                        ).show()
                        return@setOnClickListener
                    }
                    viewModel.sendSmS(
                        receiptDialogBinding.telephone.text.toString()
                    )
                    progress.visibility = View.VISIBLE
                    sendButton.isEnabled = false
                }
            }
        }.create()
        alertDialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        viewModel.showPrintDialog.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                if (printTypeDialog.isShowing)
                    printTypeDialog.cancel()
                if (printerErrorDialog.isShowing)
                    printerErrorDialog.cancel()
                alertDialog.apply {
                    receiptDialogBinding.transactionContent.text = it
                    show()
                }
                receiptDialogBinding.apply {
                    progress.visibility = View.GONE
                    sendButton.isEnabled = true
                }
            }
        }
        viewModel.shouldRefreshNibssKeys.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                if (it)
                    NetPosTerminalConfig.init(
                        requireContext().applicationContext,
                        configureSilently = true
                    )
            }
        }
        binding.process.setOnClickListener {
            viewModel.validateField()
        }

        return binding.root
    }

    /*private fun quickPay() {
        viewModel.setAccountType(IsoAccountType.SAVINGS)
        viewModel.cardData = CardData(
            track2Data = "5199110748591994D2012221000013772F",
            nibssIccSubset = "9F26087BCB3647E84652A29F2701809F10120110A040002A0400000000000000000000FF9F370451271F779F360201E3950500802480009A032009179C01009F02060000000010005F2A020566820239009F1A0205669F34034203009F3303E0F9C89F3501229F1E0831323334353637388407A00000000410109F090200009F03060000000000005F340100",
            panSequenceNumber = "000",
            posEntryMode = "051"
        ).apply {
            pinBlock = TripleDES.encrypt(
                "0425A8EF8B7A6E66",
                NetPosTerminalConfig.getKeyHolder()!!.clearPinKey
            )
        }
        viewModel.makePayment(requireContext(), transactionType)
    }*/

    /*private fun quickPay() {
        viewModel.setAccountType(IsoAccountType.SAVINGS)
        viewModel.setCardScheme("Master Card")
        viewModel.setCustomerName("SUBAIR/BABATUNDE")
        viewModel.cardData = CardData(
            track2Data = "5399834599607066D22032210014182625",
            nibssIccSubset = "9F2608F564EF96AC6AFE8F9F2701809F10120110A04003220000000000000000000000FF9F3704BAD5E42A9F3602030A950500000480009A032012039C01009F02060000000010005F2A020566820239009F1A0205669F34034203009F3303E068C89F3501229F1E0842313739314531588407A00000000410109F090200029F03060000000000005F340101",
            panSequenceNumber = "001",
            posEntryMode = "051"
        ).apply {
            pinBlock = TripleDES.encrypt(
                "0420BDCBA669F8F9",
                NetPosTerminalConfig.getKeyHolder()!!.clearPinKey
            )
        }
        viewModel.makePayment(requireContext(), transactionType)
    }*/

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
            ), message, Snackbar.LENGTH_LONG
        ).show()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        vend()
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
                socket.connect(InetSocketAddress("vend.netpluspay.com", 3535))
                printWriter = PrintWriter(socket.getOutputStream(), true)
                reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                val firstData = reader?.readLine()
                Timber.e(firstData)
            }.flatMap {
                Observable.interval(0, 5, TimeUnit.SECONDS)
            }.flatMap {
                val out = JsonObject().apply {
                    addProperty("serial_number", NetPosSdk.getDeviceSerial())
                    addProperty("status", "")
                }.toString()
                printWriter?.println(out)
                try {
                    val s = reader?.readLine()
                    Timber.e(s)
                    val vend = Singletons.gson.fromJson(s, Vend::class.java)
                    //socket.close()
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

            /*Single.fromCallable {
                Socket().run {
                    connect(InetSocketAddress("vend.netpluspay.com", 3535), 30_000)
                    soTimeout = 30_000
                    val reader = BufferedReader(InputStreamReader(getInputStream()))
                    val firstData = reader.readLine()
                    Timber.e(firstData)
                    val printWriter = PrintWriter(getOutputStream(), true)
                    val out = JsonObject().apply {
                        addProperty("serial_number", NetPosSdk.getDeviceSerial())
                        addProperty("status", "")
                    }.toString()
                    printWriter.println(out)
                    reader.readLine()
                }
            }.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally {
                    progressBar.dismiss()
                }.flatMap {
                    Timber.e(it)
                    Single.just(Singletons.gson.fromJson(it, Vend::class.java))
                }
                .subscribe { t1, t2 ->
                    t1?.let {
                        Timber.e(it.toString())
                        Toast.makeText(context, "received", Toast.LENGTH_SHORT).show()
                        Toast.makeText(context, it.amount.toString(), Toast.LENGTH_LONG).show()
                        binding.priceTextbox.setText(it.amount.toLong().toString())

                    }
                    t2?.let {
                        Toast.makeText(context, it.localizedMessage, Toast.LENGTH_SHORT).show()
                        Timber.e(it)
                    }
                }.disposeWith(compositeDisposable)*/
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.clear()
    }
}