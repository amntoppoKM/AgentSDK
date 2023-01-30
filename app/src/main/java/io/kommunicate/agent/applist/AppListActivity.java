package io.kommunicate.agent.applist;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.applozic.mobicomkit.api.account.user.UserClientService;
import com.applozic.mobicomkit.channel.service.ChannelService;
import com.applozic.mobicomkit.uiwidgets.conversation.ConversationUIService;
import com.applozic.mobicomkit.uiwidgets.kommunicate.views.KmToast;
import com.applozic.mobicommons.json.GsonUtils;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import java.util.Map;
import java.util.Objects;

import io.kommunicate.Kommunicate;
import io.kommunicate.agent.AgentSharedPreference;
import io.kommunicate.agent.KmAssigneeListHelper;
import io.kommunicate.agent.KmLoginService;
import io.kommunicate.agent.MainActivity;
import io.kommunicate.agent.R;
import io.kommunicate.agent.conversations.activity.AllConversationActivity;
import io.kommunicate.agent.model.KmAgentUser;
import io.kommunicate.agent.viewmodels.KmAuthViewModel;
import io.kommunicate.callbacks.KMLogoutHandler;
import io.kommunicate.callbacks.KmCallback;
import io.kommunicate.users.KMUser;

public class AppListActivity extends AppCompatActivity implements KmCallback {

    public static final String SAML_URL = "samlURL";
    public static final String APPLICATION_ID = "applicationId";
    public static final String OAUTH_TOKEN = "oauthToken";
    private Integer groupId = 0;
    private String userName;
    private String password;
    private boolean isGoogleSignIn = false;
    private boolean isSSOSignIn = false;
    private WebView webView;
    private LinearLayout linearLayout;
    private KmAuthViewModel authViewModel;
    private AgentSharedPreference agentSharedPreference;
    private Map<String, String> appListMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_list);

        RecyclerView recyclerView = findViewById(R.id.kmApplistRecycler);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(linearLayoutManager);
        linearLayout = findViewById(R.id.mainAppListLayout);
        webView = findViewById(R.id.webView);
        authViewModel = new ViewModelProvider(this).get(KmAuthViewModel.class);
        agentSharedPreference = AgentSharedPreference.getInstance(this);


        if (getIntent() != null) {
            String samlURL = getIntent().getStringExtra(SAML_URL);
            userName = getIntent().getStringExtra(MainActivity.KM_USER_ID);
            groupId = getIntent().getIntExtra(ConversationUIService.GROUP_ID, 0);

            if (!TextUtils.isEmpty(samlURL)) {
                toggleWebView(true);
                loadSAMLRequest(samlURL);
            } else {
                appListMap = (Map<String, String>) GsonUtils.getObjectFromJson(getIntent().getStringExtra(MainActivity.KM_APP_LIST), Map.class);
                password = getIntent().getStringExtra(MainActivity.KM_USER_PASSWORD);
                isGoogleSignIn = getIntent().getBooleanExtra(MainActivity.IS_GOOGLE_SIGN_IN, false);
                isSSOSignIn = getIntent().getBooleanExtra(MainActivity.IS_SSO_LOGIN, false);
                AppListAdapter appListAdapter = new AppListAdapter(this, appListMap, this);
                recyclerView.setAdapter(appListAdapter);
            }
            //setting the user id for crash reporting
            FirebaseCrashlytics.getInstance().setUserId(userName);
            FirebaseCrashlytics.getInstance().setCustomKey("GoogleSignIn", isGoogleSignIn);
        }
    }

    private void toggleWebView(boolean showWebView) {
        linearLayout.setVisibility(showWebView ? View.GONE : View.VISIBLE);
        webView.setVisibility(showWebView ? View.VISIBLE : View.GONE);
    }

    private void loadSAMLRequest(String url) {
        toggleWebView(true);
        if (!TextUtils.isEmpty(url)) {
            webView.getSettings().setJavaScriptEnabled(true);
            webView.getSettings().setLoadWithOverviewMode(true);
            webView.getSettings().setUseWideViewPort(true);
            webView.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    checkURLAndLogin(url);
                    return false;
                }

                @Override
                public void onPageFinished(WebView view, final String url) {
                }
            });
            webView.loadUrl(url);
        }
    }

    @Override
    public void onBackPressed() {
        if (webView.getVisibility() == View.VISIBLE) {
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);

            alertDialog.setTitle(getString(R.string.warning));
            alertDialog.setMessage(getString(R.string.go_back));

            alertDialog.setPositiveButton(getString(R.string.yes_alert), (dialog, which) -> {
                if (webView.canGoBack()) {
                    webView.goBack();
                } else {
                    toggleWebView(false);
                }
            });
            alertDialog.setNegativeButton(getString(R.string.no_alert), (dialog, which) -> dialog.dismiss());
            alertDialog.show();
        } else if(AgentSharedPreference.getInstance(this).getAgentDetails() != null) {
            Intent intent = new Intent(this, AllConversationActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onSuccess(Object message) {
        String selectedAppId = (String) message;

        if (isSSOSignIn) {
            authViewModel.initSamlLogin(userName, selectedAppId).observe(this, kmSAMLResponseResource -> {
                if (kmSAMLResponseResource.isSuccess()) {
                    loadSAMLRequest(Objects.requireNonNull(kmSAMLResponseResource.getData()).getRedirectionUrl());
                } else {
                    KmToast.error(AppListActivity.this, kmSAMLResponseResource.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } else {

            KMUser user = new KMUser();
            user.setUserName(userName);
            user.setPassword(password);
            user.setApplicationId(selectedAppId);

            ProgressDialog dialog = new ProgressDialog(this);
            dialog.setMessage("Please wait...");
            dialog.setCancelable(false);
            dialog.show();

            //If agent clicks on "Switch Application"
            //TODO: Check compatibility with /loginv2 API 
            if (agentSharedPreference.getAgentDetails() != null) {
                if (selectedAppId.equals(agentSharedPreference.getAgentDetails().getApplicationId())) {
                    Intent intent = new Intent(this, AllConversationActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    if (dialog != null)
                        dialog.dismiss();
                    startActivity(intent);
                    return;
                }
                Kommunicate.logout(this, new KMLogoutHandler() {
                    @Override
                    public void onSuccess(Context context) {

                        new UserClientService(context).clearDataAndPreference();
                        KmAssigneeListHelper.clearAll();
                        if(appListMap != null && !appListMap.isEmpty()) {
                            AgentSharedPreference.getInstance(context).setAppList(appListMap);
                            AgentSharedPreference.getInstance(context).setMultipleApplication(true);
                         }

                        if (GoogleSignIn.getLastSignedInAccount(context) != null) {
                            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                    .requestEmail()
                                    .build();
                            GoogleSignInClient mGoogleSignInClient = GoogleSignIn.getClient(context, gso);

                            mGoogleSignInClient.signOut().addOnCompleteListener((Activity) context, new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    KmLoginService.loginUser(AppListActivity.this, user, isGoogleSignIn, false, dialog, groupId, true);
                                }
                            });
                        } else {
                            KmLoginService.loginUser(AppListActivity.this, user, isGoogleSignIn, false, dialog, groupId, true);
                        }
                    }

                    @Override
                    public void onFailure(Exception exception) {

                    }
                });
            } else {
                KmLoginService.loginUser(this, user, isGoogleSignIn, false, dialog, groupId, false);
            }
        }
    }

    @Override
    public void onFailure(Object error) {

    }

    void checkURLAndLogin(String url) {
        String[] splitUrls = url.split("&");
        String applicationId = null;
        String oauthToken = null;
        for (String s : splitUrls) {
            if (s.startsWith(APPLICATION_ID)) {
                applicationId = s.split("=")[1];
            }
            if (s.startsWith(OAUTH_TOKEN)) {
                oauthToken = s.split("=")[1];
            }
        }
        startLogin(oauthToken, applicationId);
    }

    private void startLogin(String oauthToken, String applicationId) {
        KmAgentUser kmUser = new KmAgentUser();
        kmUser.setApplicationId(applicationId);
        kmUser.setUserName(userName);

        if (!TextUtils.isEmpty(oauthToken)) {
            kmUser.setOauthToken(oauthToken);
        }
        if (!TextUtils.isEmpty(password)) {
            kmUser.setPassword(password);
        }

        ProgressDialog dialog = new ProgressDialog(this);
        dialog.setMessage(getString(R.string.km_please_wait_string));
        dialog.setCancelable(false);
        dialog.show();

        KmLoginService.loginUser(this, kmUser, isGoogleSignIn, oauthToken != null, dialog, groupId, false);
    }
}
