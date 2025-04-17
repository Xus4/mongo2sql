package com.mongo2sql.parser;

/**
 * PipelineStage接口定义了MongoDB聚合管道中各个阶段的基本行为。
 * 所有具体的管道阶段（如$match、$project、$lookup等）都需要实现此接口。
 */
public interface PipelineStage {

	String getType();
    // 目前作为标记接口使用，后续可以添加通用的方法
}