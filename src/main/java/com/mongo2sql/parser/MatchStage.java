package com.mongo2sql.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

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
            
            if (value.isObject()) {
                // 处理 $or 操作符
                if ("$or".equals(fieldName)) {
                    conditions.put("$or", parseOrConditions(value));
                }
                // 处理 $expr 操作符
                else if ("$expr".equals(fieldName)) {
                    conditions.put("$expr", parseExpression(value));
                } else {
                    // 处理其他操作符，如 $in, $eq 等
                    Iterator<Map.Entry<String, JsonNode>> operators = value.fields();
                    while (operators.hasNext()) {
                        Map.Entry<String, JsonNode> operator = operators.next();
                        String operatorName = operator.getKey();
                        JsonNode operatorValue = operator.getValue();
                        
                        Map<String, Object> condition = new HashMap<>();
                        condition.put("operator", operatorName);
                        
                        if ("$in".equals(operatorName) && operatorValue.isArray()) {
                            condition.put("values", parseArrayValues(operatorValue));
                        } else if (isComparisonOperator(operatorName)) {
                            condition.put("value", parseValue(operatorValue));
                        }
                        
                        conditions.put(fieldName, condition);
                    }
                }
            } else if (value.isTextual()) {
                conditions.put(fieldName, value.asText());
            } else if (value.isNumber()) {
                conditions.put(fieldName, value.numberValue());
            } else if (value.isBoolean()) {
                conditions.put(fieldName, value.asBoolean());
            } else if (value.isArray()) {
                conditions.put(fieldName, parseArrayValues(value));
            } else {
                conditions.put(fieldName, value.toString());
            }
        }
        
        return conditions;
    }
    
    /**
     * 判断是否为比较操作符
     * @param operator 操作符名称
     * @return 是否为比较操作符
     */
    private boolean isComparisonOperator(String operator) {
        return "$eq".equals(operator) || 
               "$ne".equals(operator) || 
               "$gt".equals(operator) || 
               "$gte".equals(operator) || 
               "$lt".equals(operator) || 
               "$lte".equals(operator);
    }
    
    /**
     * 解析表达式操作符
     * @param exprNode 表达式节点
     * @return 解析后的表达式对象
     */
    private Map<String, Object> parseExpression(JsonNode exprNode) {
        Map<String, Object> expression = new HashMap<>();
        Iterator<Map.Entry<String, JsonNode>> operators = exprNode.fields();
        
        while (operators.hasNext()) {
            Map.Entry<String, JsonNode> operator = operators.next();
            String operatorName = operator.getKey();
            JsonNode operatorValue = operator.getValue();
            
            if (operatorValue.isArray()) {
                List<Object> operands = new ArrayList<>();
                for (int i = 0; i < operatorValue.size(); i++) {
                    JsonNode operand = operatorValue.get(i);
                    if (operand.isTextual() && operand.asText().startsWith("$")) {
                        // 处理字段引用
                        operands.add(new FieldReference(operand.asText().substring(1)));
                    } else {
                        operands.add(parseValue(operand));
                    }
                }
                expression.put("operator", operatorName);
                expression.put("operands", operands);
            }
        }
        
        return expression;
    }
    
    /**
     * 解析单个值
     * @param node JSON节点
     * @return 解析后的值
     */
    private Object parseValue(JsonNode node) {
        if (node.isTextual()) {
            return node.asText();
        } else if (node.isNumber()) {
            return node.numberValue();
        } else if (node.isBoolean()) {
            return node.asBoolean();
        } else if (node.isArray()) {
            return parseArrayValues(node);
        } else {
            return node.toString();
        }
    }
    
    private Object[] parseArrayValues(JsonNode arrayNode) {
        Object[] values = new Object[arrayNode.size()];
        for (int i = 0; i < arrayNode.size(); i++) {
            values[i] = parseValue(arrayNode.get(i));
        }
        return values;
    }

    /**
     * 解析 $or 条件
     * @param orNode $or 操作符的 JSON 节点
     * @return 解析后的 $or 条件列表
     */
    private List<Map<String, Object>> parseOrConditions(JsonNode orNode) {
        List<Map<String, Object>> orConditions = new ArrayList<>();
        
        if (orNode.isArray()) {
            for (int i = 0; i < orNode.size(); i++) {
                JsonNode conditionNode = orNode.get(i);
                if (conditionNode.isObject()) {
                    Map<String, Object> condition = new HashMap<>();
                    Iterator<Map.Entry<String, JsonNode>> fields = conditionNode.fields();
                    
                    while (fields.hasNext()) {
                        Map.Entry<String, JsonNode> field = fields.next();
                        String fieldName = field.getKey();
                        JsonNode fieldValue = field.getValue();
                        
                        if (fieldValue.isTextual()) {
                            condition.put(fieldName, fieldValue.asText());
                        } else if (fieldValue.isNumber()) {
                            condition.put(fieldName, fieldValue.numberValue());
                        } else if (fieldValue.isBoolean()) {
                            condition.put(fieldName, fieldValue.asBoolean());
                        }
                    }
                    
                    orConditions.add(condition);
                }
            }
        }
        
        return orConditions;
    }

    @Override
    public String getType() {
        return "$match";
    }
    
    /**
     * 表示字段引用的内部类
     */
    public static class FieldReference {
        private final String fieldName;
        
        public FieldReference(String fieldName) {
            this.fieldName = fieldName;
        }
        
        public String getFieldName() {
            return fieldName;
        }
        
        @Override
        public String toString() {
            return "$" + fieldName;
        }
    }
}