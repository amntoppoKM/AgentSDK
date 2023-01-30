package io.kommunicate.agent;

import android.util.Base64;

import com.applozic.mobicomkit.api.account.register.RegistrationResponse;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.Map;

import io.kommunicate.feeds.Result;

public class KmAgentRegistrationResponse extends RegistrationResponse {

    @SerializedName("code")
    @Expose
    private String code;

    @SerializedName("result")
    @Expose
    private KmAgentResult result;
    private Map<String, String> appList;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Map<String, String> getAppList() {
        return appList;
    }

    public void setAppList(Map<String, String> appList) {
        this.appList = appList;
    }


    public KmAgentResult getResult() {
        return result;
    }

    public void setResult(KmAgentResult result) {
        this.result = result;
    }

    public static class KmAgentResult extends Result {
        @SerializedName("encodedAccessToken")
        @Expose
        private String encodedAccessToken;

        @SerializedName("token")
        @Expose
        private String token;

        @Expose
        private Short roleType;

        @SerializedName("applicationId")
        @Expose
        private String applicationId;


        public String getApplicationId() {
            return applicationId;
        }

        public void setApplicationId(String applicationId) {
            this.applicationId = applicationId;
        }

        public String getEncodedAccessToken() {
            return encodedAccessToken;
        }

        public void setEncodedAccessToken(String encodedAccessToken) {
            this.encodedAccessToken = encodedAccessToken;
        }

        public String getDecodedAccessToken() {
            String data = new String(Base64.decode(encodedAccessToken, Base64.DEFAULT));
            return data;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public Short getRoleType() {
            return roleType;
        }

        public void setRoleType(Short roleType) {
            this.roleType = roleType;
        }
    }

    public enum PricingPackage {
        KOMMUNICATE_STARTUP(Short.valueOf("101")),
        KOMMUNICATE_PER_AGENT_MONTHLY(Short.valueOf("102")),
        KOMMUNICATE_PER_AGENT_YEARLY(Short.valueOf("103")),
        KOMMUNICATE_GROWTH_MONTHLY(Short.valueOf("104")),
        KOMMUNICATE_ENTERPRISE_MONTHLY(Short.valueOf("105")),
        KOMMUNICATE_ENTERPRISE_YEARLY(Short.valueOf("106")),
        KOMMUNICATE_EARLY_BIRD_MONTHLY(Short.valueOf("107")),
        KOMMUNICATE_EARLY_BIRD_YEARLY(Short.valueOf("108"));

        private final Short value;

        private PricingPackage(Short c) {
            value = c;
        }

        public Short getValue() {
            return value;
        }
    }
}
