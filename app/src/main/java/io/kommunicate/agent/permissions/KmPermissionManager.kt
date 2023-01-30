package io.kommunicate.agent.permissions

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import com.applozic.mobicommons.commons.core.utils.PermissionsUtils
import com.applozic.mobicommons.commons.core.utils.Utils

class KmPermissionManager(private val activity: Activity) {
    private var permissionListener: KmPermissionListener? = null
    fun handlePermissionResults(requestCode: Int, permissions: Array<String?>, grantResults: IntArray) {
        if (permissionListener != null) {
            permissionListener!!.onAction(requestCode, PermissionsUtils.verifyPermissions(grantResults))
        }
    }

    fun checkForPermission(permissionCode: Int, permissionListener: KmPermissionListener) {
        this.permissionListener = permissionListener
        when (permissionCode) {

            PermissionsUtils.REQUEST_CAMERA -> if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                permissionListener.onAction(permissionCode, true)
            } else if (Utils.hasMarshmallow()) {
                ActivityCompat.requestPermissions(activity, PermissionsUtils.PERMISSION_CAMERA, permissionCode)
            }

            PermissionsUtils.REQUEST_STORAGE -> if (PermissionsUtils.checkSelfForStoragePermission(activity)) {
                permissionListener.onAction(permissionCode, true)
            } else if (Utils.hasMarshmallow()) {
                ActivityCompat.requestPermissions(activity, PermissionsUtils.PERMISSIONS_STORAGE, permissionCode)
            }
        }
    }

    interface PermissionHandler {
        fun handlePermissionResults(requestCode: Int, permissions: Array<String?>, grantResults: IntArray)
    }
}