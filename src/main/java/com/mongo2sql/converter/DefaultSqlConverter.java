package com.mongo2sql.converter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongo2sql.parser.AggregationPipeline;
import com.mongo2sql.parser.LookupStage;
import com.mongo2sql.parser.MatchStage;
import com.mongo2sql.parser.PipelineStage;
import com.mongo2sql.parser.ProjectStage;
import com.mongo2sql.parser.SetStage;
import com.mongo2sql.parser.SortStage;
import com.mongo2sql.parser.UnwindStage;

/**
 * DefaultSqlConverter是MongoDB聚合管道到SQL查询的默认转换器实现。
 * 该类负责将MongoDB的聚合操作（如$match、$lookup、$project等）转换为等效的SQL查询语句。
 * 
 * @see SqlConverter
 * @see AggregationPipeline
 */
public class DefaultSqlConverter implements SqlConverter {
    private String collectionName; // 存储当前处理的集合名称
    /**
     * 将MongoDB聚合管道转换为SQL查询字符串。
     * 
     * @param pipeline MongoDB聚合管道对象，包含了一系列聚合操作阶段
     * @return 转换后的SQL查询字符串
     * @throws JsonProcessingException 
     * @throws JsonMappingException 
     */
    @Override
    public String convert(AggregationPipeline pipeline, String collectionName) throws JsonMappingException, JsonProcessingException {
        this.collectionName = collectionName;
        List<String> sqlParts = new ArrayList<>();
        StringBuilder whereClause = new StringBuilder();
        StringBuilder joinClause = new StringBuilder();
        StringBuilder selectClause = new StringBuilder("SELECT ");
        StringBuilder orderByClause = new StringBuilder();
        boolean hasSetStage = false;
        
        // 收集所有的 match 条件
        List<String> matchConditions = new ArrayList<>();
        
        // 收集所有需要选择的字段
        Set<String> selectedFields = new HashSet<>();
        selectedFields.add(this.collectionName + ".*");
        
        for (PipelineStage stage : pipeline.getStages()) {
            if (stage instanceof SortStage) {
                handleSortStage((SortStage) stage, orderByClause);
            } else if (stage instanceof MatchStage) {
                StringBuilder matchWhere = new StringBuilder();
                handleMatchStage((MatchStage) stage, matchWhere);
                if (matchWhere.length() > 0) {
                    matchConditions.add(matchWhere.toString());
                }
            } else if (stage instanceof LookupStage) {
                handleLookupStage((LookupStage) stage, joinClause);
                // 添加连接表的所有字段
                selectedFields.add(((LookupStage) stage).getAs() + ".*");
            } else if (stage instanceof ProjectStage) {
                handleProjectStage((ProjectStage) stage, selectClause);
            } else if (stage instanceof SetStage) {
                handleSetStage((SetStage) stage, selectClause);
                hasSetStage = true;
            } else if (stage instanceof UnwindStage) {
                handleUnwindStage((UnwindStage) stage, joinClause);
            }
        }
        
        // 合并所有的 match 条件
        if (!matchConditions.isEmpty()) {
            whereClause.append(String.join(" AND ", matchConditions));
        }
        
        // 构建SELECT子句
        if (!hasSetStage) {
            selectClause.append(String.join(", ", selectedFields));
        }
        
        // Build the SQL query
        if (hasSetStage) {
            sqlParts.add(selectClause.toString());
            if (whereClause.length() > 0) {
                sqlParts.add("WHERE " + whereClause.toString());
            }
            if (orderByClause.length() > 0) {
                sqlParts.add("ORDER BY " + orderByClause.toString());
            }
        } else {
            sqlParts.add(selectClause.toString());
            sqlParts.add("FROM " + collectionName);
            
            if (joinClause.length() > 0) {
                sqlParts.add(joinClause.toString());
            }
            
            if (whereClause.length() > 0) {
                sqlParts.add("WHERE " + whereClause.toString());
            }
            
            if (orderByClause.length() > 0) {
                sqlParts.add("ORDER BY " + orderByClause.toString());
            }
        }
        
        return String.join(" ", sqlParts);
    }
    
    /**
     * 处理MongoDB的$match阶段，将其转换为SQL的WHERE子句。
     * 
     * @param stage $match阶段对象，包含查询条件
     * @param whereClause SQL WHERE子句的StringBuilder对象
     * @throws JsonProcessingException 
     * @throws JsonMappingException 
     */
    private void handleMatchStage(MatchStage stage, StringBuilder whereClause) throws JsonMappingException, JsonProcessingException {
        Map<String, Object> conditions = stage.getConditions();
        if (conditions.isEmpty()) {
            return;
        }

        StringJoiner conditionJoiner = new StringJoiner(" AND ");
        ObjectMapper mapper = new ObjectMapper();
        
        for (Map.Entry<String, Object> condition : conditions.entrySet()) {
            String field = condition.getKey();	
            Object value = condition.getValue();
            
            // 处理 $or 操作符
            if ("$or".equals(field)) {
                StringJoiner orJoiner = new StringJoiner(" OR ");
                
                if (value instanceof List) {
                    List<Object> orConditions = (List<Object>) value;
                    for (Object orCondition : orConditions) {
                        Map<String, Object> conditionMap = null;
                        if (orCondition instanceof Map) {
                            conditionMap = (Map<String, Object>) orCondition;
                        } else if (orCondition instanceof String) {
                            try {
                                conditionMap = mapper.readValue((String) orCondition, Map.class);
                            } catch (Exception e) {
                                // 如果解析失败，跳过这个条件
                                continue;
                            }
                        }
                        
                        if (conditionMap != null) {
                            StringJoiner innerJoiner = new StringJoiner(" AND ");
                            for (Map.Entry<String, Object> entry : conditionMap.entrySet()) {
                                String columnName = entry.getKey();
                                Object columnValue = entry.getValue();
                                if (columnValue instanceof String) {
                                    innerJoiner.add(columnName + " = '" + columnValue.toString() + "'");
                                } else {
                                    innerJoiner.add(columnName + " = " + columnValue.toString());
                                }
                            }
                            orJoiner.add("(" + innerJoiner.toString() + ")");
                        }
                    }
                } else if (value instanceof Object[]) {
                    Object[] orConditions = (Object[]) value;
                    for (Object orCondition : orConditions) {
                        Map<String, Object> conditionMap = null;
                        if (orCondition instanceof Map) {
                            conditionMap = (Map<String, Object>) orCondition;
                        } else if (orCondition instanceof String) {
                            try {
                                conditionMap = mapper.readValue((String) orCondition, Map.class);
                            } catch (Exception e) {
                                // 如果解析失败，跳过这个条件
                                continue;
                            }
                        }
                        
                        if (conditionMap != null) {
                            StringJoiner innerJoiner = new StringJoiner(" AND ");
                            for (Map.Entry<String, Object> entry : conditionMap.entrySet()) {
                                String columnName = entry.getKey();
                                Object columnValue = entry.getValue();
                                if (columnValue instanceof String) {
                                    innerJoiner.add(columnName + " = '" + columnValue.toString() + "'");
                                } else {
                                    innerJoiner.add(columnName + " = " + columnValue.toString());
                                }
                            }
                            orJoiner.add("(" + innerJoiner.toString() + ")");
                        }
                    }
                }
                
                conditionJoiner.add(orJoiner.toString());
                continue;
            }
            
            // 处理表达式操作符
            if ("$expr".equals(field) && value instanceof Map) {
                Map<String, Object> exprMap = (Map<String, Object>) value;
                String operator = (String) exprMap.get("operator");
                List<Object> operands = (List<Object>) exprMap.get("operands");
                
                if (operands != null && operands.size() == 2) {
                    String leftOperand = convertOperand(operands.get(0));
                    String rightOperand = convertOperand(operands.get(1));
                    
                    switch (operator) {
                        case "$ne":
                            conditionJoiner.add(leftOperand + " != " + rightOperand);
                            break;
                        case "$eq":
                            conditionJoiner.add(leftOperand + " = " + rightOperand);
                            break;
                        case "$gt":
                            conditionJoiner.add(leftOperand + " > " + rightOperand);
                            break;
                        case "$gte":
                            conditionJoiner.add(leftOperand + " >= " + rightOperand);
                            break;
                        case "$lt":
                            conditionJoiner.add(leftOperand + " < " + rightOperand);
                            break;
                        case "$lte":
                            conditionJoiner.add(leftOperand + " <= " + rightOperand);
                            break;
                        default:
                            // 对于未知的操作符，使用等于操作符
                            conditionJoiner.add(leftOperand + " = " + rightOperand);
                    }
                }
                continue;
            }
            
            // 将MongoDB的点号路径转换为SQL列名（例如：content.formData.flowId -> flowId）
            String columnName = field.substring(field.lastIndexOf('.') + 1);
            if (columnName.isEmpty()) {
                columnName = field;
            }
            
            // 处理操作符条件
            if (value instanceof Map) {
                Map<String, Object> operatorMap = (Map<String, Object>) value;
                String operator = (String) operatorMap.get("operator");
                Object operatorValue = operatorMap.get("values");
                
                if ("$in".equals(operator) && operatorValue instanceof Object[]) {
                    Object[] inArray = (Object[]) operatorValue;
                    StringJoiner valueJoiner = new StringJoiner(", ");
                    for (Object inValue : inArray) {
                        if (inValue instanceof String) {
                            valueJoiner.add("'" + inValue.toString() + "'");
                        } else {
                            valueJoiner.add(inValue.toString());
                        }
                    }
                    conditionJoiner.add(columnName + " IN (" + valueJoiner.toString() + ")");
                } else {
                    // 处理比较操作符
                    Object compareValue = operatorMap.get("value");
                    String sqlOperator = convertOperator(operator);
                    String sqlValue = convertValue(compareValue);
                    conditionJoiner.add(columnName + " " + sqlOperator + " " + sqlValue);
                }
            } else {
                // 处理普通等值条件
                if (value instanceof String) {
                    conditionJoiner.add(columnName + " = '" + value.toString() + "'");
                } else {
                    conditionJoiner.add(columnName + " = " + value.toString());
                }
            }
        }
        
        whereClause.append(conditionJoiner.toString());
    }
    
    /**
     * 转换MongoDB操作符为SQL操作符
     * @param operator MongoDB操作符
     * @return SQL操作符
     */
    private String convertOperator(String operator) {
        switch (operator) {
            case "$eq":
                return "=";
            case "$ne":
                return "!=";
            case "$gt":
                return ">";
            case "$gte":
                return ">=";
            case "$lt":
                return "<";
            case "$lte":
                return "<=";
            default:
                return "=";
        }
    }
    
    /**
     * 转换值为SQL表达式
     * @param value 值
     * @return SQL表达式字符串
     */
    private String convertValue(Object value) {
        if (value instanceof String) {
            return "'" + value.toString() + "'";
        } else {
            return value.toString();
        }
    }
    
    /**
     * 转换操作数为SQL表达式
     * @param operand 操作数
     * @return SQL表达式字符串
     */
    private String convertOperand(Object operand) {
        if (operand instanceof MatchStage.FieldReference) {
            // 如果是字段引用，直接使用字段名
            return ((MatchStage.FieldReference) operand).getFieldName();
        } else if (operand instanceof String) {
            // 如果是字符串，添加引号
            return "'" + operand + "'";
        } else {
            // 其他类型直接转换为字符串
            return operand.toString();
        }
    }
    
    /**
     * 处理MongoDB的$lookup阶段，将其转换为SQL的JOIN子句。
     * 
     * @param stage $lookup阶段对象，包含连接条件
     * @param joinClause SQL JOIN子句的StringBuilder对象
     */
    private void handleLookupStage(LookupStage stage, StringBuilder joinClause) {
        String fromTable = stage.getFrom();
        String localField = stage.getLocalField();
        String foreignField = stage.getForeignField();
        String alias = stage.getAs();

        // 处理嵌套字段路径
        String[] localFieldParts = localField.split("\\.");
        String localFieldColumn = localFieldParts[localFieldParts.length - 1];

        // 构建JOIN语句
        String joinStatement = String.format(" LEFT JOIN %s %s ON %s.%s = %s.%s",
            fromTable,
            alias,
            this.collectionName,
            localFieldColumn,
            alias,
            foreignField);

        joinClause.append(joinStatement);
    }
    
    /**
     * 处理MongoDB的$project阶段，将其转换为SQL的SELECT子句。
     * 
     * @param stage $project阶段对象，包含字段投影信息
     * @param selectClause SQL SELECT子句的StringBuilder对象
     */
    private void handleProjectStage(ProjectStage stage, StringBuilder selectClause) {
        Map<String, Object> projections = stage.getProjections();
        if (projections.isEmpty()) return;
    
        selectClause.setLength(0);
        selectClause.append("SELECT ");
    
        List<String> includes = new ArrayList<>();
        List<String> excludes = new ArrayList<>();
        Map<String, String> aliases = new HashMap<>();
    
        projections.forEach((newField, value) -> {
            if (value instanceof Integer) {
                if ((Integer) value == 1) {
                    includes.add(stage.toSqlColumnName(newField));
                } else {
                    excludes.add(stage.toSqlColumnName(newField));
                }
            } else if (value instanceof Boolean) {
                if ((Boolean) value) {
                    includes.add(stage.toSqlColumnName(newField));
                }
            } else if (value instanceof String) {
                aliases.put(newField, stage.toSqlColumnName((String) value));
            } else if (value instanceof Map) {
                Map<?, ?> expr = (Map<?, ?>) value;
                if (expr.containsKey("$field")) {
                    String original = expr.get("$field").toString();
                    aliases.put(newField, stage.toSqlColumnName(original));
                }
            }
        });
    
        StringJoiner columns = new StringJoiner(", ");
        includes.forEach(columns::add);
        aliases.forEach((alias, col) -> columns.add(col + " AS " + alias));
    
        if (!excludes.isEmpty()) {
            selectClause.append("* EXCEPT(").append(String.join(", ", excludes)).append(")");
        } else if (columns.length() > 0) {
            selectClause.append(columns);
        } else {
            selectClause.append("*");
        }
    }
    /**
     * 处理MongoDB的$unwind阶段，将其转换为SQL的JOIN子句。
     * 对于MongoDB中的数组字段，我们将其存储在独立的关联表中，表名采用'主表名_字段名'的格式。
     * 
     * @param stage $unwind阶段对象，包含要展开的数组字段路径
     * @param joinClause SQL JOIN子句的StringBuilder对象
     */
    private void handleUnwindStage(UnwindStage stage, StringBuilder joinClause) {
        String path = stage.getPath();
        // 移除字段路径中的$符号
        if (path.startsWith("$")) {
            path = path.substring(1);
        }
        
        // 检查是否是$lookup的结果展开
        if (path.contains(".")) {
            String[] parts = path.split("\\.");
            if (parts.length >= 2) {
                String tableAlias = parts[0];
                // 如果这个表是通过$lookup连接的，不需要添加额外的JOIN
                if (joinClause.toString().contains("JOIN") && joinClause.toString().contains(tableAlias)) {
                    return;
                }
            }
        }
        
        // 只有真正的数组字段才需要创建关联表
        // 这里我们暂时不处理这种情况，因为需要更多的上下文信息
        // 比如需要知道哪些字段是数组类型
        // 所以目前对于非$lookup的$unwind，我们暂时不做处理
    }
    
    private void handleSetStage(SetStage stage, StringBuilder selectClause) {
        Map<String, Object> setFields = stage.getSetFields();
        if (setFields.isEmpty()) return;
        
        StringJoiner setJoiner = new StringJoiner(", ");
        
        setFields.forEach((field, value) -> {
            String columnName = field.substring(field.lastIndexOf('.') + 1);
            if (value instanceof Map) {
                Map<String, Object> expression = (Map<String, Object>) value;
                expression.forEach((operator, params) -> {
                    if (operator.equals("$substr") && params instanceof Object[]) {
                        Object[] substrParams = (Object[]) params;
                        if (substrParams.length >= 3) {
                            String sourceField = extractFieldName(substrParams[0]);
                            int start = ((Number) substrParams[1]).intValue() + 1; // Convert to 1-based index
                            int length = ((Number) substrParams[2]).intValue();
                            setJoiner.add(columnName + " = SUBSTRING(" + sourceField + ", " + start + ", " + length + ")");
                        }
                    } else if (params instanceof Object[]) {
                        StringJoiner paramJoiner = new StringJoiner(", ");
                        for (Object param : (Object[]) params) {
                            if (param instanceof Map && ((Map<?, ?>) param).containsKey("$field")) {
                                paramJoiner.add(extractFieldName(param));
                            } else {
                                paramJoiner.add(param.toString());
                            }
                        }
                        setJoiner.add(columnName + " " + operator + "(" + paramJoiner.toString() + ")");
                    } else {
                        setJoiner.add(columnName + " " + operator + " " + params.toString());
                    }
                });
            } else if (value instanceof String) {
                setJoiner.add(columnName + " = '" + value.toString() + "'");
            } else {
                setJoiner.add(columnName + " = " + value.toString());
            }
        });
        
        selectClause.setLength(0);
        selectClause.append("UPDATE ").append(this.collectionName)
                   .append(" SET ").append(setJoiner.toString());
    }

    private String extractFieldName(Object fieldObj) {
        if (fieldObj instanceof Map) {
            Map<?, ?> fieldMap = (Map<?, ?>) fieldObj;
            if (fieldMap.containsKey("$field")) {
                return fieldMap.get("$field").toString();
            }
        } else if (fieldObj instanceof String) {
            String fieldStr = fieldObj.toString();
            if (fieldStr.startsWith("$")) {
                return fieldStr.substring(1);
            }
        }
        return fieldObj.toString();
    }
    
    private void handleSortStage(SortStage stage, StringBuilder orderByClause) {
        JsonNode sortCriteria = stage.getCriteria();
        StringJoiner sortJoiner = new StringJoiner(", ");

        sortCriteria.fields().forEachRemaining(entry -> {
            String field = entry.getKey();
            int direction = entry.getValue().asInt();
            String columnName = field.substring(field.lastIndexOf('.') + 1);
            sortJoiner.add(columnName + (direction == 1 ? " ASC" : " DESC"));
        });

        if (sortJoiner.length() > 0) {
            orderByClause.append(sortJoiner.toString());
        }
    }
}