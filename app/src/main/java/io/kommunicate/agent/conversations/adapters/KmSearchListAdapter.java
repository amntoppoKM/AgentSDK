package io.kommunicate.agent.conversations.adapters;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.applozic.mobicomkit.api.conversation.Message;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import io.kommunicate.agent.R;
import io.kommunicate.agent.conversations.KmMessageProperties;
import io.kommunicate.agent.conversations.activity.CustomConversationActivity;
import io.kommunicate.utils.KmConstants;

public class KmSearchListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Context context;
    private List<Message> messageList;
    private KmMessageProperties kmMessageProperties;
    private String searchString;

public KmSearchListAdapter(Context context) {
    this.context = context;
    kmMessageProperties = new KmMessageProperties(context);
}

    public void setMessageList(List<Message> messageList) {
        this.messageList = messageList;
    }

    public void setSearchString(String searchString) {
        this.searchString = searchString;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (inflater == null) {
            return null;
        }
        return new KmConversationViewHolder(inflater.inflate(R.layout.km_conversation_list_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messageList.get(position);
        if (message != null && kmMessageProperties != null) {
            kmMessageProperties.setMessage(message);
            KmConversationViewHolder mViewHolder = (KmConversationViewHolder) holder;

            mViewHolder.receiverName.setText(kmMessageProperties.getReceiver());
            mViewHolder.createdAtTime.setText(kmMessageProperties.getCreatedAtTime());
            kmMessageProperties.setMessageAndAttchmentIcon(mViewHolder.messageTv, mViewHolder.attachmentIcon);
            kmMessageProperties.setUnreadCount(mViewHolder.unreadCount);
            kmMessageProperties.loadProfileImage(mViewHolder.profileImage, mViewHolder.alphabeticImage);
        }
    }

    @Override
    public int getItemCount() {
        return messageList == null ? 0 : messageList.size();
    }

    public class KmConversationViewHolder extends RecyclerView.ViewHolder {
        TextView alphabeticImage;
        CircleImageView profileImage;
        TextView receiverName;
        TextView messageTv;
        TextView unreadCount;
        TextView createdAtTime;
        ImageView attachmentIcon;

        public KmConversationViewHolder(@NonNull View view) {
            super(view);
            alphabeticImage = view.findViewById(R.id.alphabeticImage);
            profileImage = view.findViewById(R.id.contactImage);
            receiverName = view.findViewById(R.id.smReceivers);
            messageTv = view.findViewById(R.id.message);
            unreadCount = view.findViewById(R.id.unreadSmsCount);
            createdAtTime = view.findViewById(R.id.createdAtTime);
            attachmentIcon = view.findViewById(R.id.attachmentIcon);

            view.setOnClickListener(v -> {
                int itemPosition = getLayoutPosition();
                if (itemPosition != -1 && !messageList.isEmpty() && itemPosition < messageList.size()) {
                    Message message = messageList.get(itemPosition);

                    if (message != null) {
                        Intent intent = new Intent(context, CustomConversationActivity.class);
                        intent.putExtra(KmConstants.TAKE_ORDER, true);

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
