package com.company.index.scheduler.quartz;

import com.company.index.batch.job.JobLauncherService;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

import javax.sql.DataSource;
import java.util.Properties;

/**
 * Quartz 集群配置
 * 支持 JDBC JobStore、misfire 策略、时区设置
 * 注意：H2 模式使用内存 JobStore，不加载此配置
 */
@Configuration
@org.springframework.context.annotation.Profile("!h2")
public class QuartzConfig {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JobLauncherService jobLauncherService;

    @Value("${index.scheduler.quartz.threadPoolSize:10}")
    private int threadPoolSize;

    @Value("${index.scheduler.quartz.clusterCheckinInterval:20000}")
    private long clusterCheckinInterval;

    @Value("${index.scheduler.quartz.misfireThreshold:60000}")
    private long misfireThreshold;

    @Value("${index.scheduler.quartz.timeZone:UTC}")
    private String timeZone;

    /**
     * Quartz 调度器工厂
     */
    @Bean
    public SchedulerFactoryBean schedulerFactoryBean() {
        SchedulerFactoryBean factory = new SchedulerFactoryBean();
        factory.setDataSource(dataSource);
        factory.setQuartzProperties(quartzProperties());
        factory.setJobFactory(jobFactory());
        factory.setSchedulerName("IndexScheduler");
        factory.setApplicationContextSchedulerContextKey("applicationContext");
        return factory;
    }

    /**
     * Quartz 属性配置
     */
    @Bean
    public Properties quartzProperties() {
        Properties props = new Properties();
        
        // 调度器配置
        props.setProperty("org.quartz.scheduler.instanceName", "IndexScheduler");
        props.setProperty("org.quartz.scheduler.instanceId", "AUTO");
        props.setProperty("org.quartz.scheduler.skipUpdateCheck", "true");
        
        // 线程池配置
        props.setProperty("org.quartz.threadPool.class", "org.quartz.simpl.SimpleThreadPool");
        props.setProperty("org.quartz.threadPool.threadCount", String.valueOf(threadPoolSize));
        props.setProperty("org.quartz.threadPool.threadPriority", "5");
        props.setProperty("org.quartz.threadPool.threadsInheritContextClassLoaderOfInitializingThread", "true");
        
        // JobStore 配置（JDBC）
        props.setProperty("org.quartz.jobStore.class", "org.quartz.impl.jdbcjobstore.JobStoreTX");
        props.setProperty("org.quartz.jobStore.driverDelegateClass", "org.quartz.impl.jdbcjobstore.StdJDBCDelegate");
        props.setProperty("org.quartz.jobStore.tablePrefix", "QRTZ_");
        props.setProperty("org.quartz.jobStore.dataSource", "quartzDataSource");
        props.setProperty("org.quartz.jobStore.isClustered", "true");
        props.setProperty("org.quartz.jobStore.clusterCheckinInterval", String.valueOf(clusterCheckinInterval));
        props.setProperty("org.quartz.jobStore.maxMisfiresToHandleAtATime", "1");
        props.setProperty("org.quartz.jobStore.misfireThreshold", String.valueOf(misfireThreshold));
        props.setProperty("org.quartz.jobStore.txIsolationLevelSerializable", "false");
        
        // 数据源配置 - 使用应用的主数据源
        props.setProperty("org.quartz.dataSource.quartzDataSource.provider", "hikaricp");
        props.setProperty("org.quartz.dataSource.quartzDataSource.driver", "com.mysql.cj.jdbc.Driver");
        props.setProperty("org.quartz.dataSource.quartzDataSource.URL", "jdbc:mysql://localhost:3307/index_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true");
        props.setProperty("org.quartz.dataSource.quartzDataSource.user", "root");
        props.setProperty("org.quartz.dataSource.quartzDataSource.password", "root123");
        props.setProperty("org.quartz.dataSource.quartzDataSource.maxConnections", "5");
        props.setProperty("org.quartz.dataSource.quartzDataSource.validationQuery", "SELECT 1");
        
        // 时区配置
        props.setProperty("org.quartz.scheduler.timeZone", timeZone);
        
        return props;
    }

    /**
     * Job 工厂
     */
    @Bean
    public org.quartz.spi.JobFactory jobFactory() {
        return new SpringJobFactory();
    }
}
