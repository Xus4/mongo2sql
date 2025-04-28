package com.mongo2sql.parser;

import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * ProjectStage类表示MongoDB聚合管道中的$project阶段。
 * 该类负责处理字段投影，特别是将content.formData下的字段映射到SQL表的列。
 */
public class ProjectStage implements PipelineStage {
    private Map<String, Object> projections;
    private String collectionName;

    public ProjectStage() {
        this.projections = new HashMap<>();
    }

    /**
     * 构造函数，解析$project阶段的配置。
     *
     * @param config $project阶段的配置节点
     */
    public ProjectStage(JsonNode config) {
        this.projections = new HashMap<>();
        this.parseProjections(config);
    }

    /**
     * 添加需要投影的字段
     * @param field MongoDB中的字段路径
     * @param include 是否包含该字段
     */
    public void addProjection(String field, boolean include) {
        projections.put(field, include ? 1 : 0);
    }

    /**
     * 解析$project阶段的配置，构建投影映射。
     *
     * @param config 配置节点
     */
    private void parseProjections(JsonNode config) {
        Iterator<Map.Entry<String, JsonNode>> fields = config.fields();

        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String fieldName = field.getKey();
            JsonNode fieldValue = field.getValue();

            if (fieldValue.isNumber()) {
                // 处理简单的字段包含/排除（1表示包含，0表示排除）
                projections.put(fieldName, fieldValue.asInt());
            } else if (fieldValue.isObject()) {
                // 处理表达式操作符
                projections.put(fieldName, parseExpression(fieldValue));
            } else if (fieldValue.isBoolean()) {
                // 处理布尔值（true表示包含，false表示排除）
                projections.put(fieldName, fieldValue.asBoolean() ? 1 : 0);
            } else if (fieldValue.isTextual()) {
                // 处理字段重命名或字段引用，例如："xxx": "approvalDate" 或 "xxx": "$fieldName"
                String value = fieldValue.asText();
                if (value.startsWith("$")) {
                    // 如果是字段引用（以$开头），则去除$前缀
                    String originalField = value.substring(1);
                    // 只有当目标字段名与原字段名不同时，才添加重命名表达式
                    if (!fieldName.equals(originalField)) {
                        Map<String, Object> renameExpression = new HashMap<>();
                        renameExpression.put("$field", originalField);
                        projections.put(fieldName, renameExpression);
                    } else {
                        // 字段名相同时，直接使用原字段名
                        projections.put(fieldName, 1);
                    }
                } else {
                    // 处理普通字符串值
                    Map<String, Object> renameExpression = new HashMap<>();
                    renameExpression.put("$field", value);
                    projections.put(fieldName, renameExpression);
                }
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
                // 处理数组类型的操作符参数
                expression.put(operatorName, operatorValue);
            } else if (operatorValue.isTextual()) {
                // 处理字段引用或字符串值
                String value = operatorValue.asText();
                if (value.startsWith("$")) {
                    // 如果是字段引用（以$开头），则创建字段引用表达式
                    expression.put("$field", value.substring(1));
                } else {
                    // 否则作为普通字符串值处理
                    expression.put(operatorName, value);
                }
            } else {
                // 处理其他类型的操作符参数
                expression.put(operatorName, operatorValue);
            }
        }

        return expression;
    }

    /**
     * 获取投影配置
     * @return 投影字段映射
     */
    public Map<String, Object> getProjections() {
        return projections;
    }

    /**
     * 设置集合名称，将用作SQL表名
     * @param collectionName MongoDB集合名称
     */
    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
    }

    /**
     * 获取集合名称
     * @return MongoDB集合名称
     */
    public String getCollectionName() {
        return collectionName;
    }

    /**
     * 检查字段是否在content.formData路径下
     * @param field 字段路径
     * @return 如果字段在content.formData下返回true
     */
    public boolean isFormDataField(String field) {
        return field != null && field.startsWith("content.formData.");
    }

    /**
     * 将MongoDB字段路径转换为SQL列名
     * @param field MongoDB字段路径
     * @return SQL列名
     */
    public String toSqlColumnName(String field) {
        if (isFormDataField(field)) {
            // 移除"content.formData."前缀
            return field.substring("content.formData.".length());
        }
        return field;
    }

	@Override
	public String getType() {
		 return "$project";
	}
}