package com.company.index.common.util;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

/**
 * SQL 构建器 - 智能生成 SQL 语句
 * 支持：
 * 1. 自动生成 SELECT 语句
 * 2. 支持 JOIN 查询
 * 3. 字段别名自动生成
 * 4. 支持 WHERE、ORDER BY 等子句
 */
public class SqlBuilder {

    private String mainTable;
    private String mainTableAlias;
    private List<String> selectFields = new ArrayList<>();
    private List<JoinClause> joins = new ArrayList<>();
    private List<String> whereClauses = new ArrayList<>();
    private List<String> orderByClauses = new ArrayList<>();
    private Integer limit;
    private boolean distinct = false;

    public SqlBuilder() {
    }

    /**
     * 设置主表
     */
    public SqlBuilder from(String tableName) {
        this.mainTable = tableName;
        return this;
    }

    /**
     * 设置主表及别名
     */
    public SqlBuilder from(String tableName, String alias) {
        this.mainTable = tableName;
        this.mainTableAlias = alias;
        return this;
    }

    /**
     * 添加 SELECT 字段
     */
    public SqlBuilder select(String... fields) {
        selectFields.addAll(Arrays.asList(fields));
        return this;
    }

    /**
     * 从表中选择所有字段（带前缀）
     */
    public SqlBuilder selectAll(String tableAlias) {
        selectFields.add(tableAlias + ".*");
        return this;
    }

    /**
     * 从表中选择字段并添加别名
     * 例如: selectWithAlias("t1", "id", "user_id") -> t1.id AS user_id
     */
    public SqlBuilder selectWithAlias(String tableAlias, String fieldName, String alias) {
        selectFields.add(tableAlias + "." + fieldName + " AS " + alias);
        return this;
    }

    /**
     * 批量选择字段（自动添加表别名）
     */
    public SqlBuilder selectFields(String tableAlias, String... fields) {
        for (String field : fields) {
            selectFields.add(tableAlias + "." + field);
        }
        return this;
    }

    /**
     * 批量选择字段并自动生成别名（避免字段名冲突）
     * 例如: t1.name -> t1_name
     */
    public SqlBuilder selectFieldsWithPrefix(String tableAlias, String... fields) {
        for (String field : fields) {
            String alias = tableAlias + "_" + field;
            selectFields.add(tableAlias + "." + field + " AS " + alias);
        }
        return this;
    }

    /**
     * 自动从 Java 类生成 SELECT 字段
     * 根据类的字段自动生成数据库字段名（驼峰转下划线）
     */
    public SqlBuilder selectFromClass(Class<?> clazz, String tableAlias) {
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            String dbFieldName = camelCaseToUnderscore(field.getName());
            selectFields.add(tableAlias + "." + dbFieldName);
        }
        return this;
    }

    /**
     * LEFT JOIN
     */
    public SqlBuilder leftJoin(String tableName, String alias, String onCondition) {
        joins.add(new JoinClause("LEFT JOIN", tableName, alias, onCondition));
        return this;
    }

    /**
     * INNER JOIN
     */
    public SqlBuilder innerJoin(String tableName, String alias, String onCondition) {
        joins.add(new JoinClause("INNER JOIN", tableName, alias, onCondition));
        return this;
    }

    /**
     * RIGHT JOIN
     */
    public SqlBuilder rightJoin(String tableName, String alias, String onCondition) {
        joins.add(new JoinClause("RIGHT JOIN", tableName, alias, onCondition));
        return this;
    }

    /**
     * WHERE 子句
     */
    public SqlBuilder where(String condition) {
        whereClauses.add(condition);
        return this;
    }

    /**
     * AND 条件
     */
    public SqlBuilder and(String condition) {
        if (!whereClauses.isEmpty()) {
            whereClauses.add("AND " + condition);
        } else {
            whereClauses.add(condition);
        }
        return this;
    }

    /**
     * ORDER BY
     */
    public SqlBuilder orderBy(String... fields) {
        orderByClauses.addAll(Arrays.asList(fields));
        return this;
    }

    /**
     * LIMIT
     */
    public SqlBuilder limit(int limit) {
        this.limit = limit;
        return this;
    }

    /**
     * DISTINCT
     */
    public SqlBuilder distinct() {
        this.distinct = true;
        return this;
    }

    /**
     * 构建 SQL
     */
    public String build() {
        StringBuilder sql = new StringBuilder();

        // SELECT
        sql.append("SELECT ");
        if (distinct) {
            sql.append("DISTINCT ");
        }

        if (selectFields.isEmpty()) {
            sql.append("*");
        } else {
            sql.append(String.join(", ", selectFields));
        }

        // FROM
        sql.append("\nFROM ").append(mainTable);
        if (mainTableAlias != null) {
            sql.append(" ").append(mainTableAlias);
        }

        // JOINs
        for (JoinClause join : joins) {
            sql.append("\n").append(join.toString());
        }

        // WHERE
        if (!whereClauses.isEmpty()) {
            sql.append("\nWHERE ").append(String.join(" ", whereClauses));
        }

        // ORDER BY
        if (!orderByClauses.isEmpty()) {
            sql.append("\nORDER BY ").append(String.join(", ", orderByClauses));
        }

        // LIMIT
        if (limit != null) {
            sql.append("\nLIMIT ").append(limit);
        }

        return sql.toString();
    }

    /**
     * 驼峰转下划线
     */
    private String camelCaseToUnderscore(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        StringBuilder result = new StringBuilder();
        result.append(Character.toLowerCase(input.charAt(0)));

        for (int i = 1; i < input.length(); i++) {
            char c = input.charAt(i);
            if (Character.isUpperCase(c)) {
                result.append('_').append(Character.toLowerCase(c));
            } else {
                result.append(c);
            }
        }

        return result.toString();
    }

    /**
     * JOIN 子句内部类
     */
    private static class JoinClause {
        private final String joinType;
        private final String tableName;
        private final String alias;
        private final String onCondition;

        public JoinClause(String joinType, String tableName, String alias, String onCondition) {
            this.joinType = joinType;
            this.tableName = tableName;
            this.alias = alias;
            this.onCondition = onCondition;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(joinType).append(" ").append(tableName);
            if (alias != null) {
                sb.append(" ").append(alias);
            }
            sb.append(" ON ").append(onCondition);
            return sb.toString();
        }
    }

    /**
     * 创建一个新的 SqlBuilder 实例
     */
    public static SqlBuilder create() {
        return new SqlBuilder();
    }

    /**
     * 快速构建简单的 SELECT 语句
     */
    public static String simpleSelect(String tableName, String whereClause, String orderBy) {
        SqlBuilder builder = new SqlBuilder()
                .from(tableName)
                .select("*");

        if (whereClause != null && !whereClause.isEmpty()) {
            builder.where(whereClause);
        }

        if (orderBy != null && !orderBy.isEmpty()) {
            builder.orderBy(orderBy);
        }

        return builder.build();
    }

    /**
     * 构建两表 JOIN 查询
     * @param table1 主表
     * @param alias1 主表别名
     * @param table2 关联表
     * @param alias2 关联表别名
     * @param joinCondition JOIN 条件
     * @param fields1 从主表选择的字段
     * @param fields2 从关联表选择的字段
     */
    public static String twoTableJoin(
            String table1, String alias1,
            String table2, String alias2,
            String joinCondition,
            String[] fields1,
            String[] fields2) {

        SqlBuilder builder = new SqlBuilder()
                .from(table1, alias1)
                .leftJoin(table2, alias2, joinCondition);

        // 添加表1的字段
        if (fields1 != null && fields1.length > 0) {
            builder.selectFields(alias1, fields1);
        }

        // 添加表2的字段（使用前缀避免冲突）
        if (fields2 != null && fields2.length > 0) {
            builder.selectFieldsWithPrefix(alias2, fields2);
        }

        return builder.build();
    }
}

