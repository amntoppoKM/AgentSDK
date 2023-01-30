package io.kommunicate.agent.conversations.adapters;

import android.content.Context;

import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.applozic.mobicomkit.api.conversation.Message;

import java.util.List;

import io.kommunicate.agent.R;

public abstract class KmLoaderAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public static int LOADER_VIEW = 1;
    public static int CONVERSATION_VIEW = 2;
    private boolean loading;
    public List<Message> messageList;
    protected LayoutInflater mInflater;
    public Context context;

    public KmLoaderAdapter(Context context, List<Message> mItems) {
        mInflater = LayoutInflater.from(context);
        this.context = context;
        this.messageList = mItems;
    }

    public boolean isLoading() {
        return loading;
    }

    public void setMessageList(List<Message> messageList) {
        this.messageList = messageList;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        if (viewType == LOADER_VIEW) {
            return new FooterViewHolder(new LinearLayout(viewGroup.getContext()));
        } else if (viewType == CONVERSATION_VIEW) {
            return getConversationViewHolder(viewGroup);
        }

        throw new IllegalArgumentException("Invalid ViewType: " + viewType);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {

        if (viewHolder instanceof FooterViewHolder) {
            FooterViewHolder loaderViewHolder = (FooterViewHolder) viewHolder;
            loaderViewHolder.showLoadingItems(loading);

            return;
        }

        bindConversationViewHolder(viewHolder, position);
    }

    @Override
    public int getItemViewType(int position) {
        if (position == getItemCount() - 1) {
            return LOADER_VIEW;
        }
        return CONVERSATION_VIEW;
    }

    @Override
    public int getItemCount() {
        return messageList.size() + 1;
    }

    public void showLoading(boolean status) {
        loading = status;
        notifyItemChanged(messageList.size());
    }

    public class FooterViewHolder extends RecyclerView.ViewHolder {

        LinearLayout loadingItems;

        public FooterViewHolder(View itemView) {
            super(itemView);
            loadingItems = (LinearLayout) itemView;
            loadingItems.setOrientation(LinearLayout.VERTICAL);
            for(int i = 0; i < 10; i++) {
                loadingItems.addView(View.inflate(itemView.getContext(), R.layout.km_conversation_list_item_shimmer, null));
            }
        }

        void showLoadingItems(boolean show) {
            loadingItems.setVisibility(show ? View.VISIBLE : View.GONE);
            if(show) {
                loadingItems.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            } else {
                loadingItems.setLayoutParams(new LinearLayout.LayoutParams(0, 0));
            }
        }
    }

    public abstract RecyclerView.ViewHolder getConversationViewHolder(ViewGroup parent);

    public abstract void bindConversationViewHolder(RecyclerView.ViewHolder holder, int position);
}
