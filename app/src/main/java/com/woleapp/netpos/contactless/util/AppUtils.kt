package com.woleapp.netpos.contactless.util

import android.app.Activity
import android.content.Context
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import com.pixplicity.easyprefs.library.Prefs
import com.woleapp.netpos.contactless.R
import com.woleapp.netpos.contactless.util.RandomPurposeUtil.showSnackBar
import pub.devrel.easypermissions.EasyPermissions

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

private fun checkForPermission(context: Context, perms: String) =
    EasyPermissions.hasPermissions(
        context,
        perms
    )

fun genericPermissionHandler(
    host: LifecycleOwner,
    context: Context,
    perm: String,
    permCode: Int,
    permRationale: String,
    fn: () -> Unit
) {
    if (checkForPermission(context, perm)) {
        fn()
    } else {
        requestForPermission(
            host,
            permCode,
            permRationale,
            perm
        )
    }
}

private fun requestForPermission(
    host: LifecycleOwner,
    requestCode: Int,
    permissionRationale: String,
    permissionToRequest: String
) {
    if (host is Fragment) {
        EasyPermissions.requestPermissions(
            host,
            permissionRationale,
            requestCode,
            permissionToRequest
        )
    } else {
        host as Activity
        EasyPermissions.requestPermissions(
            host,
            permissionRationale,
            requestCode,
            permissionToRequest
        )
    }
}
