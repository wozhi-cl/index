# Oracle 连接问题修复说明

## 🔧 问题

Oracle XE 21c 使用可插拔数据库（PDB）架构，需要连接到 `XEPDB1` 而不是 `XE`。

## ✅ 修复内容

### 1. 更新 JDBC URL

**修复前**:
```
url: jdbc:oracle:thin:@localhost:1521:XE
username: index_user
password: index123
```

**修复后**:
```
url: jdbc:oracle:thin:@localhost:1521/XEPDB1
username: system
password: oracle123
```

### 2. 修改的文件

- ✅ `src/main/resources/application-oracle.yml`
- ✅ `src/test/resources/application-test-oracle-file.yml`
- ✅ `src/test/resources/application-test-oracle-es.yml`
- ✅ `docs/Oracle集成说明.md`
- ✅ `ORACLE_INTEGRATION_SUMMARY.md`

### 3. 正确的连接信息

| 参数 | 值 |
|------|-----|
| JDBC URL | `jdbc:oracle:thin:@localhost:1521/XEPDB1` |
| 用户名 | `system` |
| 密码 | `oracle123` |
| 服务名 | `XEPDB1` |
| 端口 | `1521` |

## 📝 重要说明

### Oracle XE 架构

Oracle XE 21c 使用以下架构：
- **CDB（容器数据库）**: XE
- **PDB（可插拔数据库）**: XEPDB1

应用应该连接到 **XEPDB1**（PDB），而不是 XE（CDB）。

### 用户管理

- 使用 `system` 用户进行测试和开发
- 生产环境建议创建专用用户：

```sql
-- 连接到 XEPDB1
sqlplus system/oracle123@//localhost:1521/XEPDB1

-- 创建专用用户
CREATE USER index_user IDENTIFIED BY index123;
GRANT CONNECT, RESOURCE TO index_user;
GRANT UNLIMITED TABLESPACE TO index_user;
```

## ✅ 验证

测试连接：
```bash
docker exec index-oracle sqlplus -s system/oracle123@//localhost:1521/XEPDB1 <<< "SELECT 'Connection OK' FROM DUAL;"
```

预期输出：
```
Connection OK
```

## 🎉 现在可以正常使用

修复后，Oracle 测试应该可以正常运行：

```bash
./scripts/test-oracle.sh file  # Oracle → File 测试
./scripts/test-oracle.sh es    # Oracle → Elasticsearch 测试
```
