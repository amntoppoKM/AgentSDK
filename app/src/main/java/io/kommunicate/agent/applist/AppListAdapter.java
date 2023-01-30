package io.kommunicate.agent.applist;

import android.content.Context;
import android.graphics.Color;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

import io.kommunicate.agent.AgentSharedPreference;
import io.kommunicate.agent.R;
import io.kommunicate.callbacks.KmCallback;

/**
 * Created by ashish on 09/02/18.
 */

public class AppListAdapter extends RecyclerView.Adapter<AppListAdapter.MyViewHolder> implements AdapterView.OnItemClickListener {

    private final Context context;
    private final Map<String, String> appMap;
    private final String[] colorArray;
    private final KmCallback callback;
    private AgentSharedPreference agentSharedPreference;

    public AppListAdapter(Context context, Map<String, String> appMap, KmCallback callback) {
        this.context = context;
        this.appMap = appMap;
        this.callback = callback;
        colorArray = context.getResources().getStringArray(R.array.applistColors);
        agentSharedPreference = AgentSharedPreference.getInstance(context);
    }

    @NotNull
    @Override
    public MyViewHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        return new MyViewHolder(inflater.inflate(R.layout.km_applist_item_view, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull @NotNull MyViewHolder myViewHolder, int position) {
        String appName = appMap.values().toArray()[position].toString();
        String appKey = appMap.keySet().toArray()[position].toString();

        if (!TextUtils.isEmpty(appKey)) {
            myViewHolder.appKey.setVisibility(View.VISIBLE);
            myViewHolder.appKey.setText(appKey);
        } else {
            myViewHolder.appKey.setVisibility(View.GONE);
        }

        if (!TextUtils.isEmpty(appName)) {
            myViewHolder.appName.setVisibility(View.VISIBLE);
            myViewHolder.appName.setText(appName);

            if (agentSharedPreference.getAgentDetails() != null && appKey.equals(agentSharedPreference.getAgentDetails().getApplicationId())) {
                myViewHolder.active.setVisibility(View.VISIBLE);
            } else {
                myViewHolder.active.setVisibility(View.GONE);
            }
        } else {
            myViewHolder.appName.setVisibility(View.GONE);
        }

        if (position < colorArray.length) {
            myViewHolder.appColor.setBackgroundColor(Color.parseColor(colorArray[position]));
        }
    }

    @Override
    public int getItemCount() {
        return appMap.size();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

    }

    class MyViewHolder extends RecyclerView.ViewHolder {

        TextView appName;
        TextView appKey;
        TextView active;
        View appColor;
        LinearLayout rootLayout;

        public MyViewHolder(View itemView) {
            super(itemView);
            appName = itemView.findViewById(R.id.kmAppName);
            appKey = itemView.findViewById(R.id.kmAppKey);
            appColor = itemView.findViewById(R.id.kmApplistColor);
            rootLayout = itemView.findViewById(R.id.rootLayout);
            active = itemView.findViewById(R.id.kmActive);

            rootLayout.setOnClickListener(v -> {
                int position = getLayoutPosition();
                if (callback != null) {
                    callback.onSuccess(appMap.keySet().toArray()[position].toString());
                }
            });
        }
    }
}
