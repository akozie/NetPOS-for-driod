package com.woleapp.netpos.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import com.danbamitale.epmslib.entities.NibssAID
import com.danbamitale.epmslib.entities.NibssCA
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.pos.sdk.emvcore.PosEmvAid
import com.pos.sdk.emvcore.PosEmvCapk
import com.pos.sdk.utils.PosUtils
import com.woleapp.netpos.R
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import okhttp3.ResponseBody
import retrofit2.HttpException
import java.text.SimpleDateFormat
import java.util.*

fun Long.formatDate(): String? =
    SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault()).format(Date(this))

fun Disposable.disposeWith(compositeDisposable: CompositeDisposable) {
    compositeDisposable.add(this)
}

fun xorHex(a: String, b: String): String? {
    // TODO: Validation
    val chars = CharArray(a.length)
    for (i in chars.indices) {
        chars[i] = toHex(fromHex(a[i]) xor fromHex(b[i]))
    }
    return String(chars)
}

private fun fromHex(c: Char): Int {
    if (c in '0'..'9') {
        return c - '0'
    }
    if (c in 'A'..'F') {
        return c - 'A' + 10
    }
    if (c in 'a'..'f') {
        return c - 'a' + 10
    }
    throw IllegalArgumentException()
}

private fun toHex(nybble: Int): Char {
    require(!(nybble < 0 || nybble > 15))
    return "0123456789ABCDEF"[nybble]
}
fun copyTextToClipboard(context: Context, label: String, text: String) {
    val clipboard: ClipboardManager? =
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
    val clip = ClipData.newPlainText(label, text)
    clipboard!!.setPrimaryClip(clip)
}

fun String.isValidIpAddress(): Boolean {
    val PATTERN =
        Regex.fromLiteral("^((0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)\\.){3}(0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)$")
    return this.matches(PATTERN)
}

fun NibssAID.toPosEmvAid(): PosEmvAid{
    val emvAid = PosEmvAid()
    emvAid.AID = PosUtils.hexStringToBytes(this.AID)
    emvAid.Version = PosUtils.hexStringToBytes(this.applicationVersion)
    emvAid.TACDefault = PosUtils.hexStringToBytes(this.tacDefault)
    emvAid.TACOnline = PosUtils.hexStringToBytes(this.tacOnline)
    emvAid.TACDenial = PosUtils.hexStringToBytes(this.tacDenial)
    emvAid.tDOL = PosUtils.hexStringToBytes(this.TDOL)
    emvAid.dDOL = PosUtils.hexStringToBytes(this.DDOL)
    emvAid.SelFlag = true
    emvAid.TargetPer = 0
    emvAid.MaxTargetPer = 99
    emvAid.FloorLimit = 0
    emvAid.Threshold = 10000
    emvAid.ContactlessTransMoneyLimit = 200000
    emvAid.TermDoCvmMoneyLimit = 200000
    emvAid.ContactlessOfflineTransMoneyLimit = 0
    return emvAid
}

fun NibssCA.toPosEmvCa(): PosEmvCapk{
    val emvCapk = PosEmvCapk()
    emvCapk.RID = PosUtils.hexStringToBytes(this.RID)
    emvCapk.KeyID = PosUtils.hexStringToBytes(this.keyIndex)[0]
    emvCapk.Modul = PosUtils.hexStringToBytes(this.modulus)
    emvCapk.Exponent = PosUtils.hexStringToBytes(this.exponent)
    emvCapk.CheckSum = PosUtils.hexStringToBytes(this.hash)
    emvCapk.HashInd = this.hashAlgorithm.toByteOrNull() ?: PosEmvCapk.HASH_IND_SHA1
    emvCapk.ArithInd = this.keyAlgorithm.toByteOrNull() ?: PosEmvCapk.AIRTH_IND_RSA
    return emvCapk
}

fun Throwable.getResponseBody(): String {
    val error: String
    error = if (isHttpException()) {
        val body: ResponseBody? =
            (this as HttpException).response()!!.errorBody()
        try {
            val sBody = body?.string()
            Log.e("tag", "body $sBody")
            sBody ?: "{\"message\": \"An unexpected error occurred, Please try again\"}"
        } catch (e1: Exception) {
            // userResponseResult.setValue(Error(Throwable("An unexpected error occurred")))
            "{\"message\": \"An unexpected error occurred, Please try again\"}"
        } finally {
            body?.close()
        }
    } else
        "{\"message\": \"An unexpected error occurred, Please try again\"}"
    return error
}

fun Throwable.isHttpException(): Boolean =
    isHttpStatusCode(400) || isHttpStatusCode(404) || isHttpStatusCode(500)

fun Throwable.isHttpStatusCode(statusCode: Int): Boolean =
    (this is HttpException && this.code() == statusCode)

fun getPlansJson(context: Context): String = context.resources.openRawResource(R.raw.plans)
    .bufferedReader().use { it.readText() }

fun getServiceProviderPlansJson(context: Context): String =
    context.resources.openRawResource(R.raw.data_plans)
        .bufferedReader().use { it.readText() }

fun encodeAsBitmap(source: String, width: Int, height: Int): Bitmap? {
    val result: BitMatrix = try {
        MultiFormatWriter().encode(source, BarcodeFormat.QR_CODE, width, height, null)
    } catch (e: Exception) {
        return null
    }

    val w = result.width
    val h = result.height
    val pixels = IntArray(w * h)

    for (y in 0 until h) {
        val offset = y * w
        for (x in 0 until w) {
            pixels[offset + x] = if (result[x, y]) Color.BLACK else Color.WHITE
        }
    }
    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    bitmap.setPixels(pixels, 0, width, 0, 0, w, h)

    return bitmap
}