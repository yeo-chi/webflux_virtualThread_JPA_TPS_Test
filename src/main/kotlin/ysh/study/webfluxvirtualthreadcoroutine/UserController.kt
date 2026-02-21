package ysh.study.webfluxvirtualthreadcoroutine

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody

@RestController
class UserController(
    private val businessService: BusinessService,
    private val auditService: AuditService,
    private val virtualThreadDispatcher: CoroutineDispatcher,
    webClientBuilder: WebClient.Builder,
) {
    private val logger = LoggerFactory.getLogger(UserController::class.java)
    private val webClient = webClientBuilder.baseUrl("http://localhost:8080").build()

    @PostMapping("/users")
    suspend fun createUser(@RequestParam name: String) = withContext(virtualThreadDispatcher) {
        coroutineScope {
            logger.info("[{}] Received request for user: {}", Thread.currentThread().name, name)

            // IO 스레드에서 병렬로 WebClient 호출
            val call1 = async<String> {
                webClient.get().uri("/mock/delay?ms=10").retrieve().awaitBody<String>()
            }
            val call2 = async<String> {
                webClient.get().uri("/mock/delay?ms=10").retrieve().awaitBody<String>()
            }
            val call3 = async<String> {
                webClient.get().uri("/mock/progressive-delay").retrieve().awaitBody<String>()
            }

            val results = listOf(call1.await(), call2.await(), call3.await())
            logger.info("[{}] WebClient calls finished: {}", Thread.currentThread().name, results)

            businessService.saveToDb(name).also { user ->
                launch(virtualThreadDispatcher) {
                    auditService.processAudit(user.id!!, user.name)
                }
            }
        }
    }
}
