package com.mongo2sql.converter;

import com.mongo2sql.parser.AggregationPipeline;

public interface SqlConverter {
    /**
     * Convert MongoDB aggregation pipeline to SQL query
     * @param pipeline The parsed aggregation pipeline
     * @return The converted SQL query string
     */
    String convert(AggregationPipeline pipeline, String collectionName);
}