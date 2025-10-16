package com.company.index.common.util;

import com.company.index.config.FieldMappingConfig.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 动态 SQL 构建器
 * 根据字段映射配置动态生成 SQL
 */
public class DynamicSqlBuilder {

    /**
     * 构建 SELECT 语句
     * 
     * @param tableName 主表名
     * @param mainFields 主表字段列表
     * @param joins 关联配置列表
     * @return SELECT 语句
     */
    public static String buildSelectSql(String tableName, List<FieldConfig> mainFields, List<JoinConfig> joins) {
        if (mainFields == null || mainFields.isEmpty()) {
            return "SELECT * FROM " + tableName;
        }

        StringBuilder sql = new StringBuilder("SELECT ");
        
        // 添加主表字段
        for (int i = 0; i < mainFields.size(); i++) {
            FieldConfig field = mainFields.get(i);
            if (i > 0) sql.append(", ");
            
            appendFieldSelect(sql, tableName, field);
        }
        
        // 添加关联表字段
        if (joins != null && !joins.isEmpty()) {
            for (JoinConfig join : joins) {
                if (join.getFields() != null) {
                    for (FieldConfig field : join.getFields()) {
                        sql.append(", ");
                        appendFieldSelect(sql, join.getTable(), field);
                    }
                }
            }
        }
        
        sql.append(" FROM ").append(tableName);
        
        // 添加 JOIN 语句
        if (joins != null && !joins.isEmpty()) {
            for (JoinConfig join : joins) {
                String joinType = join.getType() != null ? join.getType() : "LEFT";
                sql.append(" ").append(joinType).append(" JOIN ").append(join.getTable());
                sql.append(" ON ").append(tableName).append(".").append(join.getOnField());
                sql.append(" = ").append(join.getTable()).append(".id");
            }
        }
        
        return sql.toString();
    }

    /**
     * 添加字段选择语句
     */
    private static void appendFieldSelect(StringBuilder sql, String tableName, FieldConfig field) {
        // 如果有转换表达式，使用表达式
        if (field.getTransform() != null && !field.getTransform().trim().isEmpty()) {
            sql.append(field.getTransform());
        } else {
            sql.append(tableName).append(".").append(field.getName());
        }
        
        // 添加别名
        if (field.getAlias() != null && !field.getAlias().trim().isEmpty()) {
            sql.append(" AS ").append(field.getAlias());
        }
    }

    /**
     * 构建全量查询 SQL
     */
    public static String buildFullQuerySql(String tableName, List<FieldConfig> mainFields, 
                                          List<JoinConfig> joins, FieldConfig keyField) {
        String selectSql = buildSelectSql(tableName, mainFields, joins);
        
        // 添加排序
        if (keyField != null) {
            selectSql += " ORDER BY " + tableName + "." + keyField.getName();
        }
        
        return selectSql;
    }

    /**
     * 构建增量查询 SQL
     */
    public static String buildDeltaQuerySql(String tableName, List<FieldConfig> mainFields, 
                                           List<JoinConfig> joins, String timeColumn, 
                                           FieldConfig keyField) {
        String selectSql = buildSelectSql(tableName, mainFields, joins);
        
        // 添加时间范围条件
        selectSql += " WHERE " + tableName + "." + timeColumn + " BETWEEN ? AND ?";
        
        // 添加排序
        if (keyField != null) {
            selectSql += " ORDER BY " + tableName + "." + keyField.getName();
        }
        
        return selectSql;
    }

    /**
     * 构建 COUNT 查询 SQL
     */
    public static String buildCountSql(String tableName, List<JoinConfig> joins) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM " + tableName);
        
        // 如果有关联表，需要添加 JOIN
        if (joins != null && !joins.isEmpty()) {
            for (JoinConfig join : joins) {
                String joinType = join.getType() != null ? join.getType() : "LEFT";
                sql.append(" ").append(joinType).append(" JOIN ").append(join.getTable());
                sql.append(" ON ").append(tableName).append(".").append(join.getOnField());
                sql.append(" = ").append(join.getTable()).append(".id");
            }
        }
        
        return sql.toString();
    }

    /**
     * 获取所有字段名称列表（用于 RowMapper）
     */
    public static List<String> getFieldNames(List<FieldConfig> fields) {
        return fields.stream()
                .map(FieldConfig::getEffectiveName)
                .collect(Collectors.toList());
    }

    /**
     * 构建分页查询 SQL
     */
    public static String buildPagingQuerySql(String tableName, List<FieldConfig> mainFields, 
                                            List<JoinConfig> joins, FieldConfig keyField, 
                                            int pageSize) {
        String baseSql = buildSelectSql(tableName, mainFields, joins);
        
        // 添加 WHERE 条件（用于分页）
        if (keyField != null) {
            baseSql += " WHERE " + tableName + "." + keyField.getName() + " > ?";
            baseSql += " ORDER BY " + tableName + "." + keyField.getName();
            baseSql += " LIMIT " + pageSize;
        }
        
        return baseSql;
    }
}
