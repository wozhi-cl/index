package com.company.index.common.service;

import com.company.index.config.FieldMappingConfig;
import com.company.index.config.FieldMappingConfig.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 字段映射服务
 * 从配置文件加载字段映射
 */
@Service
public class FieldMappingService {

    @Autowired
    private FieldMappingConfig fieldMappingConfig;

    @PostConstruct
    public void init() {
        System.out.println("========================================");
        System.out.println("初始化字段映射配置...");
        System.out.println("FieldMappingConfig 实例: " + fieldMappingConfig);
        System.out.println("SourceMappings: " + fieldMappingConfig.getSourceMappings());
        System.out.println("IndexMappings: " + fieldMappingConfig.getIndexMappings());
        
        int sourceTableCount = fieldMappingConfig.getSourceMappings() != null ? 
                               fieldMappingConfig.getSourceMappings().size() : 0;
        int indexCount = fieldMappingConfig.getIndexMappings() != null ? 
                         fieldMappingConfig.getIndexMappings().size() : 0;
        
        System.out.println("加载了 " + sourceTableCount + " 个源表配置");
        System.out.println("加载了 " + indexCount + " 个索引配置");
        
        if (sourceTableCount == 0) {
            System.err.println("⚠️ 警告：未加载任何源表配置！");
            System.err.println("⚠️ 请检查：");
            System.err.println("   1. field-mapping.yml 文件是否存在于 classpath");
            System.err.println("   2. application.yml 中是否正确导入了 field-mapping.yml");
            System.err.println("   3. YAML 格式是否正确（注意缩进）");
        }
        
        // 打印详细信息
        if (fieldMappingConfig.getSourceMappings() != null) {
            fieldMappingConfig.getSourceMappings().forEach((tableName, mapping) -> {
                int fieldCount = mapping.getFields() != null ? mapping.getFields().size() : 0;
                int joinCount = mapping.getJoins() != null ? mapping.getJoins().size() : 0;
                System.out.println("  - 源表 " + tableName + ": " + fieldCount + " 个字段, " + joinCount + " 个关联");
            });
        }
        
        if (fieldMappingConfig.getIndexMappings() != null) {
            fieldMappingConfig.getIndexMappings().forEach((indexName, mapping) -> {
                int fieldCount = mapping.getFields() != null ? mapping.getFields().size() : 0;
                System.out.println("  - 索引 " + indexName + ": " + fieldCount + " 个字段");
            });
        }
        
        System.out.println("字段映射配置初始化完成");
        System.out.println("========================================");
    }

    /**
     * 获取源表的所有字段配置（包括主表和关联表）
     */
    public List<FieldConfig> getSourceFields(String tableName) {
        SourceTableMapping mapping = fieldMappingConfig.getSourceMappings().get(tableName);
        if (mapping == null) {
            System.err.println("未找到表 " + tableName + " 的字段映射配置");
            return new ArrayList<>();
        }

        List<FieldConfig> allFields = new ArrayList<>();
        
        // 添加主表字段
        if (mapping.getFields() != null) {
            allFields.addAll(mapping.getFields());
        }
        
        // 添加关联表字段
        if (mapping.getJoins() != null) {
            for (JoinConfig join : mapping.getJoins()) {
                if (join.getFields() != null) {
                    allFields.addAll(join.getFields());
                }
            }
        }
        
        // 按 order 排序
        allFields.sort(Comparator.comparing(FieldConfig::getOrder));
        
        return allFields;
    }

    /**
     * 获取源表的主表字段
     */
    public List<FieldConfig> getMainFields(String tableName) {
        SourceTableMapping mapping = fieldMappingConfig.getSourceMappings().get(tableName);
        if (mapping == null || mapping.getFields() == null) {
            return new ArrayList<>();
        }
        
        return mapping.getFields().stream()
                .sorted(Comparator.comparing(FieldConfig::getOrder))
                .collect(Collectors.toList());
    }

    /**
     * 获取源表的关联配置
     */
    public List<JoinConfig> getJoinConfigs(String tableName) {
        SourceTableMapping mapping = fieldMappingConfig.getSourceMappings().get(tableName);
        if (mapping == null || mapping.getJoins() == null) {
            return new ArrayList<>();
        }
        
        return mapping.getJoins();
    }

    /**
     * 获取主键字段
     */
    public FieldConfig getKeyField(String tableName) {
        List<FieldConfig> fields = getMainFields(tableName);
        return fields.stream()
                .filter(f -> f.getIsKey() != null && f.getIsKey())
                .findFirst()
                .orElse(null);
    }

    /**
     * 获取时间戳字段（用于增量索引）
     * 默认查找名为 updated_at 或 created_at 的字段
     */
    public FieldConfig getTimeColumn(String tableName) {
        List<FieldConfig> fields = getMainFields(tableName);
        // 优先查找 updated_at
        return fields.stream()
                .filter(f -> "updated_at".equalsIgnoreCase(f.getName()) || 
                             "created_at".equalsIgnoreCase(f.getName()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 获取索引字段映射
     */
    public List<IndexFieldConfig> getIndexFields(String indexName) {
        IndexMapping mapping = fieldMappingConfig.getIndexMappings().get(indexName);
        if (mapping == null) {
            System.err.println("未找到索引 " + indexName + " 的字段映射配置");
            return new ArrayList<>();
        }
        
        if (mapping.getFields() == null) {
            return new ArrayList<>();
        }
        
        return mapping.getFields().stream()
                .sorted(Comparator.comparing(IndexFieldConfig::getOrder))
                .collect(Collectors.toList());
    }

    /**
     * 获取索引的源表名
     */
    public String getSourceTableForIndex(String indexName) {
        IndexMapping mapping = fieldMappingConfig.getIndexMappings().get(indexName);
        return mapping != null ? mapping.getSourceTable() : null;
    }

    /**
     * 检查表是否有配置
     */
    public boolean hasTableMapping(String tableName) {
        return fieldMappingConfig.getSourceMappings().containsKey(tableName);
    }

    /**
     * 检查索引是否有配置
     */
    public boolean hasIndexMapping(String indexName) {
        return fieldMappingConfig.getIndexMappings().containsKey(indexName);
    }

    /**
     * 获取所有配置的表名
     */
    public List<String> getAllTableNames() {
        return new ArrayList<>(fieldMappingConfig.getSourceMappings().keySet());
    }

    /**
     * 获取所有配置的索引名
     */
    public List<String> getAllIndexNames() {
        return new ArrayList<>(fieldMappingConfig.getIndexMappings().keySet());
    }
}
