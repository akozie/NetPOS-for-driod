package com.netpluspay.kozenlib.emv

import android.os.Bundle
import com.netpluspay.terminal_core.AndroidTerminalCardReaderFactory
import com.pos.sdk.emvcore.IPosEmvCoreListener
import com.pos.sdk.emvcore.POIEmvCoreManager
import com.pos.sdk.emvcore.POIEmvCoreManager.EmvTransDataConstraints

class CardReaderService : AndroidTerminalCardReaderFactory<Int>() {
    lateinit var clearPinKey: String
    private val poiEmvCoreManager: POIEmvCoreManager = POIEmvCoreManager.getDefault()
    private val poiEmvCoreListener: POIEmvCoreListener = POIEmvCoreListener()


    override fun initiateICCCardPayment(
        amount: Long,
        cashBackAmount: Long,
        clearPinKey: String?
    ): Int {
        this.clearPinKey = clearPinKey!!
        val data = Bundle()
        data.putInt(EmvTransDataConstraints.TRANSTYPE, POIEmvCoreManager.EMV_PAYMENT)
        data.putInt(EmvTransDataConstraints.TRANSAMT, amount.toInt())
        data.putInt(EmvTransDataConstraints.CASHBACKAMT, cashBackAmount.toInt())
        val mode = 0 or POIEmvCoreManager.DEV_ICC
        data.putInt(EmvTransDataConstraints.TRANSMODE, mode)
        return 1
    }

    override fun scanForCard() {
        TODO("Not yet implemented")
    }

    override fun showPinPad() {
        TODO("Not yet implemented")
    }

    override fun readCard() {

    }

    override fun updateICCardData(p0: Int) {
        TODO("Not yet implemented")
    }

    inner class POIEmvCoreListener : IPosEmvCoreListener.Stub() {
        override fun onEmvProcess(p0: Int) {
            TODO("Not yet implemented")
        }

        override fun onSelectApplication(p0: MutableList<String>?, p1: Boolean) {
            TODO("Not yet implemented")
        }

        override fun onConfirmCardInfo(p0: Int, p1: String?) {
            TODO("Not yet implemented")
        }

        override fun onPiccKernelMode(p0: Int) {
            TODO("Not yet implemented")
        }

        override fun onPiccSecondTapCard() {
            TODO("Not yet implemented")
        }

        override fun onRequestInputPin(p0: Bundle?) {
            TODO("Not yet implemented")
        }

        override fun onRequestOnlineProcess(p0: Bundle?) {
            TODO("Not yet implemented")
        }

        override fun onTransactionResult(p0: Int, p1: Bundle?) {
            TODO("Not yet implemented")
        }

    }
}