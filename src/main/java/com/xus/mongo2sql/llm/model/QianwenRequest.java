package com.xus.mongo2sql.llm.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class QianwenRequest {
    private String model = "qwen3-235b-a22b";
    private List<Message> messages;
    private Parameters parameters;
    private boolean stream = true;

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

    public Parameters getParameters() {
        return parameters;
    }

    public void setParameters(Parameters parameters) {
        this.parameters = parameters;
    }

    public boolean isStream() {
        return stream;
    }

    public void setStream(boolean stream) {
        this.stream = stream;
    }

    public static class Message {
        private String role;
        private String content;

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }

    public static class Parameters {
        @JsonProperty("result_format")
        private String resultFormat = "text";

        public String getResultFormat() {
            return resultFormat;
        }

        public void setResultFormat(String resultFormat) {
            this.resultFormat = resultFormat;
        }
    }
}