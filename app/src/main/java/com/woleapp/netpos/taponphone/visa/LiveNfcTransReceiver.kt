package com.woleapp.netpos.taponphone.visa

import android.nfc.tech.IsoDep
import android.util.Log
import com.visa.app.ttpkernel.NfcTransceiver
import com.woleapp.netpos.util.BytesUtils
import java.io.IOException

class LiveNfcTransReceiver(
    private val log: StringBuilder,
    private val mTagCom: IsoDep?,
    private var rawResponse: ByteArray? = null
) : NfcTransceiver {
    override fun transceive(pCommand: ByteArray?): ByteArray {
        log.append("=================<br/>")
        log.append("<font color='green'><b>send:</b> " + BytesUtils.bytesToString(pCommand))
            .append("</font><br/>")
        var response: ByteArray? = null
        response = try {
            // send command to emv card
            mTagCom!!.transceive(pCommand)
        } catch (e: IOException) {
            throw IOException(e.message)
        }
        rawResponse = response
        //iccData.append(BytesUtils.bytesToString(response))
        log.append("<font color='blue'><b>resp:</b> " + BytesUtils.bytesToString(response))
            .append("</font><br/>")
//        Log.d(TAG, "resp: " + BytesUtils.bytesToString(response))
//        try {
//            Log.d(TAG, "resp: " + TlvUtil.prettyPrintAPDUResponse(response))
//            val `val` = SwEnum.getSW(response)
//            if (`val` != null) {
//                Log.d(TAG, "resp: " + `val`.detail)
//            }
//            log.append("<pre>").append(
//                TlvUtil.prettyPrintAPDUResponse(response).replace("\n", "<br/>")
//                    .replace(" ", "&nbsp;")
//            )
//                .append("</pre><br/>")
//        } catch (e: Exception) {
//        }
        return response!!
    }

    override fun destroy() {

    }

    override fun isCardPresent(): Boolean {
        return true
    }
}