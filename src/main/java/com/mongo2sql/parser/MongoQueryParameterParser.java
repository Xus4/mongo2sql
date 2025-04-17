package com.mongo2sql.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.mongo2sql.generator.QueryParameter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * MongoQueryParameterExtractor负责解析MongoDB查询字符串中的参数。
 * 该类包含一个静态方法parse，用于识别并提取以$开头的变量。
 */
public class MongoQueryParameterParser {

    /**
     * 解析MongoDB查询字符串中的参数。
     * @param mongoQuery MongoDB查询字符串
     * @return 提取的QueryParameter对象列表
     */
    public static List<QueryParameter> parse(String mongoQuery) {
        List<QueryParameter> params = new ArrayList<>();
        try {
            JsonNode rootNode = new MongoAggregationParser().getObjectMapper().readTree(mongoQuery);
            JsonNode commandNode = rootNode.get("command");

            for (JsonNode stageNode : commandNode) {
                String operator = stageNode.fieldNames().next();
                JsonNode operatorValue = stageNode.get(operator);

                if (operator.equals("$match")) {
                    Iterator<Map.Entry<String, JsonNode>> fields = operatorValue.fields();
                    while (fields.hasNext()) {
                        Map.Entry<String, JsonNode> field = fields.next();
                        JsonNode value = field.getValue();

                        if (value.isTextual()) {
                            String textValue = value.asText();
                            if (textValue.startsWith("$")) {
                                String paramName = textValue.substring(1);
                                if (paramName.startsWith(":context.props.appendData.")) {
                                    paramName = paramName.substring(":context.props.appendData.".length());
                                }
                                params.add(new QueryParameter(paramName, "String", null));
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse parameters from MongoDB query", e);
        }
        return params;
    }
}