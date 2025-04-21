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

public class ${collectionName}JDBC {

    private static Connection getConnection() throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");
        String jdbcUrl = "${jdbcUrl}";
        String username = "${username}";
        String password = "${password}";
        return DriverManager.getConnection(jdbcUrl, username, password);
    }

    public static List<Map<String, Object>> executeQuery(<#list params as param>${param.type} ${param.name}<#sep>, </#sep></#list>) {
        String sql = "${sqlQuery}";
        System.out.println("执行SQL语句: " + sql);
        List<Map<String, Object>> resultList = new ArrayList<>();
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
             
            <#list params as param>
            stmt.setObject(${param?index + 1}, ${param.name});
            </#list>

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
            <#list params as param>
            ${param.type} test${param.name?cap_first} = <#if param.type == "String">"test"<#elseif param.type == "int" || param.type == "Integer">1<#elseif param.type == "long" || param.type == "Long">1L<#elseif param.type == "double" || param.type == "Double">1.0<#elseif param.type == "boolean" || param.type == "Boolean">true<#else>null</#if>;
            </#list>
            
            List<Map<String, Object>> results = executeQuery(<#list params as param>test${param.name?cap_first}<#sep>, </#sep></#list>);
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
