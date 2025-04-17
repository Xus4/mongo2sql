# Mongo2SQL Converter

一个强大的MongoDB聚合查询转SQL工具，支持将MongoDB的聚合管道查询转换为标准SQL语句，并能生成相应的JDBC代码。

## 项目特性

- 支持MongoDB聚合管道查询转换为SQL
- 自动生成可执行的JDBC代码
- 模块化设计，易于扩展
- 支持自定义数据库连接参数

## 项目结构

项目采用模块化设计，主要包含以下核心组件：

### 1. 解析器（Parser）
- `MongoAggregationParser`: 负责解析MongoDB聚合管道查询
- `AggregationPipeline`: 表示解析后的聚合管道结构
- 支持多种管道阶段：Match、Lookup、Project等

### 2. 转换器（Converter）
- `SqlConverter`: SQL转换器接口
- `DefaultSqlConverter`: 默认的SQL转换器实现
- 支持将MongoDB操作符转换为SQL语法

### 3. 代码生成器（Generator）
- `JdbcCodeGenerator`: JDBC代码生成器接口
- `FreemarkerJdbcGenerator`: 基于Freemarker的代码生成器实现
- 支持生成完整的JDBC执行代码

## 技术栈

- Java 1.8
- Groovy 3.0.9
- Jackson 2.13.0（JSON处理）
- Freemarker 2.3.31（模板引擎）
- JUnit 4.13.2（单元测试）

## 使用示例

```java
// 创建转换器实例
Mongo2SqlConverter converter = new Mongo2SqlConverter();

// 转换MongoDB查询为SQL
String mongoQuery = "...";
String sqlQuery = converter.convertToSql(mongoQuery);

// 生成JDBC代码
String jdbcCode = converter.generateJdbcCode(mongoQuery);

// 使用自定义数据库连接参数生成JDBC代码
String customJdbcCode = converter.generateJdbcCode(
    mongoQuery,
    "jdbc:mysql://localhost:3306/db",
    "username",
    "password"
);
```

## 构建项目

```bash
# 编译项目
mvn compile

# 运行测试
mvn test

# 打包
mvn package
```

## 开发计划

- [ ] 支持更多MongoDB聚合操作符
- [ ] 优化SQL查询性能
- [ ] 添加查询验证功能
- [ ] 支持更多数据库方言

## 贡献指南

欢迎提交Issue和Pull Request来帮助改进项目。在提交代码前，请确保：

1. 代码符合项目的编码规范
2. 添加适当的单元测试
3. 更新相关文档

## 许可证

本项目采用MIT许可证。详见[LICENSE](LICENSE)文件。