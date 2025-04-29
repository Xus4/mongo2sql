package com.mongo2sql.generator;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import freemarker.template.Configuration;
import freemarker.template.Template;

public class FreemarkerJdbcGenerator implements JdbcCodeGenerator {
    private static final Logger logger = Logger.getLogger(FreemarkerJdbcGenerator.class.getName());
    private final Configuration freemarkerConfig;
    private static final String DEFAULT_JDBC_URL = "jdbc:mysql://localhost:3306/demo";
    private static final String DEFAULT_USERNAME = "root";
    private static final String DEFAULT_PASSWORD = "123456";
    
    public FreemarkerJdbcGenerator() {
        this.freemarkerConfig = new Configuration(Configuration.VERSION_2_3_31);
        this.freemarkerConfig.setClassForTemplateLoading(this.getClass(), "/templates");
    }
    
    @Override
	public String generateCode(String sqlQuery,String collectionName, List<QueryParameter> params) {
        return generateCode(sqlQuery,collectionName,params, DEFAULT_JDBC_URL, DEFAULT_USERNAME, DEFAULT_PASSWORD);
    }
    
	private String generateCode(String sqlQuery,String collectionName,  List<QueryParameter> params, String jdbcUrl, String username, String password) {
        try {
            Template template = freemarkerConfig.getTemplate("jdbc_template.ftl");
            Map<String, Object> dataModel = new HashMap<>();
            dataModel.put("sqlQuery", sqlQuery);
            String capitalizedCollectionName = collectionName.substring(0, 1).toUpperCase() + collectionName.substring(1);
            dataModel.put("collectionName", capitalizedCollectionName);
            dataModel.put("params", params);

            dataModel.put("jdbcUrl", jdbcUrl);
            dataModel.put("username", username);
            dataModel.put("password", password);
            
            StringWriter writer = new StringWriter();
            template.process(dataModel, writer);
            return writer.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate JDBC code", e);
        }
    }
    
    public String generateFile(String sqlQuery, String collectionName, List<QueryParameter> params) {
        try {
            String code = generateCode(sqlQuery, collectionName, params);
            String capitalizedCollectionName = collectionName.substring(0, 1).toUpperCase() + collectionName.substring(1);
            String filePath = "src/main/java/com/mongo2sql/jdbc/" + capitalizedCollectionName + "JDBC.java";
            
            java.nio.file.Path path = Paths.get(filePath);
            if (Files.exists(path)) {
                logger.info("删除已存在的文件: " + filePath);
                Files.delete(path);
            }
            
            Files.createDirectories(path.getParent());
            Files.write(path, code.getBytes(StandardCharsets.UTF_8));
            logger.info("成功生成文件: " + filePath);
            return code;
        } catch (IOException e) {
            logger.log(Level.SEVERE, "生成文件失败: " + e.getMessage(), e);
            throw new RuntimeException("Failed to write JDBC code to file", e);
        }
    }
}