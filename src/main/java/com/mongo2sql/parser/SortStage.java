package com.mongo2sql.parser;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

public class SortStage implements PipelineStage {
    private final LinkedHashMap<String, Integer> sortFields;
    JsonNode criteria;
    public SortStage(JsonNode sortNode) {
        super();
        this.criteria = sortNode;
        this.sortFields = parseSortFields(sortNode);
    }

    private LinkedHashMap<String, Integer> parseSortFields(JsonNode sortNode) {
        LinkedHashMap<String, Integer> fields = new LinkedHashMap<>();
        
        sortNode.fields().forEachRemaining(entry -> {
            String fieldName = entry.getKey();
            int direction = entry.getValue().asInt(1); // 默认升序
            fields.put(fieldName, direction);
        });
        
        return fields;
    }

    public Map<String, Integer> getSortFields() {
        return sortFields;
    }

    public String toSQL() {
        StringBuilder sb = new StringBuilder("ORDER BY ");
        sortFields.forEach((field, direction) -> {
            sb.append(field)
              .append(direction == 1 ? " ASC" : " DESC")
              .append(", ");
        });
        sb.setLength(sb.length() - 2); // 移除最后的逗号和空格
        return sb.toString();
    }

	@Override
	public String getType() {
		return "$sort";
	}

	public JsonNode getCriteria() {
		return criteria;
	}
}