package com.woleapp.netpos.contactless.viewmodels

import android.os.Build
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.danbamitale.epmslib.entities.CardData
import com.danbamitale.epmslib.entities.TransactionResponse
import com.google.gson.Gson
import com.mastercard.terminalsdk.listeners.PaymentDataProvider
import com.mastercard.terminalsdk.utility.ByteArrayWrapper
import com.visa.app.ttpkernel.ContactlessConfiguration
import com.visa.app.ttpkernel.ContactlessKernel
import com.visa.app.ttpkernel.TtpOutcome
import com.visa.vac.tc.emvconverter.Utils
import com.woleapp.netpos.contactless.app.NetPosApp
import com.woleapp.netpos.contactless.model.QrTransactionResponseFinalModel
import com.woleapp.netpos.contactless.taponphone.mastercard.listener.TransactionListener
import com.woleapp.netpos.contactless.taponphone.tlv.BerTag
import com.woleapp.netpos.contactless.taponphone.tlv.BerTlvParser
import com.woleapp.netpos.contactless.taponphone.tlv.HexUtil
import com.woleapp.netpos.contactless.taponphone.visa.* // ktlint-disable no-wildcard-imports
import com.woleapp.netpos.contactless.taponphone.visa.PPSEv21.PPSEManager
import com.woleapp.netpos.contactless.util.Event
import com.woleapp.netpos.contactless.util.ICCCardHelper
import com.woleapp.netpos.contactless.util.sendSMS
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.disposables.CompositeDisposable
import org.apache.commons.lang.StringUtils
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class NfcCardReaderViewModel @Inject constructor() : ViewModel() {
    private val disposable = CompositeDisposable()
    private val icc = StringBuilder()

    private var _lastPosTransactionResponse: MutableLiveData<TransactionResponse> =
        MutableLiveData()
    val lastPosTransactionResponse: LiveData<TransactionResponse> get() = _lastPosTransactionResponse

    private val _enableNfcForegroundDispatcher: MutableLiveData<Event<Boolean>> by lazy {
        MutableLiveData()
    }

    // FOR QRt
    private val _qrTransactionResponseFromWebView: MutableLiveData<QrTransactionResponseFinalModel> =
        MutableLiveData()
    val qrTransactionResponseFromWebView: LiveData<QrTransactionResponseFinalModel> get() = _qrTransactionResponseFromWebView

    val enableNfcForegroundDispatcher: LiveData<Event<Boolean>>
        get() = _enableNfcForegroundDispatcher

    private val _iccCardHelperLiveData: MutableLiveData<Event<ICCCardHelper>> by lazy {
        MutableLiveData()
    }

    val iccCardHelperLiveData: LiveData<Event<ICCCardHelper>>
        get() = _iccCardHelperLiveData

    var iccCardHelper: ICCCardHelper = ICCCardHelper()

    private val _showAccountTypeDialog: MutableLiveData<Event<Boolean>> by lazy {
        MutableLiveData()
    }

    val showAccountTypeDialog: LiveData<Event<Boolean>>
        get() = _showAccountTypeDialog

    private val _showPinPadDialog: MutableLiveData<Event<String?>> by lazy {
        MutableLiveData()
    }

    val showPinPadDialog: LiveData<Event<String?>>
        get() = _showPinPadDialog

    private val _showWaitingDialog: MutableLiveData<Event<NfcPaymentType?>> by lazy {
        MutableLiveData()
    }

    val showWaitingDialog: LiveData<Event<NfcPaymentType?>>
        get() = _showWaitingDialog

    private lateinit var amountInBytes: ByteArrayWrapper
    private lateinit var cashBackAmountInBytes: ByteArrayWrapper

    private val _message: MutableLiveData<Event<String?>> by lazy {
        MutableLiveData(null)
    }

    val message: LiveData<Event<String?>>
        get() = _message

    private val _showPrintDialog = MutableLiveData<Event<String>>()
    val showPrintDialog: LiveData<Event<String>>
        get() = _showPrintDialog

    private val _showQrPrintDialog = MutableLiveData<Event<String>>()
    val showQrPrintDialog: LiveData<Event<String>>
        get() = _showQrPrintDialog

    private val _smsSent = MutableLiveData<Event<Boolean>>()
    val smsSent: LiveData<Event<Boolean>>
        get() = _smsSent

    override fun onCleared() {
        super.onCleared()
        icc.clear()
        disposable.clear()
    }

    fun initiateNfcPayment(amount: Long, cashBackAmount: Long, nfcPaymentType: NfcPaymentType) {
        _showWaitingDialog.postValue(Event(nfcPaymentType))
        amountInBytes = ByteArrayWrapper(StringUtils.leftPad(amount.toString(), 12, '0'))
        cashBackAmountInBytes =
            ByteArrayWrapper(StringUtils.leftPad(cashBackAmount.toString(), 12, '0'))

        when (nfcPaymentType) {
            NfcPaymentType.VISA -> _enableNfcForegroundDispatcher.postValue(Event(true))
            NfcPaymentType.MASTERCARD -> doMasterCardTransaction()
        }
    }

    fun doVisaTransaction(
        nfcTransceiver: LiveNfcTransReceiver,
        contactlessKernel: ContactlessKernel
    ) {
        iccCardHelper = ICCCardHelper()
        icc.clear()
        val contactlessConfiguration = ContactlessConfiguration.getInstance()

        contactlessConfiguration.terminalData["9F02"] = amountInBytes.bytes

        contactlessConfiguration.terminalData["9F03"] = cashBackAmountInBytes.bytes

        // Select PPSE
        var selectedAid: ByteArray? = null
        val ppseManager =
            PPSEManager()
        try {
            // Set suppported AID
            ppseManager.setSupportedApps(supportedAids)

            // Perform Select PPSE
            selectedAid = ppseManager.selectPPSE(nfcTransceiver)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        var outcome: TtpOutcome = TtpOutcome.ABORTED
        var outComeResponse = ""
        while (selectedAid != null) {
            // Get the ContactlessConfiguration instance
            // Specify the terminal settings for this transaction
            val myData = contactlessConfiguration.terminalData
            myData["4F"] = selectedAid // set the selected aid
            myData["9F4E"] = byteArrayOf(
                0x00.toByte(),
                0x11.toByte(),
                0x22.toByte(),
                0x33.toByte(),
                0x44.toByte(),
                0x55.toByte(),
                0x66.toByte(),
                0x77.toByte(),
                0x88.toByte(),
                0x99.toByte(),
                0xAA.toByte(),
                0xBB.toByte(), 0xCC.toByte(), 0xDD.toByte(), 0xEE.toByte(), 0xFF.toByte()
            )

            Timber.e("available keys")
            myData.forEach {
                Timber.e(it.key)
            }
            // Call TTP Kernel performTransaction
            val contactlessResult =
                contactlessKernel.performTransaction(nfcTransceiver, contactlessConfiguration)

            // Check the transaction outcome
            outcome = contactlessResult.finalOutcome
            outComeResponse = when (outcome) {
                TtpOutcome.COMPLETED -> {
                    "Online Approval Requested"
                }
                TtpOutcome.DECLINED -> {
                    "Transaction Declined"
                }
                TtpOutcome.ABORTED -> {
                    "Transaction Terminated"
                }
                TtpOutcome.TRYNEXT -> {
                    "PPSE:Try Next Application"
                }
                TtpOutcome.SELECTAGAIN -> {
                    "GPO Returned 6986. Application Try Again."
                }
                else -> {
                    ""
                }
            }

            // Display the TTP Kernel version
            contactlessKernel.kernelData

            // Display the TTP Kernel data
            val version = contactlessKernel.kernelData
            var key: String
            var value: String
            for ((key1, value1) in version) {
                if (value1 != null) {
                    key = key1 as String
                    value = Utils.getHexString(value1) as String
                    Timber.e("key is $key")
                    if (key in REQUIRED_TAGS) {
                        icc.append(value)
                    }
                    // mainLog.text = mainLog.text.toString() + "\n" + key + ":" + value
                }
            }

            // Display the transaction results
            val cardData = contactlessResult.data
            for ((key1, value1) in cardData) {
                key = key1 as String
                Timber.e("key is $key")
                if (value1 != null) {
                    value = Utils.getHexString(value1) as String
                    if (key in REQUIRED_TAGS) {
                        icc.append(value)
                    }
                }
            }
            val internalData = contactlessResult.internalData
            for ((key1, value1) in internalData) {
                if (value1 != null) {
                    key = key1 as String
                    Timber.e("key is $key")
                    value = Utils.getHexString(value1) as String
                    if (key in REQUIRED_TAGS) {
                        icc.append(value)
                    }
                }
            }
            selectedAid = if (outcome == TtpOutcome.TRYNEXT) {
                ppseManager.nextCandidate()
            } else {
                null
            }
        }
        _enableNfcForegroundDispatcher.postValue(Event(false))
        if (outcome == TtpOutcome.COMPLETED) {
            iccCardHelper.apply {
                cardScheme = NfcPaymentType.VISA.name
                customerName = "CUSTOMER"
            }
            createVisaCardData(icc)
        } else {
            Timber.e("failed.......")
            _showWaitingDialog.postValue(Event(null))
            _iccCardHelperLiveData.postValue(Event(ICCCardHelper(error = Throwable("Error occurred while reading card: $outComeResponse"))))
        }
    }

    private fun createVisaCardData(icc: StringBuilder) {
        Timber.e("create visa card")
        icc.append("9F3303E068C8")
        icc.append("9F350122")
        icc.append("9F0902008C")
        icc.append("9F3403420300")
        val buildId =
            HexDump.toHexString(Build.ID.replace("[^a-zA-Z0-9]".toRegex(), "").toByteArray())
        val buildIdHex = "9F1E${HexDump.toHexString("${buildId.length / 2}".toByte())}$buildId"
        icc.append(buildIdHex)
        val iccData = icc.toString()
        val tlvs = BerTlvParser()
            .parse(HexUtil.parseHex(iccData))
        val panSequence = tlvs.find(
            BerTag(
                "5F34"
            )
        ).hexValue
        val track2 = tlvs.find(
            BerTag(
                "57"
            )
        ).hexValue
        val pan = track2.split("D").first()
        val cardData = CardData(
            track2,
            iccData,
            StringUtils.leftPad(panSequence, 3, "0"),
            "051"
        )
        Timber.e("got here nau, haba")
        iccCardHelper.cardData = cardData
        _showWaitingDialog.postValue(Event(null))
        _showPinPadDialog.postValue(Event(pan))
        Timber.e("got here nau, habatica")
    }

    fun setPinBlock(encryptedPinBloc: String?) {
        iccCardHelper.cardData?.apply {
            pinBlock = encryptedPinBloc
        }
        _showAccountTypeDialog.value = Event(true)
    }

    private fun doMasterCardTransaction() {
        NetPosApp.INSTANCE.outcomeObserver.resetObserver(object :
                TransactionListener {
                override fun onTransactionSuccessful() {
                }

                override fun onOnlineReferral(cardData: CardData, pan: String) {
                    iccCardHelper.apply {
                        this.cardData = cardData
                        cardScheme = NfcPaymentType.MASTERCARD.name
                        customerName = "CUSTOMER"
                    }
                    Looper.prepare()
                    _showWaitingDialog.postValue(Event(null))
                    _showPinPadDialog.postValue(Event(pan))
                    Timber.e("on online referral")
                }

                override fun onTransactionDeclined() {
                    Looper.prepare()
                    _showWaitingDialog.postValue(Event(null))
                }

                override fun onApplicationEnded() {
                    Looper.prepare()
                    _showWaitingDialog.postValue(Event(null))
                }

                override fun onTransactionCancelled() {
                    Looper.prepare()
                    _showWaitingDialog.postValue(Event(null))
                }

                override fun logToScreen(s: String?) {
                }

                override fun onTransactionError(message: String?) {
                    _message.value = Event(message)
                }
            })

        NetPosApp.INSTANCE.transactionsApi.initiatePayment(object :
                PaymentDataProvider {
                override fun getPaymentDataMap(): HashMap<Int, ByteArrayWrapper> {
                    val map = HashMap<Int, ByteArrayWrapper>()
                    map.apply {
                        put(0x9F02, amountInBytes)
                        put(
                            0x9F03,
                            cashBackAmountInBytes
                        )
                        put(0x5F2A, ByteArrayWrapper("0566"))
                        put(0x9F1A, ByteArrayWrapper("0566"))
                    }
                    return map
                }

                override fun setPaymentDataEntry(p0: Int?, p1: ByteArrayWrapper?) {
                }
            })
    }

    fun setIccCardHelperLiveData(iccCardHelper: ICCCardHelper) {
        _iccCardHelperLiveData.postValue(Event(iccCardHelper))
    }

    fun stopNfcReader() {
        try {
            NetPosApp.INSTANCE.transactionsApi.abortTransaction()
            NetPosApp.INSTANCE.nfcProvider.disconnectReader()
        } catch (e: Exception) {
        }
    }

    fun finishNfcReading() {
        Timber.e(iccCardHelper.toString())
        _enableNfcForegroundDispatcher.postValue(Event(false))
        _iccCardHelperLiveData.postValue(Event(iccCardHelper))
    }

    fun prepareSMS(it: String) {
        _showPrintDialog.value = Event(it)
    }

    fun sendSmS(message: String, toString: String) {
        sendSMS(
            message,
            toString,
            _smsSent,
            compositeDisposable = disposable
        )
    }

    // FOR QR
    fun setQrTransactionResponse(qrTransactionResponse: QrTransactionResponseFinalModel) {
        _qrTransactionResponseFromWebView.value = qrTransactionResponse
    }

    fun showReceiptDialogForQrPayment() {
        _showQrPrintDialog.value = Event(
            Gson().toJson(qrTransactionResponseFromWebView.value)
        )
    }

    fun setLastPosTransactionResponse(lastPosTrans: TransactionResponse) {
        _lastPosTransactionResponse.postValue(lastPosTrans)
    }
}
