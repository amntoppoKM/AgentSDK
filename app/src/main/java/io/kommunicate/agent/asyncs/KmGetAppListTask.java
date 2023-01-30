package io.kommunicate.agent.asyncs;

import android.content.Context;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Patterns;

import com.applozic.mobicomkit.api.MobiComKitClientService;
import com.applozic.mobicommons.commons.core.utils.Utils;
import com.applozic.mobicommons.json.GsonUtils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;


/**
 * Created by ashish on 09/02/18.
 */

public class KmGetAppListTask extends AsyncTask<Void, Void, Map<String, String>> {

    private WeakReference<Context> context;
    private AppListHandler handler;
    private String userId;
    private static final String GET_APPLICATION_LIST = "/rest/ws/user/getlist";
    private static String TAG = "KmGetAppListTask";

    public KmGetAppListTask(Context context, AppListHandler handler, String userId) {
        this.context = new WeakReference<Context>(context);
        this.handler = handler;
        this.userId = userId;
    }

    @Override
    protected Map<String, String> doInBackground(Void... voids) {
        boolean isEmail = !TextUtils.isEmpty(userId) && Patterns.EMAIL_ADDRESS.matcher(userId).matches();
        return getApplicationList(userId, isEmail);
    }

    @Override
    protected void onPostExecute(Map<String, String> s) {
        super.onPostExecute(s);
        if (s != null && !s.isEmpty()) {
            if (handler != null) {
                handler.onSuccess(context.get(), s);
            }
        } else {
            if (handler != null) {
                String message = "Some error occured";
                if (s != null && s.isEmpty()) {
                    message = "No Applications found for user, Please register on kommunicate.io to get one...";
                }
                handler.onFailure(context.get(), message);
            }
        }
    }

    public interface AppListHandler {
        void onSuccess(Context context, Map<String, String> s);

        void onFailure(Context context, String error);
    }

    public synchronized Map<String, String> getApplicationList(String userId, boolean isEmailId) {
        try {
            return (Map<String, String>) GsonUtils.getObjectFromJson(getApplicationListClient(userId, isEmailId), Map.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getApplicationListClient(String userId, boolean isEmailId) {
        if (TextUtils.isEmpty(userId)) {
            return null;
        }
        try {
            String url = getApplicationListUrl() + "?roleNameList=APPLICATION_WEB_ADMIN&" + (isEmailId ? "emailId=" : "userId=") + URLEncoder.encode(userId, "UTF-8");
            return getResponse(url, "application/json", "application/json");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private String getApplicationListUrl() {
        return new MobiComKitClientService(context.get()).getBaseUrl() + GET_APPLICATION_LIST;
    }

    public String getResponse(String urlString, String contentType, String accept) {
        Utils.printLog(context.get(), TAG, "Calling url: " + urlString);

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
            connection.connect();

            if (connection == null) {
                return null;
            }
            BufferedReader br = null;
            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                InputStream inputStream = connection.getInputStream();
                br = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            } else {
                Utils.printLog(context.get(), TAG, "Response code for getResponse is  :" + connection.getResponseCode());
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
            } finally {
                if (br != null) {
                    br.close();
                }
            }

            Utils.printLog(context.get(), TAG, "Response :" + sb.toString());

            return sb.toString();
        } catch (ConnectException e) {
            Utils.printLog(context.get(), TAG, "failed to connect Internet is not working");
        } catch (Exception e) {
            e.printStackTrace();
        } catch (Throwable e) {

        } finally {
            if (connection != null) {
                try {
                    connection.disconnect();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }
}
