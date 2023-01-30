package io.kommunicate.agent.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.ResultReceiver;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;

import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.applozic.mobicomkit.api.account.user.User;
import com.applozic.mobicomkit.channel.service.ChannelService;
import com.applozic.mobicomkit.contact.AppContactService;
import com.applozic.mobicomkit.uiwidgets.conversation.ConversationUIService;
import com.applozic.mobicomkit.uiwidgets.conversation.fragment.MobiComConversationFragment;
import com.applozic.mobicomkit.uiwidgets.kommunicate.views.KmToast;
import com.applozic.mobicommons.ApplozicService;
import com.applozic.mobicommons.commons.core.utils.DateUtils;
import com.applozic.mobicommons.people.channel.Channel;
import com.applozic.mobicommons.people.contact.Contact;
import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;
import io.kommunicate.agent.KmUserInfoHelper;
import io.kommunicate.agent.R;
import io.kommunicate.agent.adapters.KmUserInfoAdapter;

public class KmUserInfoActivity extends AppCompatActivity {

    private CircleImageView kmProfileImageView;
    private TextView kmUserDisplayName;
    private TextView kmUserPresence;
    private TextView kmEmailIdTv;
    private TextView kmPhoneNumberTv;
    private RecyclerView kmUserInfoRecycler;
    private TextView kmExpandViewTv;
    private TextView kmDeleteConversationTv;
    private LinearLayout kmUserInfoLayout;
    private LinearLayout kmFillDetailsLayout;
    private ImageView kmFillDetailsIcon;
    private TextView kmFillDetailTv;
    private EditText kmFillDetailEditText;
    private ActionBar actionBar;
    private Contact contact;
    private Integer channelKey;
    private boolean isForOneToOneChat = false;
    private RelativeLayout emailIdLayout;
    private RelativeLayout phoneNumberLayout;
    private ResultReceiver resultReceiver;
    private LinearLayout initialPageLayout;
    private TextView initialPageValue;
    private String groupCreationUrl;
    private View initialPageDivider;
    private static final int EXPAND_VIEW_ITEM_THRESHOLD = 6;
    private static final String EXPANDED_TAG = "EXPANDED";
    private static final String MINIMISED_TAG = "MINIMISED";
    public static final String IS_FOR_ONE_TO_ONE_CHAT = "IS_FOR_ONE_TO_ONE_CHAT";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_km_user_info);

        kmProfileImageView = findViewById(R.id.kmUserProfileImage);
        kmUserDisplayName = findViewById(R.id.kmUserDisplayName);
        kmUserPresence = findViewById(R.id.kmUserPresence);
        kmEmailIdTv = findViewById(R.id.kmEmailIdTv);
        kmPhoneNumberTv = findViewById(R.id.kmPhoneNumberTv);
        kmUserInfoRecycler = findViewById(R.id.kmUserInfoRecycler);
        kmExpandViewTv = findViewById(R.id.kmExpandViewButton);
        kmDeleteConversationTv = findViewById(R.id.kmDeleteConversation);
        kmUserInfoLayout = findViewById(R.id.kmUserInfoLayout);
        kmFillDetailsLayout = findViewById(R.id.kmFillDetailsLayout);
        kmFillDetailsIcon = findViewById(R.id.kmFillDetailsIcon);
        kmFillDetailTv = findViewById(R.id.kmFillDetailsTv);
        kmFillDetailEditText = findViewById(R.id.kmDetailsEditText);
        emailIdLayout = findViewById(R.id.kmAddEmailLayout);
        phoneNumberLayout = findViewById(R.id.kmAddPhoneNumberLayout);
        initialPageLayout = findViewById(R.id.initialPageLayout);
        initialPageValue = findViewById(R.id.initialPageValue);
        initialPageDivider = findViewById(R.id.initialDivider);

        Toolbar mToolbar = findViewById(R.id.kmToolbar);

        setSupportActionBar(mToolbar);
        actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setTitle(getString(R.string.km_user_info_title));
        }

        String userId = null;
        if (getIntent() != null) {
            channelKey = getIntent().getIntExtra(ConversationUIService.GROUP_ID, 0);
            userId = getIntent().getStringExtra(ConversationUIService.USER_ID);
            resultReceiver = getIntent().getParcelableExtra("resultReceiver");
            isForOneToOneChat = getIntent().getBooleanExtra(IS_FOR_ONE_TO_ONE_CHAT, false);
            setContact(new AppContactService(this).getContactById(userId));
        }

        if (isForOneToOneChat) {
            kmDeleteConversationTv.setVisibility(View.GONE);
        }

        new MobiComConversationFragment.KMUserDetailTask(this, userId, new MobiComConversationFragment.KmUserDetailsCallback() {
            @Override
            public void hasFinished(Contact contact) {
                setContact(contact);
            }
        }).execute();
    }

    public void processUserDetail(Contact contact) {

        if (!TextUtils.isEmpty(contact.getEmailId())) {
            kmEmailIdTv.setText(contact.getEmailId());
            kmEmailIdTv.setTextColor(ApplozicService.getContext(this).getResources().getColor(R.color.km_user_info_email_id_text_color));
        } else {
            kmEmailIdTv.setText(R.string.km_add_email_id_text);
            kmEmailIdTv.setTextColor(ApplozicService.getContext(this).getResources().getColor(R.color.km_user_info_email_id_empty_text_color));
        }

        if (!TextUtils.isEmpty(contact.getContactNumber())) {
            kmPhoneNumberTv.setText(contact.getContactNumber());
            kmPhoneNumberTv.setTextColor(ApplozicService.getContext(this).getResources().getColor(R.color.km_user_info_email_id_text_color));
        } else {
            kmPhoneNumberTv.setText(R.string.km_add_phone_number_text);
            kmPhoneNumberTv.setTextColor(ApplozicService.getContext(this).getResources().getColor(R.color.km_user_info_email_id_empty_text_color));
        }

        if (!TextUtils.isEmpty(contact.getImageURL())) {
            try {
                Glide.with(this).load(contact.getImageURL()).into(kmProfileImageView);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            kmProfileImageView.setImageResource(R.drawable.km_ic_contact_picture_holo_light);
        }

        kmUserDisplayName.setText(contact.getDisplayName());
        if (contact.getLastSeenAt() != 0) {
            kmUserPresence.setText(ApplozicService.getContext(this).getString(R.string.subtitle_last_seen_at_time) + " " + DateUtils.getDateAndTimeForLastSeen(ApplozicService.getContext(this), contact.getLastSeenAt(), R.string.JUST_NOW, R.plurals.MINUTES_AGO, R.plurals.HOURS_AGO, R.string.YESTERDAY));
            kmUserPresence.setTextColor(ApplozicService.getContext(this).getResources().getColor(contact.isOnline() ? R.color.km_user_online_text_color : R.color.km_user_offline_text_color));
        }
        setExpandingView(contact.getMetadata());

        Channel channel = ChannelService.getInstance(this).getChannelByChannelKey(channelKey);
        if(channel != null && channel.getMetadata() != null && channel.getMetadata().containsKey("GROUP_CREATION_URL")) {
            initialPageLayout.setVisibility(View.VISIBLE);
            groupCreationUrl = channel.getMetadata().get("GROUP_CREATION_URL");
            String url = channel.getMetadata().get("GROUP_CREATION_URL").replace("https://", "").replace("http://", "");
            initialPageValue.setText(url);
        }
        else {
            initialPageDivider.setVisibility(View.GONE);
            initialPageLayout.setVisibility(View.GONE);
        }

        if (!isForOneToOneChat) {
            handleClickListeners(contact);
        }
    }
    public void changePhoneFragment(Contact contact) {
        BottomSheetDialog sendPhoneDialog = new BottomSheetDialog(this);
        View phoneView = LayoutInflater.from(this).inflate(R.layout.km_edit_user_info, null, false);
        sendPhoneDialog.setContentView(phoneView);
        Button addNumberButton = phoneView.findViewById(R.id.km_add_number_button);
        TextView numberText = phoneView.findViewById(R.id.km_enter_phone_number_text);
        EditText numberEditText = phoneView.findViewById(R.id.km_edit_phone_number_text);
        ImageButton closeButton = phoneView.findViewById(R.id.km_close_phone);
        Button cancelButton = phoneView.findViewById(R.id.km_cancel_phone_button);
        sendPhoneDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        sendPhoneDialog.show();
        closeButton.setOnClickListener(view -> {
            sendPhoneDialog.dismiss();
        });
        cancelButton.setOnClickListener(view -> {
            sendPhoneDialog.dismiss();
        });

        numberText.setText(getString(R.string.km_enter_phone_number));

        addNumberButton.setOnClickListener(view -> {
                processContactUpdate(numberEditText, true);
            sendPhoneDialog.dismiss();

            });

    }


    public void changeEmailFragment(Contact contact) {
        BottomSheetDialog sendEmailDialog = new BottomSheetDialog(this);
        View emailView = LayoutInflater.from(this).inflate(R.layout.km_edit_user_info_email, null, false);
        sendEmailDialog.setContentView(emailView);
        Button addEmailButton = emailView.findViewById(R.id.km_add_email_button);
        TextView emailText = emailView.findViewById(R.id.km_enter_email_id_text);
        EditText emailEditText = emailView.findViewById(R.id.km_edit_email_id_text);
        ImageButton closeButton = emailView.findViewById(R.id.km_close_email);
        Button cancelButton = emailView.findViewById(R.id.km_cancel_email_button);
        sendEmailDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        sendEmailDialog.show();
        closeButton.setOnClickListener(view -> {
            sendEmailDialog.dismiss();
        });
        cancelButton.setOnClickListener(view -> {
            sendEmailDialog.dismiss();
        });

        emailText.setText(getString(R.string.km_enter_email_id));

        addEmailButton.setOnClickListener(view -> {
            processContactUpdate(emailEditText, false);
            sendEmailDialog.dismiss();

        });

    }




    public void saveUserDetails(Contact contact, boolean isForPhoneNumber) {
        kmFillDetailsIcon.setImageResource(isForPhoneNumber ? R.drawable.km_phone_large_icon : R.drawable.km_email_large_icon);
        kmFillDetailTv.setText(isForPhoneNumber ? R.string.km_fill_detail_phone_text : R.string.km_fill_detail_email_text);
        if (contact != null) {
            if (isForPhoneNumber) {
                kmFillDetailEditText.setInputType(InputType.TYPE_CLASS_PHONE);
                if (!TextUtils.isEmpty(contact.getContactNumber())) {
                    kmFillDetailEditText.setText(contact.getContactNumber());
                } else {
                    kmFillDetailEditText.setText("");
                    kmFillDetailEditText.setHint(R.string.km_fill_detail_phone_hint_text);
                }
            } else {
                kmFillDetailEditText.setInputType(InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS);
                if (!TextUtils.isEmpty(contact.getEmailId())) {
                    kmFillDetailEditText.setText(contact.getEmailId());
                } else {
                    kmFillDetailEditText.setText("");
                    kmFillDetailEditText.setHint(R.string.km_fill_detail_email_hint_text);
                }
            }
        }
    }

    public void handleClickListeners(final Contact contact) {
        kmDeleteConversationTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (channelKey > 0) {
                    KmUserInfoHelper.processDeleteConversation(KmUserInfoActivity.this, channelKey);
                }
            }
        });

        kmPhoneNumberTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changePhoneFragment(contact);
            }
        });

        kmEmailIdTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeEmailFragment(contact);
            }
        });

        emailIdLayout.setOnClickListener((v) -> {
            changeEmailFragment(contact);
        });

        phoneNumberLayout.setOnClickListener((v) -> {
            changePhoneFragment(contact);
        });

        if(groupCreationUrl != null && !TextUtils.isEmpty(groupCreationUrl)) {
            initialPageValue.setOnClickListener((v) -> {
                Intent intent = new Intent(Intent.ACTION_VIEW).setData(Uri.parse(groupCreationUrl));
                startActivity(intent);
            });
        }
    }

    public void setExpandingView(final Map<String, String> kmUserInfoMap) {
        final KmUserInfoAdapter kmUserInfoAdapter = new KmUserInfoAdapter(this, kmUserInfoMap);
        if (kmUserInfoMap != null) {
            if (kmUserInfoMap.size() > EXPAND_VIEW_ITEM_THRESHOLD) {
                kmExpandViewTv.setVisibility(View.VISIBLE);
                kmExpandViewTv.setTag(MINIMISED_TAG);
                kmUserInfoAdapter.setDataKeySet(new ArrayList<>(kmUserInfoMap.keySet()).subList(0, EXPAND_VIEW_ITEM_THRESHOLD));
            } else {
                kmExpandViewTv.setVisibility(View.GONE);
                kmUserInfoAdapter.setDataKeySet(new ArrayList<>(kmUserInfoMap.keySet()));
            }

            LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
            linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
            kmUserInfoRecycler.setLayoutManager(linearLayoutManager);
            kmUserInfoRecycler.setAdapter(kmUserInfoAdapter);

            kmExpandViewTv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (MINIMISED_TAG.equals(kmExpandViewTv.getTag())) {
                        kmUserInfoAdapter.setDataKeySet(new ArrayList<>(kmUserInfoMap.keySet()));
                        kmUserInfoAdapter.notifyDataSetChanged();
                        updateExpandingText(kmExpandViewTv, false);
                        kmExpandViewTv.setTag(EXPANDED_TAG);
                    } else if (EXPANDED_TAG.equals(kmExpandViewTv.getTag())) {
                        kmUserInfoAdapter.setDataKeySet(new ArrayList<>(kmUserInfoMap.keySet()).subList(0, EXPAND_VIEW_ITEM_THRESHOLD));
                        kmUserInfoAdapter.notifyDataSetChanged();
                        updateExpandingText(kmExpandViewTv, true);
                        kmExpandViewTv.setTag(MINIMISED_TAG);
                    }
                }
            });
        }
    }

    public void updateExpandingText(TextView kmExpandViewTv, boolean isSeeMore) {
        if (kmExpandViewTv != null) {
            kmExpandViewTv.setText(isSeeMore ? R.string.km_see_more_text : R.string.km_see_less_text);
            kmExpandViewTv.setCompoundDrawablesWithIntrinsicBounds(null, null, ApplozicService.getContext(this).getResources().getDrawable(isSeeMore ? R.drawable.km_expand_view_icon : R.drawable.km_see_less_icon), null);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.km_user_info_menu, menu);
        MenuItem item = menu.findItem(R.id.kmSaveOption);
        item.setVisible(showSaveOption());
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (R.id.kmSaveOption == item.getItemId()) {
            if (kmFillDetailEditText != null) {
                processContactUpdate(kmFillDetailEditText, isForPhoneNumber());
            }
        }
        return super.onOptionsItemSelected(item);
    }

    public void processContactUpdate(EditText editText, boolean isForPhoneNumber) {
        String data = editText.getText().toString();

        if (isForPhoneNumber && !KmUserInfoHelper.isValidPhone(data) || !isForPhoneNumber && !KmUserInfoHelper.isValidEmail(data)) {

            KmToast.error(KmUserInfoActivity.this, (getString(isForPhoneNumber ? R.string.km_invalid_phone_error : R.string.km_invalid_email_error)), Toast.LENGTH_SHORT).show();
            editText.setError(getString(isForPhoneNumber ? R.string.km_invalid_phone_error : R.string.km_invalid_email_error));
        } else if (getContact() != null) {
            User user = new User();
            user.setUserId(getContact().getUserId());
            if (isForPhoneNumber) {
                user.setContactNumber(data);
            } else {
                user.setEmail(data);
            }
            KmUserInfoHelper.updateUserDetails(this, user);

        }
    }

    public void toggleUserInfoLayout(boolean showUserInfoLayout, Contact contact, boolean isForPhoneNumber) {
        kmUserInfoLayout.setVisibility(showUserInfoLayout ? View.VISIBLE : View.GONE);
        kmFillDetailsLayout.setVisibility(showUserInfoLayout ? View.GONE : View.VISIBLE);

        if (actionBar != null) {
            actionBar.setTitle(showUserInfoLayout ? getString(R.string.km_user_info_title) : "");
        }
        if (!showUserInfoLayout) {
            saveUserDetails(contact, isForPhoneNumber);
        }

        invalidateOptionsMenu();
    }

    private boolean showSaveOption() {
        return kmFillDetailsLayout.getVisibility() == View.VISIBLE && kmUserInfoLayout.getVisibility() == View.GONE;
    }

    private boolean isForPhoneNumber() {
        return kmFillDetailTv != null && kmFillDetailTv.getText().toString().equals(getString(R.string.km_fill_detail_phone_text));
    }

    @Override
    public void onBackPressed() {
        if (showSaveOption()) {
            toggleUserInfoLayout(true, null, false);
            return;
        }
        super.onBackPressed();
    }

    @Override
    public boolean onNavigateUp() {
        onBackPressed();
        return super.onNavigateUp();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }

    public void setContact(Contact contact) {
        this.contact = contact;
        if (contact != null) {
            processUserDetail(contact);
        }
    }

    public Contact getContact() {
        return contact;
    }
}
