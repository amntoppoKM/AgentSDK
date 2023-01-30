package io.kommunicate.agent.applist;

import android.app.ProgressDialog;
import android.content.Context;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.appcompat.widget.AppCompatButton;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.applozic.mobicomkit.uiwidgets.kommunicate.views.KmToast;

import java.util.Map;

import io.kommunicate.agent.R;
import io.kommunicate.agent.asyncs.KmGetAppListTask;
import io.kommunicate.agent.asyncs.KmUserPasswordResetTask;
import io.kommunicate.agent.asyncs.KmUserPasswordResetTask.KmPassResetHandler;

public class KmForgotPasswordActivity extends AppCompatActivity {

    private LinearLayout forgotPasswordLayout;
    private LinearLayout resetPassworLayout;
    private EditText registeredEmailIdEt;
    private AppCompatButton sendInstructionsBt;
    private AppCompatButton backToLoginBt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_km_forgot_password);

        forgotPasswordLayout = findViewById(R.id.forgotPasswordLayout);
        resetPassworLayout = findViewById(R.id.resetConfirmationLayout);
        registeredEmailIdEt = findViewById(R.id.registered_email_id);
        sendInstructionsBt = findViewById(R.id.sendInstructionButton);
        backToLoginBt = findViewById(R.id.backToLoginButton);

        sendInstructionsBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (!TextUtils.isEmpty(registeredEmailIdEt.getText().toString())) {

                    final ProgressDialog dialog = new ProgressDialog(KmForgotPasswordActivity.this);
                    dialog.setMessage("Please Wait...");
                    dialog.setCancelable(false);
                    dialog.show();

                    KmGetAppListTask.AppListHandler handler = new KmGetAppListTask.AppListHandler() {
                        @Override
                        public void onSuccess(Context context, Map<String, String> s) {
                            if (s != null && !s.isEmpty()) {
                                String appKey = s.keySet().toArray()[0].toString();

                                KmPassResetHandler handler = new KmPassResetHandler() {

                                    @Override
                                    public void onSuccess(Context context, String response) {
                                        dialog.dismiss();
                                        forgotPasswordLayout.setVisibility(View.GONE);
                                        resetPassworLayout.setVisibility(View.VISIBLE);
                                    }

                                    @Override
                                    public void onFailure(Context context, String error) {
                                        dialog.dismiss();
                                    }
                                };

                                new KmUserPasswordResetTask(KmForgotPasswordActivity.this, registeredEmailIdEt.getText().toString(), appKey, handler).execute();
                            }
                        }

                        @Override
                        public void onFailure(Context context, String error) {
                            dialog.dismiss();
                            KmToast.error(KmForgotPasswordActivity.this, error, Toast.LENGTH_LONG).show();
                        }
                    };

                    new KmGetAppListTask(KmForgotPasswordActivity.this, handler, registeredEmailIdEt.getText().toString()).execute();
                } else {
                    KmToast.error(KmForgotPasswordActivity.this, R.string.km_email_field_blank_error, Toast.LENGTH_SHORT).show();
                }
            }
        });

        backToLoginBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }
}
