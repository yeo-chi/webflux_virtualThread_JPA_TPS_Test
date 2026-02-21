package ysh.study.webfluxvirtualthreadcoroutine

import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import kotlin.random.Random

@Service
class AuditService(
    private val jdbcTemplate: org.springframework.jdbc.core.JdbcTemplate,
    private val auditLogRepository: AuditLogRepository,
    private val virtualThreadDispatcher: CoroutineDispatcher,
    private val transactionTemplate: TransactionTemplate,
    webClientBuilder: WebClient.Builder,
) {
    private val logger = LoggerFactory.getLogger(AuditService::class.java)
    private val webClient = webClientBuilder.baseUrl("http://localhost:8080").build()

    suspend fun processAudit(userId: Long, userName: String) = coroutineScope {
        logger.info("[{}] audit process 시작: userId={}", Thread.currentThread().name, userId)

        // 1. 비동기로 새로운 Mysql table 2개와, 비동기 캐시 1개를 가져옴
        // Mysql 은 5~10S 정도 렌덤 응답 시뮬레이션
        val dbData1 = async { simulateDbQuery("Table1", 5000..10000) }
        val dbData2 = async { simulateDbQuery("Table2", 5000..10000) }
        val cacheData = async { simulateCacheQuery("Cache1", 100) }

        val collectedData = listOf(dbData1.await(), dbData2.await(), cacheData.await())
        logger.info("[{}] 비동기 데이터 수집 완료: {}", Thread.currentThread().name, collectedData)

        // 2. 비동기 수집 후 bulk로 저장하는 2개의 메소드를 차례로 호출
        // 두 bulk 저장 모두 다시 조회 한 다음 리턴
        val step1Result = performBulkAndQuery(userId, userName, 0, 30, "STEP_1")
        val step2Result = performBulkAndQuery(userId, userName, 30, 20, "STEP_2")

        logger.info("[{}] Bulk 저장 및 재조회 완료. Step1 size: {}, Step2 size: {}", 
            Thread.currentThread().name, step1Result.size, step2Result.size)

        // 3. 두 bulk 저장 중 첫번째 응답 데이터를 launch로 webClient 호출
        if (step1Result.isNotEmpty()) {
            val firstLog = step1Result.first()
            CoroutineScope(virtualThreadDispatcher).launch {
                logger.info("[{}] 최종 WebClient 호출 시작 (Step1 데이터 기반): {}", Thread.currentThread().name, firstLog.action)
                try {
                    val response = webClient.get()
                        .uri("/mock/delay?ms=50")
                        .retrieve()
                        .awaitBody<String>()
                    logger.info("[{}] 최종 WebClient 호출 완료: {}", Thread.currentThread().name, response)
                } catch (e: Exception) {
                    logger.error("[{}] 최종 WebClient 호출 실패", Thread.currentThread().name, e)
                }
            }
        }
    }

    private suspend fun simulateDbQuery(name: String, range: IntRange): String = withContext(virtualThreadDispatcher) {
        val delayTimeMs = Random.nextLong(range.first.toLong(), range.last.toLong() + 1)
        val delaySeconds = delayTimeMs / 1000.0
        
        logger.info("[{}] DB Sleep 시작: {} ({}s)", Thread.currentThread().name, name, delaySeconds)
        jdbcTemplate.execute("SELECT SLEEP($delaySeconds)")
        
        "$name data (delayed ${delayTimeMs}ms)"
    }

    private suspend fun simulateCacheQuery(name: String, time: Long): String {
        delay(time)
        return "$name data"
    }

    private fun performBulkAndQuery(userId: Long, userName: String, startOffset: Int, count: Int, batchName: String): List<AuditLogEntity> {
        return transactionTemplate.execute {
            val logs = (1..count).map { i ->
                AuditLogEntity(
                    userId = userId,
                    action = "AUDIT_${userName}_${batchName}_${startOffset + i}"
                )
            }
            auditLogRepository.saveAll(logs).toList()
        } ?: emptyList()
    }
}
