package uqu.drawbridge.platform.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.TaskExecutor
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor

@Configuration
@EnableAsync
class AsyncConfig {
    @Bean(name = ["notificationTaskExecutor"])
    fun notificationTaskExecutor(): TaskExecutor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = 2
        executor.maxPoolSize = 8
        executor.queueCapacity = 500
        executor.setThreadNamePrefix("notify-")
        executor.initialize()
        return executor
    }
}
