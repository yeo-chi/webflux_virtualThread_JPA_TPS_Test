package ysh.study.webfluxvirtualthreadcoroutine

import org.springframework.data.jpa.repository.JpaRepository

interface AuditLogRepository : JpaRepository<AuditLogEntity, Long> {
    fun findByUserId(userId: Long): List<AuditLogEntity>
}
