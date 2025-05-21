# Mongo2SQL 转换工具

## 项目简介
Mongo2SQL是一个强大的MongoDB聚合查询转换工具，能够将MongoDB的聚合管道查询语句智能转换为各种SQL方言。通过灵活的提示词配置，支持转换为不同类型的SQL语句（如MySQL、PostgreSQL、Oracle等），特别适用于数据库迁移或需要在MongoDB和关系型数据库之间进行数据查询转换的场景。

## 核心功能
- **MongoDB聚合查询转换**：支持将复杂的MongoDB聚合管道操作转换为等效的SQL语句，支持多种SQL方言
- **批量处理能力**：通过Excel文件批量处理多个MongoDB查询语句
- **多线程并发处理**：采用线程池技术，支持高并发转换处理
- **AI模型集成**：集成DeepSeek等AI模型，提供智能化的查询转换能力
- **实时结果输出**：转换结果实时写入Excel文件，支持断点续传

## 技术特点
- **高性能并发处理**
  - 采用线程池技术，支持最大50个并发请求
  - 使用CompletableFuture实现异步处理
  - 实现请求队列管理，避免资源耗尽

- **自定义转换规则到提示词以增强适配性，如**
  - 智能处理变量占位符（$:和$$:）
  - 保持字段命名规范（驼峰式命名）
  - 自动处理表关联和字段映射
  - 根据提示词生成对应SQL方言的语法

- **Excel文件处理**
  - 支持.xlsx格式的Excel文件读写
  - 自动复制源数据并追加转换结果
  - 实时保存转换进度

## 使用方法

### 环境要求
- Java 8或更高版本
- Maven 3.x

### 安装步骤
1. 克隆项目到本地
```bash
git clone https://github.com/xus/mongo2sql.git
```

2. 使用Maven安装依赖
```bash
mvn clean install
```

### Excel批量处理
1. 准备输入Excel文件，包含以下列：
   - 唯一标识码
   - MongoDB查询语句
   - 集合名称
   - 命令大小

2. 调用批处理方法：
```java
ExcelMongoParser parser = new ExcelMongoParser();
parser.parseAndWriteExcel("输入文件路径", "输出文件路径");
```

## 注意事项
- 确保输入的MongoDB查询语句格式正确
- 大规模批处理时注意内存使用
- 需要配置正确的AI模型API密钥
- 建议对转换结果进行人工复核