package io.kommunicate.agent.conversations.adapters;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.applozic.mobicomkit.api.conversation.database.MessageDatabaseService;
import com.applozic.mobicommons.commons.core.utils.Utils;

import java.util.ArrayList;
import java.util.List;

import io.kommunicate.agent.AgentSharedPreference;
import io.kommunicate.agent.R;
import io.kommunicate.agent.conversations.KmConversationUtils;
import io.kommunicate.agent.conversations.viewmodels.KmConversationViewModel;
import io.kommunicate.agent.conversations.viewmodels.KmNavItemModel;
import io.kommunicate.utils.KmUtils;

public class KmNavigationItemAdapter extends RecyclerView.Adapter {

    private Context context;
    private List<KmNavItemModel> navItemModelList;
    private KmNavItemClickListener listener;
    private MessageDatabaseService messageDatabaseService;
    private KmNavItemModel currentUserReportItem;

    public KmNavigationItemAdapter(Context context, KmNavItemClickListener listener) {
        this.context = context;
        this.listener = listener;

        messageDatabaseService = new MessageDatabaseService(context);

        navItemModelList = new ArrayList<>();
        KmNavItemModel awayStatusItem = new KmNavItemModel();
        awayStatusItem.setTitle(KmConversationUtils.getHtmlString(R.string.km_set_away_text, Utils.getString(context, R.string.away)));
        awayStatusItem.setIconResId(R.drawable.ic_away_icon);
        awayStatusItem.setPosition(0);
        awayStatusItem.setSelected(true);

        KmNavItemModel editProfileItem = new KmNavItemModel();
        editProfileItem.setIconResId(R.drawable.ic_edit_profile);
        editProfileItem.setPosition(1);
        editProfileItem.setTitle(KmConversationUtils.getHtmlString(R.string.km_edit_profile, ""));

        KmNavItemModel switchApplicationItem = new KmNavItemModel();
        switchApplicationItem.setIconResId(R.drawable.ic_switch_application);
        switchApplicationItem.setPosition(2);
        switchApplicationItem.setTitle(KmConversationUtils.getHtmlString(R.string.km_switch_application, ""));

        KmNavItemModel userReportItem = new KmNavItemModel();
        userReportItem.setIconResId(R.drawable.ic_share_feedback);
        userReportItem.setPosition(3);
        userReportItem.setTitle(KmConversationUtils.getHtmlString(R.string.km_share_feedback, ""));
        currentUserReportItem = userReportItem;

        KmNavItemModel logoutItem = new KmNavItemModel();
        logoutItem.setIconResId(R.drawable.ic_logout_icon);
        logoutItem.setPosition(4);
        logoutItem.setTitle(KmConversationUtils.getHtmlString(R.string.km_logout, ""));

        navItemModelList.add(awayStatusItem);
        navItemModelList.add(editProfileItem);
        if (AgentSharedPreference.getInstance(context).hasMultipleApplication() && !AgentSharedPreference.getInstance(context).isSSOLogin()) {
            navItemModelList.add(switchApplicationItem);
        }

        if (!AgentSharedPreference.getInstance(context).isTrialExpired()) {
            navItemModelList.add(userReportItem);
        }
        navItemModelList.add(logoutItem);
    }

    public void updateNavigationItem() {
        if (!AgentSharedPreference.getInstance(context).isTrialExpired()) {
            return;
        }

        if (navItemModelList.contains(currentUserReportItem)) {
            navItemModelList.remove(currentUserReportItem);
            notifyDataSetChanged();
        }
    }

    public void updateAwayStatus(Integer status) {
        navItemModelList.get(0).setTitle(KmConversationUtils.getHtmlString(R.string.km_set_away_text, Utils.getString(context, status == KmConversationViewModel.STATUS_ONLINE ? R.string.away : R.string.online)));
        notifyItemChanged(0);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (inflater == null) {
            return null;
        }
        return new MyViewHolder(inflater.inflate(R.layout.km_custom_navigation_item_view, viewGroup, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int i) {
        MyViewHolder myViewHolder = (MyViewHolder) viewHolder;
        KmNavItemModel model = navItemModelList.get(i);

        if (model.getIconResId() != 0) {
            myViewHolder.icon.setImageResource(model.getIconResId());
        }

        if (!TextUtils.isEmpty(model.getTitle())) {
            myViewHolder.title.setText(model.getTitle());
        }

        KmUtils.setBackground(context, myViewHolder.itemView, model.isSelected() ? R.color.nav_item_pressed_color : R.color.applozic_transparent_color);
    }

    @Override
    public int getItemCount() {
        return navItemModelList.size();
    }

    private class MyViewHolder extends RecyclerView.ViewHolder {

        private ImageView icon;
        private TextView title;

        public MyViewHolder(@NonNull final View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.km_navigation_item_icon);
            title = itemView.findViewById(R.id.km_navigation_item_text);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    for (KmNavItemModel temp : navItemModelList) {
                        temp.setSelected(false);
                    }
                    navItemModelList.get(getLayoutPosition()).setSelected(true);
                    if (listener != null) {
                        listener.onKmNavItemClick(getLayoutPosition(), navItemModelList.get(getLayoutPosition()));
                    }
                    notifyDataSetChanged();
                }
            });
        }
    }

    public interface KmNavItemClickListener {
        void onKmNavItemClick(int position, KmNavItemModel model);
    }
}
