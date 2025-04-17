package com.mongo2sql.converter;

import java.util.ArrayList;
import java.util.List;

/**
 * SQL查询对象，封装了SQL语句和对应的参数。
 * 支持参数化查询，可以安全地处理用户输入。
 */
public class SqlQuery {
    private final String sql;
    private final List<Object> parameters;

    public SqlQuery(String sql) {
        this(sql, new ArrayList<>());
    }

    public SqlQuery(String sql, List<Object> parameters) {
        this.sql = sql;
        this.parameters = parameters;
    }

    /**
     * 获取SQL语句。
     * @return SQL语句字符串
     */
    public String getSql() {
        return sql;
    }

    /**
     * 获取SQL参数列表。
     * @return 参数列表
     */
    public List<Object> getParameters() {
        return parameters;
    }

    /**
     * 添加参数到参数列表。
     * @param parameter 要添加的参数
     */
    public void addParameter(Object parameter) {
        parameters.add(parameter);
    }

    @Override
    public String toString() {
        return String.format("SqlQuery{sql='%s', parameters=%s}", sql, parameters);
    }
}