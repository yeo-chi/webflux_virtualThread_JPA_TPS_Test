package ysh.study.webfluxvirtualthreadcoroutine

import kotlinx.coroutines.delay
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.atomic.AtomicLong

@RestController
class MockController {
    private val counter = AtomicLong(0)

    @GetMapping("/mock/delay")
    suspend fun getDelay(@RequestParam(defaultValue = "10") ms: Long): String {
        delay(ms)
        return "Delayed for ${ms}ms"
    }

    @GetMapping("/mock/progressive-delay")
    suspend fun getProgressiveDelay(): String {
        val count = counter.incrementAndGet()
        // 10ms ~ 10000ms (10s) 점진적 증가
        // 간단하게 호출 횟수에 비례하거나 특정 로직으로 구현
        val baseDelay = 10L
        val maxDelay = 10000L
        val currentDelay = (baseDelay + (count * 10)).coerceAtMost(maxDelay)
        
        delay(currentDelay)
        return "Progressive delay: ${currentDelay}ms (Count: $count)"
    }
}
