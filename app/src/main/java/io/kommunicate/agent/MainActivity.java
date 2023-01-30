package io.kommunicate.agent;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import com.applozic.mobicomkit.api.account.user.UserClientService;
import com.applozic.mobicomkit.channel.service.ChannelService;
import com.applozic.mobicomkit.uiwidgets.conversation.ConversationUIService;
import com.applozic.mobicomkit.uiwidgets.kommunicate.views.KmToast;
import com.applozic.mobicommons.ALSpecificSettings;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import io.kommunicate.Kommunicate;
import io.kommunicate.agent.fragments.MainLoginFragment;
import io.kommunicate.agent.fragments.SSOLoginFragment;
import io.kommunicate.callbacks.KMLogoutHandler;
import io.kommunicate.users.KMUser;
import io.sentry.Sentry;
import io.sentry.protocol.User;

public class MainActivity extends AppCompatActivity implements LoginListener {

    private ImageView kmMainLogoIv;
    private ProgressDialog dialog;
    private Spinner mUrlSpinner;
    private int touchCount = 0;
    public static final String KM_APP_LIST = "KM_APP_LIST";
    public static final String KM_USER_PASSWORD = "KM_USER_PASSWORD";
    public static final String KM_USER_ID = "KM_USER_ID";
    public static final String IS_GOOGLE_SIGN_IN = "IS_GOOGLE_SIGN_IN";
    public static final String IS_SSO_LOGIN = "IS_SSO_LOGIN";
    Integer groupId = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        replaceFragment(new MainLoginFragment());
        ALSpecificSettings.getInstance(this).setDatabaseName("APPLOZIC_DATA_BASE");

        kmMainLogoIv = findViewById(R.id.kmMainLogoIv);
        mUrlSpinner = findViewById(R.id.mUrlSpinner);

        if (getIntent() != null) {
            groupId = getIntent().getIntExtra(ConversationUIService.GROUP_ID, 0);
        }

        kmMainLogoIv.setOnClickListener(v -> {
            touchCount += 1;
            if (touchCount == 5) {
                mUrlSpinner.setVisibility(View.VISIBLE);
                touchCount = 0;
            } else {
                KmToast.success(getApplicationContext(), getBaseContext().getString(R.string.click_more) + (5 - touchCount), Toast.LENGTH_SHORT).show();
            }
        });

        mUrlSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                ALSpecificSettings.getInstance(MainActivity.this).setAlBaseUrl(getResources().getStringArray(R.array.select_al_url)[i]);
                ALSpecificSettings.getInstance(MainActivity.this).setKmBaseUrl(getResources().getStringArray(R.array.select_km_url)[i]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
    }

    private void replaceFragment(Fragment fragment) {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();

        if (!fragment.isAdded() && !isFinishing()) {
            fragmentTransaction.replace(R.id.container_layout, fragment);
            fragmentTransaction.addToBackStack(fragment.getTag());
            fragmentTransaction.commit();
            getSupportFragmentManager().executePendingTransactions();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent != null) {
            groupId = intent.getIntExtra(ConversationUIService.GROUP_ID, 0);
        }
    }

    public static void performLogout(Context context, final Object object) {
        final ProgressDialog dialog = new ProgressDialog(context);
        dialog.setMessage("Logging out, please wait...");
        dialog.setCancelable(false);
        dialog.show();

        Kommunicate.logout(context, new KMLogoutHandler() {
            @Override
            public void onSuccess(Context context) {

                new UserClientService(context).clearDataAndPreference();
                KmAssigneeListHelper.clearAll();
                if (GoogleSignIn.getLastSignedInAccount(context) != null) {
                    GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestEmail()
                            .build();
                    GoogleSignInClient mGoogleSignInClient = GoogleSignIn.getClient(context, gso);

                    mGoogleSignInClient.signOut().addOnCompleteListener((Activity) context, new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            dialog.dismiss();
                        }
                    });
                } else {
                    dialog.dismiss();
                }

                if (TextUtils.isEmpty((String) object)) {
                    return;
                }
                KmToast.success(context, context.getString(R.string.user_logout_info), Toast.LENGTH_SHORT).show();
                Intent intent;
                try {
                    intent = new Intent(context, Class.forName((String) object));
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    context.startActivity(intent);
                    ((AppCompatActivity) context).finish();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Exception exception) {
                dialog.dismiss();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() == 1) {
            finish();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void processAppList(final String email, final String password, final boolean isGoogleSignIn) {
        dialog = new ProgressDialog(MainActivity.this);
        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();
        User user = new User();
        user.setId(email);
        Sentry.setUser(user);
        KMUser kmuser = new KMUser();
        if (TextUtils.isEmpty(ALSpecificSettings.getInstance(this).getDatabaseName())) {
            ALSpecificSettings.getInstance(this).setDatabaseName("APPLOZIC_DATA_BASE");
        }
        kmuser.setUserName(email);
        kmuser.setPassword(password);
        KmLoginService. loginUser(this, kmuser, isGoogleSignIn, false, dialog, groupId, false);
    }

    @Override
    public void openSSOPage() {
        replaceFragment(new SSOLoginFragment());
    }
}
