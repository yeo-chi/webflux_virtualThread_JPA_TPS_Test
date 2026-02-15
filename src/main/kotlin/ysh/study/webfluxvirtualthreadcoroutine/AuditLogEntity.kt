package ysh.study.webfluxvirtualthreadcoroutine

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "audit_logs", indexes = [jakarta.persistence.Index(name = "idx_user_id", columnList = "userId")])
class AuditLogEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    val userId: Long,
    val action: String,
)
