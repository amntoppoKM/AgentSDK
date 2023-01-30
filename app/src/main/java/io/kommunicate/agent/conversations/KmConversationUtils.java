package io.kommunicate.agent.conversations;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.applozic.mobicomkit.api.account.user.MobiComUserPreference;
import com.applozic.mobicomkit.api.conversation.Message;
import com.applozic.mobicomkit.channel.service.ChannelService;
import com.applozic.mobicomkit.uiwidgets.alphanumbericcolor.AlphaNumberColorUtil;
import com.applozic.mobicommons.ApplozicService;
import com.applozic.mobicommons.people.channel.Channel;
import com.applozic.mobicommons.people.channel.ChannelMetadata;
import com.applozic.mobicommons.people.contact.Contact;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.util.Iterator;
import java.util.List;

import io.kommunicate.utils.KmConstants;

import static android.view.View.VISIBLE;

public class KmConversationUtils {
    public static final String KM_STATUS_CLOSED = "Closed";
    public static final String KM_STATUS_SPAM_NEW = "spam";
    public static final int CLOSED_CONVERSATIONS = 2;
    public static final int ASSIGNED_CONVERSATIONS = 0;
    public static final int ALL_CONVERSATIONS = 1;

    public static int getConversationStatus(Context context, Message message) {
        if (message == null) {
            return -1;
        }

        if (message.getMetadata() == null) {
            return -1;
        }

        if (message.getMetadata().containsKey(Message.CONVERSATION_STATUS) && KM_STATUS_CLOSED.equals(message.getMetadata().get(Message.CONVERSATION_STATUS))) {
            return CLOSED_CONVERSATIONS;
        }

        if (message.getMetadata().containsKey(KmConstants.CONVERSATION_ASSIGNEE) && !TextUtils.isEmpty(message.getMetadata().get(KmConstants.CONVERSATION_ASSIGNEE))) {
            if (MobiComUserPreference.getInstance(context).getUserId().equals(message.getMetadata().get(KmConstants.CONVERSATION_ASSIGNEE))) {
                return ASSIGNED_CONVERSATIONS;
            }
            return ALL_CONVERSATIONS;
        }
        return -1;
    }

    public static void loadContactImage(Context context, Contact contact, TextView toolbarAlphabeticImage, ImageView toolbarImageView) {
        String imageUrl = "";
        String name = "";

        if (contact != null) {
            name = contact.getDisplayName();
            imageUrl = contact.getImageURL();
        }

        if (!TextUtils.isEmpty(imageUrl)) {
            toolbarAlphabeticImage.setVisibility(View.GONE);
            toolbarImageView.setVisibility(VISIBLE);
            try {
                if (contact != null) {
                    RequestOptions options = new RequestOptions()
                            .centerCrop()
                            .placeholder(com.applozic.mobicomkit.uiwidgets.R.drawable.km_ic_contact_picture_holo_light)
                            .error(com.applozic.mobicomkit.uiwidgets.R.drawable.km_ic_contact_picture_holo_light);


                    Glide.with(context).load(imageUrl).apply(options).into(toolbarImageView);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            toolbarAlphabeticImage.setVisibility(VISIBLE);
            toolbarImageView.setVisibility(View.GONE);

            String contactNumber = "";
            char firstLetter = 0;

            if (name == null) {
                return;
            }
            contactNumber = name.toUpperCase();
            firstLetter = name.toUpperCase().charAt(0);

            if (firstLetter != '+') {
                toolbarAlphabeticImage.setText(String.valueOf(firstLetter));
            } else if (contactNumber.length() >= 2) {
                toolbarAlphabeticImage.setText(String.valueOf(contactNumber.charAt(1)));
            }

            Character colorKey = AlphaNumberColorUtil.alphabetBackgroundColorMap.containsKey(firstLetter) ? firstLetter : null;
            GradientDrawable bgShape = (GradientDrawable) toolbarAlphabeticImage.getBackground();
            if (context != null) {
                bgShape.setColor(context.getResources().getColor(AlphaNumberColorUtil.alphabetBackgroundColorMap.get(colorKey)));
            }
        }
    }

    public static boolean isTypeClosed(Message message) {
        return Channel.AlConversationStatus.RESOLVED_STATUS.equalsIgnoreCase(message.getMetadata().get(Message.CONVERSATION_STATUS))
                || Channel.AlConversationStatus.SPAM_STATUS.equalsIgnoreCase(message.getMetadata().get(Message.CONVERSATION_STATUS))
                || KM_STATUS_CLOSED.equalsIgnoreCase(message.getMetadata().get(Message.CONVERSATION_STATUS))
                || KM_STATUS_SPAM_NEW.equalsIgnoreCase(message.getMetadata().get(Message.CONVERSATION_STATUS));
    }

    public static boolean isTypeOpen(Message message) {
        return Channel.AlConversationStatus.OPEN_STATUS.equalsIgnoreCase(message.getMetadata().get(Message.CONVERSATION_STATUS));
    }

    public static boolean isTypeDelete(Message message) {
        return message != null
                && message.getMetadata() != null
                && message.getMetadata().containsKey(ChannelMetadata.AL_CHANNEL_ACTION)
                && Message.GroupAction.DELETE_GROUP.getValue().equals(Short.valueOf(message.getMetadata().get(ChannelMetadata.AL_CHANNEL_ACTION)));
    }

    public static boolean isTypeAssigneeSwitch(Message message) {
        return !TextUtils.isEmpty(message.getAssigneId());
    }

    public static int addToStatus(Context context, Message message) {
        if (!message.isHidden()) {
            if (message.getGroupId() == null || message.getGroupId() == 0) {
                return 0;
            } else {
                Channel channel = ChannelService.getInstance(context).getChannelByChannelKey(message.getGroupId());
                return channel != null && Channel.GroupType.SUPPORT_GROUP.getValue().equals(channel.getType()) ? channel.getKmStatus() : 0;
            }
        }
        return -1;
    }

    //return the index of the message removed from the list
    public static synchronized int removeConversation(String userId, Integer groupId, List<Message> messageList) {
        Iterator<Message> iterator = messageList.iterator();

        while (iterator.hasNext()) {
            Message currentMessage = iterator.next();
            if ((currentMessage.getGroupId() != null && currentMessage.getGroupId() != 0 && currentMessage.getGroupId().equals(groupId))
                    || (currentMessage.getGroupId() == null && currentMessage.getContactIds() != null && currentMessage.getContactIds().equals(userId))) {
                int index = messageList.indexOf(currentMessage);
                iterator.remove();
                return index;
            }
        }
        return -1;
    }

    /*returns the index of message removed from certain position.
    If index is -1, no change is made in the list.
    If index > 0, message was removed from certain position and added at position 0;
    If index is 0, message was just added at index 0.
     */
    public static synchronized int addConversation(Message message, List<Message> messageList) {
        Iterator<Message> iterator = messageList.iterator();
        boolean shouldAdd = false;

        int removeIndex = -1;
        while (iterator.hasNext()) {
            Message currentMessage = iterator.next();

            if ((message.getGroupId() != null && currentMessage.getGroupId() != null && message.getGroupId().equals(currentMessage.getGroupId())) ||
                    (message.getGroupId() == null && currentMessage.getGroupId() == null && message.getContactIds() != null && currentMessage.getContactIds() != null &&
                            message.getContactIds().equals(currentMessage.getContactIds()))) {
                //do nothing
            } else {
                currentMessage = null;
            }

            if (currentMessage != null) {
                if (message.getCreatedAtTime() >= currentMessage.getCreatedAtTime()) {
                    removeIndex = messageList.indexOf(currentMessage);
                    iterator.remove();
                } else {
                    return -1;
                }
            }

            shouldAdd = true;
        }

        if (shouldAdd) {
            messageList.add(0, message);
            if (removeIndex == 0) {
                return -2;
            } else if (removeIndex != -1) {
                return removeIndex;
            }
            return 0;
        }
        return -1;
    }

    public static Spanned getHtmlString(int resId, Object... args) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Html.fromHtml(ApplozicService.getAppContext().getString(resId, args), Html.FROM_HTML_MODE_COMPACT);
        }
        return Html.fromHtml(ApplozicService.getAppContext().getString(resId, args));
    }
}
