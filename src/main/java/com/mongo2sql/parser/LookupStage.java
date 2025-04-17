package com.mongo2sql.parser;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 表示MongoDB $lookup阶段的实现类，用于执行左外连接操作。
 * 该阶段可以将当前集合中的文档与其他集合的文档进行关联。
 */
public class LookupStage implements PipelineStage {
    private final String from;
    private final String localField;
    private final String foreignField;
    private final String as;

    public LookupStage(JsonNode lookupSpec) {
        this.from = lookupSpec.get("from").asText();
        this.localField = lookupSpec.get("localField").asText();
        this.foreignField = lookupSpec.get("foreignField").asText();
        this.as = lookupSpec.get("as").asText();
    }

    public String getFrom() { return from; }
    public String getLocalField() { return localField; }
    public String getForeignField() { return foreignField; }
    public String getAs() { return as; }

    @Override
    public String getType() {
        return "$lookup";
    }
}