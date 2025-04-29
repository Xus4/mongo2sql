package com.xus.mongo2sql.llm;

import org.junit.jupiter.api.Test;

class QianwenClientTest {

    @Test
    void testChat() throws Exception {
        QianwenClient client = new QianwenClient();
        String prompt = "请将以下MongoDB聚合查询转换为格式化的MySQL SQL语句（只需要输出SQL语句本身）：\n" +
                "{\n" +
                "  \"$match\": { \"status\": \"active\" },\n" +
                "  \"$sort\": { \"createTime\": -1 },\n" +
                "  \"$project\": { \"id\": 1, \"name\": 1, \"status\": 1 }\n" +
                "}";
        
        String sqlContent = client.chat(prompt);
        // 解析返回的JSON结构，提取实际的SQL语句内容
        System.out.println("转换后的SQL语句：");
        System.out.println(sqlContent.trim());
    }
}