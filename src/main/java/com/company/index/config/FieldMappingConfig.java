package com.company.index.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 字段映射配置
 */
@Configuration
@ConfigurationProperties(prefix = "field-mappings")
public class FieldMappingConfig {

    private Map<String, SourceTableMapping> sourceMappings = new HashMap<>();
    private Map<String, IndexMapping> indexMappings = new HashMap<>();

    public Map<String, SourceTableMapping> getSourceMappings() {
        return sourceMappings;
    }

    public void setSourceMappings(Map<String, SourceTableMapping> sourceMappings) {
        this.sourceMappings = sourceMappings;
    }

    public Map<String, IndexMapping> getIndexMappings() {
        return indexMappings;
    }

    public void setIndexMappings(Map<String, IndexMapping> indexMappings) {
        this.indexMappings = indexMappings;
    }

    /**
     * 源表映射配置
     */
    public static class SourceTableMapping {
        private List<FieldConfig> fields;
        private List<JoinConfig> joins;

        public List<FieldConfig> getFields() {
            return fields;
        }

        public void setFields(List<FieldConfig> fields) {
            this.fields = fields;
        }

        public List<JoinConfig> getJoins() {
            return joins;
        }

        public void setJoins(List<JoinConfig> joins) {
            this.joins = joins;
        }
    }

    /**
     * 字段配置
     */
    public static class FieldConfig {
        private String name;
        private String type;
        private String alias;
        private Boolean isKey = false;
        private Boolean required = false;
        private String defaultValue;
        private String transform;
        private Integer order = 0;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getAlias() {
            return alias;
        }

        public void setAlias(String alias) {
            this.alias = alias;
        }

        public Boolean getIsKey() {
            return isKey;
        }

        public void setIsKey(Boolean isKey) {
            this.isKey = isKey;
        }

        public Boolean getRequired() {
            return required;
        }

        public void setRequired(Boolean required) {
            this.required = required;
        }

        public String getDefaultValue() {
            return defaultValue;
        }

        public void setDefaultValue(String defaultValue) {
            this.defaultValue = defaultValue;
        }

        public String getTransform() {
            return transform;
        }

        public void setTransform(String transform) {
            this.transform = transform;
        }

        public Integer getOrder() {
            return order;
        }

        public void setOrder(Integer order) {
            this.order = order;
        }

        public String getEffectiveName() {
            return alias != null && !alias.trim().isEmpty() ? alias : name;
        }
    }

    /**
     * 关联配置
     */
    public static class JoinConfig {
        private String table;
        private String onField;
        private String type = "LEFT";
        private List<FieldConfig> fields;

        public String getTable() {
            return table;
        }

        public void setTable(String table) {
            this.table = table;
        }

        public String getOnField() {
            return onField;
        }

        public void setOnField(String onField) {
            this.onField = onField;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public List<FieldConfig> getFields() {
            return fields;
        }

        public void setFields(List<FieldConfig> fields) {
            this.fields = fields;
        }
    }

    /**
     * 索引映射配置
     */
    public static class IndexMapping {
        private String sourceTable;
        private List<IndexFieldConfig> fields;

        public String getSourceTable() {
            return sourceTable;
        }

        public void setSourceTable(String sourceTable) {
            this.sourceTable = sourceTable;
        }

        public List<IndexFieldConfig> getFields() {
            return fields;
        }

        public void setFields(List<IndexFieldConfig> fields) {
            this.fields = fields;
        }
    }

    /**
     * 索引字段配置
     */
    public static class IndexFieldConfig {
        private String name;
        private String type;
        private String sourceField;
        private Boolean searchable = true;
        private Boolean sortable = false;
        private Boolean filterable = true;
        private Boolean aggregatable = false;
        private String analyzer;
        private Double boost = 1.0;
        private String format;
        private Integer order = 0;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getSourceField() {
            return sourceField;
        }

        public void setSourceField(String sourceField) {
            this.sourceField = sourceField;
        }

        public Boolean getSearchable() {
            return searchable;
        }

        public void setSearchable(Boolean searchable) {
            this.searchable = searchable;
        }

        public Boolean getSortable() {
            return sortable;
        }

        public void setSortable(Boolean sortable) {
            this.sortable = sortable;
        }

        public Boolean getFilterable() {
            return filterable;
        }

        public void setFilterable(Boolean filterable) {
            this.filterable = filterable;
        }

        public Boolean getAggregatable() {
            return aggregatable;
        }

        public void setAggregatable(Boolean aggregatable) {
            this.aggregatable = aggregatable;
        }

        public String getAnalyzer() {
            return analyzer;
        }

        public void setAnalyzer(String analyzer) {
            this.analyzer = analyzer;
        }

        public Double getBoost() {
            return boost;
        }

        public void setBoost(Double boost) {
            this.boost = boost;
        }

        public String getFormat() {
            return format;
        }

        public void setFormat(String format) {
            this.format = format;
        }

        public Integer getOrder() {
            return order;
        }

        public void setOrder(Integer order) {
            this.order = order;
        }
    }
}

