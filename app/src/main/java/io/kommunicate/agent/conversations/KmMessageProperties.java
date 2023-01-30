package io.kommunicate.agent.conversations;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;

import androidx.annotation.Nullable;

import android.text.Html;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.applozic.mobicomkit.api.account.user.MobiComUserPreference;
import com.applozic.mobicomkit.api.conversation.Message;
import com.applozic.mobicomkit.api.conversation.database.MessageDatabaseService;
import com.applozic.mobicomkit.cache.MessageSearchCache;
import com.applozic.mobicomkit.channel.database.ChannelDatabaseService;
import com.applozic.mobicomkit.channel.service.ChannelService;
import com.applozic.mobicomkit.contact.AppContactService;
import com.applozic.mobicomkit.contact.BaseContactService;
import com.applozic.mobicomkit.uiwidgets.alphanumbericcolor.AlphaNumberColorUtil;
import com.applozic.mobicomkit.uiwidgets.conversation.activity.ConversationActivity;
import com.applozic.mobicomkit.uiwidgets.conversation.richmessaging.views.KmFlowLayout;
import com.applozic.mobicomkit.uiwidgets.kommunicate.utils.DimensionsUtils;
import com.applozic.mobicommons.ApplozicService;
import com.applozic.mobicommons.commons.core.utils.DateUtils;
import com.applozic.mobicommons.commons.core.utils.Utils;
import com.applozic.mobicommons.emoticon.EmoticonUtils;
import com.applozic.mobicommons.json.GsonUtils;
import com.applozic.mobicommons.people.channel.Channel;
import com.applozic.mobicommons.people.channel.ChannelUtils;
import com.applozic.mobicommons.people.contact.Contact;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.google.android.flexbox.FlexboxLayout;
import com.google.gson.reflect.TypeToken;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.lifecycle.ViewModelProvider;
import de.hdodenhof.circleimageview.CircleImageView;
import io.kommunicate.agent.R;
import io.kommunicate.agent.exception.KmExceptionAnalytics;
import io.kommunicate.agent.model.KmTag;
import io.kommunicate.agent.viewmodels.KmTagsViewModel;
import io.kommunicate.utils.KmUtils;

/**
 * This class returns the properties of a message for your Recycler view's adapter.
 * for e.g the receiver's name from a message object.
 */

public class KmMessageProperties {
    private static final String CONVERSATION_SOURCE = "source";
    private static final String SOURCE_FACEBOOK = "FACEBOOK";
    private Context context;
    private Context appContext;
    private BaseContactService contactService;
    private ChannelDatabaseService channelService;
    private MessageDatabaseService messageDatabase;
    private Message message;
    private Contact contact;
    private Channel channel;
    private Map<Integer, String> userIdCacheMap;
    private Map<String, Contact> contactCacheMap;
    private Integer openedChannelKey;
    private String openedUserId;
    private boolean useCachedData;

    /**
     * This constructor should be initialised only once. You can do this in the constructor of your Adapter.
     *
     * @param context pass the calling Context
     */
    public KmMessageProperties(final Context context) {
        this(context, false);
    }

    public KmMessageProperties(final Context context, boolean useCachedData) {
        this.context = context;
        this.appContext = context;
        this.useCachedData = useCachedData;

        if (!useCachedData) {
            contactService = new AppContactService(context);
            messageDatabase = new MessageDatabaseService(context);
            channelService = ChannelDatabaseService.getInstance(context);

            if (contactCacheMap == null) {
                contactCacheMap = new HashMap<>();
            }
            if (userIdCacheMap == null) {
                userIdCacheMap = new HashMap<>();
            }
        }
    }

    /**
     * This method is used to set message to this object. This will save you headache passing message object into every method.
     * This has to called in your bindView/getView method of your adapter.
     * This method creates a contact object, if message is from a user and a channel object if the message is from a group.
     *
     * @param message Pass the current Message object from the position.
     * @return Instance of this.
     */
    public KmMessageProperties setMessage(Message message) {
        this.message = message;
        if (message.getGroupId() == null) {
            contact = getContactByIdWithCache(message.getContactIds());
            channel = null;
        } else {
            channel = getChannelByKeyWithCache(message.getGroupId());
            if (channel != null) {
                if (Channel.GroupType.GROUPOFTWO.getValue().equals(channel.getType())) {
                    contact = getContactByIdWithCache(ChannelService.getInstance(context).getGroupOfTwoReceiverUserId(channel.getKey()));
                    channel = null;
                } else {
                    contact = null;
                }
            }
        }
        return this;
    }

    public boolean isTagAvailable() {
        return channel != null && channel.getMetadata() != null && channel.getMetadata().containsKey("KM_TAGS");
    }

    public void setTagList(KmFlowLayout tagListLayout, List<KmTag> kmTagList) {
        if(channel != null && channel.getMetadata() != null && channel.getMetadata().containsKey("KM_TAGS") && kmTagList != null) {
            try {
                List<KmTag> tagList = KmTag.Companion.getFilteredTagList(ChannelService.getInstance(context).getChannel(channel.getKey()), kmTagList);
                tagListLayout.removeAllViews();
                FlexboxLayout.LayoutParams layoutParams = new FlexboxLayout.LayoutParams(FlexboxLayout.LayoutParams.WRAP_CONTENT, FlexboxLayout.LayoutParams.WRAP_CONTENT);
                layoutParams.setMargins(0, 0, DimensionsUtils.convertDpToPx(6), DimensionsUtils.convertDpToPx(4));
                layoutParams.setFlexBasisPercent(50);
                for (KmTag object : tagList) {
                TextView itemTextView = new TextView(tagListLayout.getContext());
                itemTextView.setText(object.getName());
                itemTextView.setPadding(DimensionsUtils.convertDpToPx(6), DimensionsUtils.convertDpToPx(5), DimensionsUtils.convertDpToPx(6), DimensionsUtils.convertDpToPx(5));
                itemTextView.setTextSize(12);
                itemTextView.setLayoutParams(layoutParams);
                itemTextView.setBackground(KmUtils.getDrawable(tagListLayout.getContext(), R.drawable.km_tag_background));
                itemTextView.setBackgroundColor(Utils.getColor(ApplozicService.getAppContext(), R.color.km_tag_background_color));
                itemTextView.setTextColor(Utils.getColor(ApplozicService.getAppContext(), R.color.km_tag_text_color));
                    tagListLayout.addView(itemTextView);
                }
            } catch (Exception e) {
                    e.printStackTrace();
                KmExceptionAnalytics.captureException(e);
            }
        } else {
            tagListLayout.setVisibility(View.GONE);
        }
    }

    /**
     * This method returns the receiver's name from the message object that this instance holds currently.
     *
     * @return If the message belongs to group it will return group's name and if it belongs to user, the display name of the user will be returned.
     */
    public String getReceiver() {
        if (channel != null && !Channel.GroupType.GROUPOFTWO.getValue().equals(channel.getType())) {
            return ChannelUtils.getChannelTitleName(channel, MobiComUserPreference.getInstance(context).getUserId());
        } else if (contact != null) {
            return contact.getDisplayName();
        }
        return null;
    }

    /**
     * Returns the Message string that needs to be displayed in the conversation list item.
     *
     * @return Message string if the message is of type text.
     * File's name if the message has attachment.
     * "Location" if the message is of type location etc
     */
    public void setMessageAndAttchmentIcon(TextView messageTv, ImageView attachmentIcon) {
        if (message.hasAttachment() && !Message.ContentType.TEXT_URL.getValue().equals(message.getContentType())) {
            messageTv.setText(message.getFileMetas() == null && message.getFilePaths() != null ? message.getFilePaths().get(0).substring(message.getFilePaths().get(0).lastIndexOf("/") + 1) :
                    message.getFileMetas() != null ? message.getFileMetas().getName() : "");
            if (attachmentIcon != null) {
                attachmentIcon.setVisibility(View.VISIBLE);
                attachmentIcon.setImageResource(R.drawable.km_ic_action_attachment);
            }
        } else if (Message.ContentType.LOCATION.getValue().equals(message.getContentType())) {
            messageTv.setText(context.getString(R.string.Location));
            if (attachmentIcon != null) {
                attachmentIcon.setVisibility(View.VISIBLE);
                attachmentIcon.setImageResource(R.drawable.mobicom_notification_location_icon);
            }
        } else if (Message.ContentType.TEXT_HTML.getValue().equals(message.getContentType())) {
            messageTv.setText(Html.fromHtml(message.getMessage()));
            if (attachmentIcon != null) {
                attachmentIcon.setVisibility(View.GONE);
            }
        } else if (Message.ContentType.PRICE.getValue().equals(message.getContentType())) {
            messageTv.setText(EmoticonUtils.getSmiledText(appContext, message.getMessage(), null));
            if (attachmentIcon != null) {
                attachmentIcon.setVisibility(View.GONE);
            }
        } else {
            messageTv.setText((!TextUtils.isEmpty(message.getMessage()) ? message.getMessage().substring(0, Math.min(message.getMessage().length(), 50)) : ""));
            showConversationSourceIcon(channel, attachmentIcon);
        }
    }

    /**
     * Sets the unread count to a textView for the current conversation.
     *
     * @param textView The TextView to display the unread count.
     */
    public void setUnreadCount(TextView textView) {
        int unreadCount = 0;

        if (!useCachedData) {
            if (message.getGroupId() == null) {
                unreadCount = messageDatabase.getUnreadMessageCountForContact(message.getContactIds());
            } else {
                unreadCount = messageDatabase.getUnreadMessageCountForChannel(message.getGroupId());
            }
        }

        if (unreadCount > 0) {
            textView.setVisibility(View.VISIBLE);
            textView.setText(String.valueOf(unreadCount));
        } else {
            textView.setVisibility(View.GONE);
        }
    }

    /**
     * Returns the formatted time for the conversation.
     *
     * @return Formatted time as String.
     */
    public String getCreatedAtTime() {
        return DateUtils.getFormattedDateAndTime(context, message.getCreatedAtTime(), R.string.JUST_NOW, R.plurals.MINUTES, R.plurals.HOURS);
    }

    /**
     * This method loads the image for a Contact into the ImageView. If the user does not have image url set, it will create an alphabeticText image.
     * This will automatically check if the image is set and handle the views visibility by itself.
     *
     * @param imageView CircularImageView which loads the image for the user.
     * @param textView  TextView which will display the alphabeticText image.
     * @param contact   The Contact object whose image is to be displayed.
     */
    public void loadContactImage(CircleImageView imageView, TextView textView, String displayName, Contact contact) {
        try {
            textView.setVisibility(View.VISIBLE);
            imageView.setVisibility(View.GONE);
            String contactNumber = "";
            char firstLetter = 0;
            contactNumber = displayName.toUpperCase();
            firstLetter = displayName.toUpperCase().charAt(0);

            if (firstLetter != '+') {
                textView.setText(String.valueOf(firstLetter));
            } else if (contactNumber.length() >= 2) {
                textView.setText(String.valueOf(contactNumber.charAt(1)));
            }

            Character colorKey = AlphaNumberColorUtil.alphabetBackgroundColorMap.containsKey(firstLetter) ? firstLetter : null;
            GradientDrawable bgShape = (GradientDrawable) textView.getBackground();
            bgShape.setColor(context.getResources().getColor(AlphaNumberColorUtil.alphabetBackgroundColorMap.get(colorKey)));

            if (contact != null) {
                if (contact.isDrawableResources()) {
                    textView.setVisibility(View.GONE);
                    imageView.setVisibility(View.VISIBLE);
                    int drawableResourceId = context.getResources().getIdentifier(contact.getrDrawableName(), "drawable", context.getPackageName());
                    imageView.setImageResource(drawableResourceId);
                } else if (contact.getImageURL() != null) {
                    loadImage(imageView, textView, contact.getImageURL(), 0);
                } else {
                    textView.setVisibility(View.VISIBLE);
                    imageView.setVisibility(View.GONE);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @return Returns the channel object if the message is from channel, null otherwise.
     */
    public Channel getChannel() {
        return channel;
    }

    /**
     * @return Returns the Contact object if the message is from a user, null otherwise.
     */
    public Contact getContact() {
        return contact;
    }

    /**
     * This method loads the channel's image into the ImageView
     *
     * @param imageView CircularImageView in which the image is to be loaded
     * @param textView  Although we do not display alphabeticImage for a group, but this is needed to handle the visibility for recycler view.
     * @param channel   Channel object whose image is to be loaded
     */
    public void loadChannelImage(CircleImageView imageView, TextView textView, Channel channel) {
        textView.setVisibility(View.GONE);
        imageView.setVisibility(View.VISIBLE);

        if (!TextUtils.isEmpty(channel.getImageUrl())) {
            loadImage(imageView, textView, channel.getImageUrl(), R.drawable.km_group_icon);
        } else if (Channel.GroupType.SUPPORT_GROUP.getValue().equals(channel.getType())) {
            loadContactImage(imageView, textView, channel.getName(), null);
        } else {
            imageView.setImageResource(R.drawable.km_group_icon);
        }
    }

    /**
     * This methods saves you a lot of work by check. Use this method in your bindView/getView.
     *
     * @param imageView CircularImageView to load the image
     * @param textView  TextView to display AlphabeticImage
     */
    public void loadProfileImage(CircleImageView imageView, TextView textView) {
        if (channel != null && !Channel.GroupType.GROUPOFTWO.getValue().equals(channel.getType())) {
            loadChannelImage(imageView, textView, channel);
        } else if (contact != null) {
            loadContactImage(imageView, textView, contact.getDisplayName(), contact);
        }
    }

    /**
     * This method loads the image into ImageView using Glide.
     *
     * @param imageView        CircularImageView
     * @param textImage        TextView
     * @param imageUrl         Image Url
     * @param placeholderImage The res id for the placeholder image
     */
    private void loadImage(final CircleImageView imageView, final TextView textImage, String imageUrl, int placeholderImage) {
        RequestOptions options = new RequestOptions()
                .centerCrop()
                .placeholder(placeholderImage)
                .error(placeholderImage);


        Glide.with(context).load(imageUrl).apply(options).listener(new RequestListener<Drawable>() {
            @Override
            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                return false;
            }

            @Override
            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                if (textImage != null) {
                    textImage.setVisibility(View.GONE);
                }
                imageView.setVisibility(View.VISIBLE);
                return false;
            }
        }).into(imageView);
    }

    /**
     * This method launches the Message thread from conversation click.
     *
     * @param message
     */
    public void handleConversationClick(Message message) {
        Intent intent = new Intent(context, ConversationActivity.class);
        intent.putExtra("takeOrder", true);
        if (message.getGroupId() == null) {
            intent.putExtra("userId", message.getContactIds());
        } else {
            intent.putExtra("groupId", message.getGroupId());
        }
        context.startActivity(intent);
    }

    private Contact getContactById(String userId) {
        if (contactCacheMap.containsKey(userId)) {
            return contactCacheMap.get(userId);
        }

        Contact contact = TextUtils.isEmpty(userId) ? null : contactService.getContactById(userId);
        if (contact != null) {
            contactCacheMap.put(userId, contact);
        }

        return contact;
    }

    public Integer getOpenedChannelKey() {
        return openedChannelKey;
    }

    public void setOpenedChannelKey(Integer openedChannelKey) {
        this.openedChannelKey = openedChannelKey;
    }

    public void setOpenedUserId(String userId) {
        this.openedUserId = userId;
    }

    public String getOpenedUserId() {
        return openedUserId;
    }

    public static boolean areContactsSame(Contact c1, Contact c2) {
        if (c1 == null || c2 == null) {
            return false;
        }
        return !TextUtils.isEmpty(c1.getDisplayName()) && c1.getDisplayName().equals(c2.getDisplayName()) &&
                !TextUtils.isEmpty(c1.getUserId()) && c1.getUserId().equals(c2.getUserId()) &&
                !TextUtils.isEmpty(c1.getImageURL()) && c1.getImageURL().equals(c2.getImageURL());
    }

    public Channel getChannelByKeyWithCache(Integer key) {
        if (useCachedData) {
            return MessageSearchCache.getChannelByKey(key);
        } else {
            return channelService.getChannelByChannelKey(key);
        }
    }

    public Contact getContactByIdWithCache(String userId) {
        if (useCachedData) {
            return MessageSearchCache.getContactById(userId);
        } else {
            return contactService.getContactById(userId);
        }
    }

    private void showConversationSourceIcon(Channel channel, ImageView attachmentIcon) {
        if (channel != null
                && Channel.GroupType.SUPPORT_GROUP.getValue().equals(channel.getType())
                && channel.getMetadata() != null
                && channel.getMetadata().containsKey(CONVERSATION_SOURCE)) {
            attachmentIcon.setVisibility(View.VISIBLE);
            if (SOURCE_FACEBOOK.equals(channel.getMetadata().get(CONVERSATION_SOURCE))) {
                attachmentIcon.setImageResource(R.drawable.ic_facebook_icon);
            }
        } else {
            attachmentIcon.setVisibility(View.GONE);
        }
    }
}
