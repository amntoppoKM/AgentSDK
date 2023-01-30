package io.kommunicate.agent;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.applozic.mobicomkit.api.account.user.MobiComUserPreference;
import com.applozic.mobicommons.ApplozicService;
import com.applozic.mobicommons.data.AlPrefSettings;
import com.applozic.mobicommons.data.SecureSharedPreferences;
import com.applozic.mobicommons.json.GsonUtils;

import java.util.Map;

import io.kommunicate.agent.asyncs.AgentGetStatusTask;

/**
 * shared preference singleton wrapper for the agent app
 * TODO: make this a sub class of {@link com.applozic.mobicomkit.api.account.user.MobiComUserPreference} once inheritance is allowed
 *
 * @author shubham
 * 7th May, 2020
 */
public class AgentSharedPreference {

    private static String JWT_TOKEN = "jwt-token";
    private static String AGENT_DETAILS = "agentDetails";
    private static String AGENT_ROLE_TYPE = "agentRoleType";
    private static String AGENT_ROUTING = "agentRouting";
    private static String GOOGLE_SIGNIN = "googleSignin";
    private static String SSO_SIGNIN = "SsoSignin";
    private static String TRIAL_EXPIRED = "trialExpired";
    private static String MULTIPLE_APPS = "kmMultipleApps";
    private static String LOG_DELETED_AT_TIME = "kmLogDeletedAt";
    private static String APP_LIST = "applicationList";
    private static AgentSharedPreference agentSharedPreference;
    private static SharedPreferences sharedPreferences;
    private static SecureSharedPreferences secureSharedPreferences;
    private static String decodedToken;

    private AgentSharedPreference(Context context) {
        context = ApplozicService.getContext(context);
        ApplozicService.initWithContext(context);
        MobiComUserPreference.renameSharedPrefFile(context);
        sharedPreferences = context.getSharedPreferences(MobiComUserPreference.AL_USER_PREF_KEY, Context.MODE_PRIVATE);
        secureSharedPreferences = new SecureSharedPreferences(AlPrefSettings.AL_PREF_SETTING_KEY, ApplozicService.getContext(context));
        moveKeysToSecured();
    }

    public static AgentSharedPreference getInstance(Context context) {
        if (agentSharedPreference == null) {
            agentSharedPreference = new AgentSharedPreference(ApplozicService.getContext(context));
        }
        return agentSharedPreference;
    }

    public void moveKeysToSecured() {
        if (sharedPreferences.contains(JWT_TOKEN)) {
            setJwtToken(sharedPreferences.getString(JWT_TOKEN, null));
            sharedPreferences.edit().remove(JWT_TOKEN).commit();
        }
    }

    public void setJwtToken(String jwtToken) {
        decodedToken = jwtToken;
        secureSharedPreferences.edit().putString(JWT_TOKEN, jwtToken).commit();
    }

    public String getJwtToken() {
        if (TextUtils.isEmpty(decodedToken)) {
            decodedToken = secureSharedPreferences.getString(JWT_TOKEN, null);
        }
        return decodedToken;
    }

    public void setAgentDetails(AgentGetStatusTask.AgentDetail agentDetails) {
        if (agentDetails != null) {
            sharedPreferences.edit().putString(AGENT_DETAILS, GsonUtils.getJsonFromObject(agentDetails, AgentGetStatusTask.AgentDetail.class)).apply();
        }
    }

    public void setAgentRoleType(Short roleType) {
        sharedPreferences.edit().putString(AGENT_ROLE_TYPE, String.valueOf(roleType)).apply();
    }

    public Short getAgentRoleType() {
        return Short.valueOf(sharedPreferences.getString(AGENT_ROLE_TYPE, "8"));
    }

    public AgentGetStatusTask.AgentDetail getAgentDetails() {
        return (AgentGetStatusTask.AgentDetail) GsonUtils.getObjectFromJson(sharedPreferences.getString(AGENT_DETAILS, null), AgentGetStatusTask.AgentDetail.class);
    }

    public void setAgentRouting(Boolean routing) {
        sharedPreferences.edit().putBoolean(AGENT_ROUTING, routing)  .apply();
    }

    public Boolean isAgentRouting() {
        return sharedPreferences.getBoolean(AGENT_ROUTING, true);
    }

    public void setGoogleLogin(Boolean isGoogleLogin) {
        sharedPreferences.edit().putBoolean(GOOGLE_SIGNIN, isGoogleLogin).apply();
    }

    public Boolean isGoogleLogin() {
        return sharedPreferences.getBoolean(GOOGLE_SIGNIN, false);
    }

    public void setSSOLogin(Boolean isSSOlogin) {
        sharedPreferences.edit().putBoolean(SSO_SIGNIN, isSSOlogin).apply();
    }

    public Boolean isSSOLogin() {
        return sharedPreferences.getBoolean(SSO_SIGNIN, false);
    }

    public void setMultipleApplication(Boolean hasMultipleApps) {
        sharedPreferences.edit().putBoolean(MULTIPLE_APPS, hasMultipleApps).apply();
    }

    public Boolean hasMultipleApplication() {
        return sharedPreferences.getBoolean(MULTIPLE_APPS, false);
    }

    public void setAppList(Map<String, String> appList) {
        if (appList != null) {
            sharedPreferences.edit().putString(APP_LIST, GsonUtils.getJsonFromObject(appList, Map.class)).apply();
        }
    }

    public Map<String, String> getAppList() {
        return (Map<String, String>) GsonUtils.getObjectFromJson(sharedPreferences.getString(APP_LIST, null), Map.class);
    }

    public void setTrialExpired(Boolean expired) {
        sharedPreferences.edit().putBoolean(TRIAL_EXPIRED, expired).apply();
    }

    public Boolean isTrialExpired() {
        return sharedPreferences.getBoolean(TRIAL_EXPIRED, false);
    }


    public long getLogDeletedAtTime() {
            return sharedPreferences.getLong(LOG_DELETED_AT_TIME, 0);
    }

    public void setLogDeletedAtTime(Long deletedAtTime) {
            sharedPreferences.edit().putLong(LOG_DELETED_AT_TIME, deletedAtTime).apply();
    }
}
