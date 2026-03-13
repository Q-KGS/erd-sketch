---
name: h2-test-setup
description: ErdSketch 백엔드 테스트 작성 시 H2 인메모리 DB 환경 구성 가이드. 테스트 클래스 작성, 테스트 환경 설정, Repository/Service/Controller 테스트 패턴을 안내한다. "테스트 작성", "테스트 환경 구성", "JUnit", "@DataJpaTest", "@WebMvcTest" 등의 키워드가 포함된 요청에 반응.
---

# H2 테스트 환경 구성 가이드

## 프로젝트 정보

- **패키지 루트:** `com.erdsketch`
- **빌드:** Maven (`pom.xml`)
- **Spring Boot:** 3.3.1, Java 21
- **테스트 DB:** H2 (인메모리, PostgreSQL 호환 모드)
- **기존 의존성:** `h2`, `testcontainers` (pom.xml에 이미 선언됨)

---

## 1. `src/test/resources/application-test.yml`

기존 `application-test.yml`은 Testcontainers(PostgreSQL)를 사용한다. H2 기반 테스트를 원하면 이 파일을 아래 내용으로 교체하거나, 별도 프로필(`h2`)로 분리한다.

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:erdsketch_test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;INIT=CREATE DOMAIN IF NOT EXISTS JSONB AS CLOB
    username: sa
    password:
    driver-class-name: org.h2.Driver
  h2:
    console:
      enabled: false
  jpa:
    hibernate:
      ddl-auto: create-drop
    properties:
      hibernate:
        dialect: org.hibernate.dialect.H2Dialect
        format_sql: true
    show-sql: true
  flyway:
    enabled: false

app:
  jwt:
    secret: test-secret-key-for-unit-tests-must-be-at-least-256-bits-long-padding-here
    access-token-expiry: 3600000
    refresh-token-expiry: 2592000000
```

> **JSONB 호환:** H2는 JSONB 타입을 모른다. JDBC URL의 `INIT=CREATE DOMAIN IF NOT EXISTS JSONB AS CLOB`으로 JSONB를 CLOB으로 매핑해 엔티티의 `@Column` 정의가 깨지지 않게 한다.

---

## 2. 테스트 지원 클래스

### 2.1 `TestSecurityConfig` — `@WebMvcTest` 보안 비활성화

```java
package com.erdsketch.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@TestConfiguration
public class TestSecurityConfig {

    @Bean
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
```

위치: `src/test/java/com/erdsketch/config/TestSecurityConfig.java`

---

### 2.2 `BaseIntegrationTest` — 통합 테스트 베이스

```java
package com.erdsketch.support;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public abstract class BaseIntegrationTest {
}
```

위치: `src/test/java/com/erdsketch/support/BaseIntegrationTest.java`

---

### 2.3 `BaseRepositoryTest` — Repository 슬라이스 테스트 베이스

```java
package com.erdsketch.support;

import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public abstract class BaseRepositoryTest {
}
```

위치: `src/test/java/com/erdsketch/support/BaseRepositoryTest.java`

> `Replace.NONE`: `application-test.yml`의 H2 설정을 그대로 사용하도록 Spring의 자동 교체를 막는다.

---

### 2.4 `TestDataFactory` — 엔티티 생성 헬퍼

```java
package com.erdsketch.support;

import com.erdsketch.document.ErdDocument;
import com.erdsketch.project.DbType;
import com.erdsketch.project.Project;
import com.erdsketch.user.User;
import com.erdsketch.workspace.Workspace;
import com.erdsketch.workspace.WorkspaceMember;
import com.erdsketch.workspace.WorkspaceRole;

import java.time.Instant;
import java.util.UUID;

public class TestDataFactory {

    public static User user() {
        return User.builder()
                .email("test-" + UUID.randomUUID() + "@example.com")
                .displayName("Test User")
                .passwordHash("$2a$10$hashedpassword")
                .build();
    }

    public static Workspace workspace(User owner) {
        return Workspace.builder()
                .name("Test Workspace")
                .slug("test-ws-" + UUID.randomUUID().toString().substring(0, 8))
                .owner(owner)
                .build();
    }

    public static WorkspaceMember workspaceMember(Workspace workspace, User user, WorkspaceRole role) {
        return WorkspaceMember.builder()
                .workspace(workspace)
                .user(user)
                .role(role)
                .joinedAt(Instant.now())
                .build();
    }

    public static Project project(Workspace workspace, User creator) {
        return Project.builder()
                .workspace(workspace)
                .name("Test Project")
                .targetDbType(DbType.POSTGRESQL)
                .createdBy(creator)
                .build();
    }

    public static ErdDocument erdDocument(Project project) {
        return ErdDocument.builder()
                .project(project)
                .name("Test ERD")
                .build();
    }
}
```

위치: `src/test/java/com/erdsketch/support/TestDataFactory.java`

---

### 2.5 `MockUserSupport` — UserPrincipal 목 유틸리티

```java
package com.erdsketch.support;

import com.erdsketch.auth.UserPrincipal;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.UUID;

public class MockUserSupport {

    /**
     * MockMvc 요청에 인증된 사용자를 설정한다.
     * 사용 예: mockMvc.perform(get("/api/v1/workspaces").with(MockUserSupport.mockUser(userId)))
     */
    public static RequestPostProcessor mockUser(UUID userId) {
        UserPrincipal principal = new UserPrincipal(userId, userId + "@test.com", "hash");
        return SecurityMockMvcRequestPostProcessors.user(principal);
    }

    public static RequestPostProcessor mockUser() {
        return mockUser(UUID.randomUUID());
    }
}
```

위치: `src/test/java/com/erdsketch/support/MockUserSupport.java`

---

## 3. 레이어별 테스트 패턴

### 3.1 Repository 테스트

```java
package com.erdsketch.workspace;

import com.erdsketch.support.BaseRepositoryTest;
import com.erdsketch.support.TestDataFactory;
import com.erdsketch.user.User;
import com.erdsketch.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import static org.assertj.core.api.Assertions.assertThat;

class WorkspaceRepositoryTest extends BaseRepositoryTest {

    @Autowired TestEntityManager em;
    @Autowired WorkspaceRepository workspaceRepository;
    @Autowired UserRepository userRepository;

    @Test
    void 워크스페이스를_소유자로_조회한다() {
        User owner = em.persistAndFlush(TestDataFactory.user());
        em.persistAndFlush(TestDataFactory.workspace(owner));
        em.clear();

        var workspaces = workspaceRepository.findByOwnerId(owner.getId());

        assertThat(workspaces).hasSize(1);
    }
}
```

---

### 3.2 Service 테스트

```java
package com.erdsketch.workspace;

import com.erdsketch.auth.UserPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class WorkspaceServiceTest {

    @Mock WorkspaceRepository workspaceRepository;
    @Mock WorkspaceMemberRepository memberRepository;
    @InjectMocks WorkspaceService workspaceService;

    @Test
    void 워크스페이스_목록을_조회한다() {
        UUID userId = UUID.randomUUID();
        given(memberRepository.findByUserId(userId)).willReturn(List.of());

        // workspaceService.getMyWorkspaces(principal) 호출 후 검증
    }
}
```

---

### 3.3 Controller 테스트 (`@WebMvcTest`)

```java
package com.erdsketch.workspace;

import com.erdsketch.config.TestSecurityConfig;
import com.erdsketch.support.MockUserSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WorkspaceController.class)
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
class WorkspaceControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean WorkspaceService workspaceService;

    @Test
    void 내_워크스페이스_목록을_조회한다() throws Exception {
        UUID userId = UUID.randomUUID();
        given(workspaceService.getMyWorkspaces(any())).willReturn(List.of());

        mockMvc.perform(get("/api/v1/workspaces")
                        .with(MockUserSupport.mockUser(userId))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}
```

---

### 3.4 통합 테스트 (`@SpringBootTest`)

```java
package com.erdsketch.workspace;

import com.erdsketch.support.BaseIntegrationTest;
import com.erdsketch.support.MockUserSupport;
import com.erdsketch.support.TestDataFactory;
import com.erdsketch.user.User;
import com.erdsketch.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class WorkspaceIntegrationTest extends BaseIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired WorkspaceRepository workspaceRepository;

    @Test
    void 워크스페이스를_생성한다() throws Exception {
        User user = userRepository.saveAndFlush(TestDataFactory.user());

        mockMvc.perform(post("/api/v1/workspaces")
                        .with(MockUserSupport.mockUser(user.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "My Workspace", "slug": "my-ws"}
                                """))
                .andExpect(status().isCreated());
    }
}
```

---

## 4. H2-PostgreSQL 호환성 이슈 및 해결책

| 이슈 | 원인 | 해결 |
|------|------|------|
| `JSONB` 타입 인식 실패 | H2가 JSONB를 모름 | JDBC URL에 `INIT=CREATE DOMAIN IF NOT EXISTS JSONB AS CLOB` 추가 |
| `BYTEA` 타입 오류 | H2의 BINARY 타입과 다름 | H2 PostgreSQL 모드에서 자동 처리됨 (문제 발생 시 `@Column(columnDefinition = "BINARY VARYING")` 사용) |
| UUID 생성 전략 | `GenerationType.UUID`는 H2에서 지원 | 정상 동작, 추가 설정 불필요 |
| `jsonb_extract_path` 등 PostgreSQL 함수 | H2 미지원 | JPQL/Criteria API 사용, 네이티브 쿼리는 테스트에서 배제 |
| 복합키 엔티티(`WorkspaceMember`) | `@IdClass` 처리 순서 | 부모 엔티티(`Workspace`, `User`)를 먼저 `persist`한 후 `WorkspaceMember` persist |
| `UserPrincipal` 인증 | `@AuthenticationPrincipal` 바인딩 | `SecurityMockMvcRequestPostProcessors.user(principal)` 사용 (`MockUserSupport.mockUser()`) |

---

## 5. 체크리스트

테스트 환경 설정 완료 여부 확인:

- [ ] `src/test/resources/application-test.yml` — H2 설정 (JSONB DOMAIN, Flyway disabled)
- [ ] `src/test/java/com/erdsketch/config/TestSecurityConfig.java`
- [ ] `src/test/java/com/erdsketch/support/BaseIntegrationTest.java`
- [ ] `src/test/java/com/erdsketch/support/BaseRepositoryTest.java`
- [ ] `src/test/java/com/erdsketch/support/TestDataFactory.java`
- [ ] `src/test/java/com/erdsketch/support/MockUserSupport.java`
- [ ] `mvn test -pl backend` 실행 시 컨텍스트 로딩 성공

---

## 6. 빠른 실행 명령

```bash
# 전체 테스트
mvn test -pl backend

# 특정 테스트 클래스
mvn test -pl backend -Dtest=WorkspaceRepositoryTest

# 특정 프로필로 실행
mvn test -pl backend -Dspring.profiles.active=test
```
