package com.company.index.reader;

import com.company.index.batch.reader.FileCsvReader;
import com.company.index.batch.reader.FileJsonReader;
import com.company.index.batch.reader.ReaderFactory;
import com.company.index.common.model.SourceRecord;
import org.junit.jupiter.api.Test;
import org.springframework.batch.item.ItemReader;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Reader 测试类
 */
@SpringBootTest
@TestPropertySource(properties = {
    "index.dataSource.type=csv",
    "index.dataSource.csvPath=./src/test/resources/sample.csv",
    "index.dataSource.csvFields=id,name,email,created_at,updated_at"
})
class ReaderTest {

    @Test
    void testCsvReader() throws Exception {
        FileCsvReader csvReader = new FileCsvReader("./src/test/resources/sample.csv", 
                                                   "id,name,email,created_at,updated_at");
        
        ItemReader<SourceRecord> reader = csvReader.createReader();
        
        SourceRecord record = reader.read();
        assertNotNull(record);
        assertEquals("1", record.getId());
        assertEquals("INSERT", record.getType());
        assertEquals("CSV", record.getSource());
        assertNotNull(record.getData());
        
        // reader.close(); // ItemReader 接口没有 close() 方法
    }

    @Test
    void testJsonReader() throws Exception {
        FileJsonReader jsonReader = new FileJsonReader("./src/test/resources/sample.json");
        
        ItemReader<SourceRecord> reader = jsonReader.createReader();
        
        SourceRecord record = reader.read();
        assertNotNull(record);
        assertNotNull(record.getId());
        assertEquals("JSON", record.getSource());
        assertNotNull(record.getData());
        
        // reader.close(); // ItemReader 接口没有 close() 方法
    }

    @Test
    void testReaderFactory() {
        ReaderFactory factory = new ReaderFactory();
        
        // 测试 CSV 读取器创建
        ItemReader<SourceRecord> csvReader = factory.createFullReader(100);
        assertNotNull(csvReader);
        
        // 测试增量读取器创建
        LocalDateTime startTime = LocalDateTime.now().minusHours(1);
        LocalDateTime endTime = LocalDateTime.now();
        ItemReader<SourceRecord> deltaReader = factory.createDeltaReader(startTime, endTime, 5);
        assertNotNull(deltaReader);
    }
}
