package com.xus.mongo2sql.excel;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.xus.mongo2sql.llm.QianwenClient;
import com.xus.mongo2sql.llm.model.QianwenRequest;

public class ExcelMongoParser {

    public static class MongoCommand {
        private String command;
        private String collectionName; // 添加collectionName属性
        private int commandSize;
        private int index;
        public MongoCommand(String command,String collectionName,int commandSize,int index) {
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

            // 处理数据行
            for (int i = 1; i <= srcSheet.getLastRowNum(); i++) {
                Row srcRow = srcSheet.getRow(i);
                Row destRow = destSheet.createRow(i);

                // 复制原始数据
                for (int j = 0; j < 5; j++) {
                    Cell srcCell = srcRow.getCell(j);
                    Cell destCell = destRow.createCell(j, srcCell.getCellType());
                    if (srcCell.getCellType() == CellType.STRING) {
                        destCell.setCellValue(srcCell.getStringCellValue());
                    } else if (srcCell.getCellType() == CellType.NUMERIC) {
                        destCell.setCellValue(srcCell.getNumericCellValue());
                    }
                }

                // 生成SQL
                String command = srcRow.getCell(2).getStringCellValue();
                String collectionName = srcRow.getCell(3).getStringCellValue();
                int commandSize = (int) srcRow.getCell(4).getNumericCellValue();
                MongoCommand mongoCommand = new MongoCommand(command, collectionName, commandSize, i);
                
                String sql = convertMongoToSql(mongoCommand);
                destRow.createCell(5, CellType.STRING).setCellValue(sql);
                
                System.out.println("当前处理序号：" + i + "，集合名：" + mongoCommand.getCollectionName() 
                    + "，MongoDB脚本：\n" + command + "\n转化后SQL：\n" + sql);
            }
            System.out.println("\n\n");
            // 确保输出目录存在
            new File(outputFilePath).getParentFile().mkdirs();
            try (FileOutputStream fos = new FileOutputStream(outputFilePath)) {
                outputWorkbook.write(fos);
            }
        } catch (IOException e) {
            throw new IOException("文件处理失败：" + e.getMessage(), e);
        }
        return null; // Adjust return type as needed
    }


   private String convertMongoToSql( MongoCommand cmd) {
        String collectionName = cmd.getCollectionName();
        long startTime = System.currentTimeMillis();
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("请将以下MongoDB聚合查询转换为MySQL SQL语句，只需要输出SQL语句本身，并且不要带任何的markdown格式字符，输出的结果要美化显示，当前MongoDB脚本对应的collection name 为")
                     .append(collectionName)
                     .append("，其他注意事项有：\n")
                     .append("1. 对MongoDB的聚合查询语句进行分析，提取出其中的聚合操作，如$match、$sort、$project等，并将其转换为对应的MySQL SQL语句。\n")
                     .append("2. content.formData.x是固定的写法，在数据库转换时，已经默认扁平化处理，也就是去掉了前缀content.formData，只保留了后面的x。\n")
                     .append("3. $:和$$:开头的都是自定义的占位符，用于替换用的，这样的地方直接保留即可，不需要做任何的替换。\n")
                     .append("脚本为：\n")
                     .append(cmd.getCommand());
        
        String prompt = promptBuilder.toString();
        
        QianwenClient client = new QianwenClient();
        String result="";
        try {
            result = client.chat(prompt, QianwenRequest.QianwenModel.QWEN3_235B);
            result = QianwenRequest.cleanSql(result);
            long duration = System.currentTimeMillis() - startTime;
            System.out.println("当前处理集合名：" + collectionName + "，耗时：" + duration + "ms");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }
}