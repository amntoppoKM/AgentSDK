package io.kommunicate.agent.services;

import android.content.Context;
import android.os.Build;
import android.text.TextUtils;
import android.util.Base64;

import com.applozic.mobicomkit.AlUserUpdate;
import com.applozic.mobicomkit.Applozic;
import com.applozic.mobicomkit.ApplozicClient;
import com.applozic.mobicomkit.api.HttpRequestUtils;
import com.applozic.mobicomkit.api.MobiComKitClientService;
import com.applozic.mobicomkit.api.account.register.RegistrationResponse;
import com.applozic.mobicomkit.api.account.user.MobiComUserPreference;
import com.applozic.mobicomkit.api.account.user.User;
import com.applozic.mobicomkit.api.authentication.JWT;
import com.applozic.mobicomkit.api.notification.NotificationChannels;
import com.applozic.mobicomkit.contact.AppContactService;
import com.applozic.mobicomkit.exception.InvalidApplicationException;
import com.applozic.mobicomkit.exception.UnAuthoriseException;
import com.applozic.mobicomkit.feed.ApiResponse;
import com.applozic.mobicommons.commons.core.utils.Utils;
import com.applozic.mobicommons.encryption.EncryptionUtils;
import com.applozic.mobicommons.json.GsonUtils;
import com.applozic.mobicommons.people.contact.Contact;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;
import java.util.TimeZone;

import io.kommunicate.agent.AgentSharedPreference;
import io.kommunicate.agent.KmAgentRegistrationResponse;
import io.kommunicate.agent.KmUtils;
import io.kommunicate.agent.R;
import io.kommunicate.agent.asyncs.AgentGetStatusTask;
import io.kommunicate.agent.conversations.KmConversationUtils;
import io.kommunicate.agent.conversations.viewmodels.KmConversationViewModel;
import io.kommunicate.agent.exception.KmExceptionAnalytics;
import io.kommunicate.models.KmApiResponse;
import io.kommunicate.services.KmUserClientService;
import io.kommunicate.users.KMUser;

/**
 * Created by ashish on 25/04/18.
 */
public class AgentClientService extends KmUserClientService {

    private static final String SUCCESS = "SUCCESS";
    private static final String TAG = "AgentClientService";
    private static final String USER_PASSWORD_RESET = "/users/password-reset";
    private static final String APPLICATION_LIST_URL = "/login?loginType=email";
    private static final String INVALID_APP_ID = "INVALID_APPLICATIONID";
    private static final String USER_LOGIN_API = "/login";
    private static final String USER_LOGIN_API_V2 = "/rest/ws/loginv2";
    private static final String GET_CONVERSATION_LIST_URL = "/rest/ws/group/support";
    private static final String CHANGE_AGENT_STATUS = "/rest/ws/users/status";
    private static final String GET_AGENT_DETAILS = "/rest/ws/users/list";
    private static final String GET_USERS_LIST = "/users/list?applicationId=";
    private static final String CHANGE_CONVERSATION_ASSIGNEE_URL = "/rest/ws/group/assignee/change?groupId=";
    private static final String CHANGE_CONVERSATION_STATUS_URL = "/rest/ws/group/status/change?groupId=";
    private static final String UPDATE_KM_USER_DETAIL = "/rest/ws/users/userdetail";
    private static final String SEND_TRANSCRIPT = "/rest/ws/message/transcript/send";
    public static final String MULTIPLE_APPS = "MULTIPLE_APPS";
    public static String APP_MODULE_NAME_KEY_HEADER = "App-Module-Name";
    private static final String OF_USER_ID_HEADER = "Of-User-Id";
    private static final String X_AUTHORIZATION_HEADER = "x-authorization";
    private static final String APZ_APP_ID_HEADER = "Apz-AppId";
    public static String APPLICATION_KEY_HEADER = "Application-Key";
    public static String DEVICE_KEY_HEADER = "Device-Key";
    private static final String APZ_PRODUCT_APP_HEADER = "Apz-Product-App";
    protected HttpRequestUtils httpRequestUtils;

    private static final String SHARED_PREF_TOKEN_KEY = "jwt-token";

    public AgentClientService(Context context) {
        super(context);
        httpRequestUtils = new KmHttpRequestUtils(context);
    }

    public String getUpdateKmUserUrl() {
        return getKmBaseUrl() + UPDATE_KM_USER_DETAIL;
    }

    //uses jwt-token in the authorisation header
    public String makePatchRequest(String stringUrl, String data, boolean isKmCall) throws Exception {
        HttpURLConnection connection;
        URL url = new URL(stringUrl);
        connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("PATCH");
        connection.setRequestProperty("Content-Type", "application/json");
        if (!TextUtils.isEmpty(MobiComUserPreference.getInstance(context).getDeviceKeyString())) {
            connection.setRequestProperty(DEVICE_KEY_HEADER, MobiComUserPreference.getInstance(context).getDeviceKeyString());
        }
        connection.setDoInput(true);
        connection.setDoOutput(true);

        if (isKmCall) {
            String token = AgentSharedPreference.getInstance(context).getJwtToken();

            if (TextUtils.isEmpty(token)) {
                Utils.printLog(context, TAG, "The JWT Token is null. Can't set authorization header.");
            } else {
                connection.setRequestProperty("Authorization", token);
            }
        } else {
            httpRequestUtils.addGlobalHeaders(connection, null);
        }

        connection.connect();

        if (data != null) {
            byte[] dataBytes = data.getBytes("UTF-8");
            DataOutputStream dataOutputStream = new DataOutputStream(connection.getOutputStream());
            dataOutputStream.write(dataBytes);
            dataOutputStream.flush();
            dataOutputStream.close();
        }

        BufferedReader bufferedReader = null;
        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            InputStream inputStream = connection.getInputStream();
            bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
        } else {
            Utils.printLog(context, TAG, "Response code for " + stringUrl + " : " + connection.getResponseCode());
        }
        StringBuilder stringBuilder = new StringBuilder();
        try {
            String line;
            if (bufferedReader != null) {
                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line);
                }
            }
        } catch (Exception exception) {
            KmExceptionAnalytics.captureException(exception);
            exception.printStackTrace();
        } finally {
            if (bufferedReader != null) {
                bufferedReader.close();
            }
        }
        Utils.printLog(context, TAG, "Response for " + stringUrl + " : " + stringBuilder.toString());
        return stringBuilder.toString();
    }

    //MAKE DELETE REQUEST
    public String makeDeleteRequest(String stringUrl, String data, boolean isKmCall) throws Exception {
        HttpURLConnection connection;
        URL url = new URL(stringUrl);
        connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("DELETE");
        connection.setRequestProperty("Content-Type", "application/json");
        if (!TextUtils.isEmpty(MobiComUserPreference.getInstance(context).getDeviceKeyString())) {
            connection.setRequestProperty(DEVICE_KEY_HEADER, MobiComUserPreference.getInstance(context).getDeviceKeyString());
        }
        connection.setDoInput(true);
        connection.setDoOutput(true);

        if (isKmCall) {
            String token = AgentSharedPreference.getInstance(context).getJwtToken();

            if (TextUtils.isEmpty(token)) {
                Utils.printLog(context, TAG, "The JWT Token is null. Can't set authorization header.");
            } else {
                connection.setRequestProperty("Authorization", token);
            }
        } else {
            httpRequestUtils.addGlobalHeaders(connection, null);
        }

        connection.connect();

        if (data != null) {
            byte[] dataBytes = data.getBytes("UTF-8");
            DataOutputStream dataOutputStream = new DataOutputStream(connection.getOutputStream());
            dataOutputStream.write(dataBytes);
            dataOutputStream.flush();
            dataOutputStream.close();
        }

        BufferedReader bufferedReader = null;
        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            InputStream inputStream = connection.getInputStream();
            bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
        } else {
            Utils.printLog(context, TAG, "Response code for " + stringUrl + " : " + connection.getResponseCode());
        }
        StringBuilder stringBuilder = new StringBuilder();
        try {
            String line;
            if (bufferedReader != null) {
                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line);
                }
            }
        } catch (Exception exception) {
            KmExceptionAnalytics.captureException(exception);
            exception.printStackTrace();
        } finally {
            if (bufferedReader != null) {
                bufferedReader.close();
            }
        }
        Utils.printLog(context, TAG, "Response for " + stringUrl + " : " + stringBuilder.toString());
        return stringBuilder.toString();
    }

    //uses jwt-token in the authorisation header
    @Override
    public String getResponse(String urlString, String contentType, String accept) {
        Utils.printLog(context, TAG, "Calling URL: " + urlString);

        HttpURLConnection connection = null;
        URL url;

        try {
            url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setInstanceFollowRedirects(true);
            connection.setRequestMethod("GET");
            connection.setUseCaches(false);
            connection.setDoInput(true);

            if (!TextUtils.isEmpty(contentType)) {
                connection.setRequestProperty("Content-Type", contentType);
            }
            if (!TextUtils.isEmpty(accept)) {
                connection.setRequestProperty("Accept", accept);
            }
            if (!TextUtils.isEmpty(MobiComUserPreference.getInstance(context).getDeviceKeyString())) {
                connection.setRequestProperty(DEVICE_KEY_HEADER, MobiComUserPreference.getInstance(context).getDeviceKeyString());
            }
            String token = AgentSharedPreference.getInstance(context).getJwtToken();
            if (TextUtils.isEmpty(token)) {
                Utils.printLog(context, TAG, "The JWT Token is null. Can't set authorization header.");
            } else {
                connection.setRequestProperty("Authorization", token);
            }
            KmExceptionAnalytics.writeToFile(context, "Header for URL: " + urlString + " -- " + connection.getRequestProperties().toString());
            connection.connect();
            KmExceptionAnalytics.writeToFile(context, "Response Code for URL: " + urlString + " -- " + connection.getResponseCode());

            BufferedReader bufferedReader = null;
            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                InputStream inputStream = connection.getInputStream();
                bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            } else if(connection.getResponseCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {
                return KmUtils.UN_AUTHORIZED;
            } else {
                Utils.printLog(context, TAG, "Response code for getResponse is  :" + connection.getResponseCode());
            }

            StringBuilder stringBuilder = new StringBuilder();
            try {
                String line;
                if (bufferedReader != null) {
                    while ((line = bufferedReader.readLine()) != null) {
                        stringBuilder.append(line);
                    }
                }
            } catch (Exception exception) {
                KmExceptionAnalytics.captureException(exception);
                exception.printStackTrace();
            } finally {
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
            }

            Utils.printLog(context, TAG, "Response :" + stringBuilder.toString());
            return stringBuilder.toString();
        } catch (ConnectException exception) {
            Utils.printLog(context, TAG, "Failed to connect. Internet is not working.");
        } catch (Exception exception) {
            exception.printStackTrace();
        } finally {
            if (connection != null) {
                try {
                    connection.disconnect();
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
            }
        }
        return null;
    }

    public String getResponseWithException(String urlString, String contentType, String accept, boolean isFileUpload, String userId) throws Exception {
        Utils.printLog(context, TAG, "Calling url **[GET]** : " + urlString);

        HttpURLConnection connection = null;
        URL url;

        try {
            url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setInstanceFollowRedirects(true);
            connection.setRequestMethod("GET");
            connection.setUseCaches(false);
            connection.setDoInput(true);

            if (!TextUtils.isEmpty(contentType)) {
                connection.setRequestProperty("Content-Type", contentType);
            }
            if (!TextUtils.isEmpty(accept)) {
                connection.setRequestProperty("Accept", accept);
            }
            httpRequestUtils.addGlobalHeaders(connection, userId);
            KmExceptionAnalytics.writeToFile(context, "Header for URL: " + urlString + " -- " + connection.getRequestProperties().toString());
            connection.connect();
            KmExceptionAnalytics.writeToFile(context, "Response Code for URL: " + urlString + " -- " + connection.getResponseCode());

            BufferedReader br = null;
            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                InputStream inputStream = connection.getInputStream();
                br = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            } else if(connection.getResponseCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {
                return KmUtils.UN_AUTHORIZED;
            } else {
                Utils.printLog(context, TAG, "\n\nResponse code for url: " + urlString + "\n** Code ** : " + connection.getResponseCode() + "\n\n");
            }

            StringBuilder sb = new StringBuilder();
            try {
                String line;
                if (br != null) {
                    while ((line = br.readLine()) != null) {
                        sb.append(line);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw e;
            } finally {
                if (br != null) {
                    br.close();
                }
            }

            Utils.printLog(context, TAG, "\n\nGET Response for url: " + urlString + "\n** Response **: " + sb.toString() + "\n\n");

            if (!TextUtils.isEmpty(sb.toString()) && !TextUtils.isEmpty(MobiComUserPreference.getInstance(context).getEncryptionKey())) {
                return isFileUpload ? sb.toString() : EncryptionUtils.decrypt(MobiComUserPreference.getInstance(context).getEncryptionKey(), sb.toString(), MobiComUserPreference.getInstance(context).getEncryptionIV());
            }
            return sb.toString();
        } catch (ConnectException e) {
            Utils.printLog(context, TAG, "failed to connect Internet is not working");
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } catch (Throwable e) {
            throw e;
        } finally {
            HttpRequestUtils.isRefreshTokenInProgress = false;
            if (connection != null) {
                connection.disconnect();
            }
        }
    }



    public String postData(String url, String data, boolean isKmCall) throws Exception {
        return postJsonToServer(url, data, null, isKmCall);
    }

    public String postJsonToServer(String stringUrl, String data, String userId, boolean isKmCall) throws Exception {
        HttpURLConnection connection;
        URL url = new URL(stringUrl);
        connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoInput(true);
        connection.setDoOutput(true);
        if (isKmCall) {
            String token = AgentSharedPreference.getInstance(context).getJwtToken();

            if (TextUtils.isEmpty(token)) {
                Utils.printLog(context, TAG, "The JWT Token is null. Can't set authorization header.");
            } else {
                connection.setRequestProperty("Authorization", token);
            }
        } else {
            httpRequestUtils.addGlobalHeaders(connection, userId);
        }
        connection.connect();
        Utils.printLog(context, TAG, "Posting data : " + data + "\nTo url : " + url);
        byte[] dataBytes = data.getBytes("UTF-8");
        DataOutputStream os = new DataOutputStream(connection.getOutputStream());
        os.write(dataBytes);
        os.flush();
        os.close();
        BufferedReader br = null;
        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            InputStream inputStream = connection.getInputStream();
            br = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
        } else {
            Utils.printLog(context, TAG, "Response code for post json is :" + connection.getResponseCode());
        }
        StringBuilder sb = new StringBuilder();
        try {
            String line;
            if (br != null) {
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
            }
        } catch (Exception e) {
            KmExceptionAnalytics.captureException(e);
            e.printStackTrace();
        } catch (Throwable e) {
        } finally {
            if (br != null) {
                br.close();
            }
        }
        Utils.printLog(context, TAG, "Response: " + sb.toString());
        return sb.toString();
    }

    @Override
    public String resetUserPassword(String userId, String appKey) {
        if (userId == null || appKey == null) {
            return null;
        }

        String url = getKmBaseUrl() + USER_PASSWORD_RESET;

        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("userName", userId);
            jsonObject.put("applicationId", appKey);

            return httpRequestUtils.postJsonToServer(url, jsonObject.toString());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String getMessageListUrl() {
        return getBaseUrl() + GET_CONVERSATION_LIST_URL;
    }

    public String getMessageListByStatus(int status, int pageSize, Long lastFetchTime) {
        try {
            StringBuilder urlBuilder = new StringBuilder(getMessageListUrl());
            if (status == KmConversationUtils.ALL_CONVERSATIONS) {
                urlBuilder.append("?pageSize=");
                urlBuilder.append(pageSize);
                if (lastFetchTime != null && lastFetchTime != 0) {
                    urlBuilder.append("&lastFetchTime=");
                    urlBuilder.append(lastFetchTime);
                }
                urlBuilder.append("&status=0&status=6");
            } else if (status == KmConversationUtils.CLOSED_CONVERSATIONS) {
                urlBuilder.append("?pageSize=");
                urlBuilder.append(pageSize);
                if (lastFetchTime != null && lastFetchTime != 0) {
                    urlBuilder.append("&lastFetchTime=");
                    urlBuilder.append(lastFetchTime);
                }
                urlBuilder.append("&status=2&status=3&status=4&status=5");
            } else if (status == KmConversationUtils.ASSIGNED_CONVERSATIONS) {
                urlBuilder.append("/assigned?userId=");
                urlBuilder.append(URLEncoder.encode(MobiComUserPreference.getInstance(context).getUserId(), "UTF-8"));
                urlBuilder.append("&pageSize=");
                urlBuilder.append(pageSize);
                if (lastFetchTime != null && lastFetchTime != 0) {
                    urlBuilder.append("&lastFetchTime=");
                    urlBuilder.append(lastFetchTime);
                }
                urlBuilder.append("&status=0&status=6&status=-1");
            }
            return httpRequestUtils.getResponse(urlBuilder.toString(), "application/json", "application/json");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    protected String getResponse(String url) {
        return getResponse(url, "application/json", "application/json");
    }

    protected String getAlResponse(String url) {
        return httpRequestUtils.getResponse(url, "application/json", "application/json");
    }

    public KmAgentRegistrationResponse loginKmUser(KMUser user, boolean isGoogleLogin, boolean isSSOLogin, boolean useEncryption) throws Exception {
        if (user == null) {
            return null;
        }

        user.setDeviceType(Short.valueOf("1"));
        user.setPrefContactAPI(Short.valueOf("2"));
        user.setTimezone(TimeZone.getDefault().getID());
        user.setEnableEncryption(user.isEnableEncryption());
        user.setRoleType(User.RoleType.AGENT.getValue());
        user.setRoleName(User.RoleName.APPLICATION_WEB_ADMIN.getValue());


        MobiComUserPreference mobiComUserPreference = MobiComUserPreference.getInstance(context);

        Gson gson = new Gson();
        user.setAppVersionCode(MOBICOMKIT_VERSION_CODE);
        user.setRegistrationId(mobiComUserPreference.getDeviceRegistrationId());

        if (getAppModuleName(context) != null) {
            user.setAppModuleName(getAppModuleName(context));
        }

        Utils.printLog(context, TAG, "Net status" + Utils.isInternetAvailable(context.getApplicationContext()));

        if (!Utils.isInternetAvailable(context.getApplicationContext())) {
            throw new ConnectException("No Internet Connection");
        }

        String loginUrl = getKmBaseUrl() + (USER_LOGIN_API_V2 + (isGoogleLogin ? "?loginType=oauth" : ""));

        Utils.printLog(context, TAG, "Registration json " + gson.toJson(user));
        Utils.printLog(context, TAG, "Login url : " + loginUrl);

        //BASE64 encoding
        JSONObject encodedJson = null;
        if(useEncryption) {
            String data = gson.toJson(user);
            String base64 = Base64.encodeToString(URLEncoder.encode(data, "UTF-8").getBytes("UTF-8"), Base64.DEFAULT);
            encodedJson = new JSONObject();
            encodedJson.put("encoded", true);
            encodedJson.put("data", base64);
        }

        String response = httpRequestUtils.postJsonToServer(loginUrl, useEncryption ? String.valueOf(encodedJson) : gson.toJson(user));

        Utils.printLog(context, TAG, "Registration response is: " + response);

        if (TextUtils.isEmpty(response) || response.contains("<html")) {
            throw new Exception("503 Service Unavailable");
        }


        //If we receive encoded response, decode it
        if ((new JSONObject(response)).optString("encoded").equals("true")) {
            byte[] decodedByte = Base64.decode(new JSONObject(response).getString("data"), Base64.DEFAULT);
            response = new String(decodedByte, "UTF-8");
        }


        if (response.contains(INVALID_APP_ID)) {
            throw new InvalidApplicationException("Invalid Application Id");
        }

        if ((new JSONObject(response)).optString("code").equals("INVALID_CREDENTIALS")) {
            throw new UnAuthoriseException(Utils.getString(context, R.string.km_invalid_credentials_error));
        }


        KmAgentRegistrationResponse kmRegistrationResponse;
        kmRegistrationResponse = gson.fromJson(response, KmAgentRegistrationResponse.class);
        if ((new JSONObject(response)).optString("code").equals(MULTIPLE_APPS)) {
            Map<String, String> appList = gson.fromJson((new JSONObject(response)).optString("result"), Map.class);
            AgentSharedPreference.getInstance(context).setAppList(appList);
            kmRegistrationResponse.setAppList(appList);
            kmRegistrationResponse.setMessage(MULTIPLE_APPS);
            return kmRegistrationResponse;
        }

        RegistrationResponse registrationResponse = null;
        if (kmRegistrationResponse != null && kmRegistrationResponse.getResult() != null) {
            registrationResponse = kmRegistrationResponse.getResult().getApplozicUser();
            AgentSharedPreference.getInstance(context).setAgentRoleType(
                    kmRegistrationResponse.getResult().getRoleType() == null
                            ? registrationResponse.getRoleType() : kmRegistrationResponse.getResult().getRoleType());
            AgentSharedPreference.getInstance(context).setJwtToken(kmRegistrationResponse.getResult().getToken());

        }

        if (registrationResponse == null) {
            KmAgentRegistrationResponse invalidResponse = new KmAgentRegistrationResponse();
            invalidResponse.setMessage("Invalid response");
            return invalidResponse;
        }

        if (registrationResponse.isPasswordInvalid()) {
            throw new UnAuthoriseException("Invalid uername/password");
        }
        Utils.printLog(context, "Registration response ", "is " + registrationResponse);
        if (registrationResponse.getNotificationResponse() != null) {
            Utils.printLog(context, "Registration response ", "" + registrationResponse.getNotificationResponse());
        }
        mobiComUserPreference.setEncryptionKey(registrationResponse.getEncryptionKey());
        mobiComUserPreference.enableEncryption(user.isEnableEncryption());
        mobiComUserPreference.setCountryCode(user.getCountryCode());
        mobiComUserPreference.setUserId(user.getUserId());
        mobiComUserPreference.setContactNumber(user.getContactNumber());
        mobiComUserPreference.setEmailVerified(user.isEmailVerified());
        mobiComUserPreference.setDisplayName(user.getDisplayName());
        mobiComUserPreference.setMqttBrokerUrl(registrationResponse.getBrokerUrl());
        mobiComUserPreference.setDeviceKeyString(registrationResponse.getDeviceKey());
        mobiComUserPreference.setEmailIdValue(user.getEmail());
        mobiComUserPreference.setImageLink(user.getImageLink());
        mobiComUserPreference.setSuUserKeyString(registrationResponse.getUserKey());
        mobiComUserPreference.setLastSyncTimeForMetadataUpdate(String.valueOf(registrationResponse.getCurrentTimeStamp()));
        mobiComUserPreference.setLastSyncTime(String.valueOf(registrationResponse.getCurrentTimeStamp()));
        mobiComUserPreference.setLastSeenAtSyncTime(String.valueOf(registrationResponse.getCurrentTimeStamp()));
        mobiComUserPreference.setChannelSyncTime(String.valueOf(registrationResponse.getCurrentTimeStamp()));
        mobiComUserPreference.setUserBlockSyncTime("10000");
        mobiComUserPreference.setPassword(user.getPassword());
        mobiComUserPreference.setPricingPackage(registrationResponse.getPricingPackage());
        mobiComUserPreference.setAuthenticationType(String.valueOf(user.getAuthenticationTypeId()));
        mobiComUserPreference.setUserRoleType(registrationResponse.getRoleType());
        ApplozicClient.getInstance(context).skipDeletedGroups(true).hideActionMessages(true);
        AgentSharedPreference.getInstance(context).setGoogleLogin(isGoogleLogin);
        AgentSharedPreference.getInstance(context).setSSOLogin(isSSOLogin);

        if (isGoogleLogin && !TextUtils.isEmpty(kmRegistrationResponse.getResult().getEncodedAccessToken())) {
            mobiComUserPreference.setPassword(kmRegistrationResponse.getResult().getDecodedAccessToken());
        }
        if (user.getUserTypeId() != null) {
            mobiComUserPreference.setUserTypeId(String.valueOf(user.getUserTypeId()));
        }
        if (!TextUtils.isEmpty(user.getNotificationSoundFilePath())) {
            Applozic.getInstance(context).setCustomNotificationSound(user.getNotificationSoundFilePath());
        }
        JWT.parseToken(context, registrationResponse.getAuthToken());
        Contact contact = new Contact();
        contact.setUserId(user.getUserId());
        contact.setFullName(registrationResponse.getDisplayName());
        contact.setImageURL(registrationResponse.getImageLink());
        contact.setContactNumber(registrationResponse.getContactNumber());
        if (user.getUserTypeId() != null) {
            contact.setUserTypeId(user.getUserTypeId());
        }
        contact.setRoleType(user.getRoleType());
        contact.setMetadata(user.getMetadata());
        contact.setStatus(registrationResponse.getStatusMessage());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Applozic.getInstance(context).setNotificationChannelVersion(NotificationChannels.NOTIFICATION_CHANNEL_VERSION - 1);
            new NotificationChannels(context, Applozic.getInstance(context).getCustomNotificationSound()).prepareNotificationChannels();
        }
        new AppContactService(context).upsert(contact);

        return kmRegistrationResponse;
    }

    /**
     * to update the agent status on the server
     *
     * @param userId         the userId of the agent here whose status to update
     * @param applicationKey the application key for the agent
     * @param online         the status, true = 1, false = 0
     * @return the http request response, getCode should equal SUCCESS for a successful request
     */
    public KmApiResponse<String> setAgentStatus(String userId, String applicationKey, boolean online) {
        if (TextUtils.isEmpty(userId) || TextUtils.isEmpty(applicationKey)) {
            Utils.printLog(context, TAG, "User Id or Application Key is null/empty.");
            return null;
        }

        try {
            JSONObject statusChangePayload = new JSONObject();
            Utils.printLog(context, TAG, "Changing status to online : " + online);
            statusChangePayload.put("status", online ? KmConversationViewModel.STATUS_ONLINE : KmConversationViewModel.STATUS_AWAY);
            statusChangePayload.put("applicationId", applicationKey);
            statusChangePayload.put("userName", userId);

            //post json, get response string, deserialize it and display response code
            String stringResponse = makePatchRequest(getKmBaseUrl() + CHANGE_AGENT_STATUS, statusChangePayload.toString(), true);
            KmApiResponse<String> response = (KmApiResponse<String>) GsonUtils.getObjectFromJson(stringResponse, KmApiResponse.class);
            Utils.printLog(context, TAG, "Agent status update response code: " + response.getCode());

            return response;
        } catch (Exception exception) {
            KmExceptionAnalytics.captureException(exception);
            exception.printStackTrace();
            Utils.printLog(context, TAG, "Error posting request to server for agent status change.");
            return null;
        }
    }

    /**
     * to Send Transcript to the user's emailID
     *
     * @param groupId       The groupID of the conversation of which the transcript is to be sent
     * @param email         Email ID of the user in which the transcript would be sent
     * @return              the http request response, getCode should equal SUCCESS for a successful request
     */
    public ApiResponse sendTranscript(Integer groupId, String email) {
        if (groupId == null || TextUtils.isEmpty(email)) {
            Utils.printLog(context, TAG, "group Id or email is null/empty.");
            return null;
        }
        try {
            String url = getBaseUrl() + SEND_TRANSCRIPT + "?groupId=" + groupId + "&tos=" + URLEncoder.encode(email, "UTF-8").trim();
            String response = httpRequestUtils.postJsonToServer(url, "");
            Utils.printLog(context, TAG, "Send Transcript POST method response: " + response);
            return ((ApiResponse) GsonUtils.getObjectFromJson(response, ApiResponse.class));
        } catch (Exception exception) {
            KmExceptionAnalytics.captureException(exception);
            exception.printStackTrace();
        }
        return null;
    }

    //this uses the get method instead of the post method, compliant with the rest api
    public String getUserDetails(String userId, String applicationKey) {
        if (TextUtils.isEmpty(userId) || TextUtils.isEmpty(applicationKey)) {
            Utils.printLog(context, TAG, "User Id or Application Key is null/empty.");
            return null;
        }

        try {
            String url = getKmBaseUrl() + GET_AGENT_DETAILS + "?applicationId=" + applicationKey.trim() + "&userName=" + URLEncoder.encode(userId, "UTF-8").trim();
            String response = getResponse(url, "application/json", "application/json, text/plain, */*");
            Utils.printLog(context, TAG, "User details GET method response: " + response);
            return response;
        } catch (Exception exception) {
            KmExceptionAnalytics.captureException(exception);
            exception.printStackTrace();
        }

        return null;
    }

    public String getUsersList(Short... roles) throws Exception {
        if (roles == null) {
            return null;
        }
        StringBuilder urlBuilder = new StringBuilder(getKmBaseUrl() + GET_USERS_LIST);
        urlBuilder.append(MobiComKitClientService.getApplicationKey(context));

        for (Short role : roles) {
            urlBuilder.append("&roleType=").append(role);
        }
        return getResponse(urlBuilder.toString(), "application/json", "application/json, text/plain, */*");
    }

    public String switchConversationAssignee(Integer groupId, String assigneeId, boolean switchAssignee, boolean sendNotifyMessage, boolean takeOverFromBot) {
        try {
            String url = getBaseUrl() + CHANGE_CONVERSATION_ASSIGNEE_URL + groupId
                    + "&assignee=" + URLEncoder.encode(assigneeId, "UTF-8").trim()
                    + "&switchAssignee=" + switchAssignee
                    + "&sendNotifyMessage=" + sendNotifyMessage
                    + "&takeOverFromBot=" + takeOverFromBot;

            return makePatchRequest(url, null, false);
        } catch (Exception e) {
            KmExceptionAnalytics.captureException(e);
            e.printStackTrace();
        }
        return null;
    }

    public String changeConversationStatus(Integer groupId, int status, boolean sendNotifyMessage) {
        try {
            String url = getBaseUrl() + CHANGE_CONVERSATION_STATUS_URL + groupId
                    + "&status=" + status
                    + "&sendNotifyMessage=" + sendNotifyMessage;

            return makePatchRequest(url, null, false);
        } catch (Exception e) {
            KmExceptionAnalytics.captureException(e);
            e.printStackTrace();
        }
        return null;
    }

    public ApiResponse updateDisplayNameORImageLink(User user) {
        AlUserUpdate userUpdate = new AlUserUpdate();
        AgentGetStatusTask.AgentDetail kmAgent = new AgentGetStatusTask.AgentDetail();
        try {
            userUpdate.setDisplayName(user.getDisplayName());
            kmAgent.setName(user.getDisplayName());
            userUpdate.setEmail(user.getEmail());
            kmAgent.setUserName(user.getUserId());
            userUpdate.setPhoneNumber(user.getContactNumber());
            kmAgent.setContactNo(user.getContactNumber());
            userUpdate.setMetadata(user.getMetadata());
            userUpdate.setImageLink(user.getImageLink());
            kmAgent.setApplicationId(getApplicationKey(context));

            if (user.getMetadata() != null && user.getMetadata().containsKey(KmConversationViewModel.DESIGNATION)) {
                kmAgent.setRole(user.getMetadata().get(KmConversationViewModel.DESIGNATION));
            }

            String response = httpRequestUtils.postData(getUserProfileUpdateUrl() + "?elasticUpdate=true&allowEmail=true", GsonUtils.getJsonFromObject(userUpdate, AlUserUpdate.class), user.getUserId());
            KmApiResponse kmApiResponse = (KmApiResponse) GsonUtils.getObjectFromJson(makePatchRequest(getUpdateKmUserUrl(), GsonUtils.getJsonFromObject(kmAgent, AgentGetStatusTask.AgentDetail.class), true), KmApiResponse.class);
            if (kmApiResponse != null && SUCCESS.equalsIgnoreCase(kmApiResponse.getCode())) {
                AgentSharedPreference.getInstance(context).setAgentDetails(kmAgent);
            }
            Utils.printLog(context, TAG, response);
            return ((ApiResponse) GsonUtils.getObjectFromJson(response, ApiResponse.class));
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
