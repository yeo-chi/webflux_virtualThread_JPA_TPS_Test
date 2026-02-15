package ysh.study.webfluxvirtualthreadcoroutine

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import kotlin.system.measureTimeMillis

@Service
class AuditService(
    private val jdbcTemplate: org.springframework.jdbc.core.JdbcTemplate,
    private val auditLogRepository: AuditLogRepository,
    private val virtualThreadDispatcher: CoroutineDispatcher,
    private val transactionTemplate: TransactionTemplate,
) {
    private val logger = LoggerFactory.getLogger(AuditService::class.java)
    private val semaphore = java.util.concurrent.Semaphore(29)

    @EventListener
    fun handleStep1Event(event: UserStep1Event) {
        CoroutineScope(virtualThreadDispatcher).launch {
            semaphore.acquire()
            try {
                transactionTemplate.execute {
                    performBulkInsert(event.userId, event.userName, 0, 30, "STEP_1")
                }
            } finally {
                semaphore.release()
            }
        }
    }

    @EventListener
    fun handleStep2Event(event: UserStep2Event) {
        CoroutineScope(virtualThreadDispatcher).launch {
            semaphore.acquire()
            try {
                transactionTemplate.execute {
                    performBulkInsert(event.userId, event.userName, 30, 20, "STEP_2")
                }
            } finally {
                semaphore.release()
            }
        }
    }

    private fun performBulkInsert(userId: Long, userName: String, startOffset: Int, count: Int, batchName: String) {
        val sql = "INSERT INTO audit_logs (user_id, action) VALUES (?, ?)"
        jdbcTemplate.batchUpdate(sql, object : org.springframework.jdbc.core.BatchPreparedStatementSetter {
            override fun setValues(ps: java.sql.PreparedStatement, i: Int) {
                ps.setLong(1, userId)
                ps.setString(2, "USER_CREATED_${userName}_${batchName}_${startOffset + i + 1}")
            }
            override fun getBatchSize(): Int = count
        })
        logger.info("Bulk-inserted {} logs for user {} ({})", count, userId, batchName)
    }
}
