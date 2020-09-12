package com.woleapp.netpos.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
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