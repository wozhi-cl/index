package com.company.index.common.util;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.JdbcUtils;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.BiFunction;

/**
 * 通用 RowMapper - 自动映射数据库字段到 Java 对象
 * 支持：
 * 1. 自动字段映射（下划线转驼峰）
 * 2. 特殊字段处理器
 * 3. 字段名映射配置
 * 4. 类型自动转换
 */
public class GenericRowMapper<T> implements RowMapper<T> {

    private final Class<T> targetClass;
    private final Map<String, String> fieldMapping;  // 数据库字段名 -> Java字段名
    private final Map<String, BiFunction<ResultSet, String, Object>> specialHandlers;  // 特殊字段处理器
    private final boolean autoUnderscoreToCamelCase;  // 自动转换下划线为驼峰

    private GenericRowMapper(Builder<T> builder) {
        this.targetClass = builder.targetClass;
        this.fieldMapping = builder.fieldMapping;
        this.specialHandlers = builder.specialHandlers;
        this.autoUnderscoreToCamelCase = builder.autoUnderscoreToCamelCase;
    }

    @Override
    public T mapRow(ResultSet rs, int rowNum) throws SQLException {
        try {
            T instance = targetClass.getDeclaredConstructor().newInstance();
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            // 获取所有字段（包括父类）
            Map<String, Field> fieldMap = getAllFields(targetClass);

            // 遍历所有数据库列
            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnName(i);
                String columnLabel = metaData.getColumnLabel(i);
                
                // 优先使用列别名
                String dbFieldName = columnLabel != null ? columnLabel : columnName;
                
                // 检查是否有特殊处理器
                if (specialHandlers.containsKey(dbFieldName)) {
                    Object value = specialHandlers.get(dbFieldName).apply(rs, dbFieldName);
                    setFieldValue(instance, dbFieldName, value, fieldMap);
                    continue;
                }

                // 获取 Java 字段名
                String javaFieldName = getJavaFieldName(dbFieldName);
                
                // 获取字段值
                Object value = getColumnValue(rs, i, dbFieldName);
                
                // 设置字段值
                setFieldValue(instance, javaFieldName, value, fieldMap);
            }

            return instance;

        } catch (Exception e) {
            throw new SQLException("Failed to map row to " + targetClass.getName(), e);
        }
    }

    /**
     * 获取数据库列的值（自动处理类型转换）
     */
    private Object getColumnValue(ResultSet rs, int columnIndex, String columnName) throws SQLException {
        Object value = JdbcUtils.getResultSetValue(rs, columnIndex);
        
        if (value == null) {
            return null;
        }

        // 处理时间戳类型
        if (value instanceof java.sql.Timestamp) {
            return ((java.sql.Timestamp) value).toLocalDateTime();
        }
        
        // 处理日期类型
        if (value instanceof java.sql.Date) {
            return ((java.sql.Date) value).toLocalDate().atStartOfDay();
        }

        return value;
    }

    /**
     * 获取 Java 字段名（考虑映射和驼峰转换）
     */
    private String getJavaFieldName(String dbFieldName) {
        // 1. 检查显式映射
        if (fieldMapping.containsKey(dbFieldName)) {
            return fieldMapping.get(dbFieldName);
        }

        // 2. 自动转换下划线为驼峰
        if (autoUnderscoreToCamelCase) {
            return underscoreToCamelCase(dbFieldName);
        }

        // 3. 直接使用数据库字段名
        return dbFieldName;
    }

    /**
     * 下划线转驼峰
     */
    private String underscoreToCamelCase(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        StringBuilder result = new StringBuilder();
        boolean nextUpperCase = false;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '_') {
                nextUpperCase = true;
            } else {
                if (nextUpperCase) {
                    result.append(Character.toUpperCase(c));
                    nextUpperCase = false;
                } else {
                    result.append(Character.toLowerCase(c));
                }
            }
        }

        return result.toString();
    }

    /**
     * 设置字段值（使用反射）
     */
    private void setFieldValue(T instance, String fieldName, Object value, Map<String, Field> fieldMap) {
        try {
            Field field = fieldMap.get(fieldName.toLowerCase());
            if (field == null) {
                // 字段不存在，忽略（这是正常的，因为可能只选择部分字段）
                return;
            }

            field.setAccessible(true);
            
            // 类型转换
            Object convertedValue = convertValue(value, field.getType());
            field.set(instance, convertedValue);

        } catch (Exception e) {
            // 记录警告但不抛出异常，允许部分字段映射失败
            System.err.println("Warning: Failed to set field " + fieldName + " with value " + value + ": " + e.getMessage());
        }
    }

    /**
     * 类型转换
     */
    private Object convertValue(Object value, Class<?> targetType) {
        if (value == null) {
            return null;
        }

        // 如果类型已经匹配，直接返回
        if (targetType.isAssignableFrom(value.getClass())) {
            return value;
        }

        // String 转换
        if (targetType == String.class) {
            return value.toString();
        }

        // 数字类型转换
        if (value instanceof Number) {
            Number numValue = (Number) value;
            if (targetType == Integer.class || targetType == int.class) {
                return numValue.intValue();
            }
            if (targetType == Long.class || targetType == long.class) {
                return numValue.longValue();
            }
            if (targetType == Double.class || targetType == double.class) {
                return numValue.doubleValue();
            }
            if (targetType == Float.class || targetType == float.class) {
                return numValue.floatValue();
            }
        }

        // 默认返回原值
        return value;
    }

    /**
     * 获取类的所有字段（包括父类）
     */
    private Map<String, Field> getAllFields(Class<?> clazz) {
        Map<String, Field> fieldMap = new HashMap<>();
        Class<?> currentClass = clazz;

        while (currentClass != null && currentClass != Object.class) {
            Field[] fields = currentClass.getDeclaredFields();
            for (Field field : fields) {
                // 使用小写字段名作为 key，方便查找
                fieldMap.putIfAbsent(field.getName().toLowerCase(), field);
            }
            currentClass = currentClass.getSuperclass();
        }

        return fieldMap;
    }

    /**
     * Builder 模式
     */
    public static class Builder<T> {
        private final Class<T> targetClass;
        private Map<String, String> fieldMapping = new HashMap<>();
        private Map<String, BiFunction<ResultSet, String, Object>> specialHandlers = new HashMap<>();
        private boolean autoUnderscoreToCamelCase = true;

        public Builder(Class<T> targetClass) {
            this.targetClass = targetClass;
        }

        /**
         * 添加字段映射（数据库字段名 -> Java字段名）
         */
        public Builder<T> addFieldMapping(String dbFieldName, String javaFieldName) {
            this.fieldMapping.put(dbFieldName, javaFieldName);
            return this;
        }

        /**
         * 批量添加字段映射
         */
        public Builder<T> addFieldMappings(Map<String, String> mappings) {
            this.fieldMapping.putAll(mappings);
            return this;
        }

        /**
         * 添加特殊字段处理器
         */
        public Builder<T> addSpecialHandler(String fieldName, BiFunction<ResultSet, String, Object> handler) {
            this.specialHandlers.put(fieldName, handler);
            return this;
        }

        /**
         * 设置是否自动转换下划线为驼峰
         */
        public Builder<T> autoUnderscoreToCamelCase(boolean enable) {
            this.autoUnderscoreToCamelCase = enable;
            return this;
        }

        /**
         * 添加常用的日期时间处理器
         */
        public Builder<T> addDateTimeHandler(String fieldName) {
            return addSpecialHandler(fieldName, (rs, name) -> {
                try {
                    java.sql.Timestamp timestamp = rs.getTimestamp(name);
                    return timestamp != null ? timestamp.toLocalDateTime() : null;
                } catch (SQLException e) {
                    return null;
                }
            });
        }

        /**
         * 添加 CLOB 处理器
         */
        public Builder<T> addClobHandler(String fieldName) {
            return addSpecialHandler(fieldName, (rs, name) -> {
                try {
                    java.sql.Clob clob = rs.getClob(name);
                    if (clob == null) return null;
                    return clob.getSubString(1, (int) clob.length());
                } catch (SQLException e) {
                    return null;
                }
            });
        }

        public GenericRowMapper<T> build() {
            return new GenericRowMapper<>(this);
        }
    }
}

