package com.mongo2sql.parser;

import java.util.List;

/**
 * MongoDB聚合管道的表示类，用于存储和管理聚合操作的各个阶段。
 * 该类封装了一系列有序的管道阶段（PipelineStage），每个阶段代表一个特定的聚合操作。
 */
public class AggregationPipeline {
    private final List<PipelineStage> stages;

    public AggregationPipeline(List<PipelineStage> stages) {
        this.stages = stages;
    }

    public List<PipelineStage> getStages() {
        return stages;
    }
}