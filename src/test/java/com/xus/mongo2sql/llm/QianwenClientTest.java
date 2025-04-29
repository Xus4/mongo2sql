package com.xus.mongo2sql.llm;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.xus.mongo2sql.excel.ExcelMongoParser;
import com.xus.mongo2sql.llm.model.QianwenRequest;

class QianwenClientTest {

//    @Test
//    void testCompareModels() throws Exception {
//        QianwenClient client = new QianwenClient();
//        String prompt = "请将以下MongoDB聚合查询转换为MySQL SQL语句，只需要输出SQL语句本身，并且不要带任何的markdown格式字符，输出的结果要美化显示：\n" +
//                "{\n" +
//                "  \"$match\": { \"status\": \"active\" },\n" +
//                "  \"$sort\": { \"createTime\": -1 },\n" +
//                "  \"$project\": { \"id\": 1, \"name\": 1, \"status\": 1 }\n" +
//                "}";
//
//        System.out.println("=== 模型对比结果 ===");
//
//        // 测试QWEN_TURBO模型
//        long startTime = System.currentTimeMillis();
//        String resultTurbo = client.chat(prompt, QianwenRequest.QianwenModel.QWEN_TURBO);
//        long turboTime = System.currentTimeMillis() - startTime;
//        System.out.println("\nQWEN_TURBO 模型结果（耗时：" + turboTime + "ms）：");
//        System.out.println(resultTurbo.trim());
//        System.out.println("");
//
//        // 测试QWEN_PLUS模型
//        startTime = System.currentTimeMillis();
//        String resultPlus = client.chat(prompt, QianwenRequest.QianwenModel.QWEN_PLUS);
//        long plusTime = System.currentTimeMillis() - startTime;
//        System.out.println("\nQWEN_PLUS 模型结果（耗时：" + plusTime + "ms）：");
//        System.out.println(resultPlus.trim());
//        System.out.println("");
//        
//        // 测试QWEN3_235B模型
//        startTime = System.currentTimeMillis();
//        String resultQWEN3 = client.chat(prompt, QianwenRequest.QianwenModel.QWEN3_235B);
//        long qwen3Time = System.currentTimeMillis() - startTime;
//        System.out.println("\nQWEN3_235B 模型结果（耗时：" + qwen3Time + "ms）：");
//        System.out.println(resultQWEN3.trim());
//        System.out.println("");
//        
//        // 打印总耗时对比
//        System.out.println("\n=== 耗时对比 ===");
//        System.out.println("QWEN_TURBO: " + turboTime + "ms");
//        System.out.println("QWEN_PLUS: " + plusTime + "ms");
//        System.out.println("QWEN3_235B: " + qwen3Time + "ms");
//
//    }

    @Test
    void testExcelConversionAndWrite() throws Exception {
        ExcelMongoParser parser = new ExcelMongoParser();
        parser.parseAndWriteExcel("G:\\test88_188.xlsx", "G:\\output.xlsx");
    }
}