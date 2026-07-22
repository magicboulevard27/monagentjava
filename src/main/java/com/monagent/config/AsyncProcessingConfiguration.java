package com.monagent.config;

import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.core.task.TaskExecutor;

@Configuration
@EnableConfigurationProperties(AsyncProcessingProperties.class)
public class AsyncProcessingConfiguration {

    @Bean(name = "collectorTaskExecutor")
    public TaskExecutor collectorTaskExecutor(AsyncProcessingProperties properties) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.workerThreads());
        executor.setMaxPoolSize(properties.workerThreads());
        executor.setQueueCapacity(properties.queueCapacity());
        executor.setThreadNamePrefix("collector-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds((int) properties.dispatchTimeout().toSeconds());
        executor.initialize();
        return executor;
    }

    @Bean(name = "collectorWorkerExecutor")
    public Executor collectorWorkerExecutor(@Qualifier("collectorTaskExecutor") TaskExecutor taskExecutor) {
        return taskExecutor;
    }
}
