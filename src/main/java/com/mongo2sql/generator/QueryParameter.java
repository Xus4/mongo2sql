package com.mongo2sql.generator;

public class QueryParameter {
    private final String name;
    private final Object type;
    private final Object value;

    public QueryParameter(String name, Object type, Object value) {
        this.name = name;
        this.type = type;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public Object getType() {
        return type;
    }

    public Object getValue() {
        return value;
    }
}