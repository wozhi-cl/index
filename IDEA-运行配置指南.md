# IDEA 运行配置指南

## 问题分析
IDEA 运行时仍然使用 H2 数据库，说明配置文件没有正确加载。

## 解决方案

### 方案 1: 设置 IDEA 运行参数

1. **打开 Run Configuration**
   - Run → Edit Configurations
   - 选择您的 Application 配置

2. **设置 Program arguments**
   ```
   --spring.profiles.active=dev
   ```

3. **设置 VM options**
   ```
   -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005
   ```

4. **设置 Environment variables**
   ```
   SPRING_PROFILES_ACTIVE=dev
   ```

### 方案 2: 修改 application.yml

在 `application.yml` 中明确指定数据库配置：

```yaml
spring:
  application:
    name: index
  profiles:
    active: dev
  datasource:
    url: jdbc:mysql://localhost:3307/index_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
    username: root
    password: root123
    driver-class-name: com.mysql.cj.jdbc.Driver
```

### 方案 3: 使用命令行参数

在 IDEA 的 Terminal 中运行：

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev" -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"
```

### 方案 4: 临时禁用 Quartz

如果不需要调度功能，可以临时禁用：

在 `application-dev.yml` 中添加：
```yaml
spring:
  quartz:
    job-store-type: memory
    auto-startup: false
```

## 推荐步骤

1. **首先尝试方案 1** - 在 IDEA 中设置运行参数
2. **如果不行，尝试方案 2** - 修改 application.yml
3. **最后尝试方案 3** - 使用命令行运行

## 验证方法

启动后检查日志中是否出现：
```
HikariPool-1 - Starting...
HikariPool-1 - Added connection conn0: url=jdbc:mysql://localhost:3307/index_db
```

而不是：
```
HikariPool-1 - Added connection conn0: url=jdbc:h2:mem:testdb
```
