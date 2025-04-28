package com.mongo2sql.parser;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

/**
 * SetStage类表示MongoDB聚合管道中的$set阶段。
 * 该类负责处理字段设置操作，包括字符串处理函数如$substr等。
 */
public class SetStage implements PipelineStage {
    private Map<String, Object> setFields;

    public SetStage() {
        this.setFields = new HashMap<>();
    }

    /**
     * 构造函数，解析$set阶段的配置。
     *
     * @param config $set阶段的配置节点
     */
    public SetStage(JsonNode config) {
        this.setFields = new HashMap<>();
        this.parseSetFields(config);
    }

    /**
     * 解析$set阶段的配置，构建字段设置映射。
     *
     * @param config 配置节点
     */
    private void parseSetFields(JsonNode config) {
        Iterator<Map.Entry<String, JsonNode>> fields = config.fields();

        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String fieldName = field.getKey();
            JsonNode fieldValue = field.getValue();

            if (fieldValue.isObject()) {
                // 处理表达式操作符
                setFields.put(fieldName, parseExpression(fieldValue));
            } else if (fieldValue.isTextual()) {
                // 处理字段引用，例如："$fieldName"
                String value = fieldValue.asText();
                if (value.startsWith("$")) {
                    Map<String, Object> fieldReference = new HashMap<>();
                    fieldReference.put("$field", value.substring(1));
                    setFields.put(fieldName, fieldReference);
                } else {
                    // 处理普通字符串值
                    setFields.put(fieldName, value);
                }
            } else {
                // 处理其他类型的值（数字、布尔等）
                setFields.put(fieldName, fieldValue);
            }
        }
    }

    /**
     * 解析字段表达式。
     *
     * @param expressionNode 表达式节点
     * @return 解析后的表达式对象
     */
    private Object parseExpression(JsonNode expressionNode) {
        Map<String, Object> expression = new HashMap<>();
        Iterator<Map.Entry<String, JsonNode>> operators = expressionNode.fields();

        while (operators.hasNext()) {
            Map.Entry<String, JsonNode> operator = operators.next();
            String operatorName = operator.getKey();
            JsonNode operatorValue = operator.getValue();

            if (operatorValue.isArray()) {
                // 处理数组类型的操作符参数，如$substr
                Object[] params = new Object[operatorValue.size()];
                for (int i = 0; i < operatorValue.size(); i++) {
                    JsonNode param = operatorValue.get(i);
                    if (param.isTextual() && param.asText().startsWith("$")) {
                        // 处理字段引用
                        Map<String, Object> fieldRef = new HashMap<>();
                        fieldRef.put("$field", param.asText().substring(1));
                        params[i] = fieldRef;
                    } else if (param.isNumber()) {
                        // 直接保留数值类型
                        params[i] = param.numberValue();
                    } else {
                        // 处理其他类型的参数
                        params[i] = getNodeValue(param);
                    }
                }
                expression.put(operatorName, params);
            } else {
                expression.put(operatorName, getNodeValue(operatorValue));
            }
        }

        return expression;
    }

    /**
     * 获取JsonNode的值
     *
     * @param node JsonNode节点
     * @return 节点的值
     */
    private Object getNodeValue(JsonNode node) {
        if (node.isTextual()) {
            return node.asText();
        } else if (node.isNumber()) {
            return node.numberValue();
        } else if (node.isBoolean()) {
            return node.asBoolean();
        } else {
            return node.toString();
        }
    }

    /**
     * 获取设置字段的配置
     * @return 设置字段的映射
     */
    public Map<String, Object> getSetFields() {
        return setFields;
    }

    @Override
    public String getType() {
        return "$set";
    }
}