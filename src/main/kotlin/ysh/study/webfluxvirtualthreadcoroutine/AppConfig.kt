package ysh.study.webfluxvirtualthreadcoroutine

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Configuration
@EnableAsync
class AppConfig {
    @Bean(destroyMethod = "close")
    fun virtualThreadExecutor(): ExecutorService =
        Executors.newVirtualThreadPerTaskExecutor()

    @Bean
    fun virtualThreadDispatcher(executor: ExecutorService): CoroutineDispatcher =
        executor.asCoroutineDispatcher()

    @Bean
    fun webClientBuilder(): org.springframework.web.reactive.function.client.WebClient.Builder =
        org.springframework.web.reactive.function.client.WebClient.builder()
}
