package com.mongo2sql;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class Mongo2SqlConverterTest {

	@Test
	public void testGenerateJdbcCodeWithSpecialParam() throws JsonMappingException, JsonProcessingException {
		Mongo2SqlConverter converter = new Mongo2SqlConverter();
		String mongoQuery = "{\r\n"
				+ "  \"command\": [\r\n"
				+ "    {\r\n"
				+ "      \"$lookup\": {\r\n"
				+ "        \"from\": \"open_bank\",\r\n"
				+ "        \"foreignField\": \"uniqueId\",\r\n"
				+ "        \"localField\": \"content.formData.bankName\",\r\n"
				+ "        \"as\": \"bank\"\r\n"
				+ "      }\r\n"
				+ "    },\r\n"
				+ "    {\r\n"
				+ "      \"$unwind\": \"$bank\"\r\n"
				+ "    }\r\n"
				+ "  ]\r\n"
				+ "}";
		
		String collectionName = "testCollection";

		String jdbcCode = converter.generateJdbcCode(mongoQuery, collectionName);
		System.out.println("生成的JDBC代码:\n" + jdbcCode);
		
	}
}