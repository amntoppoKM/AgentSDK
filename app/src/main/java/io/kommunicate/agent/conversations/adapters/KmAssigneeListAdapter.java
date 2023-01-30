package io.kommunicate.agent.conversations.adapters;

import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.TextAppearanceSpan;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.applozic.mobicommons.ApplozicService;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import io.kommunicate.agent.R;
import io.kommunicate.agent.databinding.KmAssigneeItemLayoutBinding;
import io.kommunicate.agent.model.KmAssignee;
import io.kommunicate.callbacks.KmCallback;

public class KmAssigneeListAdapter<T> extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements Filterable {

    private List<T> assigneeList;
    private List<T> originalList;
    private String assigneeId;
    private String searchText;
    private TextAppearanceSpan highlightTextSpan;
    public KmCallback callback;

    public KmAssigneeListAdapter(String assigneeId, KmCallback callback) {
        this.assigneeList = new ArrayList<>();
        this.assigneeId = assigneeId;
        this.highlightTextSpan = new TextAppearanceSpan(ApplozicService.getAppContext(), R.style.KmAssigneeNameBold);
        this.callback = callback;
    }

    @NonNull
    @Override
    public KmAssigneeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new KmAssigneeViewHolder(DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()),
                R.layout.km_assignee_item_layout,
                parent,
                false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ((KmAssigneeViewHolder) holder).bind(assigneeList.get(position));
    }

    public void addAssigneeList(List<T> assigneeList) {
        if (assigneeList != null && !assigneeList.isEmpty()) {
            this.assigneeList.clear();
            this.assigneeList.addAll(assigneeList);
            notifyDataSetChanged();
        }
    }

    public boolean isListEmpty() {
        return assigneeList == null || assigneeList.isEmpty();
    }

    @Override
    public int getItemCount() {
        return assigneeList == null ? 0 : assigneeList.size();
    }

    public class KmAssigneeViewHolder extends RecyclerView.ViewHolder {
        private KmAssigneeItemLayoutBinding binding;

        public KmAssigneeViewHolder(@NonNull KmAssigneeItemLayoutBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(T object) {
            KmAssignee item = (KmAssignee) object;
            if (item == null || TextUtils.isEmpty(item.getId())) {
                return;
            }

            String name = item.getTitle();
            binding.kmNameTextView.setText(name);
            binding.setIsCurrentAssignee(item.getId().equals(assigneeId));
            binding.setKmCallback(callback);
            binding.setObject(assigneeList.get(getAdapterPosition()));

            int startIndex = indexOfSearchQuery(item.getTitle());
            if (startIndex != -1) {
                final SpannableString highlightedName = new SpannableString(name);
                highlightedName.setSpan(highlightTextSpan, startIndex, startIndex + searchText.length(), 0);
                binding.kmNameTextView.setText(highlightedName);
            }

            binding.executePendingBindings();
        }
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {

                final FilterResults oReturn = new FilterResults();
                final List<KmAssignee> results = new ArrayList<>();
                if (originalList == null)
                    originalList = assigneeList;
                if (constraint != null) {
                    searchText = constraint.toString();
                    if (originalList != null && originalList.size() > 0) {
                        for (final T item : originalList) {
                            KmAssignee contact = (KmAssignee) item;
                            if (contact.getTitle().toLowerCase().contains(constraint.toString())) {
                                results.add(contact);
                            }
                        }
                    }
                    oReturn.values = results;
                } else {
                    oReturn.values = originalList;
                }
                return oReturn;
            }

            @SuppressWarnings("unchecked")
            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                assigneeList = (ArrayList<T>) results.values;
                callback.onSuccess(assigneeList.isEmpty());
                notifyDataSetChanged();
            }
        };
    }

    private int indexOfSearchQuery(String name) {
        if (!TextUtils.isEmpty(searchText)) {
            return name.toLowerCase(Locale.getDefault()).indexOf(searchText.toLowerCase(Locale.getDefault()));
        }
        return -1;
    }
}
