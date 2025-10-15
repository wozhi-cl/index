package com.company.index.batch.support;

/**
 * Batch 常量定义
 */
public class BatchConstants {
    
    // 默认 Chunk 大小
    public static final int DEFAULT_CHUNK_SIZE = 1000;
    
    // 默认并行线程数
    public static final int DEFAULT_THREAD_COUNT = 4;
    
    // 默认分片数
    public static final int DEFAULT_PARTITION_COUNT = 4;
    
    // 默认重试次数
    public static final int DEFAULT_RETRY_COUNT = 3;
    
    // 默认重试延迟（毫秒）
    public static final long DEFAULT_RETRY_DELAY = 1000;
    
    // 最大重试延迟（毫秒）
    public static final long MAX_RETRY_DELAY = 30000;
    
    // 默认跳过限制
    public static final int DEFAULT_SKIP_LIMIT = 100;
    
    // 默认提交间隔
    public static final int DEFAULT_COMMIT_INTERVAL = 1000;
    
    // 默认队列容量
    public static final int DEFAULT_QUEUE_CAPACITY = 100;
    
    // 默认保持活跃时间（秒）
    public static final int DEFAULT_KEEP_ALIVE_SECONDS = 60;
    
    // 默认超时时间（毫秒）
    public static final long DEFAULT_TIMEOUT = 30000;
    
    // 默认错误率阈值
    public static final double DEFAULT_ERROR_RATE_THRESHOLD = 0.01;
    
    // 默认采样大小
    public static final int DEFAULT_SAMPLE_SIZE = 1000;
    
    // 默认重叠窗口（分钟）
    public static final int DEFAULT_OVERLAP_MINUTES = 5;
    
    // 默认时间窗口（分钟）
    public static final int DEFAULT_TIME_WINDOW_MINUTES = 5;
    
    // Job 名称
    public static final String FULL_INDEX_JOB_NAME = "fullIndexJob";
    public static final String INCREMENTAL_INDEX_JOB_NAME = "incrementalIndexJob";
    
    // Step 名称
    public static final String CLEANUP_STEP_NAME = "cleanupStep";
    public static final String READ_PROCESS_WRITE_STEP_NAME = "readProcessWriteStep";
    public static final String CHECK_STEP_NAME = "checkStep";
    public static final String SWITCH_STEP_NAME = "switchStep";
    public static final String CONDITIONAL_REBUILD_STEP_NAME = "conditionalRebuildStep";
    
    // 数据源类型
    public static final String DATA_SOURCE_MYSQL = "mysql";
    public static final String DATA_SOURCE_ORACLE = "oracle";
    public static final String DATA_SOURCE_H2 = "h2";
    public static final String DATA_SOURCE_CSV = "csv";
    public static final String DATA_SOURCE_JSON = "json";
    
    // 索引目标类型
    public static final String INDEX_TARGET_ELASTICSEARCH = "elasticsearch";
    public static final String INDEX_TARGET_GETQUICK = "getquick";
    public static final String INDEX_TARGET_FILE = "file";
    
    // 记录类型
    public static final String RECORD_TYPE_INSERT = "INSERT";
    public static final String RECORD_TYPE_UPDATE = "UPDATE";
    public static final String RECORD_TYPE_DELETE = "DELETE";
    
    // 任务状态
    public static final String JOB_STATUS_STARTED = "STARTED";
    public static final String JOB_STATUS_RUNNING = "RUNNING";
    public static final String JOB_STATUS_COMPLETED = "COMPLETED";
    public static final String JOB_STATUS_FAILED = "FAILED";
    public static final String JOB_STATUS_CANCELLED = "CANCELLED";
}
