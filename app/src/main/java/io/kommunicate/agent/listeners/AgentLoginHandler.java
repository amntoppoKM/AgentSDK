package io.kommunicate.agent.listeners;

import android.content.Context;

import com.applozic.mobicomkit.api.account.register.RegistrationResponse;

import java.util.Map;

import io.kommunicate.agent.KmAgentRegistrationResponse;

public interface AgentLoginHandler {
    void onSuccess(KmAgentRegistrationResponse registrationResponse, Context context);

    void onFailure(KmAgentRegistrationResponse registrationResponse, Exception exception);

    void onMultipleApp(Map<String, String> appList, Context context);
}
