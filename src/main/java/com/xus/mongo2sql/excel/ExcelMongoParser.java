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
import java.util.concurrent.Executors;
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
import com.xus.mongo2sql.llm.model.QianwenRequest;

public class ExcelMongoParser {
	private static final Logger logger = LoggerFactory.getLogger(ExcelMongoParser.class);
	private static final int MAX_CONCURRENT_REQUESTS = 20;
	private static final int THREAD_POOL_SIZE = MAX_CONCURRENT_REQUESTS * 2; // 考虑到每个请求会创建两个子任务
	private final ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

	public static class MongoCommand {
		private String command;
		private String collectionName; // 添加collectionName属性
		private int commandSize;
		private int index;

		public MongoCommand(String command, String collectionName, int commandSize, int index) {
			this.command = command;
			this.collectionName = collectionName;
			this.commandSize = commandSize;
			this.index = index;
		}

		public String getCommand() {
			return command;
		}

		public void setCommand(String command) {
			this.command = command;
		}

		// 添加collectionName的getter和setter方法
		public String getCollectionName() {
			return collectionName;
		}

		public void setCollectionName(String collectionName) {
			this.collectionName = collectionName;
		}

		public int getCommandSize() {
			return commandSize;
		}

		public void setCommandSize(int commandSize) {
			this.commandSize = commandSize;
		}

		public int getIndex() {
			return index;
		}

		public void setIndex(int index) {
			this.index = index;
		}
	}

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

				Cell srcCell2 = srcRow.getCell(2);
				Cell srcCell3 = srcRow.getCell(3);
				Cell srcCell4 = srcRow.getCell(4);

				if (srcCell2 == null || srcCell3 == null || srcCell4 == null) {
					continue;
				}

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

				ModelClient qianwenClient = new CommonModelClient(
						new ModelConfigForRequest("sk-5301c805d71e4e97821cbe4665b16436",
								"https://dashscope.aliyuncs.com/compatible-mode/v1", ModelType.QWEN3_235B.getValue()));
				ModelClient dsClient = new CommonModelClient(
						new ModelConfigForRequest("sk-5301c805d71e4e97821cbe4665b16436",
								"https://dashscope.aliyuncs.com/compatible-mode/v1", ModelType.DEEPSEEK_R1.getValue()));

				MongoCommand mongoCommand = new MongoCommand(command, collectionName, commandSize, rowIndex);

				CompletableFuture<Map<String, String>> future = CompletableFuture.supplyAsync(() -> {
					Map<String, String> results = new HashMap<>();
					try {
						CompletableFuture<String> future1 = CompletableFuture
								.supplyAsync(() -> convertMongoToSql(mongoCommand, qianwenClient), executorService);
						CompletableFuture<String> future2 = CompletableFuture
								.supplyAsync(() -> convertMongoToSql(mongoCommand, dsClient), executorService);

						String sql = future1.get(300, TimeUnit.SECONDS);
						String sql_r1 = future2.get(300, TimeUnit.SECONDS);

						results.put("sql", sql);
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
								

								System.out.println("转化结果----序号：" + rowIndex + "，集合名：" + results.get("collectionName")
										+ "，MongoDB脚本：\n" + destRows.get(rowIndex).getCell(2) + "\n" + ModelType.QWEN3_235B
										+ "转化后SQL:\n " + "\n " + results.get("sql") + "\n\n" + ModelType.DEEPSEEK_R1
										+ "转化后SQL:\n " + "\n " + results.get("sql_r1"));
								System.out.println("\n\n");
							}
						} catch (Exception e) {
							logger.error("处理结果时发生错误", e);
						}
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
		promptBuilder.append("请将以下MongoDB聚合查询转换为MySQL SQL语句，只需要输出SQL语句本身，输出的结果要美化显示，当前MongoDB脚本对应的collection name 为")
				.append(collectionName).append("，转换规则有：")
				.append("1. content.formData.x是固定的写法，在数据库转换时，已经默认扁平化处理，也就是去掉了前缀content.formData，只保留了后面的x,遇到content.formData按照扁平化的结构处理。")
				.append("2. $:和$$:开头的和${}包裹的都是自定义的占位符，用于变量替换的，这样的地方直接保留即可，作为字符串,使用单引号包裹，不需要做其他任何处理。")
				.append("3. 不要添加任何markdown的格式符号。如` 。").append("4. Boolean类型的true改为字符串类型。")
				.append("5. 对于脚本中出现的驼峰式字段，保留驼峰式的命名方式，不需要改写成下划线连接的形式。")
				.append("6. unwind中path为content.formData.x识别为从表,表名为：主表_x,主表中的id为从表中的parentId。")
				.append("7. MongoDB脚本中的_id和uniqueId字段，转成mysql sql后，映射为id字段。")
				.append("8. MongoDB脚本中所有pipeline都需要处理，包括$group。").append("9. 注意使用$lookup中的as属性来起别名。")
				.append("10. unwind里如果没有使用\"preserveNullAndEmptyArrays\": true，则用‌INNER JOIN，存在用left join。")
				.append("MongoDB脚本为：").append(cmd.getCommand());

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
				System.out.println("第 "+cmd.getIndex()+" 个转化完毕！集合名：" + collectionName + "，模型是:" + client.getModelConfig().getModelType()
						+ ", 耗时：" + duration + "ms");
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