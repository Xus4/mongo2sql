package com.mongo2sql.converter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import com.mongo2sql.parser.AggregationPipeline;
import com.mongo2sql.parser.LookupStage;
import com.mongo2sql.parser.MatchStage;
import com.mongo2sql.parser.PipelineStage;
import com.mongo2sql.parser.ProjectStage;

/**
 * DefaultSqlConverter是MongoDB聚合管道到SQL查询的默认转换器实现。
 * 该类负责将MongoDB的聚合操作（如$match、$lookup、$project等）转换为等效的SQL查询语句。
 * 
 * @see SqlConverter
 * @see AggregationPipeline
 */
public class DefaultSqlConverter implements SqlConverter {
    /**
     * 将MongoDB聚合管道转换为SQL查询字符串。
     * 
     * @param pipeline MongoDB聚合管道对象，包含了一系列聚合操作阶段
     * @return 转换后的SQL查询字符串
     */
    @Override
    public String convert(AggregationPipeline pipeline, String collectionName) {
        List<String> sqlParts = new ArrayList<>();
        StringBuilder whereClause = new StringBuilder();
        StringBuilder joinClause = new StringBuilder();
        StringBuilder selectClause = new StringBuilder("SELECT *");
        
        for (PipelineStage stage : pipeline.getStages()) {
            if (stage instanceof MatchStage) {
                handleMatchStage((MatchStage) stage, whereClause);
            } else if (stage instanceof LookupStage) {
                handleLookupStage((LookupStage) stage, joinClause);
            } else if (stage instanceof ProjectStage) {
                handleProjectStage((ProjectStage) stage, selectClause);
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
            // 将MongoDB的点号路径转换为SQL列名（例如：content.formData.flowId -> flowId）
            String columnName = field.substring(field.lastIndexOf('.') + 1);
            if (columnName.isEmpty()) {
                columnName = field;
            }
            conditionJoiner.add(columnName + " = ?");
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
        if (projections.isEmpty()) {
            return;
        }
        
        // 清除默认的SELECT *
        selectClause.setLength(0);
        selectClause.append("SELECT ");
        
        StringJoiner columnJoiner = new StringJoiner(", ");
        for (Map.Entry<String, Object> projection : projections.entrySet()) {
            String field = projection.getKey();
            Object include = projection.getValue();
            
            // 只处理被包含的字段
            if (include instanceof Integer && (Integer)include == 1) {
                String columnName = stage.toSqlColumnName(field);
                columnJoiner.add(columnName);
            }
        }
        
        // 如果没有指定字段，使用所有字段
        if (columnJoiner.length() == 0) {
            selectClause.append("*");
        } else {
            selectClause.append(columnJoiner.toString());
        }
    }
}