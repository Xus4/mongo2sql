package com.mongo2sql.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.sql.ResultSetMetaData;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public class TestCollectionJDBC {

    private static Connection getConnection() throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");
        String jdbcUrl = "jdbc:mysql://localhost:3306/demo";
        String username = "root";
        String password = "123456";
        return DriverManager.getConnection(jdbcUrl, username, password);
    }

    public static List<Map<String, Object>> executeQuery() {
        String sql = "SELECT uniqueId, content FROM testCollection WHERE content.formData.custRole IN ('4')";
        System.out.println("执行SQL语句: " + sql);
        List<Map<String, Object>> resultList = new ArrayList<>();
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
             

            ResultSet rs = stmt.executeQuery();
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnLabel(i); // getColumnName() 也可以
                    Object value = rs.getObject(i);
                    row.put(columnName, value);
                }
                resultList.add(row);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return resultList;
    }

    public static void main(String[] args) {
        try {
            
            List<Map<String, Object>> results = executeQuery();
            System.out.println("查询结果：");
            for (Map<String, Object> row : results) {
                System.out.println(row);
            }
        } catch (Exception e) {
            System.err.println("测试查询时发生错误：");
            e.printStackTrace();
        }
    }
}
