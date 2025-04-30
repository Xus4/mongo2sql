package com.xus.mongo2sql.llm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xus.mongo2sql.llm.model.QianwenRequest;
import com.xus.mongo2sql.llm.model.QianwenResponse;

public class QianwenClient {
    private static final Logger log = LoggerFactory.getLogger(QianwenClient.class);
    private static final String BASE_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1";
    private static final String API_KEY = "sk-5301c805d71e4e97821cbe4665b16436";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private QianwenRequest.QianwenModel currentModel;

    public QianwenRequest.QianwenModel getCurrentModel() {
        return currentModel;
    }

    public String chat(String prompt, QianwenRequest.QianwenModel model) throws IOException {
        this.currentModel = model;
        QianwenRequest request = new QianwenRequest();
        request.setModel(model.getValue());
        QianwenRequest.Message message = new QianwenRequest.Message();
        message.setRole("user");
        message.setContent(prompt);
        request.setMessages(Collections.singletonList(message));
        request.setParameters(new QianwenRequest.Parameters());

        String requestBody = objectMapper.writeValueAsString(request);
        log.debug("Request body: {}", requestBody);

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(BASE_URL + "/chat/completions");
            httpPost.setHeader("Authorization", "Bearer " + API_KEY);
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setHeader("Accept", "text/event-stream");
            httpPost.setEntity(new StringEntity(requestBody, StandardCharsets.UTF_8));

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    StringBuilder resultBuilder = new StringBuilder();
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (line.startsWith("data:")) {
                                String jsonData = line.substring(5).trim();
                                if (!jsonData.equals("[DONE]")) {
                                    QianwenResponse streamResponse = objectMapper.readValue(jsonData, QianwenResponse.class);
                                    if (streamResponse.getChoices() != null && !streamResponse.getChoices().isEmpty()) {
                                        QianwenResponse.Message delta = streamResponse.getChoices().get(0).getDelta();
                                        if (delta != null && delta.getContent() != null) {
                                            resultBuilder.append(delta.getContent());
                                        }
                                    }
                                }
                            }
                        }
                    }
                    return resultBuilder.toString();
                }
                throw new IOException("No response content available");
            }
        }
    }
}