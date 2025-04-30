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

public class CommonModelClient implements ModelClient {
    private static final Logger log = LoggerFactory.getLogger(CommonModelClient.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

	private ModelConfigForRequest modelConfig;

    public ModelConfigForRequest getModelConfig() {
		return modelConfig;
	}

	public void setModelConfig(ModelConfigForRequest modelConfig) {
		this.modelConfig = modelConfig;
	}

	public CommonModelClient(ModelConfigForRequest modelConfig) {
        this.modelConfig = modelConfig;
    }

    @Override
    public String chat(String prompt) throws IOException {
        QianwenRequest request = new QianwenRequest();
        request.setModel(this.modelConfig.getModelType());
        QianwenRequest.Message message = new QianwenRequest.Message();
        message.setRole("user");
        message.setContent(prompt);
        request.setMessages(Collections.singletonList(message));
        request.setParameters(new QianwenRequest.Parameters());

        String requestBody = objectMapper.writeValueAsString(request);
        log.debug("Request body: {}", requestBody);

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(modelConfig.getBaseUrl() + "/chat/completions");
            httpPost.setHeader("Authorization", "Bearer " + modelConfig.getApiKey());
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
