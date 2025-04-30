package com.xus.mongo2sql.llm;

public class ModelConfigForRequest {
    private String apiKey;
    private String baseUrl;
    private String modelType;

    public ModelConfigForRequest(String apiKey, String baseUrl, String modelType) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.modelType = modelType;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getModelType() {
        return modelType;
    }

    public void setModelType(String modelType) {
        this.modelType = modelType;
    }
}