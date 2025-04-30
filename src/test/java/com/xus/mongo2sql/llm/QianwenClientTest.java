package com.xus.mongo2sql.llm;

import org.junit.jupiter.api.Test;

import com.xus.mongo2sql.excel.ExcelMongoParser;

class QianwenClientTest {

    @Test
    void testExcelConversionAndWrite() throws Exception {
        ExcelMongoParser parser = new ExcelMongoParser();
        parser.parseAndWriteExcel("G:\\xy_prd.schema0429_fortest.xlsx", "G:\\output.xlsx");
    }
}