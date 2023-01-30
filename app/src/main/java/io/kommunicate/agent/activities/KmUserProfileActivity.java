package io.kommunicate.agent.activities;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProviders;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import com.applozic.mobicomkit.api.MobiComKitConstants;
import com.applozic.mobicomkit.api.account.user.User;
import com.applozic.mobicomkit.api.attachment.FileClientService;
import com.applozic.mobicomkit.listners.AlCallback;
import com.applozic.mobicomkit.uiwidgets.kommunicate.views.KmToast;
import com.applozic.mobicomkit.uiwidgets.uilistener.MobicomkitUriListener;
import com.applozic.mobicommons.ApplozicService;
import com.applozic.mobicommons.commons.core.utils.Utils;
import com.bumptech.glide.Glide;
import com.canhub.cropper.CropImage;
import com.canhub.cropper.CropImageContract;
import com.canhub.cropper.CropImageContractOptions;
import com.canhub.cropper.CropImageOptions;
import com.canhub.cropper.CropImageView;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import io.kommunicate.agent.R;
import io.kommunicate.agent.asyncs.AgentDetailsUpdateTask;
import io.kommunicate.agent.asyncs.KmProfileImageUploadTask;
import io.kommunicate.agent.conversations.viewmodels.KmConversationViewModel;
import io.kommunicate.agent.databinding.ActivityKmUserProfileBinding;
import io.kommunicate.agent.exception.KmExceptionAnalytics;
import io.kommunicate.agent.fragments.KmProfileImageOptionsFragment;
import io.kommunicate.callbacks.KmCallback;

public class KmUserProfileActivity extends AppCompatActivity implements MobicomkitUriListener{

    private static final String TAG = "KmUserProfileActivity";
    KmConversationViewModel conversationViewModel;
    ActivityKmUserProfileBinding binding;
    private File profilePhotoFile;
    private Uri imageUri;
    private boolean isImageFromGallery;
    KmProfileImageOptionsFragment profileImageOptionsFragment;
    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_km_user_profile);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_km_user_profile);
        Toolbar toolbar = findViewById(R.id.km_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }
        conversationViewModel = ViewModelProviders.of(this).get(KmConversationViewModel.class);
        loadUserDetails();
    }

    private void loadUserDetails() {
        User user = KmConversationViewModel.getUser(this);
        if (user != null && binding != null) {
            binding.setContact(user);
            if (!TextUtils.isEmpty(user.getImageLink())) {
                Glide.with(KmUserProfileActivity.this).load(user.getImageLink()).placeholder(R.drawable.km_image_loader).into(binding.kmUserProfileImage);
            } else {
                binding.kmUserProfileImage.setImageDrawable(getResources().getDrawable(R.drawable.ic_edit_profile));
            }

            if (user.getMetadata() != null && user.getMetadata().containsKey(KmConversationViewModel.DESIGNATION)) {
                binding.kmEditDesignationField.setText(user.getMetadata().get(KmConversationViewModel.DESIGNATION));
            }
            binding.kmEditEmailField.setEnabled(false);
        }
    }

    public void openImageModeSelectDialog(View view) {
        profileImageOptionsFragment = new KmProfileImageOptionsFragment();
        profileImageOptionsFragment.show(getSupportFragmentManager(), KmProfileImageOptionsFragment.FRAG_TAG);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }

    public void updateUser(View view) {
        if (binding != null) {
            User user = KmConversationViewModel.getUser(this);
            String designation = null;
            String displayName = null;
            String email = null;
            String contactNumber = null;

            if (binding.kmEditNameField.getText() != null) {
                displayName = binding.kmEditNameField.getText().toString();
            }
            if (binding.kmEditEmailField.getText() != null) {
                email = binding.kmEditEmailField.getText().toString();
            }
            if (binding.kmEditContactNumberField.getText() != null) {
                contactNumber = binding.kmEditContactNumberField.getText().toString();
            }
            if (binding.kmEditDesignationField.getText() != null) {
                designation = binding.kmEditDesignationField.getText().toString();
            }
            user.setDisplayName(displayName);
            user.setEmail(email);
            user.setContactNumber(contactNumber);
            Map<String, String> metadata = user.getMetadata();
            if (metadata == null) {
                metadata = new HashMap<>();
            }
            if (!TextUtils.isEmpty(designation)) {
                metadata.put(KmConversationViewModel.DESIGNATION, designation);
            }
            user.setMetadata(metadata);
            updateUserProfile(this, user);
        }
    }

    public void updateUserProfile(Context context, User user) {
        if(user == null) {
            return;
        }
        ProgressDialog dialog = new ProgressDialog(context);
        dialog.setCancelable(false);
        dialog.setMessage(Utils.getString(context, R.string.please_wait_info));
        dialog.show();

        new AgentDetailsUpdateTask(ApplozicService.getContext(context), user, new AlCallback() {
            @Override
            public void onSuccess(Object response) {
                dialog.dismiss();
                loadUserDetails();
            }

            @Override
            public void onError(Object error) {
                dialog.dismiss();
                KmToast.error(getApplicationContext(), Utils.getString(getApplicationContext(), R.string.km_update_failed_message), Toast.LENGTH_LONG).show();
            }
        }).execute();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case KmProfileImageOptionsFragment.REQUEST_CODE_ATTACH_PHOTO:
                Uri selectedFileUri = (data == null ? null : data.getData());
                isImageFromGallery = true;
                beginCrop(selectedFileUri);
                break;

            case KmProfileImageOptionsFragment.REQUEST_CODE_TAKE_PHOTO:
                beginCrop(imageUri);
                break;
        }
    }

    void handleProfileImageUpload(boolean isSaveFile, Uri imageUri, File file) {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
        progressDialog.setMessage(Utils.getString(this, R.string.please_wait_info));
        progressDialog.show();
        new KmProfileImageUploadTask(isSaveFile, imageUri, file, this, new KmCallback() {
            @Override
            public void onSuccess(Object message) {
                if (progressDialog != null) {
                    progressDialog.dismiss();
                }
                loadUserDetails();
            }

            @Override
            public void onFailure(Object error) {
                if (progressDialog != null) {
                    progressDialog.dismiss();
                }
            }
        }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }


    ActivityResultLauncher<CropImageContractOptions> cropImage = registerForActivityResult(new CropImageContract(), new ActivityResultCallback<CropImageView.CropResult>() {
        @Override
        public void onActivityResult(CropImageView.CropResult result) {
            if (result.isSuccessful()) {
                if (result == null) {
                    return;
                }
                    imageUri = result.getUriContent();
                if(isImageFromGallery) {
                    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                    String imageFileName = "JPEG_" + timeStamp + "_" + ".jpeg";
                    profilePhotoFile = FileClientService.getFilePath(imageFileName, KmUserProfileActivity.this, "image/jpeg");
                }
                if (imageUri != null) {
                    handleProfileImageUpload(true, imageUri, profilePhotoFile);
                }
            } else{
                Utils.printLog(KmUserProfileActivity.this, TAG, "Cropping failed: " + (result != null ? result.getError() : null));
            }
        }
    });

    void beginCrop(Uri imageUri) {
        try {
           CropImageContractOptions options = new CropImageContractOptions(imageUri, new CropImageOptions());
           options.setGuidelines(CropImageView.Guidelines.OFF);
           cropImage.launch(options);
        } catch (Exception e) {
            KmExceptionAnalytics.captureException(e);
            e.printStackTrace();
        }
    }

    @Override
    public Uri getCurrentImageUri() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_" + ".jpeg";
        profilePhotoFile = FileClientService.getFilePath(imageFileName, getApplicationContext(), "image/jpeg");
        imageUri = FileProvider.getUriForFile(this, Utils.getMetaDataValue(this, MobiComKitConstants.PACKAGE_NAME) + ".provider", profilePhotoFile);
        return imageUri;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        profileImageOptionsFragment.handlePermissionResults(requestCode, permissions, grantResults);
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}