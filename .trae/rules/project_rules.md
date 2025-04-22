# Mongo2SQL Converter

一个强大的MongoDB聚合查询转SQL转换工具，支持将MongoDB的聚合管道查询转换为标准SQL语句，并能自动生成对应的JDBC代码。

## 功能特性

- MongoDB聚合管道查询转SQL转换
  - 支持$match阶段转换为WHERE子句
  - 支持$sort阶段转换为ORDER BY子句
  - 支持$project阶段转换为SELECT子句
  - 支持$lookup阶段转换为JOIN子句（开发中）
- 智能参数解析
  - 自动识别以$开头的变量作为JDBC方法参数
  - 支持复杂的嵌套JSON数据结构
- JDBC代码生成
  - 基于Freemarker模板引擎
  - 生成完整可执行的Java代码
  - 支持自定义集合名称映射到表名

## 快速开始

### Maven依赖

```xml
<dependency>
    <groupId>com.mongo2sql</groupId>
    <artifactId>mongo2sql</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 基本使用

```java
// 创建转换器实例
Mongo2SqlConverter converter = new Mongo2SqlConverter();

// MongoDB聚合查询示例
String mongoQuery = "{
    \"$match\": { \"status\": \"$status\" },
    \"$sort\": { \"createTime\": -1 },
    \"$project\": { \"id\": 1, \"name\": 1, \"status\": 1 }
}";

// 转换为SQL查询
String sqlQuery = converter.convertToSql(mongoQuery, "orders");
// 输出: SELECT id, name, status FROM orders WHERE status = ? ORDER BY createTime DESC

// 解析查询参数
List<QueryParameter> params = converter.parseParams(mongoQuery);
// 参数列表将包含: status

// 生成JDBC代码
String jdbcCode = converter.generateJdbcCode(mongoQuery, "orders");
```

## 项目架构

### 核心组件

1. **查询解析器**
   - `MongoAggregationParser`: 解析MongoDB聚合管道JSON
   - `MongoQueryParameterParser`: 提取查询中的参数变量

2. **SQL转换器**
   - `DefaultSqlConverter`: 实现MongoDB到SQL的转换逻辑
   - 支持多种聚合阶段的转换处理

3. **代码生成器**
   - `FreemarkerJdbcGenerator`: 基于模板生成JDBC代码
   - 支持参数绑定和SQL执行

## 支持的MongoDB操作符

### 查询操作符
- `$match`: 转换为SQL WHERE子句
- `$sort`: 转换为SQL ORDER BY子句
- `$project`: 转换为SQL SELECT子句字段投影
- `$lookup`: 转换为SQL JOIN子句（规划中）

### 字段映射规则
- MongoDB字段路径（如`user.name`）将被转换为对应的SQL列名
- 支持提取嵌套JSON中的字段

## 构建项目

1. 克隆仓库：
```bash
git clone https://github.com/yourusername/mongo2sql.git
```

2. 使用Maven构建：
```bash
mvn clean install
```

## 贡献指南

欢迎提交Pull Request或Issue！

1. Fork本仓库
2. 创建特性分支：`git checkout -b feature/YourFeature`
3. 提交更改：`git commit -m '添加新特性'`
4. 推送分支：`git push origin feature/YourFeature`
5. 提交Pull Request

## 许可证

本项目采用MIT许可证。详情请参阅[LICENSE](LICENSE)文件。