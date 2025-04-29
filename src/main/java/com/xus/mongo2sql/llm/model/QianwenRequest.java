package com.xus.mongo2sql.llm.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class QianwenRequest {
    public enum QianwenModel {
        QWEN_TURBO("qwen-turbo"),
        QWEN_PLUS("qwen-plus-latest"),
        QWEN3_235B("qwen3-235b-a22b"),
    	R1("deepseek-r1");

        private final String value;

        QianwenModel(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    private String model = QianwenModel.QWEN_TURBO.getValue();
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

    public static String cleanSql(String sql) {
        return sql.replaceAll("^```sql\\s+|\\s+```$", "");
    }
}