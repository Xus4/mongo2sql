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
				+ "      \"$match\": {\r\n"
				+ "        \"content.formData.businessCode\": \"$:context.refData.businessCode\"\r\n"
				+ "      }\r\n"
				+ "    },\r\n"
				+ "    {\r\n"
				+ "      \"$match\": {\r\n"
				+ "        \"$expr\": {\r\n"
				+ "          \"$eq\": [\r\n"
				+ "            \"$content.formData.isFinishOut\",\r\n"
				+ "            \"1\"\r\n"
				+ "          ]\r\n"
				+ "        }\r\n"
				+ "      }\r\n"
				+ "    },\r\n"
				+ "    {\r\n"
				+ "      \"$lookup\": {\r\n"
				+ "        \"from\": \"bond_trade_apply_res\",\r\n"
				+ "        \"localField\": \"content.formData.applyBusinessCode\",\r\n"
				+ "        \"foreignField\": \"content.formData.businessCode\",\r\n"
				+ "        \"as\": \"bondTrade\"\r\n"
				+ "      }\r\n"
				+ "    },\r\n"
				+ "    {\r\n"
				+ "      \"$lookup\": {\r\n"
				+ "        \"from\": \"bond_trading_out_or_in_approval_res\",\r\n"
				+ "        \"localField\": \"content.formData.businessCode\",\r\n"
				+ "        \"foreignField\": \"content.formData.businessCode\",\r\n"
				+ "        \"as\": \"bondTrading\"\r\n"
				+ "      }\r\n"
				+ "    },\r\n"
				+ "    {\r\n"
				+ "      \"$unwind\": {\r\n"
				+ "        \"path\": \"$bondTrading\"\r\n"
				+ "      }\r\n"
				+ "    },\r\n"
				+ "    {\r\n"
				+ "      \"$unwind\": {\r\n"
				+ "        \"path\": \"$bondTrade\"\r\n"
				+ "      }\r\n"
				+ "    },\r\n"
				+ "    {\r\n"
				+ "      \"$project\": {\r\n"
				+ "        \"uniqueId\": \"$bondTrade.uniqueId\",\r\n"
				+ "        \"tradeDate\": \"$bondTrading.content.formData.tradeDate\",\r\n"
				+ "        \"tradeAmount\": \"$bondTrading.content.formData.tradeAmount\",\r\n"
				+ "        \"netPrice\": \"$bondTrading.content.formData.netPrice\",\r\n"
				+ "        \"surfaceTotal\": \"$bondTrading.content.formData.surfaceTotal\"\r\n"
				+ "      }\r\n"
				+ "    },\r\n"
				+ "    {\r\n"
				+ "      \"$lookup\": {\r\n"
				+ "        \"from\": \"financing_collection\",\r\n"
				+ "        \"localField\": \"uniqueId\",\r\n"
				+ "        \"foreignField\": \"content.calId\",\r\n"
				+ "        \"as\": \"collection\"\r\n"
				+ "      }\r\n"
				+ "    },\r\n"
				+ "    {\r\n"
				+ "      \"$unwind\": {\r\n"
				+ "        \"path\": \"$collection\"\r\n"
				+ "      }\r\n"
				+ "    },\r\n"
				+ "    {\r\n"
				+ "      \"$project\": {\r\n"
				+ "        \"totalAmount\": {\r\n"
				+ "          \"$sum\": \"$collection.content.financialPlanList.interest\"\r\n"
				+ "        },\r\n"
				+ "        \"tradeDate\": 1,\r\n"
				+ "        \"tradeAmount\": 1,\r\n"
				+ "        \"netPrice\": 1,\r\n"
				+ "        \"surfaceTotal\": 1\r\n"
				+ "      }\r\n"
				+ "    }\r\n"
				+ "  ]\r\n"
				+ "}";
		
		String collectionName = "testCollection";

		String jdbcCode = converter.generateJdbcCode(mongoQuery, collectionName);
		System.out.println("生成的JDBC代码:\n" + jdbcCode);
		
	}
}