package com.company.index.batch.job;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

/**
 * 所有数据源和索引目标组合的测试套件
 * 
 * 测试矩阵:
 * ┌─────────────────────────────────────────────────────────────────────────┐
 * │  数据源       │  索引目标          │  测试类                             │
 * ├───────────────┼───────────────────┼─────────────────────────────────────┤
 * │  H2           │  File             │  FullIndexJobTest                   │
 * │  H2           │  File             │  IncrementalIndexJobTest            │
 * │  H2           │  ES (Mock)        │  H2ToElasticsearchJobTest           │
 * │  CSV          │  File             │  CsvToFileJobTest                   │
 * │  JSON         │  File             │  JsonToFileJobTest                  │
 * │  MySQL (容器) │  ES (真实)        │  MysqlToElasticsearchJobTest        │
 * │  MySQL (容器) │  File             │  MysqlToFileJobTest                 │
 * └─────────────────────────────────────────────────────────────────────────┘
 * 
 * 运行方式:
 * 1. IDE中右键点击此类 → Run 'AllCombinationsTestSuite'
 * 2. Maven命令: mvn test -Dtest=AllCombinationsTestSuite
 * 
 * 注意:
 * - MySQL测试需要Docker服务运行: docker-compose up -d mysql elasticsearch
 * - ES测试会真实写入数据，可通过Kibana (localhost:5601) 查看
 */
@Suite
@SuiteDisplayName("所有数据源和索引目标组合测试")
@SelectClasses({
    // H2 → File (内存数据库)
    FullIndexJobTest.class,
    IncrementalIndexJobTest.class,
    
    // File → File (文件读取)
    CsvToFileJobTest.class,          // CSV → File
    JsonToFileJobTest.class,         // JSON → File
    
    // H2 → Elasticsearch (Mock)
    H2ToElasticsearchJobTest.class,
    
    // MySQL → 各种目标 (需要Docker: docker-compose up -d mysql elasticsearch)
    MysqlToElasticsearchJobTest.class,  // MySQL → Elasticsearch (真实环境)
    MysqlToFileJobTest.class             // MySQL → File
})
public class AllCombinationsTestSuite {
    // 此类仅用于组织测试套件，不需要任何代码
}

