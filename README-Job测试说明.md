# Job 测试说明文档

**创建时间**: 2025-10-14  
**测试框架**: JUnit 5 + Spring Batch Test

---

## 📋 概述

本文档说明如何使用创建的测试类来测试全量索引Job和增量索引Job。

---

## 📁 测试文件结构

```
src/test/java/com/company/index/batch/job/
├── FullIndexJobTest.java           # 全量索引Job测试
├── IncrementalIndexJobTest.java    # 增量索引Job测试
└── JobTestConfiguration.java       # 测试配置类

src/test/resources/
└── application-test.yml            # 测试环境配置
```

---

## 🎯 测试覆盖

### FullIndexJobTest - 全量索引Job测试

**测试场景** (共10个测试用例):

1. **数据初始化验证** - 验证测试数据是否正确准备
2. **Job完整执行测试** - 测试全量索引Job的完整流程
3. **清理步骤测试** - 测试cleanupStep单独执行
4. **读写处理步骤测试** - 测试readProcessWriteStep单独执行
5. **检查步骤测试** - 测试fullCheckStep单独执行
6. **切换步骤测试** - 测试switchStep单独执行
7. **空数据集测试** - 测试没有数据时Job的行为
8. **大数据量测试** - 测试处理1100条记录的性能
9. **幂等性测试** - 测试Job的重复执行
10. **数据完整性测试** - 验证处理后数据的完整性

### IncrementalIndexJobTest - 增量索引Job测试

**测试场景** (共10个测试用例):

1. **数据初始化验证** - 验证基础数据和增量数据
2. **Job完整执行测试** - 测试增量索引Job的完整流程
3. **增量读写步骤测试** - 测试deltaReadProcessWriteStep单独执行
4. **检查步骤测试** - 测试checkStep单独执行
5. **条件重建步骤测试** - 测试conditionalRebuildStep单独执行
6. **实时更新测试** - 测试新增增量数据的处理
7. **无增量数据测试** - 测试没有新数据时Job的行为
8. **大量增量数据测试** - 测试处理500条增量记录
9. **幂等性测试** - 测试Job的重复执行
10. **数据完整性测试** - 验证处理后数据的完整性

---

## 🚀 运行测试

### 方式1: 使用Maven命令行

```bash
# 运行所有Job测试
mvn test -Dtest="*JobTest"

# 运行全量索引Job测试
mvn test -Dtest=FullIndexJobTest

# 运行增量索引Job测试
mvn test -Dtest=IncrementalIndexJobTest

# 运行指定的测试方法
mvn test -Dtest=FullIndexJobTest#testFullIndexJobExecution

# 运行测试并生成报告
mvn clean test surefire-report:report
```

### 方式2: 使用IDEA运行

1. **运行整个测试类**:
   - 右键点击测试类 → `Run 'FullIndexJobTest'`

2. **运行单个测试方法**:
   - 右键点击测试方法 → `Run 'testFullIndexJobExecution()'`

3. **调试测试**:
   - 右键点击 → `Debug 'FullIndexJobTest'`

4. **查看测试覆盖率**:
   - 右键点击 → `Run 'FullIndexJobTest' with Coverage`

### 方式3: 使用测试脚本

创建一个测试脚本：

```bash
#!/bin/bash
# scripts/run-job-tests.sh

echo "======================================"
echo "运行Job测试"
echo "======================================"
echo ""

# 运行全量索引Job测试
echo "1. 运行全量索引Job测试..."
mvn test -Dtest=FullIndexJobTest -q

if [ $? -eq 0 ]; then
    echo "✅ 全量索引Job测试通过"
else
    echo "❌ 全量索引Job测试失败"
fi
echo ""

# 运行增量索引Job测试
echo "2. 运行增量索引Job测试..."
mvn test -Dtest=IncrementalIndexJobTest -q

if [ $? -eq 0 ]; then
    echo "✅ 增量索引Job测试通过"
else
    echo "❌ 增量索引Job测试失败"
fi
echo ""

echo "======================================"
echo "测试完成"
echo "======================================"
```

---

## 📊 测试数据准备

### FullIndexJobTest 数据准备

测试类会自动在`@BeforeEach`中准备数据：

```java
private void initTestData() {
    // 插入100条基础测试数据
    for (int i = 1; i <= 100; i++) {
        String name = "TestUser_" + i;
        String email = "user" + i + "@test.com";
        int dataValue = i * 10;
        jdbcTemplate.update(sql, name, email, dataValue, timestamp, timestamp);
    }
}
```

**数据特点**:
- 100条基础记录
- ID从1到100
- 包含name, email, data_value字段
- 时间戳分布在过去100小时内

### IncrementalIndexJobTest 数据准备

```java
private void initTestData() {
    // 50条旧数据（30分钟前）
    for (int i = 1; i <= 50; i++) {
        // 插入旧数据...
    }
    
    // 30条新数据（最近2分钟内）
    for (int i = 51; i <= 80; i++) {
        // 插入新数据...
    }
}
```

**数据特点**:
- 50条旧数据（30分钟前更新）
- 30条新数据（最近2分钟内更新）
- 用于测试增量读取逻辑

---

## 🔍 测试验证点

### 1. Job执行状态验证

```java
assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());
```

### 2. Step执行验证

```java
// 验证Step数量
assertEquals(4, jobExecution.getStepExecutions().size());

// 验证每个Step状态
for (StepExecution stepExecution : jobExecution.getStepExecutions()) {
    assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
}
```

### 3. 数据处理验证

```java
// 验证读取和写入数量
assertTrue(stepExecution.getReadCount() > 0);
assertEquals(stepExecution.getReadCount(), stepExecution.getWriteCount());
```

### 4. 数据完整性验证

```java
// 验证源数据未被修改
assertEquals(countBefore, countAfter);

// 验证数据有效性
Integer validRecords = jdbcTemplate.queryForObject(
    "SELECT COUNT(*) FROM sample_data WHERE id IS NOT NULL", Integer.class);
```

---

## 📝 测试输出示例

### 成功的测试输出

```
====== Job执行详情 ======
Job名称: fullIndexJob
Job状态: COMPLETED
开始时间: 2025-10-14 14:30:00
结束时间: 2025-10-14 14:30:05
执行耗时: 5000ms

====== Step执行详情 ======

Step名称: cleanupStep
  状态: COMPLETED
  读取数: 0
  写入数: 0
  提交数: 1
  跳过数: 0
  退出状态: COMPLETED

Step名称: readProcessWriteStep
  状态: COMPLETED
  读取数: 100
  写入数: 100
  提交数: 10
  跳过数: 0
  退出状态: COMPLETED

Step名称: checkStep
  状态: COMPLETED
  读取数: 0
  写入数: 0
  提交数: 1
  跳过数: 0
  退出状态: COMPLETED

Step名称: switchStep
  状态: COMPLETED
  读取数: 0
  写入数: 0
  提交数: 1
  跳过数: 0
  退出状态: COMPLETED

✅ 测试通过
```

---

## ⚠️ 常见问题

### 问题1: 测试无法找到JobLauncherTestUtils

**原因**: 缺少Spring Batch Test依赖

**解决方案**: 确认`pom.xml`包含以下依赖：
```xml
<dependency>
    <groupId>org.springframework.batch</groupId>
    <artifactId>spring-batch-test</artifactId>
    <scope>test</scope>
</dependency>
```

### 问题2: H2数据库初始化失败

**原因**: SQL脚本语法问题

**解决方案**: 
1. 检查`init-h2.sql`语法是否正确
2. 确认使用`AUTO_INCREMENT`而不是`IDENTITY`
3. 查看测试日志中的详细错误信息

### 问题3: Job执行失败但不清楚原因

**原因**: 缺少依赖的Bean或配置

**解决方案**:
1. 检查是否有Mock对象未配置
2. 查看测试配置文件`application-test.yml`
3. 在测试类上添加必要的`@Import`注解

### 问题4: checkStep总是失败

**原因**: Elasticsearch或其他外部服务未连接

**解决方案**: 这是正常的，测试环境通常不会连接真实的ES。可以：
1. Mock IndexCheckService
2. 或者接受checkStep失败（在测试中不强制验证其成功）

### 问题5: 测试数据没有正确清理

**原因**: 测试隔离问题

**解决方案**:
```java
@AfterEach
void tearDown() {
    cleanupTestData();
}
```

---

## 🛠️ 测试配置

### application-test.yml 关键配置

```yaml
spring:
  batch:
    job:
      enabled: false  # ⚠️ 重要：测试时必须禁用自动启动

index:
  parallelism:
    chunkSize: 10  # 测试时使用小chunk提高速度
    threads: 2     # 测试时使用较少线程
  
  indexTarget:
    type: file     # 测试时输出到文件而不是ES
    path: ./target/test-output
```

### 测试注解说明

```java
@SpringBootTest                     // Spring Boot测试
@SpringBatchTest                    // Spring Batch测试支持
@ActiveProfiles("h2")               // 使用H2配置
@TestPropertySource(properties={})  // 覆盖特定属性
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)  // 按顺序执行
@Order(1)                           // 指定执行顺序
@DisplayName("测试描述")            // 测试方法显示名称
```

---

## 📈 测试覆盖率

运行带覆盖率的测试：

```bash
# 使用JaCoCo生成覆盖率报告
mvn clean test jacoco:report

# 查看报告
open target/site/jacoco/index.html
```

**预期覆盖率**:
- FullIndexJobConfig: > 80%
- IncrementalIndexJobConfig: > 80%
- 相关Reader/Writer/Processor: > 70%

---

## 🔧 扩展测试

### 添加自定义测试

```java
@Test
@Order(11)
@DisplayName("11. 自定义测试场景")
void testCustomScenario() throws Exception {
    // 1. 准备特定测试数据
    prepareCustomData();
    
    // 2. 执行Job
    JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);
    
    // 3. 验证结果
    assertCustomResult(jobExecution);
}
```

### Mock外部依赖

```java
@MockBean
private IndexCheckService indexCheckService;

@BeforeEach
void setUp() {
    // Mock外部服务
    when(indexCheckService.checkIndexIntegrity()).thenReturn(true);
}
```

---

## 📚 相关文档

- [Spring Batch Testing](https://docs.spring.io/spring-batch/docs/current/reference/html/testing.html)
- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [Spring Boot Testing](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing)

---

## ✅ 测试检查清单

运行测试前确认：

- [ ] H2数据库配置正确
- [ ] init-h2.sql语法正确（使用AUTO_INCREMENT）
- [ ] Spring Batch Test依赖已添加
- [ ] 测试配置文件application-test.yml存在
- [ ] Job自动启动已禁用（spring.batch.job.enabled=false）
- [ ] 测试输出目录存在（./target/test-output）
- [ ] 所有必需的Bean都能正确注入

---

**最后更新**: 2025-10-14  
**维护者**: Development Team  
**测试框架版本**: JUnit 5.x + Spring Batch Test 5.x

