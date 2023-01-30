package io.kommunicate.agent.conversations.adapters;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.os.ResultReceiver;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.applozic.mobicomkit.api.conversation.Message;
import com.applozic.mobicomkit.uiwidgets.conversation.richmessaging.views.KmFlowLayout;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

import io.kommunicate.agent.R;
import io.kommunicate.agent.conversations.KmConversationStatusListener;
import io.kommunicate.agent.conversations.KmMessageProperties;
import io.kommunicate.agent.conversations.activity.AllConversationActivity;
import io.kommunicate.agent.conversations.activity.CustomConversationActivity;
import io.kommunicate.agent.model.KmTag;
import io.kommunicate.utils.KmConstants;

public class KmConversationListAdapter extends KmLoaderAdapter {

    private KmMessageProperties kmMessageProperties;
    private KmConversationStatusListener listener;
    private List<KmTag> kmTagList;

    public KmConversationListAdapter(Context context, List<Message> mItems, KmConversationStatusListener listener) {
        super(context, mItems);
        kmMessageProperties = new KmMessageProperties(context);
        this.listener = listener;
    }

    public void updateKmTagList(List<KmTag> tagList) {
        if (tagList != null) {
            this.kmTagList = tagList;
            notifyDataSetChanged();
        }
    }

    @Override
    public RecyclerView.ViewHolder getConversationViewHolder(ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (inflater == null) {
            return null;
        }
        return new KmConversationViewHolder(inflater.inflate(R.layout.km_conversation_list_item, parent, false));
    }

    @Override
    public void bindConversationViewHolder(RecyclerView.ViewHolder holder, int position) {
        Message message = messageList.get(position);
        if (message != null && kmMessageProperties != null) {
            kmMessageProperties.setMessage(message);
            KmConversationViewHolder mViewHolder = (KmConversationViewHolder) holder;

            mViewHolder.receiverName.setText(kmMessageProperties.getReceiver());
            mViewHolder.createdAtTime.setText(kmMessageProperties.getCreatedAtTime());
            kmMessageProperties.setMessageAndAttchmentIcon(mViewHolder.messageTv, mViewHolder.attachmentIcon);
            kmMessageProperties.setUnreadCount(mViewHolder.unreadCount);
            kmMessageProperties.loadProfileImage(mViewHolder.profileImage, mViewHolder.alphabeticImage);
            if(kmMessageProperties.isTagAvailable()) {
                mViewHolder.tagList.setVisibility(View.VISIBLE);
                kmMessageProperties.setTagList(mViewHolder.tagList, kmTagList);
            }
            else {
                mViewHolder.tagList.setVisibility(View.GONE);

            }
        }
    }

    public class KmConversationViewHolder extends RecyclerView.ViewHolder {
        TextView alphabeticImage;
        CircleImageView profileImage;
        TextView receiverName;
        TextView messageTv;
        TextView unreadCount;
        TextView createdAtTime;
        ImageView attachmentIcon;
        KmFlowLayout tagList;

        public KmConversationViewHolder(@NonNull View view) {
            super(view);
            alphabeticImage = view.findViewById(R.id.alphabeticImage);
            profileImage = view.findViewById(R.id.contactImage);
            receiverName = view.findViewById(R.id.smReceivers);
            messageTv = view.findViewById(R.id.message);
            unreadCount = view.findViewById(R.id.unreadSmsCount);
            createdAtTime = view.findViewById(R.id.createdAtTime);
            attachmentIcon = view.findViewById(R.id.attachmentIcon);
            tagList = view.findViewById(R.id.km_tags_list);

            view.setOnClickListener(v -> {
                int itemPosition = getLayoutPosition();

                if (itemPosition != -1 && !messageList.isEmpty() && itemPosition < messageList.size()) {
                    Message message = messageList.get(itemPosition);

                    if (message != null) {
                        Intent intent = new Intent(context, CustomConversationActivity.class);
                        intent.putExtra(KmConstants.TAKE_ORDER, true);

                        intent.putExtra(AllConversationActivity.CONVERSATION_RESULT_RECEIVER, new ResultReceiver(null) {
                            @Override
                            protected void onReceiveResult(int resultCode, Bundle resultData) {
                                if (listener != null) {
                                    switch (resultCode) {
                                        case CustomConversationActivity.STATUS_RESULT_CODE:
                                            listener.onStatusChange(resultData.getInt(CustomConversationActivity.NEW_STATUS));
                                            break;
                                        case CustomConversationActivity.ASSIGNEE_RESULT_CODE:
                                            listener.onAssigneeChange(resultData.getString(CustomConversationActivity.NEW_ASSIGNEE_ID), null);
                                            break;
                                    }
                                }
                            }
                        });

                        if (message.getGroupId() == null) {
                            intent.putExtra(KmConstants.USER_ID, message.getContactIds());
                            if (kmMessageProperties != null) {
                                kmMessageProperties.setOpenedUserId(message.getContactIds());
                            }
                        } else {
                            intent.putExtra(KmConstants.GROUP_ID, message.getGroupId());
                            if (kmMessageProperties != null) {
                                kmMessageProperties.setOpenedChannelKey(message.getGroupId());
                            }
                        }
                        context.startActivity(intent);
                    }
                }
            });
        }
    }
}
