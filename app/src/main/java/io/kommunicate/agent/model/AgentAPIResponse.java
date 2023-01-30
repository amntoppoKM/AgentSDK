package io.kommunicate.agent.model;

import java.util.ArrayList;

import io.kommunicate.models.KmApiResponse;

public class AgentAPIResponse<T> extends KmApiResponse {
    public static final String SUCCESS = "SUCCESS";
    private ArrayList<T> response;

    public ArrayList<T> getResponse() {
        return response;
    }

    public void setResponse(ArrayList<T> response) {
        this.response = response;
    }

    public boolean isSuccess() {
        return SUCCESS.equals(getCode());
    }
}
