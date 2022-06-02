package com.woleapp.netpos.contactless.util

import com.woleapp.netpos.contactless.R

data class DialogHelper(
    val dialogType: DialogType,
    val message: String,
    val actionName: String = "Retry",
    val action: (() -> Unit)? = null
)

enum class DialogType(val title: String, val icon: Int) {
    SUCCESS(
        "Success",
        R.drawable.ic_success
    ),
    FAILURE("Failure", R.drawable.ic_error),
    CONFIRMATION("Confirm", R.drawable.ic_warning)
}