package com.mongo2sql.converter;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.mongo2sql.parser.AggregationPipeline;

/**
 * SqlConverter接口定义了将MongoDB聚合管道转换为SQL查询的方法。
 */
public interface SqlConverter {
    /**
     * 将MongoDB聚合管道转换为SQL查询字符串。
     * 
     * @param pipeline MongoDB聚合管道对象
     * @param collectionName 集合名称
     * @return 转换后的SQL查询字符串
     * @throws JsonMappingException
     * @throws JsonProcessingException
     */
    String convert(AggregationPipeline pipeline, String collectionName) throws JsonMappingException, JsonProcessingException;

    /**
     * 将MongoDB聚合管道转换为SQL查询字符串和参数列表。
     * 
     * @param pipeline MongoDB聚合管道对象
     * @param collectionName 集合名称
     * @return 包含SQL查询字符串和参数列表的SqlQueryResult对象
     * @throws JsonMappingException
     * @throws JsonProcessingException
     */
    SqlQueryResult convertWithParameters(AggregationPipeline pipeline, String collectionName) throws JsonMappingException, JsonProcessingException;
}