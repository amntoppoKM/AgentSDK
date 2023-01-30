package io.kommunicate.agent.conversations;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;

import com.applozic.mobicomkit.api.conversation.Message;

import java.util.List;

public class KmMessageDiffCallback extends DiffUtil.Callback {

    private final List<Message> mOldMessageList;
    private final List<Message> mNewMessageList;

    public KmMessageDiffCallback(List<Message> mOldMessageList, List<Message> mNewMessageList) {
        this.mOldMessageList = mOldMessageList;
        this.mNewMessageList = mNewMessageList;
    }

    @Override
    public int getOldListSize() {
        return mOldMessageList != null ? mOldMessageList.size() : 0;
    }

    @Override
    public int getNewListSize() {
        return mNewMessageList != null ? mNewMessageList.size() : 0;
    }

    @Override
    public boolean areItemsTheSame(int i, int i1) {
        return mOldMessageList.get(i).getKeyString().equals(mNewMessageList.get(i1).getKeyString());
    }

    @Override
    public boolean areContentsTheSame(int i, int i1) {
        final Message oldMessage = mOldMessageList.get(i);
        final Message newMessage = mNewMessageList.get(i1);

        return oldMessage.equals(newMessage);
    }

    @Nullable
    @Override
    public Object getChangePayload(int oldItemPosition, int newItemPosition) {
        return super.getChangePayload(oldItemPosition, newItemPosition);
    }
}
