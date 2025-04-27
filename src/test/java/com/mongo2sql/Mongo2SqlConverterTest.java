package com.mongo2sql;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

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
				+ "      \"$project\": {\r\n"
				+ "        \"content\": 1,\r\n"
				+ "        \"uniqueId\": 1,\r\n"
				+ "        \"createdTime\": 1\r\n"
				+ "      }\r\n"
				+ "    },\r\n"
				+ "    {\r\n"
				+ "      \"$sort\": {\r\n"
				+ "        \"createdTime\": -1\r\n"
				+ "      }\r\n"
				+ "    }\r\n"
				+ "  ]\r\n"
				+ "}";
		String collectionName = "testCollection";

		String jdbcCode = converter.generateJdbcCode(mongoQuery, collectionName);
		System.out.println(jdbcCode);
	}
}