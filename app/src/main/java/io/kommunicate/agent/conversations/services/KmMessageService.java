package io.kommunicate.agent.conversations.services;

import android.content.Context;
import android.content.Intent;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import io.kommunicate.agent.KmUtils;
import io.kommunicate.agent.exception.KmExceptionAnalytics;

import com.applozic.mobicomkit.ApplozicClient;
import com.applozic.mobicomkit.api.MobiComKitConstants;
import com.applozic.mobicomkit.api.account.user.MobiComUserPreference;
import com.applozic.mobicomkit.api.attachment.FileClientService;
import com.applozic.mobicomkit.api.conversation.AlConversationResponse;
import com.applozic.mobicomkit.api.conversation.Message;
import com.applozic.mobicomkit.api.conversation.MobiComConversationService;
import com.applozic.mobicomkit.channel.service.ChannelService;
import com.applozic.mobicomkit.exception.ApplozicException;
import com.applozic.mobicomkit.feed.ApiResponse;
import com.applozic.mobicommons.commons.core.utils.Utils;
import com.applozic.mobicommons.json.GsonUtils;
import com.applozic.mobicommons.people.channel.Channel;
import com.applozic.mobicommons.people.contact.Contact;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class KmMessageService extends MobiComConversationService {

    private final static String TAG = "KmMessageService";

    private KmMessageClientService messageClientService;
    private boolean isHideActionMessage = false;

    public KmMessageService(Context context) {
        super(context);
        messageClientService = new KmMessageClientService(context);
        this.isHideActionMessage = ApplozicClient.getInstance(context).isActionMessagesHidden();
    }

    @Override
    public List<Message> getConversationSearchList(String searchString) throws Exception {
        String response = messageClientService.getMessageSearchResult(searchString);
        ApiResponse<AlConversationResponse> apiResponse = (ApiResponse<AlConversationResponse>) GsonUtils.getObjectFromJson(response, new TypeToken<ApiResponse<AlConversationResponse>>() {
        }.getType());
        if (apiResponse != null) {
            if (apiResponse.isSuccess()) {
                if (apiResponse.getResponse().getUserDetails() != null) {
                    processUserDetails(apiResponse.getResponse().getUserDetails());
                }
                if (apiResponse.getResponse().getGroupFeeds() != null) {
                    ChannelService.getInstance(context).processChannelFeedList(apiResponse.getResponse().getGroupFeeds(), false);
                }
                return Arrays.asList(apiResponse.getResponse().getMessage());
            } else if (apiResponse.getErrorResponse() != null) {
                throw new ApplozicException(GsonUtils.getJsonFromObject(apiResponse.getErrorResponse(), List.class));
            }
        }
        return null;
    }

    @Override
    public synchronized List<Message> getAlConversationList(int status, int pageSize, Long lastFetchTime, boolean makeServerCall) throws Exception {
        List<Message> conversationList = new ArrayList<>();
        List<Message> cachedConversationList = messageDatabaseService.getAlConversationList(status, lastFetchTime);

        if (!makeServerCall && !cachedConversationList.isEmpty()) {
            return cachedConversationList;
        }

        AlConversationResponse kmConversationResponse = null;
        try {
            String messageData = messageClientService.getKmConversationList(status, pageSize, lastFetchTime);
            if(messageData.equals(KmUtils.UN_AUTHORIZED)) {
                throw new Exception(KmUtils.UN_AUTHORIZED);
            }
            ApiResponse<AlConversationResponse> apiResponse = (ApiResponse<AlConversationResponse>) GsonUtils.getObjectFromJson(messageData, new TypeToken<ApiResponse<AlConversationResponse>>() {
            }.getType());

            if(apiResponse != null && apiResponse.getStatus().equals(KmUtils.ERROR) && apiResponse.getErrorResponse() != null && apiResponse.getErrorResponse().get(0).getErrorCode().equals(KmUtils.USER_NOT_FOUND_ERROR_CODE)) {
                throw new Exception(KmUtils.UN_AUTHORIZED);
            }
            if (apiResponse != null && apiResponse.getResponse() != null) {
                kmConversationResponse = apiResponse.getResponse();
            }
        } catch (Exception e) {
            KmExceptionAnalytics.captureException(e);
            e.printStackTrace();
            throw e;
        }

        if (kmConversationResponse == null) {
            return null;
        }

        try {
            if (kmConversationResponse.getUserDetails() != null) {
                processUserDetails(kmConversationResponse.getUserDetails());
            }

            if (kmConversationResponse.getGroupFeeds() != null) {
                ChannelService.getInstance(context).processChannelFeedList(kmConversationResponse.getGroupFeeds(), false);
            }

            Message[] messages = kmConversationResponse.getMessage();
            MobiComUserPreference userPreferences = MobiComUserPreference.getInstance(context);

            if (messages != null && messages.length > 0 && cachedConversationList.size() > 0 && cachedConversationList.get(0).isLocalMessage()) {
                if (cachedConversationList.get(0).equals(messages[0])) {
                    Utils.printLog(context, TAG, "Both messages are same.");
                    deleteMessage(cachedConversationList.get(0));
                }
            }

            for (Message message : messages) {
                if (!message.isCall() || userPreferences.isDisplayCallRecordEnable()) {
                    if (message.getTo() == null) {
                        continue;
                    }

                    if (message.hasAttachment() && !(message.getContentType() == Message.ContentType.TEXT_URL.getValue())) {
                        setFilePathifExist(message);
                    }
                    if (message.getContentType() == Message.ContentType.CONTACT_MSG.getValue()) {
                        FileClientService fileClientService = new FileClientService(context);
                        fileClientService.loadContactsvCard(message);
                    }
                    if (Message.MetaDataType.HIDDEN.getValue().equals(message.getMetaDataValueForKey(Message.MetaDataType.KEY.getValue())) || Message.MetaDataType.PUSHNOTIFICATION.getValue().equals(message.getMetaDataValueForKey(Message.MetaDataType.KEY.getValue()))) {
                        continue;
                    }
                    if (isHideActionMessage && message.isActionMessage() || message.isDeletedForAll()) {
                        message.setHidden(true);
                    }
                    if (messageDatabaseService.isMessagePresent(message.getKeyString(), Message.ReplyMessage.HIDE_MESSAGE.getValue())) {
                        messageDatabaseService.updateMessageReplyType(message.getKeyString(), Message.ReplyMessage.NON_HIDDEN.getValue());
                    } else {
                        messageDatabaseService.createMessage(message);
                    }

                    if (message.hasHideKey()) {
                        if (message.getGroupId() != null) {
                            Channel newChannel = ChannelService.getInstance(context).getChannelByChannelKey(message.getGroupId());
                            if (newChannel != null) {
                                getMessages(null, null, null, newChannel, null, true, false);
                            }
                        } else {
                            getMessages(null, null, new Contact(message.getContactIds()), null, null, true, false);
                        }
                    }
                }
                conversationList.add(message);
            }
            Intent intent = new Intent(MobiComKitConstants.APPLOZIC_UNREAD_COUNT);
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
        } catch (Exception e) {
            KmExceptionAnalytics.captureException(e);
            e.printStackTrace();
            throw e;
        }

        List<Message> finalMessageList = messageDatabaseService.getAlConversationList(status, lastFetchTime);
        List<String> messageKeys = new ArrayList<>();
        for (Message msg : finalMessageList) {
            if (msg.getTo() == null) {
                continue;
            }
            if (Message.MetaDataType.HIDDEN.getValue().equals(msg.getMetaDataValueForKey(Message.MetaDataType.KEY.getValue())) || Message.MetaDataType.PUSHNOTIFICATION.getValue().equals(msg.getMetaDataValueForKey(Message.MetaDataType.KEY.getValue()))) {
                continue;
            }
            if (msg.getMetadata() != null && msg.getMetaDataValueForKey(Message.MetaDataType.AL_REPLY.getValue()) != null && !messageDatabaseService.isMessagePresent(msg.getMetaDataValueForKey(Message.MetaDataType.AL_REPLY.getValue()))) {
                messageKeys.add(msg.getMetaDataValueForKey(Message.MetaDataType.AL_REPLY.getValue()));
            }
        }
        if (messageKeys != null && messageKeys.size() > 0) {
            Message[] replyMessageList = getMessageListByKeyList(messageKeys);
            if (replyMessageList != null) {
                for (Message replyMessage : replyMessageList) {
                    if (replyMessage.getTo() == null) {
                        continue;
                    }
                    if (Message.MetaDataType.HIDDEN.getValue().equals(replyMessage.getMetaDataValueForKey(Message.MetaDataType.KEY.getValue())) || Message.MetaDataType.PUSHNOTIFICATION.getValue().equals(replyMessage.getMetaDataValueForKey(Message.MetaDataType.KEY.getValue()))) {
                        continue;
                    }
                    if (replyMessage.hasAttachment() && !(replyMessage.getContentType() == Message.ContentType.TEXT_URL.getValue())) {
                        setFilePathifExist(replyMessage);
                    }
                    if (replyMessage.getContentType() == Message.ContentType.CONTACT_MSG.getValue()) {
                        FileClientService fileClientService = new FileClientService(context);
                        fileClientService.loadContactsvCard(replyMessage);
                    }
                    replyMessage.setReplyMessage(Message.ReplyMessage.HIDE_MESSAGE.getValue());
                    messageDatabaseService.createMessage(replyMessage);
                }
            }
        }

        if (!conversationList.isEmpty()) {
            Collections.sort(conversationList, new Comparator<Message>() {
                @Override
                public int compare(Message lhs, Message rhs) {
                    return lhs.getCreatedAtTime().compareTo(rhs.getCreatedAtTime());
                }
            });
        }
        return finalMessageList;
    }
}
