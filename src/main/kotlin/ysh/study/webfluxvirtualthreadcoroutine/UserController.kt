package ysh.study.webfluxvirtualthreadcoroutine

import ysh.study.webfluxvirtualthreadcoroutine.BusinessService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class UserController(
    private val businessService: BusinessService,
) {
    @PostMapping("/users")
    suspend fun createUser(@RequestParam name: String) {
        businessService.saveToDb(name)
    }
}
