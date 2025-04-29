package com.xus.mongo2sql.llm;

import org.junit.jupiter.api.Test;

import com.xus.mongo2sql.llm.model.QianwenRequest;

class QianwenClientTest {

    @Test
    void testCompareModels() throws Exception {
        QianwenClient client = new QianwenClient();
        String prompt = "请将以下MongoDB聚合查询转换为格式化的MySQL SQL语句（只需要输出SQL语句本身,不需要任何markdown格式，但是需要输出格式化sql）：\n" +
                "{\n" +
                "  \"$match\": { \"status\": \"active\" },\n" +
                "  \"$sort\": { \"createTime\": -1 },\n" +
                "  \"$project\": { \"id\": 1, \"name\": 1, \"status\": 1 }\n" +
                "}";

        // 调用三个不同的模型
        String resultTurbo = client.chat(prompt, QianwenRequest.QianwenModel.QWEN_TURBO);
        String resultPlus = client.chat(prompt, QianwenRequest.QianwenModel.QWEN_PLUS);
        String resultQWEN3 = client.chat(prompt, QianwenRequest.QianwenModel.QWEN3_235B);

        // 打印对比结果
        System.out.println("=== 模型对比结果 ===");
        System.out.println("\n QWEN_TURBO 模型结果：");
        System.out.println(resultTurbo.trim());
        
        System.out.println("\n QWEN_PLUS 模型结果：");
        System.out.println(resultPlus.trim());
        
        System.out.println("\n QWEN3_235B 模型结果：");
        System.out.println(resultQWEN3.trim());

    }
}