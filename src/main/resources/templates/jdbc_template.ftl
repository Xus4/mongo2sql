import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.sql.ResultSetMetaData;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public class GeneratedJdbcQuery {

    private static Connection getConnection() throws Exception {
        String jdbcUrl = "${jdbcUrl}";
        String username = "${username}";
        String password = "${password}";
        return DriverManager.getConnection(jdbcUrl, username, password);
    }

    public static List<Map<String, Object>> executeQuery(<#list params as param>${param.type} ${param.name}<#sep>, </#sep></#list>) {
        String sql = "${sqlQuery}";
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
                System.out.println(row); // 打印每一行
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return resultList;
    }
}
