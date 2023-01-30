package io.kommunicate.agent.asyncs;

import android.content.Context;
import android.os.AsyncTask;
import android.text.TextUtils;

import com.applozic.mobicomkit.api.MobiComKitClientService;
import com.applozic.mobicomkit.api.account.user.MobiComUserPreference;
import com.applozic.mobicommons.json.GsonUtils;
import com.applozic.mobicommons.json.JsonMarker;
import com.google.gson.reflect.TypeToken;

import java.lang.ref.WeakReference;

import io.kommunicate.agent.AgentSharedPreference;
import io.kommunicate.agent.model.AgentAPIResponse;
import io.kommunicate.agent.services.AgentClientService;

/**
 * this will return the user details for the given userId and application key and then get the status from it
 * you can modify it to get user details if required
 */
public class AgentGetStatusTask extends AsyncTask<Void, Void, String> {
    private WeakReference<Context> contextWeakReference;
    private String userId;
    private KmAgentGetStatusHandler kmAgentGetStatusHandler;

    public AgentGetStatusTask(Context context, String userId, KmAgentGetStatusHandler kmAgentGetStatusHandler) {
        contextWeakReference = new WeakReference<>(context);
        this.userId = userId;
        this.kmAgentGetStatusHandler = kmAgentGetStatusHandler;
    }

    @Override
    protected String doInBackground(Void... voids) {
        AgentClientService agentClientService = new AgentClientService(contextWeakReference.get());
        return agentClientService.getUserDetails(userId, MobiComKitClientService.getApplicationKey(contextWeakReference.get()));
    }

    @Override
    protected void onPostExecute(String response) {
        super.onPostExecute(response);
        if (!TextUtils.isEmpty(response)) {
            try {
                AgentAPIResponse<AgentDetail> agentAPIResponse = (AgentAPIResponse<AgentDetail>) GsonUtils.getObjectFromJson(response, new TypeToken<AgentAPIResponse<AgentDetail>>() {
                }.getType());
                if (agentAPIResponse != null && agentAPIResponse.getResponse() != null && !agentAPIResponse.getResponse().isEmpty()) {
                    if (agentAPIResponse.getResponse() != null) {
                        for (AgentDetail agentDetail : agentAPIResponse.getResponse()) {
                            if (MobiComUserPreference.getInstance(contextWeakReference.get()).getUserId().equals(agentDetail.getUserName())) {
                                AgentSharedPreference.getInstance(contextWeakReference.get()).setAgentDetails(agentDetail);
                            }
                        }
                    }
                    kmAgentGetStatusHandler.onFinished(agentAPIResponse.getResponse().get(0).status == 1);
                } else {
                    kmAgentGetStatusHandler.onError("Response object is null, but the response string isn't empty or null.");
                }
            } catch (Exception exception) {
                exception.printStackTrace();
                kmAgentGetStatusHandler.onError(exception.getMessage());
            }
        } else {
            kmAgentGetStatusHandler.onError("The response string is null.");
        }
    }

    public interface KmAgentGetStatusHandler {
        void onFinished(boolean status);

        void onError(String error);
    }

    public static class AgentDetail extends JsonMarker {
        String userName;
        int status;
        private String email;
        private String name;
        private String role;
        private String contactNo;
        private String companyName;
        private String applicationId;
        private String industry;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getUserName() {
            return userName;
        }

        public void setUserName(String userName) {
            this.userName = userName;
        }

        public int getStatus() {
            return status;
        }

        public void setStatus(int status) {
            this.status = status;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getContactNo() {
            return contactNo;
        }

        public void setContactNo(String contactNo) {
            this.contactNo = contactNo;
        }

        public String getCompanyName() {
            return companyName;
        }

        public void setCompanyName(String companyName) {
            this.companyName = companyName;
        }

        public String getApplicationId() {
            return applicationId;
        }

        public void setApplicationId(String applicationId) {
            this.applicationId = applicationId;
        }

        public String getIndustry() {
            return industry;
        }

        public void setIndustry(String industry) {
            this.industry = industry;
        }

        public enum RoleType {
            SUPER_ADMIN(Short.valueOf("0")),
            ADMIN(Short.valueOf("1")),
            AGENT(Short.valueOf("2")),
            BOT(Short.valueOf("3")),
            DEVELOPER(Short.valueOf("11")),
            OPERATOR(Short.valueOf("12"));

            private Short value;

            RoleType(Short r) {
                value = r;
            }

            public Short getValue() {
                return value;
            }
        }

        public enum RoleName {
            BOT("BOT"),
            APPLICATION_ADMIN("APPLICATION_ADMIN"),
            USER("USER"),
            ADMIN("ADMIN"),
            BUSINESS("BUSINESS"),
            APPLICATION_BROADCASTER("APPLICATION_BROADCASTER"),
            SUPPORT("SUPPORT"),
            APPLICATION_WEB_ADMIN("APPLICATION_WEB_ADMIN"),
            OPERATOR("OPERATOR");

            private String value;

            RoleName(String r) {
                value = r;
            }

            public String getValue() {
                return value;
            }
        }
    }
}
