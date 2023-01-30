package io.kommunicate.agent.exception;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.applozic.mobicomkit.api.account.user.UserClientService;
import com.applozic.mobicomkit.uiwidgets.kommunicate.views.KmToast;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import io.kommunicate.Kommunicate;
import io.kommunicate.agent.KmAssigneeListHelper;
import io.kommunicate.agent.R;
import io.kommunicate.callbacks.KMLogoutHandler;

public class KmExceptionHandle {
    private Context context;
    public static KmExceptionHandle kmExceptionHandle;
    private boolean loggingOut = false;

    private KmExceptionHandle(Context context) {
        this.context = context;
    }
    public static KmExceptionHandle getInstance(Context context) {
        if (kmExceptionHandle == null) {
            kmExceptionHandle = new KmExceptionHandle(context);
        }
        return kmExceptionHandle;
    }

    public void handleUnauthorizedAccess() {
        if(loggingOut) {
            return;
        }
        loggingOut = true;
        KmToast.error(context, context.getString(R.string.km_unauthorized), Toast.LENGTH_LONG).show();
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
                            loggingOut = false;
                        }
                    });
                } else {
                    loggingOut = false;
                }
                Intent intent;
                try {
                    intent = new Intent(context, Class.forName("io.kommunicate.agent.MainActivity"));
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    context.startActivity(intent);
                    ((AppCompatActivity) context).finish();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
            @Override
            public void onFailure(Exception exception) {
                loggingOut = false;
            }
        });
    }
}
