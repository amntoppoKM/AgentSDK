package io.kommunicate.agent.conversations.viewmodels;

import android.text.Spanned;

public class KmNavItemModel {
    private int iconResId = 0;
    private Spanned title;
    private boolean showNotificationIcon = false;
    private boolean isSelected = false;
    private int position;

    public int getIconResId() {
        return iconResId;
    }

    public void setIconResId(int iconResId) {
        this.iconResId = iconResId;
    }

    public Spanned getTitle() {
        return title;
    }

    public void setTitle(Spanned title) {
        this.title = title;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public boolean isShowNotificationIcon() {
        return showNotificationIcon;
    }

    public void setShowNotificationIcon(boolean showNotificationIcon) {
        this.showNotificationIcon = showNotificationIcon;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }
}
