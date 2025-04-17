package com.mongo2sql.parser;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 表示MongoDB $unwind阶段的实现类，用于展开数组字段。
 * 该阶段将数组中的每个元素都创建为独立的输出文档。
 */
public class UnwindStage implements PipelineStage {
    private final String path;
    private final boolean preserveNullAndEmptyArrays;

    public UnwindStage(JsonNode unwindSpec) {
        if (unwindSpec.isTextual()) {
            this.path = unwindSpec.asText();
            this.preserveNullAndEmptyArrays = false;
        } else {
            this.path = unwindSpec.get("path").asText();
            this.preserveNullAndEmptyArrays = unwindSpec.has("preserveNullAndEmptyArrays") && 
                                             unwindSpec.get("preserveNullAndEmptyArrays").asBoolean();
        }
    }

    public String getPath() { return path; }
    public boolean isPreserveNullAndEmptyArrays() { return preserveNullAndEmptyArrays; }

    @Override
    public String getType() {
        return "$unwind";
    }
}