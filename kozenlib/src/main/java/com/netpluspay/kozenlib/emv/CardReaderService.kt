package com.netpluspay.kozenlib.emv

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.netpluspay.kozenlib.KozenLib
import com.netpluspay.kozenlib.emv.data.TransactionData
import com.netpluspay.kozenlib.utils.DeviceConfig
import com.netpluspay.kozenlib.utils.GlobalData
import com.netpluspay.kozenlib.utils.POSException
import com.netpluspay.kozenlib.utils.tlv.BerTlvBuilder
import com.netpluspay.kozenlib.utils.tlv.BerTlvParser
import com.netpluspay.kozenlib.utils.tlv.BerTlvs
import com.netpluspay.kozenlib.utils.tlv.HexUtil
import com.netpluspay.kozenlib.view.MaterialDialog
import com.netpluspay.kozenlib.view.PasswordDialog
import com.netpluspay.terminalcore.AndroidTerminalCardReaderFactory
import com.pos.sdk.emvcore.IPosEmvCoreListener
import com.pos.sdk.emvcore.POIEmvCoreManager
import com.pos.sdk.emvcore.POIEmvCoreManager.*
import com.pos.sdk.emvcore.PosEmvErrCode
import com.pos.sdk.utils.PosUtils
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import java.util.*

class CardReaderService(activity: Activity) :
    AndroidTerminalCardReaderFactory<Observable<CardReaderEvent>>() {
    private val logTag = CardReaderService::class.java.simpleName
    private var transactionData = TransactionData()
    private lateinit var emitter: ObservableEmitter<CardReaderEvent>
    private var isOnline = false
    private lateinit var passwordDialog: PasswordDialog
    private var isUpdate = false
    private var cardType = 0
    private lateinit var reqData: Bundle
    private val emvCoreManager: POIEmvCoreManager = getDefault()
    private lateinit var cardPinBlock: String
    private val emvCoreListener: IPosEmvCoreListener.Stub = object : IPosEmvCoreListener.Stub() {
        override fun onEmvProcess(cardMode: Int) {
            if (cardMode == DEV_ICC
                || cardMode == DEV_MAG
                || cardMode == DEV_PICC
            ) {
                cardType = cardMode
                when (cardMode) {
                    DEV_ICC -> {
                        //tvMessage1.setText("Icc Card Trans")
                    }
                    DEV_MAG -> {
                        //tvMessage1.setText("Mag Card Trans")
                    }
                    DEV_PICC -> {
                        Log.e("TAG", "PICC Card trans")
                        //tvMessage1.setText("Picc Card Trans")
                    }
                }
                emitter.onNext(CardReaderEvent.CardDetected())
                //tvMessage2.setText("Processing")
                transactionData.cardType = cardType
            } else {
                when (cardMode) {
                    PosEmvErrCode.EMV_TIMEOUT -> {
                        Log.e(logTag, "Detection Card Timeout")
                        transEnd(cardMode, "Card Detection Timeout")
                        return
                    }
                    PosEmvErrCode.EMV_CANCEL -> {
                        Log.e(logTag, "Transaction Cancel")
                        transEnd(cardMode, "Transaction Cancel")
                        return
                    }
                    PosEmvErrCode.EMV_MULTI_PICC -> {
                        Log.e(logTag, "Multiple cards , Present a single card")
                        transEnd(cardMode, "Multiple cards, Present a single card")
                        return
                    }
                    PosEmvErrCode.EMV_FALLBACK -> {
                        transEnd(cardMode, "Could not read card")
                    }
                    PosEmvErrCode.EMV_ICC_INTERFACE -> {
                        scanForCard()
                        isUpdate = true
                    }
                }
            }
        }

        override fun onSelectApplication(appNameList: MutableList<String>, isFirstSelect: Boolean) {
            val appNames: Array<String> = appNameList.toTypedArray()
            val mDialog = MaterialDialog(KozenLib.getContext())
            mDialog.showListConfirmChoseDialog(
                "Select Application", appNames
            ) { position -> emvCoreManager.onSetSelAppResponse(position) }
        }

        override fun onConfirmCardInfo(p0: Int, p1: String?) {
            emvCoreManager.onSetConfirmCardInfo(true)
        }

        override fun onPiccKernelMode(p0: Int) {

        }

        override fun onPiccSecondTapCard() {

        }

        override fun onRequestInputPin(p0: Bundle?) {
            Handler(Looper.getMainLooper()).post {
                val isIcSlot = cardType == DEV_ICC
                passwordDialog = PasswordDialog(
                    activity, isIcSlot, p0,
                    DeviceConfig.TPKIndex
                ).apply {
                    setPinListener(object : PasswordDialog.Listener {
                        override fun onConfirm(
                            verifyResult: Int,
                            pinBlock: ByteArray?,
                            pinKsn: ByteArray?
                        ) {
                            cardPinBlock = ""
                            pinBlock?.let {
                                cardPinBlock = HexUtil.toHexString(it).toLowerCase(Locale.getDefault())
                            }
                            emvCoreManager.onSetConfirmPin(Bundle())
                        }

                        override fun onError(verifyResult: Int, pinTryCntOut: Int) {
                            emitter.onError(Throwable("Pinpad Error $verifyResult $pinTryCntOut"))
                        }
                    })
                    showDialog()
                }
            }
        }

        override fun onRequestOnlineProcess(dataBundle: Bundle) {
            Log.e(logTag, "GOT HERE")
            isOnline = true
            var buff = dataBundle.getByteArray(EmvOnlineRequestConstraints.EMVDATA)
            Log.d(logTag, "Emv Data :" + HexUtil.toHexString(buff))

            val tlvParser = BerTlvParser()
            val tlvs: BerTlvs = tlvParser.parse(buff)
            for (tlv in tlvs.list) {
                Log.d(
                    logTag,
                    "Emv Tag :" + tlv.tag.toString() + "    Emv Value :" + tlv.hexValue
                )
            }

            val result = dataBundle.getInt(
                EmvOnlineRequestConstraints.ENCRYPTRESULT,
                PosEmvErrCode.EMV_UNENCRYPTED
            )
            when (result) {
                PosEmvErrCode.EMV_OK -> {
                    val encryptData = dataBundle.getByteArray(EmvOnlineRequestConstraints.ENCRYPTDATA)
                    val encryptTlvParser = BerTlvParser()
                    val encryptTlvs: BerTlvs = encryptTlvParser.parse(encryptData)
                    for (tlv in encryptTlvs.list) {
                        Log.d(
                            logTag,
                            "Emv Encrypt Tag :" + tlv.tag
                                .toString() + "    Emv Value :" + tlv.hexValue
                        )
                    }
                    val tlvBuilder = BerTlvBuilder()
                    for (tlv in tlvs.list) {
                        tlvBuilder.addBerTlv(tlv)
                    }
                    for (tlv in encryptTlvs.list) {
                        tlvBuilder.addBerTlv(tlv)
                    }
                    buff = tlvBuilder.buildArray()
                }
                PosEmvErrCode.EMV_UNENCRYPTED -> {
                }
                else -> {
                    Log.e(logTag, "EncryptResult :$result")
                }
            }
            transactionData.transData = buff
            val onlineInput = Bundle()
            onlineInput.putInt(
                EmvOnlineResultConstraints.REQUESTAC,
                GlobalData.getTransOnlineResult()
            )
            emvCoreManager.onSetOnlineResponse(onlineInput)
        }

        override fun onTransactionResult(result: Int, resultData: Bundle?) {
            Log.d("TAG", "GOT HERE hehe")
            var buff = resultData?.getByteArray(EmvProcessResultConstraints.EMVDATA)
            Log.d(logTag, "Emv Data :" + HexUtil.toHexString(buff))

            val tlvParser = BerTlvParser()
            val tlvs = tlvParser.parse(buff)
            for (tlv in tlvs.list) {
                Log.d(logTag, "Emv Tag :" + tlv.tag.toString() + "    Emv Value :" + tlv.hexValue)
            }

            val encryptResult = resultData?.getInt(
                EmvOnlineRequestConstraints.ENCRYPTRESULT,
                PosEmvErrCode.EMV_UNENCRYPTED
            )
            when (encryptResult) {
                PosEmvErrCode.EMV_OK -> {
                    val encryptData = resultData.getByteArray(EmvOnlineRequestConstraints.ENCRYPTDATA)
                    val encryptTlvParser = BerTlvParser()
                    val encryptTlvs = encryptTlvParser.parse(encryptData)
                    for (tlv in encryptTlvs.list) {
                        Log.e(
                            logTag,
                            "Emv Encrypt Tag :" + tlv.tag.toString() + "    Emv Value :" + tlv.hexValue
                        )
                    }
                    val tlvBuilder = BerTlvBuilder()
                    for (tlv in tlvs.list) {
                        tlvBuilder.addBerTlv(tlv)
                    }
                    for (tlv in encryptTlvs.list) {
                        tlvBuilder.addBerTlv(tlv)
                    }
                    buff = tlvBuilder.buildArray()
                }
                PosEmvErrCode.EMV_UNENCRYPTED -> {

                }
                else -> {
                    Log.e(logTag, "EncryptResult :$encryptResult")
                }
            }

            val scriptBuff = resultData?.getByteArray(EmvProcessResultConstraints.SCRIPTRESULT)
            if (scriptBuff != null) {
                Log.d(logTag, "Script :" + PosUtils.bytesToHexString(scriptBuff))
            }

            when (val cvm = resultData?.getInt(EmvProcessResultConstraints.CVM)) {
                EmvProcessResultConstraints.CVM_SIGNATURE -> Log.d(logTag, "Cvm :CVM_SIGNATURE")
                EmvProcessResultConstraints.CVM_CONFIRMATION_CODE_VERIFIED -> Log.d(
                    logTag,
                    "Cvm :CVM_CONFIRMATION_CODE_VERIFIED"
                )
                EmvProcessResultConstraints.CVM_NO_CVM -> Log.d(logTag, "Cvm :CVM_NO_CVM")
                EmvProcessResultConstraints.CVM_SEE_PHONE -> {
                    emitter.onError(POSException(cvm, "CVM: CVM_NO_CVM"))
                    return
                }
            }

            if (result == PosEmvErrCode.EMV_APP_EMPTY) {
                emitter.onError(POSException(result, "AID Empty"))
                return
            }

            if (!isOnline) {
                transactionData.transData = buff
            }

            transactionData.transState = result

            when (result) {
                PosEmvErrCode.EMV_APPROVED_ONLINE, PosEmvErrCode.EMV_APPROVED -> if (isOnline) {
                    transactionData.transState = PosEmvErrCode.EMV_APPROVED_ONLINE
                } else {
                    transactionData.transState = PosEmvErrCode.EMV_APPROVED
                }
            }
            emitter.onNext(CardReaderEvent.CardRead(CardReadResult(result, transactionData).apply {
                encryptedPinBlock = cardPinBlock
            }))
            emitter.onComplete()
        }

    }

    override fun initiateICCCardPayment(
        p0: Long,
        p1: Long
    ): Observable<CardReaderEvent> {
        reqData = Bundle().apply {
            putInt(EmvTransDataConstraints.TRANSTYPE, EMV_GOODS)
            putInt(EmvTransDataConstraints.TRANSAMT, p0.toInt())
            putInt(EmvTransDataConstraints.CASHBACKAMT, p1.toInt())
            putInt(EmvTransDataConstraints.TRANSMODE, 0 or DEV_ICC or DEV_PICC)
            putInt(EmvTransDataConstraints.TRANSTIMEOUTMS, 60)
        }
        transactionData.apply {
            transType = EMV_GOODS
            transAmount = java.lang.Double.valueOf(p0.toDouble())
            transCashAmount = java.lang.Double.valueOf(p1.toDouble())
            transState = PosEmvErrCode.EMV_OTHER_ERROR
        }
        return Observable.create {
            emitter = it
            scanForCard()
        }
    }

    override fun scanForCard() {
        val ret = emvCoreManager.startTransaction(reqData, emvCoreListener)
        when {
            PosEmvErrCode.EMV_CANCEL == ret -> {
                Log.e(logTag, "start Transaction cancel")
                transEnd(ret, "start Transaction cancel")
            }
            PosEmvErrCode.EXCEPTION_ERR == ret -> {
                Log.e(logTag, "start Transaction exception error")
                transEnd(ret, "start Transaction exception error")
            }
            PosEmvErrCode.EMV_ENCRYPT_ERROR == ret -> {
                Log.e(logTag, "start Transaction encrypt error")
                transEnd(ret, "start Transaction encrypt error")
            }
        }
    }

    private fun transEnd(errorCode: Int = PosEmvErrCode.EMV_CANCEL, message: String = "POS Error") {
        emitter.onError(POSException(errorCode, message))
    }

    override fun showPinPad() {
        Log.e(logTag, "Redundant")
    }

    override fun readCard() {
        Log.e("TAG", "Unnecessary")
    }

    override fun updateICCardData(p0: Int) {
        Log.e("TAG", "Unnecessary")
    }
}