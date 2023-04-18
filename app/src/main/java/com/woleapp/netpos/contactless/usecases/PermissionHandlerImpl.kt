package com.woleapp.netpos.contactless.usecases

import android.app.Activity
import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.LifecycleOwner
import com.woleapp.netpos.contactless.domain.PermissionHandler
import pub.devrel.easypermissions.EasyPermissions

class PermissionHandlerImpl : PermissionHandler {
    override fun checkForPermission(context: Context, perms: String): Boolean =
        EasyPermissions.hasPermissions(
            context,
            perms,
        )

    override fun requestForPermission(
        host: LifecycleOwner,
        requestCode: Int,
        permissionRationale: String,
        permissionToRequest: String,
    ) {
        EasyPermissions.requestPermissions(
            host as Activity,
            permissionRationale,
            requestCode,
            permissionToRequest,
        )
    }

    override fun permissionHandler(
        host: LifecycleOwner,
        context: Context,
        permissionToRequest: String,
        permRequestCode: Int,
        @StringRes permRationaleStringId: Int,
        fn: () -> Unit,
    ) {
        if (checkForPermission(context, permissionToRequest)) {
            fn()
        } else {
            requestForPermission(
                host,
                permRequestCode,
                context.resources.getString(permRationaleStringId),
                permissionToRequest,
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
    }
}
