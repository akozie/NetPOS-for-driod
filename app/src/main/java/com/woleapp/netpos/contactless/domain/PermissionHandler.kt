package com.woleapp.netpos.contactless.domain

import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.LifecycleOwner
import pub.devrel.easypermissions.EasyPermissions

interface PermissionHandler : EasyPermissions.PermissionCallbacks {
    fun checkForPermission(context: Context, perms: String): Boolean
    fun requestForPermission(
        host: LifecycleOwner,
        requestCode: Int,
        permissionRationale: String,
        permissionToRequest: String,
    )

    fun permissionHandler(
        host: LifecycleOwner,
        context: Context,
        permissionToRequest: String,
        permRequestCode: Int,
        @StringRes permRationaleStringId: Int,
        fn: () -> Unit,
    )
}
