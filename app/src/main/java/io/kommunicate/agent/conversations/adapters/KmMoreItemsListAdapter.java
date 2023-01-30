package io.kommunicate.agent.conversations.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.library.baseAdapters.BR;
import androidx.recyclerview.widget.RecyclerView;

import com.applozic.mobicommons.ApplozicService;

import java.util.List;

import io.kommunicate.agent.R;
import io.kommunicate.agent.conversations.KmClickHandler;
import io.kommunicate.agent.databinding.KmMoreItemViewBinding;
import io.kommunicate.agent.model.KmMoreItem;
import io.kommunicate.agent.model.KmTag;
import io.kommunicate.utils.KmUtils;

public class KmMoreItemsListAdapter extends RecyclerView.Adapter<KmMoreItemsListAdapter.StatusViewHolder> {
    public static final int RESOLVE_ITEM_POSITION = 0;
    public static final int ASSIGNEE_ITEM_POSITION = 1;
    public static final int SPAM_ITEM_POSITION = 3;
    public static final int TAGS_ITEM_POSITION = 2;

    private List<KmMoreItem> kmMoreItemList;
    private KmClickHandler<KmMoreItem> kmClickHandler;

    public KmMoreItemsListAdapter(List<KmMoreItem> kmMoreItemList, KmClickHandler<KmMoreItem> kmClickHandler) {
        this.kmMoreItemList = kmMoreItemList;
        this.kmClickHandler = kmClickHandler;
    }

    public void updateItem(int position, KmMoreItem kmMoreItem) {
        kmMoreItemList.remove(position);
        kmMoreItemList.add(position, kmMoreItem);
        notifyItemChanged(position);
    }

    @NonNull
    @Override
    public StatusViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new StatusViewHolder(DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()),
                R.layout.km_more_item_view,
                parent,
                false));
    }

    @Override
    public void onBindViewHolder(@NonNull StatusViewHolder holder, int position) {
        holder.bind(kmMoreItemList.get(position));
    }

    @Override
    public int getItemCount() {
        return kmMoreItemList.size();
    }

    public class StatusViewHolder extends RecyclerView.ViewHolder {
        private final KmMoreItemViewBinding binding;

        public StatusViewHolder(KmMoreItemViewBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            binding.setClickHandler(kmClickHandler);
        }

        public void bind(KmMoreItem obj) {
            binding.setVariable(BR.moreItem, obj);
            binding.executePendingBindings();
            if (obj.getIconTintColorId() > 0) {
                KmUtils.setDrawableTint(binding.conversationStatusTextView, ApplozicService.getAppContext().getResources().getColor(obj.getIconTintColorId() == 0 ? obj.getColorResId() : obj.getIconTintColorId()), 0);
            }
        }
    }

    public void updateKmTagList(List<KmTag> tagList) {
        if (tagList != null) {
            kmMoreItemList.get(TAGS_ITEM_POSITION).setItemList(tagList);
            notifyItemChanged(TAGS_ITEM_POSITION);
        }
    }
}
