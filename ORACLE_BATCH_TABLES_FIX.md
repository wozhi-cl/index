# Oracle Spring Batch 表缺失问题修复

## 🐛 问题描述

运行 Oracle 测试时出现以下错误：

```
org.springframework.jdbc.BadSqlGrammarException: 
bad SQL grammar [SELECT JOB_INSTANCE_ID, JOB_NAME FROM BATCH_JOB_INSTANCE ...]

Caused by: java.sql.SQLSyntaxErrorException: ORA-00942: 表或视图不存在
```

## 🔍 根本原因

`oracle_init.sql` 初始化脚本中**缺少 Spring Batch 的元数据表**。

Spring Batch 需要以下表来存储作业执行元数据：
- `BATCH_JOB_INSTANCE` - 作业实例
- `BATCH_JOB_EXECUTION` - 作业执行
- `BATCH_JOB_EXECUTION_PARAMS` - 作业参数
- `BATCH_STEP_EXECUTION` - 步骤执行
- `BATCH_STEP_EXECUTION_CONTEXT` - 步骤上下文
- `BATCH_JOB_EXECUTION_CONTEXT` - 作业上下文
- `BATCH_*_SEQ` - 序列（用于生成ID）

## ✅ 解决方案

### 1️⃣ 更新 `oracle_init.sql`

在 `sql/oracle_init.sql` 文件末尾添加了 Spring Batch 表的 DDL：

```sql
-- ===================================
-- Spring Batch 元数据表（Oracle 版本）
-- ===================================

-- 创建 Spring Batch 序列
CREATE SEQUENCE BATCH_STEP_EXECUTION_SEQ START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE BATCH_JOB_EXECUTION_SEQ START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE BATCH_JOB_SEQ START WITH 1 INCREMENT BY 1 NOCACHE;

-- BATCH_JOB_INSTANCE 表
CREATE TABLE BATCH_JOB_INSTANCE (
    JOB_INSTANCE_ID NUMBER(19) NOT NULL PRIMARY KEY,
    VERSION NUMBER(19),
    JOB_NAME VARCHAR2(100) NOT NULL,
    JOB_KEY VARCHAR2(32) NOT NULL,
    CONSTRAINT JOB_INST_UN UNIQUE (JOB_NAME, JOB_KEY)
);

-- ... 其他表 ...
```

### 2️⃣ 重新创建 Oracle 容器

由于 `oracle_init.sql` 只在容器**首次创建时**执行，需要删除旧容器和数据卷，重新创建：

**方法一：使用脚本（推荐）**

```bash
cd /Users/cailiang/Desktop/java/index
./scripts/recreate-oracle.sh
```

**方法二：手动执行**

```bash
# 1. 停止并删除容器
docker-compose stop oracle
docker-compose rm -f oracle

# 2. 删除数据卷
docker volume rm index_oracle_data

# 3. 重新启动
docker-compose up -d oracle

# 4. 等待启动完成（约 1-2 分钟）
docker-compose ps oracle
# 等待状态变为 "Up (healthy)"
```

## 📝 修改文件清单

| 文件 | 修改内容 | 状态 |
|------|---------|------|
| `sql/oracle_init.sql` | 添加 Spring Batch 元数据表 | ✅ 已完成 |
| `scripts/recreate-oracle.sh` | 创建重新部署脚本 | ✅ 已完成 |

## 🔍 Oracle vs MySQL 差异

Oracle 和 MySQL 的 Spring Batch 表主要差异：

| 特性 | MySQL | Oracle |
|------|-------|--------|
| 主键类型 | `BIGINT` | `NUMBER(19)` |
| 自增 | `AUTO_INCREMENT` | 使用 `SEQUENCE` |
| 时间类型 | `DATETIME(6)` | `TIMESTAMP(6)` |
| 大文本类型 | `TEXT` | `CLOB` |
| 序列管理 | 序列表 + 特殊INSERT | 原生 SEQUENCE |

## ✅ 验证步骤

### 1. 检查容器状态

```bash
docker-compose ps oracle
# 应该显示 "Up (healthy)"
```

### 2. 验证表是否创建

```bash
docker exec -it index-oracle sqlplus system/oracle123@XEPDB1
```

在 SQL*Plus 中执行：
```sql
-- 查看所有表
SELECT table_name FROM user_tables WHERE table_name LIKE 'BATCH_%';

-- 应该看到以下表：
-- BATCH_JOB_INSTANCE
-- BATCH_JOB_EXECUTION
-- BATCH_JOB_EXECUTION_PARAMS
-- BATCH_STEP_EXECUTION
-- BATCH_STEP_EXECUTION_CONTEXT
-- BATCH_JOB_EXECUTION_CONTEXT

-- 查看序列
SELECT sequence_name FROM user_sequences WHERE sequence_name LIKE 'BATCH_%';

-- 应该看到：
-- BATCH_STEP_EXECUTION_SEQ
-- BATCH_JOB_EXECUTION_SEQ
-- BATCH_JOB_SEQ

-- 退出
EXIT;
```

### 3. 运行 Oracle 测试

在 IDEA 中运行：
- `OracleToFileJobTest`
- `OracleToElasticsearchJobTest`

## 🎯 预期结果

测试应该能够成功启动并完成：

```
准备 Oracle 测试数据...
Oracle 服务已运行且健康。

[INFO] Starting fullIndexJob...
[INFO] Step: cleanupStep COMPLETED
[INFO] Step: readProcessWriteStep COMPLETED (Read: 5, Write: 5)
[INFO] Step: fullCheckStep COMPLETED
[INFO] Step: switchStep COMPLETED

✅ 测试 Oracle → File 全量索引成功！
```

## 📚 相关文档

- [Oracle 集成说明](docs/Oracle集成说明.md)
- [IDEA 测试运行指南](IDEA-测试运行指南.md)
- [最终修复总结](最终修复总结.md)

## 🔄 完整修复历史

| # | 问题 | 修复文件 | 文档 | 状态 |
|---|------|---------|------|------|
| 1 | 缺少 HttpClient 依赖 | `pom.xml` | ORACLE_CONNECTION_FIX.md | ✅ |
| 2 | GetQuickClientConfig Profile | `GetQuickClientConfig.java` | ORACLE_CONNECTION_FIX.md | ✅ |
| 3 | GetQuickWriter Profile | `GetQuickWriter.java` | ORACLE_WRITER_FIX.md | ✅ |
| 4 | 缺少 Spring Batch 表 | `sql/oracle_init.sql` | ORACLE_BATCH_TABLES_FIX.md | ✅ |

---

**修复完成时间**: 2025-10-16 22:45  
**影响范围**: Oracle Spring Batch 元数据存储  
**修复方式**: 更新初始化脚本并重新创建容器  
**状态**: ✅ 已完成，等待重新创建容器

