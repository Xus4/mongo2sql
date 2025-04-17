package com.mongo2sql.generator;

import java.util.List;

public interface JdbcCodeGenerator {
    /**
     * Generate parameterized JDBC code
     * @param sqlQuery The SQL query with parameters
     * @param params List of query parameters ({@code $} prefixed variables)
     * @return The generated JDBC code as a string
     */
    String generateCode(String sqlQuery,String collectionName, List<QueryParameter> params);
}