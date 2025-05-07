package com.xus.mongo2sql.excel;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xus.mongo2sql.llm.CommonModelClient;
import com.xus.mongo2sql.llm.ModelClient;
import com.xus.mongo2sql.llm.ModelConfigForRequest;
import com.xus.mongo2sql.llm.ModelType;
import com.xus.mongo2sql.llm.MongoCommand;
import com.xus.mongo2sql.llm.model.QianwenRequest;

public class ExcelMongoParser {
	private static final Logger logger = LoggerFactory.getLogger(ExcelMongoParser.class);
	private static final int MAX_CONCURRENT_REQUESTS = 20;//不能大于CORE_POOL_SIZE，否则等待会超时
	private static final int CORE_POOL_SIZE = 20;
	
	private static final int MAX_POOL_SIZE = 200;
	private static final int QUEUE_CAPACITY = 1000;
	private static final long KEEP_ALIVE_TIME = 60L;
	private final ExecutorService executorService = new ThreadPoolExecutor(CORE_POOL_SIZE, MAX_POOL_SIZE,
			KEEP_ALIVE_TIME, TimeUnit.SECONDS, new LinkedBlockingQueue<>(QUEUE_CAPACITY),
			new ThreadPoolExecutor.CallerRunsPolicy());

//	ModelClient qianwenClient = new CommonModelClient(
//			new ModelConfigForRequest("sk-5301c805d71e4e97821cbe4665b16436",
//					"https://dashscope.aliyuncs.com/compatible-mode/v1", ModelType.QWEN3_235B.getValue()));
	ModelClient dsClient = new CommonModelClient(new ModelConfigForRequest("sk-5301c805d71e4e97821cbe4665b16436",
			"https://dashscope.aliyuncs.com/compatible-mode/v1", ModelType.DEEPSEEK_R1.getValue()));

	public List<MongoCommand> parseAndWriteExcel(String inputFilePath, String outputFilePath) throws IOException {
		try (InputStream fis = new FileInputStream(inputFilePath);
				Workbook workbook = new XSSFWorkbook(fis);
				Workbook outputWorkbook = new XSSFWorkbook()) {

			Sheet srcSheet = workbook.getSheetAt(0);
			Sheet destSheet = outputWorkbook.createSheet(srcSheet.getSheetName());

			// 复制表头
			Row headerRow = destSheet.createRow(0);
			for (Cell cell : srcSheet.getRow(0)) {
				Cell newCell = headerRow.createCell(cell.getColumnIndex(), cell.getCellType());
				newCell.setCellValue(cell.getStringCellValue());
			}

			List<CompletableFuture<Map<String, String>>> futures = new ArrayList<>();
			Map<Integer, Row> destRows = new HashMap<>();

			// 处理数据行
			for (int i = 1; i <= srcSheet.getLastRowNum(); i++) {
				Row srcRow = srcSheet.getRow(i);
				Row destRow = destSheet.createRow(i);
				destRows.put(i, destRow);

				// 复制原始数据
				for (int j = 0; j < 5; j++) {
					Cell srcCell = srcRow.getCell(j);
					if (srcCell == null) {
						continue;
					}
					Cell destCell = destRow.createCell(j, srcCell.getCellType());
					if (srcCell.getCellType() == CellType.STRING) {
						destCell.setCellValue(srcCell.getStringCellValue());
					} else if (srcCell.getCellType() == CellType.NUMERIC) {
						destCell.setCellValue(srcCell.getNumericCellValue());
					}
				}
				Cell srcCell0 = srcRow.getCell(0);
				Cell srcCell2 = srcRow.getCell(2);
				Cell srcCell3 = srcRow.getCell(3);
				Cell srcCell4 = srcRow.getCell(4);

				if (srcCell0==null||srcCell2 == null || srcCell3 == null || srcCell4 == null) {
					continue;
				}
				String uniqueCode= srcCell0.getStringCellValue();
				String jsonStr = srcCell2.getStringCellValue();
				String command = "";
				try {
					ObjectMapper objectMapper = new ObjectMapper();
					JsonNode jsonNode = objectMapper.readTree(jsonStr);
					JsonNode commandNode = jsonNode.get("command");
					command = commandNode != null ? objectMapper.writeValueAsString(commandNode) : jsonStr;
				} catch (Exception e) {
					logger.warn("JSON解析失败，使用原始字符串作为command", e);
				}
				String collectionName = srcCell3.getStringCellValue();
				int commandSize = (int) srcCell4.getNumericCellValue();
				final int rowIndex = i;

				MongoCommand mongoCommand = new MongoCommand(uniqueCode,jsonStr,command, collectionName, commandSize, rowIndex);

				CompletableFuture<Map<String, String>> future = CompletableFuture.supplyAsync(() -> {
					Map<String, String> results = new HashMap<>();
					try {
//						CompletableFuture<String> future1 = CompletableFuture
//								.supplyAsync(() -> convertMongoToSql(mongoCommand, qianwenClient), executorService)
//								.exceptionally(e -> {
//									logger.error("千问模型转换失败", e);
//									return "转换错误：" + e.getMessage();
//								});

						CompletableFuture<String> future2 = CompletableFuture
								.supplyAsync(() -> convertMongoToSql(mongoCommand, dsClient), executorService)
								.exceptionally(e -> {
									logger.error("DeepSeek模型转换失败", e);
									return "转换错误：" + e.getMessage();
								});

						// String sql = future1.get(300, TimeUnit.SECONDS);
						String sql_r1 = future2.get(1000, TimeUnit.SECONDS);

						// results.put("sql", sql);
						results.put("sql_r1", sql_r1);
						results.put("rowIndex", String.valueOf(rowIndex));
						results.put("command", mongoCommand.getCommand());
						results.put("collectionName", collectionName);
					} catch (Exception e) {
						logger.error("处理行 {} 时发生错误", rowIndex, e);
					}
					return results;
				}, executorService);

				futures.add(future);

				// 当累积的任务达到最大并发数或是最后一批数据时，等待当前批次完成
				if (futures.size() >= MAX_CONCURRENT_REQUESTS || i == srcSheet.getLastRowNum()) {
					CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

					// 处理完成的结果
					for (CompletableFuture<Map<String, String>> completedFuture : futures) {
						try {
							Map<String, String> results = completedFuture.get();
							if (!results.isEmpty()) {
								int resultRowIndex = Integer.parseInt(results.get("rowIndex"));
								Row resultRow = destRows.get(resultRowIndex);

								resultRow.createCell(5, CellType.STRING).setCellValue(results.get("sql"));
								resultRow.createCell(6, CellType.STRING).setCellValue(results.get("sql_r1"));
								System.out.println("\n\n");
							}
						} catch (Exception e) {
							logger.error("处理结果时发生错误", e);
						}
					}

					// 实时写回Excel文件
					try (FileOutputStream fos = new FileOutputStream(outputFilePath)) {
						outputWorkbook.write(fos);
					} catch (IOException e) {
						logger.error("写入Excel文件时发生错误", e);
					}

					futures.clear();
				}
			}

			// 确保输出目录存在
			new File(outputFilePath).getParentFile().mkdirs();
			try (FileOutputStream fos = new FileOutputStream(outputFilePath)) {
				outputWorkbook.write(fos);
			}
		} catch (IOException e) {
			throw new IOException("文件处理失败：" + e.getMessage(), e);
		} finally {
			executorService.shutdown();
			try {
				if (!executorService.awaitTermination(200, TimeUnit.SECONDS)) {
					executorService.shutdownNow();
				}
			} catch (InterruptedException e) {
				executorService.shutdownNow();
				Thread.currentThread().interrupt();
			}
		}
		return null;
	}

	private String convertMongoToSql(MongoCommand cmd, ModelClient client) {
		String collectionName = cmd.getCollectionName();
		long startTime = System.currentTimeMillis();
		StringBuilder promptBuilder = new StringBuilder();
		promptBuilder.append("请将以下MongoDB聚合查询转换为Oracle 12c SQL语句，只需要输出SQL语句本身，输出的结果要美化显示，当前MongoDB脚本对应的collection name 为")
				.append(collectionName)
				.append("，转换规则有：")
				.append("1. content.formData.x是固定的写法，在数据库转换时，已经默认扁平化处理，也就是去掉了前缀content.formData，只保留了后面的x,遇到content.formData按照扁平化的结构处理。")
				.append("2. $:和$$:开头的和${}包裹的都是自定义的占位符，用于变量替换的，这样的地方直接保留即可，作为字符串,使用单引号包裹，不需要做其他任何处理。")
				.append("3. 不要添加任何markdown的格式符号。如` 。")
				.append("4. Boolean类型的true改为字符串类型。")
				.append("5. 对于脚本中出现的驼峰式字段，保留驼峰式的命名方式，不需要改写成下划线连接的形式。")
				.append("6. unwind中path为content.formData.x识别为从表,表名为：主表_x,主表中的id为从表中的parentId。")
				.append("7. MongoDB脚本中的_id和uniqueId字段，在关系型数据库中人工转成id字段。转成后不要再出现_id和uniqueId，用id替代即可。")
				.append("8. MongoDB脚本中所有pipeline都需要处理，包括$group。")
				.append("9. 注意使用$lookup中的as属性来起别名。")
				.append("10. unwind里如果没有使用\"preserveNullAndEmptyArrays\": true，则用‌INNER JOIN，存在用left join。")
				.append("11. 不要使用操作json的函数。")
				.append("12. 给主表起别名固定为main_content即可。")
				.append("本次任务要转的MongoDB脚本为：").append(cmd.getCommand())
				.append("\r\n下面是三个转化的案例，供你参考:\r\n"
						+ "案例一，MongoDB脚本是：\r\n"
						+ "{\r\n"
						+ "  \"command\": [\r\n"
						+ "    {\r\n"
						+ "      \"$match\": {\r\n"
						+ "        \"queryMonth\": {\r\n"
						+ "          \"$eq\": \"$:context.queryFormData.queryMonth\"\r\n"
						+ "        }\r\n"
						+ "      }\r\n"
						+ "    },\r\n"
						+ "    {\r\n"
						+ "      \"$project\": {\r\n"
						+ "        \"content\": \"$content\"\r\n"
						+ "      }\r\n"
						+ "    },\r\n"
						+ "    {\r\n"
						+ "      \"$unwind\": {\r\n"
						+ "        \"path\": \"$content\",\r\n"
						+ "        \"preserveNullAndEmptyArrays\": true\r\n"
						+ "      }\r\n"
						+ "    },\r\n"
						+ "    {\r\n"
						+ "      \"$project\": {\r\n"
						+ "        \"uniqueId\": \"$content.uniqueId\",\r\n"
						+ "        \"flag\": \"$content.flag\",\r\n"
						+ "        \"amountType\": \"$content.amountType\",\r\n"
						+ "        \"dateDay\": \"$content.dateDay\",\r\n"
						+ "        \"rent\": \"$content.rent\",\r\n"
						+ "        \"principal\": \"$content.principal\",\r\n"
						+ "        \"interest\": \"$content.interest\",\r\n"
						+ "        \"rentTotal\": \"$content.rentTotal\",\r\n"
						+ "        \"capitalPrincipal\": \"$content.capitalPrincipal\",\r\n"
						+ "        \"capitalInterest\": \"$content.capitalInterest\"\r\n"
						+ "      }\r\n"
						+ "    },\r\n"
						+ "    {\r\n"
						+ "      \"$sort\": {\r\n"
						+ "        \"uniqueId\": 1\r\n"
						+ "      }\r\n"
						+ "    }\r\n"
						+ "  ]\r\n"
						+ "}\r\n"
						+ "，转成sql应该是：\r\n"
						+ "SELECT\r\n"
						+ "    sd_content.id AS id,\r\n"
						+ "    sd_content.id AS uniqueId,\r\n"
						+ "    sd_content.flag AS flag,\r\n"
						+ "    sd_content.amountType AS amountType,\r\n"
						+ "    sd_content.dateDay AS dateDay,\r\n"
						+ "    sd_content.rent AS rent,\r\n"
						+ "    sd_content.principal AS principal,\r\n"
						+ "    sd_content.interest AS interest,\r\n"
						+ "    sd_content.rentTotal AS rentTotal,\r\n"
						+ "    sd_content.capitalPrincipal AS capitalPrincipal,\r\n"
						+ "    sd_content.capitalInterest AS capitalInterest\r\n"
						+ "FROM\r\n"
						+ "    system_date main_content\r\n"
						+ "LEFT JOIN\r\n"
						+ "    system_date_content sd_content ON main_content.id = sd_content.parentId\r\n"
						+ "WHERE\r\n"
						+ "    main_content.queryMonth = '$:context.queryFormData.queryMonth'\r\n"
						+ "ORDER BY\r\n"
						+ "    id ASC;\r\n"
						+ "案例二，MongoDB脚本是：\r\n"
						+ "{\r\n"
						+ "  \"command\": [\r\n"
						+ "    {\r\n"
						+ "      \"$match\": {\r\n"
						+ "        \"content.formData.primaryMG\": \"$:context.currentUser['id']\"\r\n"
						+ "      }\r\n"
						+ "    },\r\n"
						+ "    {\r\n"
						+ "      \"$project\": {\r\n"
						+ "        \"uniqueId\": 1,\r\n"
						+ "        \"content\": 1,\r\n"
						+ "        \"principal\": {\r\n"
						+ "          \"$cond\": {\r\n"
						+ "            \"if\": {\r\n"
						+ "              \"$eq\": [\r\n"
						+ "                \"$content.formData.leaseType\",\r\n"
						+ "                \"2\"\r\n"
						+ "              ]\r\n"
						+ "            },\r\n"
						+ "            \"then\": \"$content.formData.olAmount\",\r\n"
						+ "            \"else\": \"$content.formData.principal\"\r\n"
						+ "          }\r\n"
						+ "        }\r\n"
						+ "      }\r\n"
						+ "    },\r\n"
						+ "    {\r\n"
						+ "      \"$addFields\": {\r\n"
						+ "        \"content.formData.referenceRate\": {\r\n"
						+ "          \"$cond\": {\r\n"
						+ "            \"if\": {\r\n"
						+ "              \"$eq\": [\r\n"
						+ "                \"$content.formData.referenceRate\",\r\n"
						+ "                \"0\"\r\n"
						+ "              ]\r\n"
						+ "            },\r\n"
						+ "            \"then\": \"\",\r\n"
						+ "            \"else\": \"$content.formData.referenceRate\"\r\n"
						+ "          }\r\n"
						+ "        }\r\n"
						+ "      }\r\n"
						+ "    }\r\n"
						+ "  ]\r\n"
						+ "}\r\n"
						+ "，转成sql应该是：\r\n"
						+ "SELECT \r\n"
						+ "    main_content.id AS uniqueId,\r\n"
						+ "    main_content.*,\r\n"
						+ "    CASE \r\n"
						+ "        WHEN main_content.leaseType = '2' THEN main_content.olAmount\r\n"
						+ "        ELSE main_content.principal\r\n"
						+ "    END AS principal,\r\n"
						+ "    CASE \r\n"
						+ "        WHEN main_content.referenceRate = '0' THEN ''\r\n"
						+ "        ELSE main_content.referenceRate\r\n"
						+ "    END AS referenceRate\r\n"
						+ "FROM \r\n"
						+ "    quotation_collection main_content\r\n"
						+ "WHERE \r\n"
						+ "    main_content.primaryMG = '$:context.currentUser[''id'']'\r\n"
						+ "	\r\n"
						+ "案例三，MongoDB脚本是：\r\n"
						+ "{\r\n"
						+ "  \"requiresPermission\": \"statistics:query\",\r\n"
						+ "  \"chiefDept\": \"chiefDept\",\r\n"
						+ "  \"querySecondOrgChild\": true,\r\n"
						+ "  \"command\": [\r\n"
						+ "    {\r\n"
						+ "      \"$lookup\": {\r\n"
						+ "        \"from\": \"customer_basic_info\",\r\n"
						+ "        \"localField\": \"content.formData.custId\",\r\n"
						+ "        \"foreignField\": \"uniqueId\",\r\n"
						+ "        \"as\": \"custInfo\"\r\n"
						+ "      }\r\n"
						+ "    },\r\n"
						+ "    {\r\n"
						+ "      \"$unwind\": \"$custInfo\"\r\n"
						+ "    },\r\n"
						+ "    {\r\n"
						+ "      \"$project\": {\r\n"
						+ "        \"content\": 1,\r\n"
						+ "        \"createdTime\": 1,\r\n"
						+ "        \"uniqueId\": 1,\r\n"
						+ "        \"custCode\": \"$custInfo.content.formData.custId\",\r\n"
						+ "        \"chiefDept\": \"$content.formData.chiefDept\",\r\n"
						+ "        \"isSelect\": {\r\n"
						+ "          \"$cond\": {\r\n"
						+ "            \"if\": {\r\n"
						+ "              \"$in\": [\r\n"
						+ "                \"$content.formData.projectNo\",\r\n"
						+ "                \"$:context.parentFormData.projectList==undefined?[]:context.parentFormData.projectList.map(i=>i.projectNo)\"\r\n"
						+ "              ]\r\n"
						+ "            },\r\n"
						+ "            \"then\": true,\r\n"
						+ "            \"else\": false\r\n"
						+ "          }\r\n"
						+ "        }\r\n"
						+ "      }\r\n"
						+ "    },\r\n"
						+ "    {\r\n"
						+ "      \"$match\": {\r\n"
						+ "        \"isSelect\": false\r\n"
						+ "      }\r\n"
						+ "    },\r\n"
						+ "    {\r\n"
						+ "      \"$sort\": {\r\n"
						+ "        \"createdTime\": -1\r\n"
						+ "      }\r\n"
						+ "    },\r\n"
						+ "    {\r\n"
						+ "      \"$lookup\": {\r\n"
						+ "        \"from\": \"contract_loan_info_res\",\r\n"
						+ "        \"let\": {\r\n"
						+ "          \"projectUniqueId\": \"$uniqueId\"\r\n"
						+ "        },\r\n"
						+ "        \"pipeline\": [\r\n"
						+ "          {\r\n"
						+ "            \"$lookup\": {\r\n"
						+ "              \"from\": \"contract_main_info_res\",\r\n"
						+ "              \"localField\": \"content.formData.contractId\",\r\n"
						+ "              \"foreignField\": \"uniqueId\",\r\n"
						+ "              \"as\": \"contract\"\r\n"
						+ "            }\r\n"
						+ "          },\r\n"
						+ "          {\r\n"
						+ "            \"$unwind\": {\r\n"
						+ "              \"path\": \"$contract\",\r\n"
						+ "              \"preserveNullAndEmptyArrays\": true\r\n"
						+ "            }\r\n"
						+ "          },\r\n"
						+ "          {\r\n"
						+ "            \"$match\": {\r\n"
						+ "              \"$expr\": {\r\n"
						+ "                \"$eq\": [\r\n"
						+ "                  \"$contract.content.formData.projectUniqueId\",\r\n"
						+ "                  \"$$projectUniqueId\"\r\n"
						+ "                ]\r\n"
						+ "              }\r\n"
						+ "            }\r\n"
						+ "          }\r\n"
						+ "        ],\r\n"
						+ "        \"as\": \"loan\"\r\n"
						+ "      }\r\n"
						+ "    },\r\n"
						+ "    {\r\n"
						+ "      \"$addFields\": {\r\n"
						+ "        \"isStartingRent\": {\r\n"
						+ "          \"$cond\": {\r\n"
						+ "            \"if\": {\r\n"
						+ "              \"$eq\": [\r\n"
						+ "                {\r\n"
						+ "                  \"$size\": \"$loan\"\r\n"
						+ "                },\r\n"
						+ "                0\r\n"
						+ "              ]\r\n"
						+ "            },\r\n"
						+ "            \"then\": \"0\",\r\n"
						+ "            \"else\": \"1\"\r\n"
						+ "          }\r\n"
						+ "        }\r\n"
						+ "      }\r\n"
						+ "    },\r\n"
						+ "    {\r\n"
						+ "      \"$match\": {\r\n"
						+ "        \"isStartingRent\": \"$:context.refData.isStartingRent\"\r\n"
						+ "      }\r\n"
						+ "    }\r\n"
						+ "  ]\r\n"
						+ "}\r\n"
						+ "，转成sql应该是：\r\n"
						+ "SELECT main_content.id AS id,\r\n"
						+ "       main_content.createdTime,\r\n"
						+ "       custInfo.\"custId\" AS custCode,\r\n"
						+ "       main_content.chiefDept,\r\n"
						+ "       CASE \r\n"
						+ "           WHEN main_content.projectNo IN ('$:context.parentFormData.projectList==undefined?[]:context.parentFormData.projectList.map(i=>i.projectNo)') THEN 'true'\r\n"
						+ "           ELSE 'false'\r\n"
						+ "       END AS isSelect,\r\n"
						+ "       CASE \r\n"
						+ "           WHEN loan_sub.countSize = 0 THEN '0'\r\n"
						+ "           ELSE '1'\r\n"
						+ "       END AS isStartingRent\r\n"
						+ "FROM project_basic_info_res main_content\r\n"
						+ "INNER JOIN customer_basic_info custInfo ON main_content.custId = custInfo.id\r\n"
						+ "LEFT JOIN (\r\n"
						+ "    select \r\n"
						+ "         COUNT(contract.id) countSize,\r\n"
						+ "         contract.projectUniqueId\r\n"
						+ "    FROM contract_loan_info_res loan\r\n"
						+ "    LEFT JOIN contract_main_info_res contract ON loan.contractId = contract.id\r\n"
						+ "    group by contract.projectUniqueId\r\n"
						+ ") loan_sub ON main_content.id = loan_sub.projectUniqueId\r\n"
						+ "WHERE (CASE \r\n"
						+ "           WHEN main_content.projectNo IN ('$:context.parentFormData.projectList==undefined?[]:context.parentFormData.projectList.map(i=>i.projectNo)') THEN 'true'\r\n"
						+ "           ELSE 'false'\r\n"
						+ "       END) = 'false'\r\n"
						+ "  AND (CASE \r\n"
						+ "           WHEN loan_sub.countSize = 0 THEN '0'\r\n"
						+ "           ELSE '1'\r\n"
						+ "       END) = '$:context.refData.isStartingRent'\r\n"
						+ "ORDER BY main_content.createdTime DESC;\r\n"
						+ "");
		String prompt = promptBuilder.toString();

		String result = "";

		// 添加重试机制
		int maxRetries = 3;
		int retryCount = 0;
		while (retryCount < maxRetries) {
			try {
				result = client.chat(prompt);
				result = QianwenRequest.cleanSql(result);
				long duration = System.currentTimeMillis() - startTime;
				logger.info("转换开始----序号：" + cmd.getIndex() + " , uniqueCode: "+cmd.getUniqueCode()+",集合名：{}", collectionName);
				logger.info("转化结果----序号：" + cmd.getIndex() + " , uniqueCode: "+cmd.getUniqueCode()+",集合名：" + collectionName + ", 耗时：" + duration + "ms"
						+ "，MongoDB脚本：\n" + cmd.getSourceCommand() + "\n" + client.getModelConfig().getModelType()
						+ "转化后SQL:\n " + "\n " + result + "\n\n");
				logger.info("转换完成----序号：" + cmd.getIndex() + " , uniqueCode: "+cmd.getUniqueCode()+",集合名：{}，耗时：{}ms", collectionName, duration);
				break;
			} catch (IOException e) {
				retryCount++;
				if (retryCount == maxRetries) {
					logger.error("转换失败，已重试{}次", maxRetries, e);
				} else {
					System.out.println("转换失败，正在进行第" + retryCount + "次重试...");
					try {
						Thread.sleep(1000 * retryCount); // 递增重试延迟
					} catch (InterruptedException ie) {
						Thread.currentThread().interrupt();
						break;
					}
				}
			}
		}
		return result;
	}
}