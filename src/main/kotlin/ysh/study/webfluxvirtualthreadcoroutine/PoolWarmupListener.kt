package ysh.study.webfluxvirtualthreadcoroutine

import com.zaxxer.hikari.HikariDataSource
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Component
import java.util.concurrent.Executors
import java.util.concurrent.CountDownLatch
import javax.sql.DataSource

@Component
class PoolWarmupListener(
    private val dataSource: DataSource,
) : ApplicationListener<ApplicationReadyEvent> {
    private val logger = LoggerFactory.getLogger(PoolWarmupListener::class.java)

    override fun onApplicationEvent(event: ApplicationReadyEvent) {
        if (dataSource !is HikariDataSource) {
            logger.info("DataSource is not HikariDataSource, skipping warm-up")
            return
        }

        val poolSize = dataSource.maximumPoolSize
        val executor = Executors.newVirtualThreadPerTaskExecutor()
        val latch = CountDownLatch(poolSize)
        val startTime = System.currentTimeMillis()

        logger.info("Starting HikariCP warm-up for {} connections...", poolSize)

        repeat(poolSize) {
            executor.submit {
                try {
                    dataSource.connection.use { conn ->
                        conn.prepareStatement("SELECT 1").use { stmt ->
                            stmt.executeQuery()
                        }
                        latch.countDown()
                        latch.await() // Hold connection until all others are acquired
                    }
                } catch (e: Exception) {
                    logger.error("Warm-up connection failed", e)
                    latch.countDown()
                }
            }
        }

        executor.shutdown()
        while (!executor.isTerminated) {
            Thread.sleep(500)
            val pool = dataSource.hikariPoolMXBean
            logger.info("Warm-up progress: Total={}, Active={}, Idle={}, Waiting={}",
                pool.totalConnections, pool.activeConnections, pool.idleConnections, pool.threadsAwaitingConnection)
            if (pool.totalConnections >= poolSize) break
        }

        logger.info("HikariCP warm-up completed in {}ms. Total connections: {}", 
            System.currentTimeMillis() - startTime, dataSource.hikariPoolMXBean.totalConnections)
    }
}
