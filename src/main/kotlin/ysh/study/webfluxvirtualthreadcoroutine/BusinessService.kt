package ysh.study.webfluxvirtualthreadcoroutine

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate

@Service
class BusinessService(
    private val repository: UserRepository,
    private val virtualThreadDispatcher: CoroutineDispatcher,
    private val transactionTemplate: TransactionTemplate,
) {
    private val logger = LoggerFactory.getLogger(BusinessService::class.java)

    suspend fun saveToDb(data: String) =
        withContext(virtualThreadDispatcher) {
            logger.info("[{}] Saving user to DB: {}", Thread.currentThread().name, data)

            transactionTemplate.execute {
                repository.save(UserEntity(name = data))
            }!!
        }
}
