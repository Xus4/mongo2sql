package com.mongo2sql.converter;

import java.util.List;

/**
 * SqlQueryResult类用于存储SQL查询结果，包含SQL语句和参数列表。
 */
public class SqlQueryResult {
    private final String sql;
    private final List<Object> parameters;

    public SqlQueryResult(String sql, List<Object> parameters) {
        this.sql = sql;
        this.parameters = parameters;
    }

    public String getSql() {
        return sql;
    }

    public List<Object> getParameters() {
        return parameters;
    }
} 