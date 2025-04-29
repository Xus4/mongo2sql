package com.mongo2sql.parser;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashMap;
import java.util.Map;

/**
 * 表示MongoDB $group阶段的实现类，用于执行分组操作。
 * 该阶段可以将文档按照指定字段分组，并对每个分组应用聚合操作。
 */
public class GroupStage implements PipelineStage {
    private final String idField;
    private final Map<String, GroupOperation> operations;

    public GroupStage(JsonNode groupSpec) {
        this.operations = new HashMap<>();
        
        // 解析_id字段
        JsonNode idNode = groupSpec.get("_id");
        if (idNode.isTextual()) {
            this.idField = removeDollarPrefix(idNode.asText());
        } else if (idNode.isObject()) {
            // 处理复合_id
            this.idField = parseCompositeId(idNode);
        } else {
            this.idField = null;
        }

        // 解析聚合操作
        groupSpec.fields().forEachRemaining(entry -> {
            String fieldName = entry.getKey();
            if (!"_id".equals(fieldName)) {
                JsonNode operationNode = entry.getValue();
                GroupOperation operation = parseOperation(operationNode);
                operations.put(fieldName, operation);
            }
        });
    }

    private String removeDollarPrefix(String field) {
        if (field.startsWith("$")) {
            return field.substring(1);
        }
        return field;
    }

    private String parseCompositeId(JsonNode idNode) {
        StringBuilder compositeId = new StringBuilder();
        idNode.fields().forEachRemaining(entry -> {
            if (compositeId.length() > 0) {
                compositeId.append(", ");
            }
            String fieldName = removeDollarPrefix(entry.getValue().asText());
            compositeId.append(entry.getKey()).append(": ").append(fieldName);
        });
        return compositeId.toString();
    }

    private GroupOperation parseOperation(JsonNode operationNode) {
        if (operationNode.isObject()) {
            String operator = operationNode.fieldNames().next();
            JsonNode value = operationNode.get(operator);
            String field = value.isTextual() ? removeDollarPrefix(value.asText()) : null;
            return new GroupOperation(operator, field);
        }
        return null;
    }

    public String getIdField() {
        return idField;
    }

    public Map<String, GroupOperation> getOperations() {
        return operations;
    }

    @Override
    public String getType() {
        return "$group";
    }

    public static class GroupOperation {
        private final String operator;
        private final String field;

        public GroupOperation(String operator, String field) {
            this.operator = operator;
            this.field = field;
        }

        public String getOperator() {
            return operator;
        }

        public String getField() {
            return field;
        }
    }
} 