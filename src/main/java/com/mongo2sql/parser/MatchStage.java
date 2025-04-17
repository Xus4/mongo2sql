package com.mongo2sql.parser;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

/**
 * 表示MongoDB $match阶段的实现类，用于过滤文档。
 * 该阶段允许使用MongoDB查询操作符来筛选满足特定条件的文档。
 */
public class MatchStage implements PipelineStage {
    private final JsonNode criteria;

    public MatchStage(JsonNode criteria) {
        this.criteria = criteria;
    }

    public JsonNode getCriteria() {
        return criteria;
    }

    /**
     * 获取匹配条件的Map表示
     * @return 包含字段名和对应值的Map
     */
    public Map<String, Object> getConditions() {
        Map<String, Object> conditions = new HashMap<>();
        Iterator<Map.Entry<String, JsonNode>> fields = criteria.fields();
        
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String fieldName = field.getKey();
            JsonNode value = field.getValue();
            
            if (value.isTextual()) {
                conditions.put(fieldName, value.asText());
            } else if (value.isNumber()) {
                conditions.put(fieldName, value.numberValue());
            } else if (value.isBoolean()) {
                conditions.put(fieldName, value.asBoolean());
            } else {
                conditions.put(fieldName, value.toString());
            }
        }
        
        return conditions;
    }

    @Override
    public String getType() {
        return "$match";
    }
}