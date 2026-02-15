package ysh.study.webfluxvirtualthreadcoroutine

import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import javax.sql.DataSource
import kotlin.system.measureTimeMillis

@Service
class BusinessService(
    private val repository: UserRepository,
    private val virtualThreadDispatcher: CoroutineDispatcher,
    private val dataSource: DataSource,
    private val eventPublisher: ApplicationEventPublisher,
    private val transactionTemplate: TransactionTemplate,
) {
    private val logger = LoggerFactory.getLogger(BusinessService::class.java)
    private val semaphore = java.util.concurrent.Semaphore(56)

    suspend fun saveToDb(data: String) = withContext(virtualThreadDispatcher) {
        semaphore.acquire()
        try {
            transactionTemplate.execute {
                val time = measureTimeMillis {
                    repository.save(UserEntity(name = data)).also { 
                        eventPublisher.publishEvent(UserStep1Event(it.id!!, data))
                        eventPublisher.publishEvent(UserStep2Event(it.id!!, data))
                    }
                }

                if (dataSource is HikariDataSource) {
                    val pool = dataSource.hikariPoolMXBean
                    if (pool != null) {
                        logger.info(
                            "Saved in {}ms. Pool: Active={}, Idle={}, Total={}, Waiting={}",
                            time,
                            pool.activeConnections,
                            pool.idleConnections,
                            pool.totalConnections,
                            pool.threadsAwaitingConnection
                        )
                    }
                }
            }
        } finally {
            semaphore.release()
        }
    }
}