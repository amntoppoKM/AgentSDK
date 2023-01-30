package io.kommunicate.agent.exception;

import android.content.Context;
import android.text.TextUtils;

import com.applozic.mobicommons.ALSpecificSettings;
import com.applozic.mobicommons.commons.core.utils.Utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import io.kommunicate.agent.AgentSharedPreference;
import io.sentry.Attachment;
import io.sentry.Scope;
import java.net.ConnectException;
import java.net.UnknownHostException;
import io.sentry.Sentry;

public class KmExceptionAnalytics {
    private static Long logDeletionTimeFrame = 24 * 60 * 60000L;

    public static void captureException(Exception exception) {
        if(exception != null && !(exception instanceof ConnectException) && !(exception instanceof UnknownHostException)) {
            Sentry.captureException(exception);
        }
    }
    public static void captureMessage(String message) {
        if(!TextUtils.isEmpty(message)) {
            Sentry.captureMessage(message);
        }
    }
    public static void captureMessageWithLogs(Context context, String message) {
        if(!TextUtils.isEmpty(message)) {
            Sentry.configureScope(scope -> {
                String folder = "/" + Utils.getMetaDataValue(context, "main_folder_name");
                File dir = new File(context.getFilesDir().getAbsolutePath() + folder);
                String fileName = "/" + ALSpecificSettings.getInstance(context).getTextLogFileName() + ".txt";
                File file = new File(dir, fileName);
                if(!file.exists())
                    return;
                Attachment attachment = new Attachment(file.getPath());
                scope.addAttachment(attachment);
            });
            Sentry.captureMessage(message);
            Sentry.configureScope(Scope::clearAttachments);
        }
    }

    public static void writeToFile(Context context, String log) {
        try {
            String fileName = "/" + ALSpecificSettings.getInstance(context).getTextLogFileName() + ".txt";
            BufferedWriter bufferedWriter = null;
            try {
                String folder = "/" + Utils.getMetaDataValue(context, "main_folder_name");
                File dir = new File(context.getFilesDir().getAbsolutePath() + folder);
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                File file = new File(dir, fileName);
                if (!file.exists()) {
                    file.createNewFile();
                }

                FileWriter writer = new FileWriter(file, true);
                bufferedWriter = new BufferedWriter(writer);
                bufferedWriter.append(log);
                bufferedWriter.append("\r\n\n");

                bufferedWriter.flush();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (bufferedWriter != null) {
                    bufferedWriter.close();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void deleteLogFile(Context context) {
        AgentSharedPreference agentSharedPreference = AgentSharedPreference.getInstance(context);
        if((System.currentTimeMillis() - agentSharedPreference.getLogDeletedAtTime()) / 60000 < logDeletionTimeFrame) {
            return;
        }
        try {
            String fileName = "/" + ALSpecificSettings.getInstance(context).getTextLogFileName() + ".txt";
            String folder = "/" + Utils.getMetaDataValue(context, "main_folder_name");
            File dir = new File(context.getFilesDir().getAbsolutePath() + folder);
            File file = new File(dir, fileName);
            if (file.exists()) {
                if(file.delete()) {
                    agentSharedPreference.setLogDeletedAtTime(System.currentTimeMillis());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
