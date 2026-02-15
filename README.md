# Spring WebFlux + Virtual Thread + JPA Performance Test (700 TPS)

이 프로젝트는 **Spring WebFlux, Kotlin Coroutines, 그리고 Java 21의 가상 스레드(Virtual Thread)**를 결합하여, 대규모 트래픽(700 TPS 이상) 환경에서 데이터베이스 I/O 병목을 해결하고 시스템 안정성을 확보하는 아키텍처를 실증하기 위해 만들어졌습니다.

## 🚀 핵심 목표
- **700 TPS (Transactions Per Second) 달성**: 초당 700건의 사용자 저장 및 관련 어딧 로그(50건/요청) 저장 처리.
- **리소스 최적화**: 한정된 DB 커넥션 풀(100개) 환경에서 효율적인 배분 및 보호.
- **장애 탄력성**: 트래픽 폭증 상황에서도 시스템 마비 없이 100% 성공률 유지.

## 🏗 아키텍처 특징

### 1. 가상 스레드(Virtual Thread) 기반 동시성 모델
- **Thread-per-Task**: 경량 스레드인 가상 스레드를 활용하여 수천 개의 동시 요청을 차단(Blocking) 오버헤드 없이 처리합니다.
- **Coroutine 통합**: 가상 스레드 디스패처를 사용하여 선언적이고 효율적인 비동기 코드를 작성했습니다.

### 2. 이벤트 주도 설계 (EDA)
- **비결합성**: 메인 서비스(`BusinessService`)는 핵심 저장 로직 후 이벤트를 발행하고 즉시 응답합니다.
- **단계별 어딧 로직**: 어딧 저장은 별도의 이벤트 리스너(`AuditService`)에서 비동기로 처리되며, 각 단계(Step 1, Step 2)가 독립된 트랜잭션으로 관리됩니다.

### 3. 하이브리드 인서트 및 JdbcTemplate 벌크 최적화
- **JPA + JdbcTemplate**: 단건 저장은 JPA를, 대량 로그 저장은 `JdbcTemplate.batchUpdate`를 활용합니다.
- **MySQL 배칭**: `rewriteBatchedStatements=true` 설정을 통해 물리적인 네트워크往復을 최소화했습니다.

### 4. 세마포어(Semaphore) 기반 Throttling (자원 보호)
- **커넥션 풀 보호**: 전체 100개의 커넥션 중 메인 서비스(56개), 백그라운드 어딧(29개)으로 점유율을 **85%**로 제한했습니다.
- **가시성 제어**: 세마포어를 트랜잭션 획득 직전에 배치하여, 불필요한 커넥션 점유 대기 시간을 제거했습니다.

## 📊 성능 테스트 결과 (700 TPS 부하)

| 지표 | 결과 |
| :--- | :--- |
| **총 요청 수** | 7,000건 |
| **성공률 (Success Rate)** | **100.00%** |
| **평균 지연 시간** | **~103 ms** |
| **P95 지연 시간** | **~289 ms** |
| **실제 처리량 (Actual RPS)** | **~696 RPS** |

## 🛠 실행 방법

### 환경 요구 사항
- Java 21+
- MySQL (rewriteBatchedStatements 옵션 필수)
- Gradle

### 설정 (application.yaml 준비)
`src/main/resources/application.yaml` 파일을 생성하고 아래 내용을 참고하여 본인의 DB 정보를 입력하세요.

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/db_name?rewriteBatchedStatements=true
    username: YOUR_USERNAME
    password: YOUR_PASSWORD
    hikari:
      maximum-pool-size: 100
  jpa:
    database-platform: org.hibernate.dialect.MySQLDialect
```

### 테스트 실행
```bash
# 애플리케이션 실행
./gradlew bootRun

# 부하 테스트 실행 (Python 3.x 필수)
python3 performance_test.py
```

---
**Author**: yeoseunghyeon
**Project**: High-Performance TPS Testbed
