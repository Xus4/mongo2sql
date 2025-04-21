package com.mongo2sql;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class Mongo2SqlConverterTest {

//    @Test
//    public void testGenerateJdbcCode() {
//        Mongo2SqlConverter converter = new Mongo2SqlConverter();
//        String mongoQuery = "{\"command\": [{\"$match\": {\"field\": \"$field\"}}]}";
//        String collectionName = "testCollection";
//
//        String jdbcCode = converter.generateJdbcCode(mongoQuery, collectionName);
//        System.out.println(jdbcCode);
//        // 检查生成的JDBC代码是否包含预期的SQL查询和参数绑定逻辑
//        assertTrue(jdbcCode.contains("SELECT"));
//        assertTrue(jdbcCode.contains("FROM testCollection"));
//    }
//    
    @Test
    public void testGenerateJdbcCodeWithSpecialParam() {
        Mongo2SqlConverter converter = new Mongo2SqlConverter();
        String mongoQuery = "{\r\n"
        		+ "  \"command\": [\r\n"
        		+ "    {\r\n"
        		+ "      \"$unwind\": {\r\n"
        		+ "        \"path\": \"$content.rentSchedule.rentCell\",\r\n"
        		+ "        \"preserveNullAndEmptyArrays\": true\r\n"
        		+ "      }\r\n"
        		+ "    }\r\n"
        		+ "  ]\r\n"
        		+ "}";
        String collectionName = "testCollection";

        String jdbcCode = converter.generateJdbcCode(mongoQuery, collectionName);
        System.out.println(jdbcCode);
    }
}