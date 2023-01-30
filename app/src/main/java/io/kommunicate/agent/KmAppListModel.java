package io.kommunicate.agent;

import java.util.Map;

public class KmAppListModel {
    private String code;
    private Map<String, String> result;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Map<String, String> getResult() {
        return result;
    }

    public void setResult(Map<String, String> result) {
        this.result = result;
    }
}
