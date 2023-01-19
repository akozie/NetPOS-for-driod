package com.woleapp.netpos.contactless.util

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ClickableSpan
import android.text.style.StyleSpan
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.woleapp.netpos.contactless.R
import com.woleapp.netpos.contactless.model.*
import com.woleapp.netpos.contactless.ui.dialog.LoadingDialog
import com.woleapp.netpos.contactless.util.AppConstants.STRING_LOADING_DIALOG_TAG
import kotlinx.android.synthetic.main.dialog_print_type.*
import retrofit2.HttpException
import timber.log.Timber
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

object RandomPurposeUtil {
    fun stringToBase64(text: String): String {
        val data: ByteArray = text.toByteArray()
        return Base64.encodeToString(data, Base64.DEFAULT)
    }

    fun base64ToPlainText(base64String: String): String {
        val decodedString = Base64.decode(base64String, Base64.DEFAULT)
        return String(decodedString)
    }

    fun LifecycleOwner.showSnackBar(rootView: View, message: String) {
        Snackbar.make(rootView, message, Snackbar.LENGTH_LONG).show()
    }

    @SuppressLint("ConstantLocale")
    fun getCurrentDateTime() =
        SimpleDateFormat(
            "yyyy-MM-dd hh:mm a",
            Locale.getDefault()
        ).format(System.currentTimeMillis())
            .format(Date())

    @SuppressLint("SimpleDateFormat")
    fun dateStr2Long(dateStr: String, inputDateFormat: String): Long {
        val formattedDateString = dateStr.removeSuffix(dateStr.takeLast(3))
        return try {
            val c = Calendar.getInstance()
            c.time = SimpleDateFormat(inputDateFormat)
                .parse(dateStr)!!
            c.timeInMillis
        } catch (e: ParseException) {
            e.printStackTrace()
            0
        }
    }

    fun generateRandomRrn(length: Int): String {
        val random = Random()
        var digits = ""
        digits += (random.nextInt(9) + 1).toString()
        for (i in 1 until length) {
            digits += (random.nextInt(10) + 0).toString()
        }
        return digits
    }

    @Throws(IndexOutOfBoundsException::class)
    fun customSpannableString(
        text: String,
        startIndex: Int,
        endIndex: Int,
        clickAction: () -> Unit
    ): SpannableString {
        val spannedText = SpannableString(text)
        val styleSpan = StyleSpan(Typeface.BOLD)
        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                clickAction()
            }
        }
        if (startIndex < 0) throw IndexOutOfBoundsException("$startIndex must be at least 0")
        if (text.isEmpty()) throw IndexOutOfBoundsException("$text can't be empty")
        if (endIndex > text.length) throw IndexOutOfBoundsException("$endIndex can't be greater than the length of $text")
        spannedText.setSpan(styleSpan, startIndex, endIndex, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
        spannedText.setSpan(
            clickableSpan,
            startIndex,
            endIndex,
            Spanned.SPAN_INCLUSIVE_EXCLUSIVE
        )
        return spannedText
    }

    fun <T> observeServerResponseActivity(
        context: Context,
        lifecycle: LifecycleOwner,
        serverResponse: LiveData<Resource<T>>,
        loadingDialog: LoadingDialog,
        fragmentManager: FragmentManager,
        successAction: () -> Unit
    ) {
        serverResponse.observe(lifecycle) {
            when (it.status) {
                Status.SUCCESS -> {
                    loadingDialog.dismiss()
                    if (
                        it.data is PostQrToServerResponse ||
                        it.data is PostQrToServerVerveResponseModel ||
                        it.data is QrTransactionResponseModel ||
                        it.data is VerveTransactionResponse ||
                        it.data is ConfirmOTPResponse ||
                        it.data is ExistingAccountRegisterResponse
                    ) {
                       // Toast.makeText(context, "Success", Toast.LENGTH_SHORT).show()
                        successAction()
                    } else {
                        Toast.makeText(context, R.string.an_error_occurred, Toast.LENGTH_SHORT).show()
                    }
                }
                Status.LOADING -> {
                    loadingDialog.show(
                        fragmentManager,
                        STRING_LOADING_DIALOG_TAG
                    )
                }
                Status.ERROR -> {
                    loadingDialog.dismiss()
                    Toast.makeText(context, R.string.an_error_occurred, Toast.LENGTH_SHORT).show()
                }
                Status.TIMEOUT -> {
                    loadingDialog.dismiss()
                    Toast.makeText(context, R.string.timeOut, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    fun <T> Fragment.observeServerResponse(
        serverResponse: LiveData<Resource<T>>,
        loadingDialog: LoadingDialog,
        fragmentManager: FragmentManager,
        successAction: () -> Unit
    ) {
        serverResponse.observe(this.viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    loadingDialog.dismiss()
                    if (
                        it.data is PostQrToServerResponse ||
                        it.data is PostQrToServerVerveResponseModel ||
                        it.data is QrTransactionResponseModel ||
                        it.data is VerveTransactionResponse ||
                        it.data is AccountNumberLookUpResponse ||
                        it.data is ConfirmOTPResponse ||
                        it.data is ExistingAccountRegisterResponse
                    ) {
                        successAction()
                    } else {
                        showSnackBar(this.requireView(), getString(R.string.an_error_occurred))
                    }
                }
                Status.LOADING -> {
                    loadingDialog.show(
                        fragmentManager,
                        STRING_LOADING_DIALOG_TAG
                    )
                }
                Status.ERROR -> {
                    loadingDialog.cancel
                    loadingDialog.dismiss()
                    //Timber.d("ADSFRTHG--->$it")
                }
                Status.TIMEOUT -> {
                    loadingDialog.cancel
                    loadingDialog.dismiss()
                    //loadingDialog.requireActivity().finish()
                    showSnackBar(this.requireView(), getString(R.string.timeOut))
                }
            }
        }
    }


    fun <T> Fragment.observeServerResponse(
        serverResponse: LiveData<Resource<T>>,
        loadingDialog: AlertDialog,
        fragmentManager: FragmentManager,
        successAction: () -> Unit
    ) {
        serverResponse.observe(this.viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    loadingDialog.dismiss()
                    if (
                        it.data is PostQrToServerResponse ||
                        it.data is PostQrToServerVerveResponseModel ||
                        it.data is QrTransactionResponseModel ||
                        it.data is VerveTransactionResponse ||
                        it.data is AccountNumberLookUpResponse ||
                        it.data is ConfirmOTPResponse ||
                        it.data is ExistingAccountRegisterResponse
                    ) {
                        successAction()
                    } else {
                        showSnackBar(this.requireView(), getString(R.string.an_error_occurred))
                    }
                }
                Status.LOADING -> {
                    loadingDialog.show()
                }
                Status.ERROR -> {
                    loadingDialog.cancel()
                    loadingDialog.dismiss()
                }
                Status.TIMEOUT -> {
                    loadingDialog.cancel()
                    loadingDialog.dismiss()
                    //loadingDialog.requireActivity().finish()
                    showSnackBar(this.requireView(), getString(R.string.timeOut))
                }
            }
        }
    }

    fun closeSoftKeyboard(context: Context, activity: Activity) {
        activity.currentFocus?.let { view ->
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    fun formatFailedVerveTransRespToExtractIswResponse(transResponse: VerveTransactionResponse): VerveTransactionResponse {
        val iswResponseInStringFormat =
            transResponse.message.split("response:\n").last().removeSuffix("\n\"")
                .replace("\\", "")
        val iswResponse = Gson().fromJson(iswResponseInStringFormat, Response::class.java)
        return transResponse.copy(
            code = iswResponse.errors.first().code,
            message = iswResponse.errors.first().message
        )
    }

    fun alertDialog(
        context: Context
    ): AlertDialog{
        val dialogView: View = LayoutInflater.from(context)
            .inflate(R.layout.layout_loading_dialog, null)
        val dialogBuilder: AlertDialog.Builder = AlertDialog.Builder(context)
        dialogBuilder.setView(dialogView)

    return dialogBuilder.create()
    }
}
