package com.mongo2sql.converter;

/**
 * SQL转换器工厂类，用于创建适合不同数据库类型的SQL转换器。
 */
public class SqlConverterFactory {
    
    /**
     * 创建SQL转换器。
     * 
     * @param databaseType 数据库类型（如"mysql", "postgresql"等）
     * @return 对应的SQL转换器实例
     */
    public static SqlConverter createConverter(String databaseType) {
        // 目前只实现了默认的转换器，后续可以根据不同数据库类型返回不同的实现
        return new DefaultSqlConverter();
    }
}