package com.mongo2sql;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class Mongo2SqlConverterTest {

	@Test
	public void testGenerateJdbcCodeWithSpecialParam() throws JsonMappingException, JsonProcessingException {
		Mongo2SqlConverter converter = new Mongo2SqlConverter();
		String mongoQuery = ""
				+ "{\r\n"
				+ "  \"command\": [\r\n"
				+ "    {\r\n"
				+ "      \"$match\": {\r\n"
				+ "        \"content.formData.custRole\": {\r\n"
				+ "          \"$in\": [\r\n"
				+ "            \"4\"\r\n"
				+ "          ]\r\n"
				+ "        }\r\n"
				+ "      }\r\n"
				+ "    },\r\n"
				+ "    {\r\n"
				+ "      \"$project\": {\r\n"
				+ "        \"uniqueId\": 1,\r\n"
				+ "        \"content\": 1\r\n"
				+ "      }\r\n"
				+ "    }\r\n"
				+ "  ]\r\n"
				+ "}";
		
		String collectionName = "testCollection";

		String jdbcCode = converter.generateJdbcCode(mongoQuery, collectionName);
		System.out.println("生成的JDBC代码:\n" + jdbcCode);
		
	}
}