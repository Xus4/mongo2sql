package com.mongo2sql;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class Mongo2SqlConverterTest {

	@Test
	public void testGenerateJdbcCodeWithSpecialParam() throws JsonMappingException, JsonProcessingException {
		Mongo2SqlConverter converter = new Mongo2SqlConverter();
		String mongoQuery = "{\"command\":[\r\n"
				+ "{\"$match\":{\"$expr\":{\"$eq\":[\"$createdTime\",\"22030101\"]}}},\r\n"
				+ "{\"$match\":{\"$expr\":{\"$eq\":[\"$subjectCode\",\"22030101\"]}}}]}";
		
		String collectionName = "testCollection";

		String jdbcCode = converter.generateJdbcCode(mongoQuery, collectionName);
		System.out.println("生成的JDBC代码:\n" + jdbcCode);
		
	}
}