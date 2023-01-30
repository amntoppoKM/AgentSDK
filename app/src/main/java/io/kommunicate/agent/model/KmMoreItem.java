package io.kommunicate.agent.model;

import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;
import androidx.databinding.BindingAdapter;
import androidx.databinding.library.baseAdapters.BR;

import com.applozic.mobicomkit.uiwidgets.conversation.richmessaging.views.KmFlowLayout;
import com.applozic.mobicomkit.uiwidgets.kommunicate.utils.DimensionsUtils;
import com.applozic.mobicommons.ApplozicService;
import com.applozic.mobicommons.commons.core.utils.Utils;
import com.google.android.flexbox.FlexboxLayout;

import java.util.List;

import io.kommunicate.agent.R;
import io.kommunicate.utils.KmUtils;

public class KmMoreItem extends BaseObservable {
    private MoreItemType moreItemType;
    private boolean visible;
    private Drawable icon;
    private String titleText;
    private int colorResId;
    private String extensionText;
    private boolean titleTextStyleBold;
    private int iconTintColorId;
    private boolean tagsViewAvailable = true;
    private List<KmTag> itemList;

    public KmMoreItem() {
    }

    private KmMoreItem(MoreItemType moreItemType, String titleText, int colorResId, int iconId, int iconTintColorId) {
        this.moreItemType = moreItemType;
        this.iconTintColorId = iconTintColorId;
        setTitleText(titleText);
        setColorResId(colorResId);
        setIconId(iconId);
    }

    public static KmMoreItem getStatusMoreItem(int status) {
        return new KmMoreItem(MoreItemType.STATUS, KmConversationStatus.getStatusText(status), R.color.black, KmConversationStatus.getIconId(false), R.color.km_resolve_icon_color);
    }

    public static KmMoreItem getSpamMoreItem() {
        return new KmMoreItem(MoreItemType.SPAM, KmConversationStatus.MARK_AS_SPAM, R.color.black, KmConversationStatus.getIconId(true), R.color.km_spam_icon_color);
    }

    public static KmMoreItem getTranscriptMoreItem() {
        return new KmMoreItem(MoreItemType.TRANSCRIPT, Utils.getString(ApplozicService.getAppContext(), R.string.km_send_transcript), R.color.black, R.drawable.km_transcript_icon, R.color.holo_blue);
    }

    public static KmMoreItem getAssigneeNameMoreItem(String assigneeName) {
        KmMoreItem assigneeItem = new KmMoreItem();
        assigneeItem.setMoreItemType(MoreItemType.ASSIGNEE_CHANGE);
        assigneeItem.setTitleText(Utils.getString(ApplozicService.getAppContext(), R.string.km_assign_to_message));
        assigneeItem.setIconId(R.drawable.ic_assignee);
        assigneeItem.setTitleTextStyleBold(true);
        assigneeItem.setExtensionText(assigneeName);
        assigneeItem.setColorResId(R.color.black);
        assigneeItem.setIconTintColorId(R.color.km_assignee_icon_tint_color);
        return assigneeItem;
    }

    public static KmMoreItem getTagsMoreItem(boolean isTagFeatureAvailable) {
        KmMoreItem tagsItem = new KmMoreItem();
        tagsItem.setMoreItemType(MoreItemType.TAG);
        tagsItem.setTagsViewAvailable(isTagFeatureAvailable);
        tagsItem.setTitleText(Utils.getString(ApplozicService.getAppContext(), R.string.km_tags));
        tagsItem.setIconId(R.drawable.ic_tag);
        tagsItem.setColorResId(R.color.black);

        if (isTagFeatureAvailable) {
            tagsItem.setExtensionText(Utils.getString(ApplozicService.getAppContext(), R.string.km_manage));
        }
        return tagsItem;
    }

    @Bindable
    public Drawable getIcon() {
        return icon;
    }

    public void setIconId(int iconResId) {
        this.icon = ContextCompat.getDrawable(ApplozicService.getAppContext(), iconResId);
        notifyPropertyChanged(BR.icon);
    }

    @Bindable
    public String getTitleText() {
        return titleText;
    }

    public void setTitleText(String titleText) {
        this.titleText = titleText;
        notifyPropertyChanged(BR.titleText);
    }

    @Bindable
    public int getColorResId() {
        return colorResId;
    }

    public void setColorResId(int colorResId) {
        this.colorResId = ContextCompat.getColor(ApplozicService.getAppContext(), colorResId);
        notifyPropertyChanged(BR.colorResId);
    }

    @Bindable
    public String getExtensionText() {
        return extensionText;
    }

    public void setExtensionText(String extensionText) {
        this.extensionText = extensionText;
        notifyPropertyChanged(BR.extensionText);
    }

    @Bindable
    public boolean isTitleTextStyleBold() {
        return titleTextStyleBold;
    }

    public void setTitleTextStyleBold(boolean titleTextStyleBold) {
        this.titleTextStyleBold = titleTextStyleBold;
        notifyPropertyChanged(BR.titleTextStyleBold);
    }

    public int getIconTintColorId() {
        return iconTintColorId;
    }

    public void setIconTintColorId(int iconTintColorId) {
        this.iconTintColorId = iconTintColorId;
    }

    @BindingAdapter("isBold")
    public static void setBold(TextView view, boolean statusTextStyleBold) {
        view.setTypeface(null, statusTextStyleBold ? Typeface.BOLD : Typeface.NORMAL);
    }

    @BindingAdapter("setData")
    public static void setData(KmFlowLayout flowLayout, List<KmTag> itemList) {
        if (itemList == null) {
            return;
        }

        flowLayout.removeAllViews();

        FlexboxLayout.LayoutParams layoutParams = new FlexboxLayout.LayoutParams(FlexboxLayout.LayoutParams.WRAP_CONTENT, FlexboxLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(0, 0, DimensionsUtils.convertDpToPx(4), DimensionsUtils.convertDpToPx(5));
        for (KmTag object : itemList) {
            TextView itemTextView = new TextView(flowLayout.getContext());
            itemTextView.setText(((KmTag) object).getName());
            itemTextView.setPadding(DimensionsUtils.convertDpToPx(8), DimensionsUtils.convertDpToPx(6), DimensionsUtils.convertDpToPx(8), DimensionsUtils.convertDpToPx(6));
            itemTextView.setTextSize(15);
            itemTextView.setLayoutParams(layoutParams);
            itemTextView.setBackground(KmUtils.getDrawable(flowLayout.getContext(), R.drawable.km_tag_background));
            itemTextView.setBackgroundColor(Utils.getColor(ApplozicService.getAppContext(), R.color.km_tag_background_color));
            itemTextView.setTextColor(Utils.getColor(ApplozicService.getAppContext(), R.color.km_tag_text_color));
            //To be used in future when color codes become stable. Currently no option to set color.
            /*try {
                ((GradientDrawable) itemTextView.getBackground()).setColor(Color.parseColor(object.getColor()));
            } catch (Exception e) {
                e.printStackTrace();
            }*/
            flowLayout.addView(itemTextView);
        }

    }

    public MoreItemType getMoreItemType() {
        return moreItemType;
    }

    public void setMoreItemType(MoreItemType moreItemType) {
        this.moreItemType = moreItemType;
    }

    public boolean isTagsViewAvailable() {
        return tagsViewAvailable;
    }

    public void setTagsViewAvailable(boolean tagsViewAvailable) {
        this.tagsViewAvailable = tagsViewAvailable;
    }

    public List<KmTag> getItemList() {
        return itemList;
    }

    public void setItemList(List<KmTag> itemList) {
        this.itemList = itemList;
    }

    @Override
    public String toString() {
        return "KmMoreItem{" +
                "visible=" + visible +
                ", iconResId=" + icon +
                ", titleText='" + titleText + '\'' +
                ", colorResId=" + colorResId +
                '}';
    }

    public enum MoreItemType {
        STATUS, ASSIGNEE_CHANGE, TAG, SPAM, TRANSCRIPT;
    }
}
