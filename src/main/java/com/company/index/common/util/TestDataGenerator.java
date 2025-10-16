package com.company.index.common.util;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Supplier;

/**
 * 测试数据生成器 - 智能生成测试数据
 * 支持：
 * 1. 根据字段类型自动生成随机数据
 * 2. 自定义字段值生成器
 * 3. 批量生成数据
 * 4. 生成 SQL INSERT 语句
 */
public class TestDataGenerator {

    private static final Random RANDOM = new Random();
    
    // 示例名称列表
    private static final List<String> FIRST_NAMES = Arrays.asList(
        "张", "王", "李", "赵", "刘", "陈", "杨", "黄", "周", "吴"
    );
    
    private static final List<String> LAST_NAMES = Arrays.asList(
        "明", "华", "强", "伟", "芳", "娜", "静", "丽", "勇", "军"
    );

    private static final List<String> STATUSES = Arrays.asList(
        "ACTIVE", "INACTIVE", "PENDING", "COMPLETED", "CANCELLED"
    );

    private final Class<?> targetClass;
    private final Map<String, Supplier<Object>> customGenerators;
    private final Set<String> excludeFields;

    private TestDataGenerator(Builder builder) {
        this.targetClass = builder.targetClass;
        this.customGenerators = builder.customGenerators;
        this.excludeFields = builder.excludeFields;
    }

    /**
     * 生成单个测试对象
     */
    public <T> T generateOne() {
        try {
            @SuppressWarnings("unchecked")
            T instance = (T) targetClass.getDeclaredConstructor().newInstance();
            
            Field[] fields = targetClass.getDeclaredFields();
            for (Field field : fields) {
                if (excludeFields.contains(field.getName())) {
                    continue;
                }

                field.setAccessible(true);
                Object value = generateFieldValue(field);
                field.set(instance, value);
            }

            return instance;
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate test data for " + targetClass.getName(), e);
        }
    }

    /**
     * 生成多个测试对象
     */
    public <T> List<T> generateList(int count) {
        List<T> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            list.add(generateOne());
        }
        return list;
    }

    /**
     * 生成字段值
     */
    private Object generateFieldValue(Field field) {
        String fieldName = field.getName();
        
        // 1. 检查是否有自定义生成器
        if (customGenerators.containsKey(fieldName)) {
            return customGenerators.get(fieldName).get();
        }

        // 2. 根据字段名生成特定值
        Object namedValue = generateByFieldName(fieldName, field.getType());
        if (namedValue != null) {
            return namedValue;
        }

        // 3. 根据类型生成默认值
        return generateByType(field.getType());
    }

    /**
     * 根据字段名生成值
     */
    private Object generateByFieldName(String fieldName, Class<?> type) {
        String lowerName = fieldName.toLowerCase();

        // ID 字段
        if (lowerName.equals("id") && (type == Long.class || type == long.class)) {
            return (long) RANDOM.nextInt(1000000);
        }

        // 名称字段
        if (lowerName.contains("name")) {
            return generateRandomName();
        }

        // 邮箱字段
        if (lowerName.contains("email")) {
            return generateRandomEmail();
        }

        // 电话字段
        if (lowerName.contains("phone") || lowerName.contains("mobile")) {
            return generateRandomPhone();
        }

        // 地址字段
        if (lowerName.contains("address")) {
            return generateRandomAddress();
        }

        // 状态字段
        if (lowerName.contains("status")) {
            return randomElement(STATUSES);
        }

        // 描述字段
        if (lowerName.contains("description") || lowerName.contains("desc") || lowerName.contains("remark")) {
            return "测试描述信息 " + RANDOM.nextInt(1000);
        }

        // 时间字段
        if (lowerName.contains("time") || lowerName.contains("date") || lowerName.contains("at")) {
            if (type == LocalDateTime.class) {
                return LocalDateTime.now().minusDays(RANDOM.nextInt(30));
            }
        }

        // 版本字段
        if (lowerName.contains("version")) {
            return (long) RANDOM.nextInt(100);
        }

        return null;
    }

    /**
     * 根据类型生成值
     */
    private Object generateByType(Class<?> type) {
        if (type == String.class) {
            return "test_" + UUID.randomUUID().toString().substring(0, 8);
        }

        if (type == Integer.class || type == int.class) {
            return RANDOM.nextInt(1000);
        }

        if (type == Long.class || type == long.class) {
            return (long) RANDOM.nextInt(1000);
        }

        if (type == Double.class || type == double.class) {
            return RANDOM.nextDouble() * 1000;
        }

        if (type == Float.class || type == float.class) {
            return RANDOM.nextFloat() * 1000;
        }

        if (type == Boolean.class || type == boolean.class) {
            return RANDOM.nextBoolean();
        }

        if (type == LocalDateTime.class) {
            return LocalDateTime.now().minusDays(RANDOM.nextInt(30));
        }

        if (type == Date.class) {
            return new Date();
        }

        if (type.isEnum()) {
            Object[] values = type.getEnumConstants();
            return values[RANDOM.nextInt(values.length)];
        }

        // Map 类型
        if (Map.class.isAssignableFrom(type)) {
            Map<String, Object> map = new HashMap<>();
            map.put("key1", "value1");
            map.put("key2", RANDOM.nextInt(100));
            return map;
        }

        return null;
    }

    /**
     * 生成随机姓名
     */
    private String generateRandomName() {
        return randomElement(FIRST_NAMES) + randomElement(LAST_NAMES);
    }

    /**
     * 生成随机邮箱
     */
    private String generateRandomEmail() {
        return "user" + RANDOM.nextInt(10000) + "@example.com";
    }

    /**
     * 生成随机电话
     */
    private String generateRandomPhone() {
        return String.format("138%08d", RANDOM.nextInt(100000000));
    }

    /**
     * 生成随机地址
     */
    private String generateRandomAddress() {
        String[] cities = {"北京", "上海", "广州", "深圳", "杭州"};
        String[] districts = {"朝阳区", "海淀区", "浦东新区", "天河区", "南山区"};
        return randomElement(Arrays.asList(cities)) + randomElement(Arrays.asList(districts)) + 
               "某街道" + RANDOM.nextInt(100) + "号";
    }

    /**
     * 从列表中随机选择一个元素
     */
    private <T> T randomElement(List<T> list) {
        return list.get(RANDOM.nextInt(list.size()));
    }

    /**
     * 生成 INSERT SQL 语句
     */
    public String generateInsertSql(String tableName) {
        try {
            Object instance = generateOne();
            return generateInsertSql(tableName, instance);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate INSERT SQL", e);
        }
    }

    /**
     * 根据对象生成 INSERT SQL 语句
     */
    public String generateInsertSql(String tableName, Object instance) {
        try {
            StringBuilder sql = new StringBuilder("INSERT INTO ");
            sql.append(tableName).append(" (");

            List<String> columns = new ArrayList<>();
            List<String> values = new ArrayList<>();

            Field[] fields = instance.getClass().getDeclaredFields();
            for (Field field : fields) {
                if (excludeFields.contains(field.getName())) {
                    continue;
                }

                field.setAccessible(true);
                Object value = field.get(instance);
                if (value == null) {
                    continue;
                }

                // 字段名（驼峰转下划线）
                String columnName = camelCaseToUnderscore(field.getName());
                columns.add(columnName);

                // 字段值
                values.add(formatSqlValue(value));
            }

            sql.append(String.join(", ", columns));
            sql.append(") VALUES (");
            sql.append(String.join(", ", values));
            sql.append(")");

            return sql.toString();

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate INSERT SQL", e);
        }
    }

    /**
     * 批量生成 INSERT SQL 语句
     */
    public List<String> generateInsertSqlList(String tableName, int count) {
        List<String> sqlList = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            sqlList.add(generateInsertSql(tableName));
        }
        return sqlList;
    }

    /**
     * 格式化 SQL 值
     */
    private String formatSqlValue(Object value) {
        if (value == null) {
            return "NULL";
        }

        if (value instanceof String) {
            return "'" + value.toString().replace("'", "''") + "'";
        }

        if (value instanceof LocalDateTime) {
            return "'" + value.toString().replace('T', ' ') + "'";
        }

        if (value instanceof Date) {
            return "'" + new java.sql.Timestamp(((Date) value).getTime()).toString() + "'";
        }

        if (value instanceof Boolean) {
            return ((Boolean) value) ? "1" : "0";
        }

        return value.toString();
    }

    /**
     * 驼峰转下划线
     */
    private String camelCaseToUnderscore(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        StringBuilder result = new StringBuilder();
        result.append(Character.toUpperCase(input.charAt(0)));

        for (int i = 1; i < input.length(); i++) {
            char c = input.charAt(i);
            if (Character.isUpperCase(c)) {
                result.append('_').append(c);
            } else {
                result.append(Character.toUpperCase(c));
            }
        }

        return result.toString();
    }

    /**
     * Builder 模式
     */
    public static class Builder {
        private Class<?> targetClass;
        private Map<String, Supplier<Object>> customGenerators = new HashMap<>();
        private Set<String> excludeFields = new HashSet<>();

        public Builder(Class<?> targetClass) {
            this.targetClass = targetClass;
        }

        /**
         * 添加自定义字段生成器
         */
        public Builder withCustomGenerator(String fieldName, Supplier<Object> generator) {
            this.customGenerators.put(fieldName, generator);
            return this;
        }

        /**
         * 排除指定字段（不生成）
         */
        public Builder excludeField(String fieldName) {
            this.excludeFields.add(fieldName);
            return this;
        }

        /**
         * 排除多个字段
         */
        public Builder excludeFields(String... fieldNames) {
            this.excludeFields.addAll(Arrays.asList(fieldNames));
            return this;
        }

        public TestDataGenerator build() {
            return new TestDataGenerator(this);
        }
    }

    /**
     * 创建 Builder
     */
    public static Builder forClass(Class<?> clazz) {
        return new Builder(clazz);
    }
}

