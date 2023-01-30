package io.kommunicate.agent.fragments

import android.app.AlertDialog
import android.content.ClipData
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import com.applozic.mobicomkit.api.account.user.MobiComUserPreference
import com.applozic.mobicomkit.api.account.user.User
import com.applozic.mobicomkit.contact.AppContactService
import com.applozic.mobicomkit.uiwidgets.kommunicate.views.KmToast
import com.applozic.mobicomkit.uiwidgets.uilistener.MobicomkitUriListener
import com.applozic.mobicommons.ApplozicService
import com.applozic.mobicommons.commons.core.utils.PermissionsUtils
import com.applozic.mobicommons.commons.core.utils.Utils
import com.applozic.mobicommons.people.contact.Contact
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.kommunicate.agent.R
import io.kommunicate.agent.activities.KmUserProfileActivity
import io.kommunicate.agent.conversations.viewmodels.KmConversationViewModel
import io.kommunicate.agent.permissions.KmPermissionListener
import io.kommunicate.agent.permissions.KmPermissionManager
import io.kommunicate.agent.services.AgentClientService


class KmProfileImageOptionsFragment : BottomSheetDialogFragment() {

    companion object {
        const val REQUEST_CODE_ATTACH_PHOTO = 101
        const val REQUEST_CODE_TAKE_PHOTO = 102
        const val REQUEST_REMOVE_PHOTO = 103
        public const val FRAG_TAG = "KmProfileImageOptionsFragment"
    }

    private lateinit var kmPermissionManager: KmPermissionManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.km_profile_image_select_layout, container)

        optionsVisibilitySetUp(view)
        kmPermissionManager = KmPermissionManager(requireActivity())
        return view
    }

    private fun optionsVisibilitySetUp(view: View) {

        view.findViewById<LinearLayout>(R.id.remove_profile_photo).setOnClickListener {
            processDeletePhoto()
        }
        view.findViewById<LinearLayout>(R.id.gallery_option).setOnClickListener {
            processGalleryOption()
        }
        view.findViewById<LinearLayout>(R.id.camera_option).setOnClickListener {
            processCameraOption()
        }
    }

    private fun processDeletePhoto() {
        val alertDialog = AlertDialog.Builder(activity);
        alertDialog.setMessage(R.string.km_remove_profile_photo)
        alertDialog.setNegativeButton(R.string.cancel) { _, _ ->
            dismiss()
        }
        alertDialog.setPositiveButton(R.string.remove) { _, _ ->
            val user = KmConversationViewModel.getUser(context)
            user.imageLink = ""
            dismiss()

            (activity as KmUserProfileActivity).updateUserProfile(context, user)
        }
        alertDialog.show()
    }

    private fun processGalleryOption() {
        val contentChooserIntent = Intent(
            Intent.ACTION_PICK,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        )
        requireActivity().startActivityForResult(contentChooserIntent, REQUEST_CODE_ATTACH_PHOTO)
        dismissAllowingStateLoss()
    }

    private fun processCameraOption() {
        kmPermissionManager.checkForPermission(
            PermissionsUtils.REQUEST_CAMERA,
            object : KmPermissionListener {
                override fun onAction(code: Int, didGrant: Boolean) {
                    if (didGrant) {
                        openCamera()
                    } else {
                        KmToast.error(
                            ApplozicService.getAppContext(),
                            R.string.km_camera_permission_rejected,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    dismissAllowingStateLoss()
                }
            })
    }

    private fun openCamera() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        if (activity !is MobicomkitUriListener) {
            Utils.printLog(
                context,
                FRAG_TAG,
                "Activity must implement MobicomkitUriListener to get image file uri"
            )
            return
        }

        if (cameraIntent.resolveActivity(requireContext().packageManager) != null) {
            val capturedImageUri = (activity as MobicomkitUriListener?)!!.currentImageUri

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                cameraIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                cameraIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                val clip =
                    ClipData.newUri(requireActivity().contentResolver, "a Photo", capturedImageUri)
                cameraIntent.clipData = clip
                cameraIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                cameraIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } else {
                val resInfoList = requireActivity().packageManager
                    .queryIntentActivities(cameraIntent, PackageManager.MATCH_DEFAULT_ONLY)
                for (resolveInfo in resInfoList) {
                    val packageName = resolveInfo.activityInfo.packageName
                    requireActivity().grantUriPermission(
                        packageName, capturedImageUri,
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                    requireActivity().grantUriPermission(
                        packageName, capturedImageUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
            }
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, capturedImageUri)
            requireActivity().startActivityForResult(cameraIntent, REQUEST_CODE_TAKE_PHOTO)
        }
    }

    fun handlePermissionResults(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        kmPermissionManager.handlePermissionResults(requestCode, permissions, grantResults)
    }
}