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

## 安装步骤

1. 克隆仓库：
   ```bash
   git clone https://github.com/yourusername/mongo2sql.git
   ```

2. 导入项目到您的IDE（如Eclipse或IntelliJ IDEA）。

3. 使用Maven构建项目：
   ```bash
   mvn clean install
   ```

## 使用说明

- 使用`Mongo2SqlConverter`类进行转换。
- 调用`convertToSql`方法将MongoDB查询转换为SQL。
- 使用`generateJdbcCode`方法生成JDBC代码。

## 贡献指南

欢迎贡献代码！请遵循以下步骤：

1. Fork本仓库。
2. 创建您的特性分支：`git checkout -b feature/AmazingFeature`。
3. 提交您的更改：`git commit -m '添加了一些特性'`。
4. 推送到分支：`git push origin feature/AmazingFeature`。
5. 打开一个Pull Request。

## 许可证

此项目使用MIT许可证。详情请参阅LICENSE文件。