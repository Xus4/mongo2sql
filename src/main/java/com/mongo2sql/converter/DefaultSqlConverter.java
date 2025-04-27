package com.mongo2sql.converter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import com.fasterxml.jackson.databind.JsonNode;
import com.mongo2sql.parser.AggregationPipeline;
import com.mongo2sql.parser.LookupStage;
import com.mongo2sql.parser.MatchStage;
import com.mongo2sql.parser.PipelineStage;
import com.mongo2sql.parser.ProjectStage;
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
     */
    @Override
    public String convert(AggregationPipeline pipeline, String collectionName) {
        this.collectionName = collectionName; // 保存集合名称
        List<String> sqlParts = new ArrayList<>();
        StringBuilder whereClause = new StringBuilder();
        StringBuilder joinClause = new StringBuilder();
        StringBuilder selectClause = new StringBuilder("SELECT *");
        StringBuilder orderByClause = new StringBuilder();
        
        for (PipelineStage stage : pipeline.getStages()) {
            if (stage instanceof SortStage) {
                handleSortStage((SortStage) stage, orderByClause);
            }
            if (stage instanceof MatchStage) {
                handleMatchStage((MatchStage) stage, whereClause);
            } else if (stage instanceof LookupStage) {
                handleLookupStage((LookupStage) stage, joinClause);
            } else if (stage instanceof ProjectStage) {
                handleProjectStage((ProjectStage) stage, selectClause);
            } else if (stage instanceof UnwindStage) {
                handleUnwindStage((UnwindStage) stage, joinClause);
            }
        }
        
        // Build the SQL query
        sqlParts.add(selectClause.toString());
        sqlParts.add("FROM " + collectionName); // 使用传入的collectionName作为表名
        
        if (joinClause.length() > 0) {
            sqlParts.add(joinClause.toString());
        }
        
        if (whereClause.length() > 0) {
            sqlParts.add("WHERE " + whereClause.toString());
        }
        
        if (orderByClause.length() > 0) {
            sqlParts.add("ORDER BY " + orderByClause.toString());
        }
        
        return String.join(" ", sqlParts);
    }
    
    /**
     * 处理MongoDB的$match阶段，将其转换为SQL的WHERE子句。
     * 
     * @param stage $match阶段对象，包含查询条件
     * @param whereClause SQL WHERE子句的StringBuilder对象
     */
    private void handleMatchStage(MatchStage stage, StringBuilder whereClause) {
        Map<String, Object> conditions = stage.getConditions();
        if (conditions.isEmpty()) {
            return;
        }

        StringJoiner conditionJoiner = new StringJoiner(" AND ");
        for (Map.Entry<String, Object> condition : conditions.entrySet()) {
            String field = condition.getKey();
            Object value = condition.getValue();
            
            // 将MongoDB的点号路径转换为SQL列名（例如：content.formData.flowId -> flowId）
            String columnName = field.substring(field.lastIndexOf('.') + 1);
            if (columnName.isEmpty()) {
                columnName = field;
            }
            
            // 处理操作符条件
            if (value instanceof Map) {
                Map<String, Object> operatorMap = (Map<String, Object>) value;
                String operator = (String) operatorMap.get("operator");
                Object values = operatorMap.get("values");
                
                if ("$in".equals(operator) && values instanceof Object[]) {
                    Object[] inArray = (Object[]) values;
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
                    // 对于未知的操作符，使用等于操作符
                    conditionJoiner.add(columnName + " = '" + value.toString() + "'");
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
     * 处理MongoDB的$lookup阶段，将其转换为SQL的JOIN子句。
     * 
     * @param stage $lookup阶段对象，包含连接条件
     * @param joinClause SQL JOIN子句的StringBuilder对象
     */
    private void handleLookupStage(LookupStage stage, StringBuilder joinClause) {
        // TODO: Implement lookup conversion
        // This will involve converting MongoDB $lookup to SQL JOIN statements
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
        
        // 获取数组字段名（去除路径中的点号）
        String arrayField = path.substring(path.lastIndexOf('.') + 1);
        
        // 构建数组元素表名（约定：主表名_字段名）
        String arrayTableName = String.format("%s_%s", this.collectionName, arrayField);
        
        // 构建JOIN子句，使用parent_id作为外键关联
        String joinStatement = String.format(" %sJOIN %s ON %s.parent_id = %s.id",
            stage.isPreserveNullAndEmptyArrays() ? "LEFT " : "INNER ",
            arrayTableName,
            arrayTableName,
            this.collectionName);
        
        joinClause.append(joinStatement);
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