package com.mongo2sql.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;

/**
 * MongoDB聚合查询解析器，负责将MongoDB聚合查询字符串解析为结构化的管道对象。
 * 该类使用Jackson库解析JSON格式的MongoDB聚合查询，并创建相应的管道阶段对象。
 */
public class MongoAggregationParser {
    private final ObjectMapper objectMapper;

    /**
     * 构造函数，初始化JSON解析器。
     */
    public MongoAggregationParser() {
        this.objectMapper = new ObjectMapper();
        // 配置ObjectMapper以处理格式化的JSON字符串
        this.objectMapper.configure(com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        this.objectMapper.configure(com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
        this.objectMapper.configure(com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_COMMENTS, true);
    }

    /**
     * 解析MongoDB聚合查询字符串，转换为聚合管道对象。
     * 
     * @param mongoQuery MongoDB聚合查询的JSON字符串
     * @return 解析后的聚合管道对象
     * @throws RuntimeException 当解析失败时抛出异常
     */
    public AggregationPipeline parse(String mongoQuery) {
        try {
            JsonNode rootNode = objectMapper.readTree(mongoQuery);
            JsonNode pipelineNode = rootNode.get("command");
            
            // 如果没有command字段，则直接使用根节点作为管道数组
            if (pipelineNode == null) {
                pipelineNode = rootNode;
            }
            
            List<PipelineStage> stages = new ArrayList<>();
            for (JsonNode stageNode : pipelineNode) {
                String operator = stageNode.fieldNames().next();
                JsonNode operatorValue = stageNode.get(operator);
                
                PipelineStage stage = createStage(operator, operatorValue);
                stages.add(stage);
            }
            
            return new AggregationPipeline(stages);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse MongoDB aggregation query", e);
        }
    }

    /**
     * 根据操作符类型创建对应的管道阶段对象。
     * 
     * @param operator 管道阶段的操作符（如$match、$lookup等）
     * @param operatorValue 操作符对应的配置值
     * @return 创建的管道阶段对象
     * @throws UnsupportedOperationException 当遇到不支持的操作符时抛出异常
     */
    private PipelineStage createStage(String operator, JsonNode operatorValue) {
        switch (operator) {
            case "$match":
                return new MatchStage(operatorValue);
            case "$lookup":
                return new LookupStage(operatorValue);
            case "$unwind":
                return new UnwindStage(operatorValue);
            case "$project":
                return new ProjectStage(operatorValue);
            default:
                throw new UnsupportedOperationException("Unsupported aggregation operator: " + operator);
        }
    }

    public JsonNode getPipelineNode(String mongoQuery) {
        try {
            JsonNode rootNode = objectMapper.readTree(mongoQuery);
            JsonNode pipelineNode = rootNode.get("command");
            
            // 如果没有command字段，则直接使用根节点作为管道数组
            if (pipelineNode == null) {
                pipelineNode = rootNode;
            }
            
            return pipelineNode;
        } catch (Exception e) {
            throw new RuntimeException("Failed to get pipeline node from MongoDB query", e);
        }
    }

    public ObjectMapper getObjectMapper() {
        return this.objectMapper;
    }
}