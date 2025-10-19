package com.company.index.common;

import com.company.index.common.model.SourceRecord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 测试数据生成器
 * 生成 SourceRecord 测试数据
 */
@Component
public class TestDataGenerator {

    private final JdbcTemplate jdbcTemplate;
    private final Random random = new Random();
    private static int orderNoCounter = 1000; // 全局计数器确保唯一性

    public TestDataGenerator(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    /**
     * 创建测试表
     */
    public void createTable(String tableName) throws Exception {
        // 检查表名并创建对应的表结构
        if ("orders".equalsIgnoreCase(tableName) || "ORDERS".equalsIgnoreCase(tableName)) {
            createOrdersTable();
        } else if ("users".equalsIgnoreCase(tableName) || "USERS".equalsIgnoreCase(tableName)) {
            createUsersTable();
        } else {
            // 默认创建 orders 表
            createOrdersTable();
        }
    }

    /**
     * 插入测试数据
     */
    public void insertTestData(String tableName, int count) throws Exception {
        if ("orders".equalsIgnoreCase(tableName) || "ORDERS".equalsIgnoreCase(tableName)) {
            insertOrdersData(count);
        } else if ("users".equalsIgnoreCase(tableName) || "USERS".equalsIgnoreCase(tableName)) {
            insertUsersData(count);
        } else {
            // 默认插入 orders 数据
            insertOrdersData(count);
        }
    }

    /**
     * 创建 orders 表
     */
    private void createOrdersTable() throws Exception {
        String createOrdersSql = """
            CREATE TABLE IF NOT EXISTS orders (
                id BIGINT PRIMARY KEY AUTO_INCREMENT,
                order_no VARCHAR(50) NOT NULL UNIQUE,
                amount DECIMAL(10,2) NOT NULL,
                user_id BIGINT NOT NULL,
                status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
            )
            """;
        
        try {
            jdbcTemplate.execute(createOrdersSql);
            System.out.println("✓ Orders 表创建成功");
        } catch (Exception e) {
            // 尝试 Oracle 语法
            String createOrdersOracleSql = """
                CREATE TABLE ORDERS (
                    ID NUMBER PRIMARY KEY,
                    ORDER_NO VARCHAR2(50) NOT NULL UNIQUE,
                    AMOUNT NUMBER(10,2) NOT NULL,
                    USER_ID NUMBER NOT NULL,
                    STATUS VARCHAR2(20) DEFAULT 'PENDING',
                    CREATED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    UPDATED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """;
            try {
                jdbcTemplate.execute(createOrdersOracleSql);
                System.out.println("✓ ORDERS 表创建成功 (Oracle)");
            } catch (Exception e2) {
                System.err.println("创建 orders 表失败: " + e.getMessage());
                throw e;
            }
        }
    }

    /**
     * 创建 users 表
     */
    private void createUsersTable() throws Exception {
        String createUsersSql = """
            CREATE TABLE IF NOT EXISTS users (
                id BIGINT PRIMARY KEY AUTO_INCREMENT,
                name VARCHAR(100) NOT NULL,
                phone VARCHAR(20),
                email VARCHAR(100),
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
            )
            """;
        
        try {
            jdbcTemplate.execute(createUsersSql);
            System.out.println("✓ Users 表创建成功");
        } catch (Exception e) {
            // 尝试 Oracle 语法
            String createUsersOracleSql = """
                CREATE TABLE USERS (
                    ID NUMBER PRIMARY KEY,
                    NAME VARCHAR2(100) NOT NULL,
                    PHONE VARCHAR2(20),
                    EMAIL VARCHAR2(100),
                    CREATED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    UPDATED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """;
            try {
                jdbcTemplate.execute(createUsersOracleSql);
                System.out.println("✓ USERS 表创建成功 (Oracle)");
            } catch (Exception e2) {
                System.err.println("创建 users 表失败: " + e.getMessage());
                throw e;
            }
        }
    }

    /**
     * 插入 orders 测试数据
     */
    private void insertOrdersData(int count) throws Exception {
        // 先插入一些 users 数据
        insertUsersData(Math.max(10, count / 10));
        
        String insertSql = """
            INSERT INTO orders (order_no, amount, user_id, status, created_at, updated_at) 
            VALUES (?, ?, ?, ?, ?, ?)
            """;
        
        String insertOracleSql = """
            INSERT INTO ORDERS (ORDER_NO, AMOUNT, USER_ID, STATUS, CREATED_AT, UPDATED_AT) 
            VALUES (?, ?, ?, ?, ?, ?)
            """;
        
        List<Object[]> batchArgs = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            String orderNo = "ORD" + (orderNoCounter++);
            double amount = 10.0 + random.nextDouble() * 990.0; // 10-1000
            long userId = 1 + random.nextInt(10); // 1-10
            String status = getRandomStatus();
            LocalDateTime createdAt = LocalDateTime.now().minusDays(random.nextInt(30));
            LocalDateTime updatedAt = createdAt.plusMinutes(random.nextInt(60));
            
            batchArgs.add(new Object[]{orderNo, amount, userId, status, createdAt, updatedAt});
        }
        
        try {
            jdbcTemplate.batchUpdate(insertSql, batchArgs);
            System.out.println("✓ 插入 " + count + " 条 orders 测试数据成功");
        } catch (Exception e) {
            try {
                jdbcTemplate.batchUpdate(insertOracleSql, batchArgs);
                System.out.println("✓ 插入 " + count + " 条 ORDERS 测试数据成功 (Oracle)");
            } catch (Exception e2) {
                System.err.println("插入 orders 数据失败: " + e.getMessage());
                throw e;
            }
        }
    }

    /**
     * 插入 users 测试数据
     */
    private void insertUsersData(int count) throws Exception {
        // 先创建 users 表
        createUsersTable();
        
        String insertSql = """
            INSERT INTO users (name, phone, email, created_at, updated_at) 
            VALUES (?, ?, ?, ?, ?)
            """;
        
        String insertOracleSql = """
            INSERT INTO USERS (NAME, PHONE, EMAIL, CREATED_AT, UPDATED_AT) 
            VALUES (?, ?, ?, ?, ?)
            """;
        
        List<Object[]> batchArgs = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            String name = "User" + (i + 1);
            String phone = "138" + String.format("%08d", random.nextInt(100000000));
            String email = "user" + (i + 1) + "@example.com";
            LocalDateTime createdAt = LocalDateTime.now().minusDays(random.nextInt(30));
            LocalDateTime updatedAt = createdAt.plusMinutes(random.nextInt(60));
            
            batchArgs.add(new Object[]{name, phone, email, createdAt, updatedAt});
        }
        
        try {
            jdbcTemplate.batchUpdate(insertSql, batchArgs);
            System.out.println("✓ 插入 " + count + " 条 users 测试数据成功");
        } catch (Exception e) {
            try {
                jdbcTemplate.batchUpdate(insertOracleSql, batchArgs);
                System.out.println("✓ 插入 " + count + " 条 USERS 测试数据成功 (Oracle)");
            } catch (Exception e2) {
                System.err.println("插入 users 数据失败: " + e.getMessage());
                throw e;
            }
        }
    }

    /**
     * 生成随机状态
     */
    private String getRandomStatus() {
        String[] statuses = {"PENDING", "CONFIRMED", "SHIPPED", "DELIVERED", "CANCELLED"};
        return statuses[random.nextInt(statuses.length)];
    }

    /**
     * 生成测试记录
     */
    public List<SourceRecord> generateRecords(String tableName, int count) {
        List<SourceRecord> records = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            SourceRecord record = new SourceRecord();
            record.setId((long) (i + 1));
            record.setOrderNo("ORD" + (orderNoCounter++));
            record.setAmount(10.0 + random.nextDouble() * 990.0);
            record.setUserId(1L + random.nextInt(10));
            record.setStatus(getRandomStatus());
            record.setCreatedAt(LocalDateTime.now().minusDays(random.nextInt(30)));
            record.setUpdatedAt(record.getCreatedAt().plusMinutes(random.nextInt(60)));
            record.setUserName("User" + (i + 1));
            record.setUserPhone("138" + String.format("%08d", random.nextInt(100000000)));
            
            // 设置元数据
            record.setSource("RDB:" + tableName);
            record.setVersion(1L);
            record.setOperation(SourceRecord.OperationType.INSERT);
            record.setTimestamp(record.getCreatedAt());
            
            records.add(record);
        }
        
        return records;
    }
}