package ysh.study.webfluxvirtualthreadcoroutine

import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<UserEntity, Long>
