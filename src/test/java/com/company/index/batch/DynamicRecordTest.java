package com.company.index.batch;

import com.company.index.common.model.DynamicRecord;
import com.company.index.common.service.FieldMappingService;
import com.company.index.common.service.RecordBuilderService;
import com.company.index.config.FieldMappingConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 动态记录测试
 */
@SpringBootTest
@ActiveProfiles("h2")
public class DynamicRecordTest {

    @Autowired
    private FieldMappingService fieldMappingService;

    @Autowired
    private RecordBuilderService recordBuilderService;

    @Test
    public void testDynamicRecordCreation() {
        // 测试创建源记录
        DynamicRecord sourceRecord = DynamicRecord.createSourceRecord("orders");
        sourceRecord.setField("order_id", 1L);
        sourceRecord.setField("order_no", "ORD001");
        sourceRecord.setField("amount", 199.99);
        sourceRecord.setField("user_name", "张三");
        sourceRecord.setTimestamp(LocalDateTime.now());

        assertNotNull(sourceRecord);
        assertEquals(DynamicRecord.RecordType.SOURCE, sourceRecord.getRecordType());
        assertEquals("orders", sourceRecord.getTableName());
        assertEquals(1L, sourceRecord.getField("order_id"));
        assertEquals("ORD001", sourceRecord.getField("order_no"));
        
        System.out.println("✅ 源记录创建成功: " + sourceRecord);
    }

    @Test
    public void testSourceToIndexConversion() {
        // 创建源记录
        DynamicRecord sourceRecord = DynamicRecord.createSourceRecord("orders");
        sourceRecord.setField("order_id", 1L);
        sourceRecord.setField("order_no", "ORD001");
        sourceRecord.setField("amount", 199.99);
        sourceRecord.setField("user_name", "张三");
        sourceRecord.setField("user_phone", "13800138001");
        sourceRecord.setField("order_time", LocalDateTime.now());

        // 转换为索引记录
        DynamicRecord indexRecord = recordBuilderService.convertToIndexRecord(sourceRecord, "order_index");

        assertNotNull(indexRecord);
        assertEquals(DynamicRecord.RecordType.INDEX, indexRecord.getRecordType());
        assertEquals("order_index", indexRecord.getTableName());
        
        System.out.println("✅ 索引记录转换成功: " + indexRecord);
        System.out.println("   源记录字段数: " + sourceRecord.getFields().size());
        System.out.println("   索引记录字段数: " + indexRecord.getFields().size());
    }

    @Test
    public void testFieldMappingLoaded() {
        // 测试字段映射是否正确加载
        List<FieldMappingConfig.FieldConfig> orderFields = 
            fieldMappingService.getMainFields("orders");
        
        assertNotNull(orderFields);
        assertFalse(orderFields.isEmpty());
        
        System.out.println("✅ 订单表字段映射加载成功:");
        orderFields.forEach(field -> {
            System.out.println("   - " + field.getName() + " (" + field.getType() + ") " +
                             (field.getIsKey() ? "[主键]" : ""));
        });

        // 测试关联配置
        List<FieldMappingConfig.JoinConfig> joins = 
            fieldMappingService.getJoinConfigs("orders");
        
        assertNotNull(joins);
        assertFalse(joins.isEmpty());
        
        System.out.println("✅ 订单表关联配置加载成功:");
        joins.forEach(join -> {
            System.out.println("   - " + join.getType() + " JOIN " + join.getTable() +
                             " ON " + join.getOnField());
            join.getFields().forEach(field -> {
                System.out.println("     • " + field.getName() + " AS " + field.getAlias());
            });
        });
    }

    @Test
    public void testIndexFieldMappingLoaded() {
        // 测试索引字段映射
        List<FieldMappingConfig.IndexFieldConfig> indexFields = 
            fieldMappingService.getIndexFields("order_index");
        
        assertNotNull(indexFields);
        assertFalse(indexFields.isEmpty());
        
        System.out.println("✅ 订单索引字段映射加载成功:");
        indexFields.forEach(field -> {
            System.out.println("   - " + field.getName() + " (" + field.getType() + ") " +
                             "<- " + field.getSourceField());
        });
    }

    @Test
    public void testRecordConversion() {
        // 模拟从数据库读取的数据
        Map<String, Object> dbData = new HashMap<>();
        dbData.put("order_id", 1L);
        dbData.put("order_no", "ORD001");
        dbData.put("amount", 199.99);
        dbData.put("user_id", 1L);
        dbData.put("user_name", "张三");
        dbData.put("user_phone", "13800138001");
        dbData.put("order_time", LocalDateTime.now());

        // 创建源记录
        DynamicRecord sourceRecord = recordBuilderService.buildSourceRecordFromMap(dbData, "orders");
        
        assertNotNull(sourceRecord);
        assertEquals(7, sourceRecord.getFields().size());
        
        // 转换为索引记录
        DynamicRecord indexRecord = recordBuilderService.convertToIndexRecord(sourceRecord, "order_index");
        
        assertNotNull(indexRecord);
        
        // 转换为 Map（用于写入）
        Map<String, Object> outputMap = recordBuilderService.convertRecordToMap(indexRecord);
        
        assertNotNull(outputMap);
        assertTrue(outputMap.containsKey("_id"));
        assertTrue(outputMap.containsKey("_operation"));
        assertTrue(outputMap.containsKey("_timestamp"));
        
        System.out.println("✅ 记录转换完整流程测试成功:");
        System.out.println("   源数据字段: " + dbData.keySet());
        System.out.println("   源记录字段: " + sourceRecord.getFields().keySet());
        System.out.println("   索引记录字段: " + indexRecord.getFields().keySet());
        System.out.println("   输出数据字段: " + outputMap.keySet());
    }
}

