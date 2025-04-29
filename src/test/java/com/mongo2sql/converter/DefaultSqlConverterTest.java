package com.mongo2sql.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongo2sql.parser.AggregationPipeline;
import com.mongo2sql.parser.LookupStage;
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

    @Test
    public void testLookupConversion() throws Exception {
        // 准备测试数据
        String mongoQuery = "{ \"$lookup\": { \"from\": \"users\", \"localField\": \"userId\", \"foreignField\": \"id\", \"as\": \"userInfo\" } }";
        
        // 解析MongoDB查询
        ObjectMapper mapper = new ObjectMapper();
        JsonNode lookupNode = mapper.readTree(mongoQuery).get("$lookup");
        LookupStage lookupStage = new LookupStage(lookupNode);
        
        // 创建转换器
        DefaultSqlConverter converter = new DefaultSqlConverter();
        StringBuilder joinClause = new StringBuilder();
        
        // 执行转换
        converter.handleLookupStage(lookupStage, joinClause);
        
        // 验证结果
        String expectedSql = " LEFT JOIN users userInfo ON orders.userId = userInfo.id";
        assertEquals(expectedSql, joinClause.toString());
    }
    
    @Test
    public void testLookupInPipeline() throws Exception {
        // 准备测试数据
        String mongoQuery = "["
            + "{ \"$match\": { \"status\": \"active\" } },"
            + "{ \"$lookup\": { \"from\": \"users\", \"localField\": \"userId\", \"foreignField\": \"id\", \"as\": \"userInfo\" } },"
            + "{ \"$project\": { \"id\": 1, \"name\": 1, \"userInfo.name\": 1 } }"
            + "]";
        
        // 解析MongoDB查询
        MongoAggregationParser parser = new MongoAggregationParser();
        AggregationPipeline pipeline = parser.parse(mongoQuery);
        
        // 创建转换器
        DefaultSqlConverter converter = new DefaultSqlConverter();
        
        // 执行转换
        String sql = converter.convert(pipeline, "orders");
        
        // 验证结果
        String expectedSql = "SELECT id, name, userInfo.name FROM orders LEFT JOIN users userInfo ON orders.userId = userInfo.id WHERE status = 'active'";
        assertEquals(expectedSql, sql);
    }
}