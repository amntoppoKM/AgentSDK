package io.kommunicate.agent.model;

import android.text.TextUtils;

import org.jetbrains.annotations.NotNull;

import io.kommunicate.users.KmContact;

public class KmAgentContact extends KmContact implements KmAssignee {
    private String apzToken;
    private String name;
    private String userName;

    public String getApzToken() {
        return apzToken;
    }

    public void setApzToken(String apzToken) {
        this.apzToken = apzToken;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    @Override
    public String getDisplayName() {
        return !TextUtils.isEmpty(name) ? name : super.getDisplayName();
    }

    @Override
    public String getContactIds() {
        return !TextUtils.isEmpty(userName) ? userName : super.getContactIds();
    }

    @Override
    public String getUserId() {
        return !TextUtils.isEmpty(userName) ? userName : super.getUserId();
    }

    @NotNull
    @Override
    public String getId() {
        return getUserId();
    }

    @NotNull
    @Override
    public String getTitle() {
        return getDisplayName();
    }
}
