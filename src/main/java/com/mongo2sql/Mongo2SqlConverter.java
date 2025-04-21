package com.mongo2sql;

import java.util.List;

import com.mongo2sql.converter.DefaultSqlConverter;
import com.mongo2sql.converter.SqlConverter;
import com.mongo2sql.generator.FreemarkerJdbcGenerator;
import com.mongo2sql.generator.JdbcCodeGenerator;
import com.mongo2sql.generator.QueryParameter;
import com.mongo2sql.parser.AggregationPipeline;
import com.mongo2sql.parser.MongoAggregationParser;
import com.mongo2sql.parser.MongoQueryParameterExtractor;

/**
 * MongoDB聚合查询转SQL转换器
 * 该类提供了将MongoDB聚合管道查询转换为SQL查询的功能，并能生成相应的JDBC代码
 */
public class Mongo2SqlConverter {
    private final MongoAggregationParser parser;
    private final SqlConverter sqlConverter;
    private final JdbcCodeGenerator jdbcGenerator;
    
    /**
     * 构造函数，初始化转换器所需的组件
     * 包括MongoDB聚合查询解析器、SQL转换器和JDBC代码生成器
     */
    public Mongo2SqlConverter() {
        this.parser = new MongoAggregationParser();
        this.sqlConverter = new DefaultSqlConverter();
        this.jdbcGenerator = new FreemarkerJdbcGenerator();
    }
    
    /**
     * 将MongoDB聚合查询转换为SQL查询
     * @param mongoQuery MongoDB聚合管道查询字符串
     * @return 转换后的SQL查询字符串
     */
    public String convertToSql(String mongoQuery, String collectionName) {
        AggregationPipeline pipeline = parser.parse(mongoQuery);
        return sqlConverter.convert(pipeline, collectionName);
    }
    
    /***
     * 从mongoQuery中解析出参数列表，
     * 解析规则：
     * 1、如果json数据中的key-value对的value是一个$开头的，代表这是个变量，是要将这个变量提取出来作为JAVA JDBC代码方法中的入参，外部调用该方法的时候要传递。

     * @param mongoQuery
     * @return
     */
    public List<QueryParameter> parseParams(String mongoQuery) {
        return MongoQueryParameterExtractor.parse(mongoQuery);
    }
    
    
    /**
     * 生成用于执行转换后SQL查询的JDBC代码
     * @param mongoQuery MongoDB聚合管道查询字符串
     * @return 生成的JDBC代码字符串
     */
    public String generateJdbcCode(String mongoQuery, String collectionName) {
        String sqlQuery = convertToSql(mongoQuery, collectionName);
        List<QueryParameter> params = parseParams(mongoQuery);
        if (params == null || params.isEmpty()) {
            throw new IllegalArgumentException("Params cannot be null or empty");
        }
        return jdbcGenerator.generateFile(sqlQuery, collectionName, params);
    }
}