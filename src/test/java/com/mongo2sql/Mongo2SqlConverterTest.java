package com.mongo2sql;

import org.junit.jupiter.api.Test;

public class Mongo2SqlConverterTest {

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
		System.out.println("生成的JDBC代码:\n" + jdbcCode);
		
		// 验证生成的JDBC代码是否包含必要的SQL查询结构
		org.junit.Assert.assertTrue("JDBC代码应该包含SELECT语句", jdbcCode.contains("SELECT"));
		org.junit.Assert.assertTrue("JDBC代码应该包含指定的字段", 
			jdbcCode.toLowerCase().contains("content") && 
			jdbcCode.toLowerCase().contains("uniqueid") && 
			jdbcCode.toLowerCase().contains("createdtime"));
		org.junit.Assert.assertTrue("JDBC代码应该包含FROM子句和表名", jdbcCode.contains("FROM " + collectionName));
		org.junit.Assert.assertTrue("JDBC代码应该包含ORDER BY子句", 
			jdbcCode.toLowerCase().contains("order by") && 
			jdbcCode.toLowerCase().contains("createdtime"));
	}
}