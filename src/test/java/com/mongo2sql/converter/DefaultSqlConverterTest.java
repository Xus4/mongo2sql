package com.mongo2sql.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.mongo2sql.parser.AggregationPipeline;
import com.mongo2sql.parser.MongoAggregationParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class DefaultSqlConverterTest {
    private DefaultSqlConverter converter;
    private MongoAggregationParser parser;

    @BeforeEach
    void setUp() {
        converter = new DefaultSqlConverter();
        parser = new MongoAggregationParser();
    }

    @Test
    void testMatchStageWithNestedField() throws JsonMappingException, JsonProcessingException {
        // 准备测试数据
        String mongoQuery = "{"
            + "\"command\": [{"
            + "\"$match\": {"
            + "\"content.formData.flowId\": \"123456\""
            + "}}"
            + "]}";
        String collectionName = "collectionTest";
        // 解析MongoDB查询并转换为SQL
        AggregationPipeline pipeline = parser.parse(mongoQuery);
        String sqlQuery = converter.convert(pipeline,collectionName);
        // 验证转换结果
        String expectedSql = "SELECT * FROM collectionTest WHERE flowId = '123456'";
        System.out.println(sqlQuery);
        System.out.println(expectedSql);

       assertEquals(expectedSql, sqlQuery.trim());
    }
}