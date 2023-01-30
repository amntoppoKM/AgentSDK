package io.kommunicate.agent.model;

import io.kommunicate.users.KMUser;

public class KmAgentUser extends KMUser {
    private String oauthToken;

    public String getOauthToken() {
        return oauthToken;
    }

    public void setOauthToken(String oauthToken) {
        this.oauthToken = oauthToken;
    }
}
