# 배포 전략 (Deployment Strategy)

## 개요

ErdSketch는 **Fly.io 단일 컨테이너**로 운영됩니다.
Spring Boot가 프론트엔드 정적 파일과 백엔드 API를 함께 제공하는 모놀리식 구조입니다.

---

## 인프라 구성

| 항목 | 내용 |
|------|------|
| 플랫폼 | Fly.io (region: nrt — 도쿄) |
| 컨테이너 | shared-cpu-1x, 512MB RAM |
| 데이터베이스 | Fly Postgres (PostgreSQL 15) |
| 이메일 | SendGrid SMTP (`smtp.sendgrid.net:587`) |
| URL | https://erdsketch.fly.dev |

### 아키텍처

```
Browser
  │
  HTTPS/WSS
  │
Fly.io Proxy (443)
  │
Spring Boot :8080
  ├── /api/v1/**        → REST API
  ├── /ws/**            → WebSocket (Yjs)
  ├── /actuator/health  → 헬스체크
  └── /**               → 정적 파일 (React SPA)
  │
  └── Fly Postgres (PostgreSQL 15)
```

### Docker 멀티스테이지 빌드

```
Stage 1: node:20-alpine      → npm run build (React)
Stage 2: eclipse-temurin:21-jdk-alpine → mvnw package (Spring Boot + 정적 파일 포함)
Stage 3: eclipse-temurin:21-jre-alpine → 런타임 (app.jar 복사)
```

---

## 헬스체크

Fly.io가 15초마다 `/actuator/health`를 호출합니다.

| 설정 | 값 |
|------|-----|
| 엔드포인트 | `GET /actuator/health` |
| 응답 | `{"status":"UP"}` |
| 체크 간격 | 15초 |
| 타임아웃 | 5초 |
| 유예 기간 | 30초 (Spring Boot 기동 시간 대기) |

헬스체크가 연속 실패하면 Fly.io가 해당 머신으로의 트래픽 라우팅을 차단합니다.

---

## CI/CD 파이프라인

`main` 브랜치에 push 시 GitHub Actions가 자동 실행됩니다.

```
push to main
  │
  ├── [병렬] Backend Build & Test (./mvnw verify)
  ├── [병렬] Frontend Lint & Typecheck
  ├── [병렬] Frontend Unit Tests
  └── [병렬] Frontend E2E Tests (Playwright)
  │
  └── [위 4개 모두 통과 시] Deploy to Fly.io
        │
        ├── fly deploy --remote-only --auto-rollback --wait-timeout 180
        │     → Depot 원격 빌드 (로컬 Docker 불필요)
        │     → 롤링 배포 (다운타임 없음)
        │     → 헬스체크 실패 시 자동 롤백
        │
        └── Smoke Test
              → GET https://erdsketch.fly.dev/actuator/health
              → HTTP 200 아니면 파이프라인 실패 기록
```

### 배포 전략 — Rolling

새 버전 컨테이너가 헬스체크를 통과한 후 트래픽을 전환합니다.

```
1. 새 이미지 빌드
2. 새 머신 기동 → 30초 유예 → 헬스체크 통과 확인
3. 기존 머신에서 트래픽 전환
4. 기존 머신 종료
```

### 자동 롤백

`--auto-rollback` 플래그로 배포 중 다음 상황에서 자동 복구됩니다:

- 새 컨테이너가 기동 실패 (JVM crash, OOM 등)
- `wait-timeout 180초` 내에 헬스체크를 통과하지 못한 경우
- 배포 중 기존 이미지로 즉시 복원

---

## 환경 설정

### Fly Postgres 초기 설정

```bash
# 최초 1회: Fly Postgres 클러스터 생성 및 연결
fly postgres create --name erdsketch-db --region nrt
fly postgres attach --app erdsketch erdsketch-db
# attach 후 DATABASE_URL 시크릿이 자동 설정됨
```

### Fly.io Secrets (환경 변수)

```bash
fly secrets list          # 현재 설정된 시크릿 목록
fly secrets set KEY=VALUE # 시크릿 추가/수정
```

| 시크릿 | 설명 |
|--------|------|
| `JWT_SECRET` | JWT 서명 키 (256비트 이상) |
| `DATABASE_URL` | PostgreSQL 연결 URL (Fly Postgres attach 시 자동 설정) |
| `DATABASE_USERNAME` | DB 사용자명 (기본값: erdsketch) |
| `DATABASE_PASSWORD` | DB 비밀번호 |
| `SENDGRID_API_KEY` | SendGrid API 키 (이메일 발송) |

### Spring 프로파일

Fly.io 배포 시 `SPRING_PROFILES_ACTIVE=fly`가 자동 설정되어 `application-fly.yml`이 적용됩니다.

```yaml
# application-fly.yml 주요 설정
spring:
  datasource:
    url: ${DATABASE_URL}        # Fly Postgres URL
    driver-class-name: org.postgresql.Driver
  jpa.hibernate.ddl-auto: validate
  flyway.enabled: true          # Flyway 마이그레이션 활성화
  mail: SendGrid SMTP 설정

app:
  jwt.secret: ${JWT_SECRET}     # Fly secret에서 주입

management:
  endpoints.web.exposure.include: health  # health만 외부 노출
```

---

## 수동 운영 명령어

### 상태 확인

```bash
fly status                         # 머신 상태 확인
fly logs                           # 실시간 로그 스트림
fly logs --no-tail | tail -100     # 최근 로그 100줄
```

### 배포 관련

```bash
fly deploy --remote-only           # 수동 배포
fly releases                       # 배포 이력 조회
fly deploy --image <이미지ID>      # 특정 버전으로 롤백
```

### 긴급 롤백

```bash
# 1. 이전 배포 이미지 ID 확인
fly releases

# 2. 해당 이미지로 즉시 배포
fly deploy --image registry.fly.io/erdsketch:deployment-<이전ID>
```

### 머신 접속 (디버깅)

```bash
fly ssh console                    # 컨테이너 shell 접속
```

### DB 관리

```bash
fly postgres connect -a erdsketch-db   # PostgreSQL 직접 접속
fly postgres db list -a erdsketch-db   # DB 목록 확인
```

---

## 모니터링

### 헬스 엔드포인트

```bash
curl https://erdsketch.fly.dev/actuator/health
# 정상: {"status":"UP"}
# 비정상: HTTP 503 또는 연결 불가
```

### Fly.io 대시보드

- **Monitoring** 탭: CPU, 메모리, 요청 수 그래프
- **Logs** 탭: 실시간 애플리케이션 로그
- **Releases** 탭: 배포 이력 및 롤백

### 로그 레벨 (fly 환경)

```yaml
logging:
  level:
    com.erdsketch: INFO        # 앱 로그
    org.springframework.security: WARN  # Security 로그 최소화
```

---

## 알려진 제약사항 및 주의사항

### OAuth

- Google/GitHub OAuth 코드 구현 완료
- 현재 `app.oauth.enabled=false`로 비활성화 상태
- 활성화 방법: `application-fly.yml`에 `app.oauth.enabled: true` 추가 + OAuth 앱 credentials를 Fly secret으로 설정

---

## 로컬 개발 환경과의 차이

| 항목 | 로컬 | Fly.io |
|------|------|--------|
| DB | PostgreSQL (Docker) | Fly Postgres |
| 마이그레이션 | Flyway | Flyway |
| 이메일 | 미발송 (EmailService 빈 미생성) | SendGrid 발송 |
| OAuth | 비활성화 | 비활성화 (설정 가능) |
| 프로파일 | default | fly |
