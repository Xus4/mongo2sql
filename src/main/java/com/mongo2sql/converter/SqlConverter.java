package com.mongo2sql.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.mongo2sql.parser.AggregationPipeline;

public interface SqlConverter {
    /**
     * Convert MongoDB aggregation pipeline to SQL query
     * @param pipeline The parsed aggregation pipeline
     * @return The converted SQL query string
     * @throws JsonProcessingException 
     * @throws JsonMappingException 
     */
    String convert(AggregationPipeline pipeline, String collectionName) throws JsonMappingException, JsonProcessingException;
}