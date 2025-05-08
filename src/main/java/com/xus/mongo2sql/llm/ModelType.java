package com.xus.mongo2sql.llm;

public enum ModelType {
    QWEN_TURBO("qwen-turbo"),
    QWEN_PLUS("qwen-plus-latest"),
    QWEN3_235B("qwen3-235b-a22b"),
    DEEPSEEK_R1("deepseek-r1"),
    DEEPSEEK_R1_ALIYUN("deepseek-r1:aliyun"),

	GPT_4o("gpt-4o");

    private final String value;

    ModelType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
