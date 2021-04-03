package com.woleapp.netpos.util

import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import androidx.databinding.BindingAdapter
import com.netpluspay.nibssclient.util.formatCurrencyAmount
import com.woleapp.netpos.R

@BindingAdapter("processButtonState")
fun Button.processButtonState(transactionState: Int) {
    when (transactionState) {
        STATE_PAYMENT_STAND_BY -> {
            text = context.getString(R.string.proceess)
            isEnabled = true
        }
        STATE_PAYMENT_STARTED -> {
            text = context.getString(R.string.processing_transaction)
            isEnabled = false
        }
        STATE_PAYMENT_APPROVED -> {
            text = context.getString(R.string.payment_approved)
            isEnabled = false
        }
    }
}

@BindingAdapter("paymentProgress")
fun ProgressBar.paymentProgress(transactionState: Int) {
    visibility = if (transactionState == STATE_PAYMENT_STAND_BY)
        View.GONE
    else View.VISIBLE
}

@BindingAdapter("formatAmount")
fun EditText.formatAmount(amount: Long) {
    setText(amount.div(100).formatCurrencyAmount("\u20A6"))
}

@BindingAdapter("buttonInProgress")
fun Button.buttonInProgress(inpProgress: Boolean) {
    isEnabled = inpProgress.not()
}

@BindingAdapter("progressBarInProgress")
fun ProgressBar.progressBarInProgress(boolean: Boolean) {
    visibility = if (boolean)
        View.VISIBLE
    else View.GONE
}