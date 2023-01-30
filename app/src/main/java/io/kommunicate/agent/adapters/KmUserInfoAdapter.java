package io.kommunicate.agent.adapters;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;
import java.util.Map;

import io.kommunicate.agent.KmUserInfoHelper;
import io.kommunicate.agent.R;

public class KmUserInfoAdapter extends RecyclerView.Adapter {
    private Context context;
    private Map<String, String> dataMap;
    private List<String> dataKeySet;

    public KmUserInfoAdapter(Context context, Map<String, String> dataMap) {
        this.context = context;
        this.dataMap = dataMap;
    }

    public void setDataKeySet(List<String> dataKeySet) {
        this.dataKeySet = KmUserInfoHelper.filterAndReturnUserInfoDataKeySet(dataKeySet);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        return new MyViewHolder(inflater.inflate(R.layout.km_user_details_item_layout, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        MyViewHolder mViewHolder = (MyViewHolder) holder;
        String dataKey = dataKeySet.get(position);

        if (!TextUtils.isEmpty(dataKey)) {
            mViewHolder.userInfoTitle.setText(dataKey);
        } else {
            mViewHolder.userInfoTitle.setText("");
        }

        if (!TextUtils.isEmpty(dataMap.get(dataKey))) {
            mViewHolder.userInfoValue.setText(dataMap.get(dataKey));
        } else {
            mViewHolder.userInfoValue.setText("");
        }
    }

    @Override
    public int getItemCount() {
        return dataKeySet != null ? dataKeySet.size() : 0;
    }

    private class MyViewHolder extends RecyclerView.ViewHolder {
        private TextView userInfoTitle;
        private TextView userInfoValue;

        private MyViewHolder(View itemView) {
            super(itemView);
            userInfoTitle = itemView.findViewById(R.id.kmUserInfoTitle);
            userInfoValue = itemView.findViewById(R.id.kmUserInfoValue);
        }
    }
}
