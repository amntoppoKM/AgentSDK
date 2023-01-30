package io.kommunicate.agent;

public interface LoginListener {
    void processAppList(String email, String password, boolean isGoogleSignIn);

    void openSSOPage();
}
