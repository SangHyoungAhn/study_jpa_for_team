# `@DataJpaTest` vs `@SpringBootTest` 언제 붙이나

스프링 테스트에서 어떤 테스트 어노테이션을 붙일지 고르는 기준을 정리한다.

> 관련: `MemberRepositoryTest`(`@DataJpaTest` 예시), `StudyjpaApplicationTests`(`@SpringBootTest` 예시), `jpa-auditing-guide.md`(Auditing 설정과 슬라이스 테스트의 관계).

---

## 1. 핵심 기준

> **"이 테스트가 검증하려는 게 어느 계층인가?"**

원칙은 하나다 — **필요한 최소한의 컨텍스트만 띄운다.**

`@SpringBootTest`를 기본값처럼 쓰면 당장은 편하지만, 테스트가 수십 개 쌓였을 때

- 전체 테스트 실행 시간이 감당이 안 되고,
- 테스트가 깨졌을 때 어디가 문제인지 추적이 어려워진다.

---

## 2. `@DataJpaTest` — JPA 계층만 검증할 때

`@DataJpaTest`는 **슬라이스(slice) 테스트**다. 전체 애플리케이션이 아니라 JPA 관련 빈만 골라서 띄운다.

**로딩되는 것**

- `@Entity` 스캔, Spring Data Repository, `EntityManager`, `DataSource`

**로딩 안 되는 것**

- Service, Controller, `@Component` 등

### 왜 이걸 쓰나

1. **빠르다.** 전체 컨텍스트를 띄우면 프로젝트가 커질수록 테스트 시작이 수 초~수십 초 걸리는데, JPA 빈만 띄우면 훨씬 가볍다.
2. **실패 원인이 좁혀진다.** 이 테스트가 깨지면 "엔티티 매핑이나 쿼리 문제"라고 바로 알 수 있다. 전체 컨텍스트 테스트는 보안 설정, 다른 빈 초기화 실패 등 무관한 이유로도 깨진다. (유지보수 관점에서 제일 큰 장점.)
3. **자동 롤백.** `@DataJpaTest` 안에 `@Transactional`이 메타 어노테이션으로 포함되어 있어서, 각 테스트가 끝나면 자동으로 롤백된다. `em.persist(dept)` 한 데이터가 다음 테스트에 안 남는 이유가 이것.

### 예시 (현재 프로젝트)

```java
@DataJpaTest
public class MemberRepositoryTest {

    @Autowired
    MemberRepository memberRepository;

    @PersistenceContext
    EntityManager em;

    @Test
    @DisplayName("같은 부서 멤버 2명")
    void findByDeptId_같은부서2명(){
        //given
        Department dept = new Department("D011", "비즈테크팀");
        em.persist(dept);

        memberRepository.save(new Member("안상형","shahn0718@donga.com", dept));
        memberRepository.save(new Member("장현수","wkdgustn@donga.com", dept));

        //when
        List<Member> members = memberRepository.findByDeptId(dept.getId());

        //then
        assertThat(members).hasSize(2);
    }
}
```

`findByDeptId` 쿼리가 제대로 동작하는지만 보는 테스트 → `@DataJpaTest`가 정확한 선택.

---

## 3. `@SpringBootTest` — 여러 계층을 관통하는 통합 테스트일 때

`@SpringBootTest`는 **전체 애플리케이션 컨텍스트**를 띄운다. Service, Repository, 설정 빈이 전부 올라온다.

### 이걸 쓰는 경우

- **Service가 여러 빈과 협력하는 흐름**을 실제 빈 조립 그대로 검증하고 싶을 때
  (예: 회원 가입 → 검증 → 저장 → 이벤트 발행이 한 트랜잭션에서 잘 도는지)
- **설정 자체가 잘 조립되는지** 확인하고 싶을 때
  (`StudyjpaApplicationTests`의 `contextLoads()`가 그 역할)

### 주의

`@SpringBootTest`에는 `@Transactional`이 **포함되어 있지 않다.** 롤백을 원하면 직접 붙여야 한다.

---

## 4. 정리표

| 테스트 대상 | 어노테이션 | 이유 |
|---|---|---|
| Repository, 엔티티 매핑, 쿼리 | `@DataJpaTest` | JPA 빈만 필요. 빠르고 실패 원인이 좁음 |
| Controller (요청/응답, 검증) | `@WebMvcTest` | MVC 빈만 필요. Service는 `@MockitoBean`으로 대체 |
| Service 단독 로직 | 어노테이션 없음 (순수 Mockito) | 스프링 컨텍스트 자체가 불필요 |
| 여러 계층 관통 흐름, 설정 조립 | `@SpringBootTest` | 실제 빈 조립 그대로 검증해야 의미 있음 |

---

## 5. `@WebMvcTest` — 컨트롤러(웹 계층)만 검증할 때

`@WebMvcTest`도 **슬라이스 테스트**다. `@DataJpaTest`가 JPA 빈만 띄우듯, `@WebMvcTest`는 **MVC 관련 빈만** 띄운다.

**로딩되는 것**

- 지정한 Controller, `@ControllerAdvice`, `Filter`, `MockMvc`, JSON 직렬화(`Jackson`) 등

**로딩 안 되는 것**

- Service, Repository, `@Component`, JPA 빈 등

### 왜 이걸 쓰나

컨트롤러가 검증하는 건 **HTTP 요청/응답 자체**다 — URL 매핑, 상태 코드, 요청 파라미터 바인딩, `@Valid` 검증, JSON 변환. 이건 Service나 DB 없이도 검증할 수 있다.
그래서 협력 대상인 Service는 진짜 빈을 띄우는 대신 **`@MockitoBean`으로 가짜를 주입**해서, 컨트롤러 로직만 떼어내 테스트한다.

> `@MockitoBean`은 스프링 부트 3.4부터의 어노테이션이다. 그 이전 버전에서는 `@MockBean`을 쓴다.

### 예시 (현재 프로젝트 구조 기준)

`MemberController`가 아래처럼 조회 API를 가진다고 가정하면:

```java
@RestController
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @GetMapping("/members/{id}")
    public MemberResponse findOne(@PathVariable Long id) {
        return memberService.findOne(id);
    }
}
```

웹 계층 테스트는 이렇게 작성한다:

```java
@WebMvcTest(MemberController.class)   // 띄울 컨트롤러를 콕 집어준다
public class MemberControllerTest {

    @Autowired
    MockMvc mockMvc;                  // 실제 서버를 띄우지 않고 요청을 흉내 낸다

    @MockitoBean
    MemberService memberService;      // 진짜 빈이 아니라 가짜를 주입

    @Test
    @DisplayName("회원 단건 조회 - 200 OK")
    void findOne_정상() throws Exception {
        //given
        given(memberService.findOne(1L))
                .willReturn(new MemberResponse(1L, "안상형", "shahn0718@donga.com"));

        //when & then
        mockMvc.perform(get("/members/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("안상형"))
                .andExpect(jsonPath("$.email").value("shahn0718@donga.com"));
    }
}
```

핵심은 **`memberService`가 실제로 DB를 조회하지 않는다**는 점이다. `given(...).willReturn(...)`으로 "이 입력이 오면 이걸 돌려줘라"라고 미리 짜놓고, 컨트롤러가 그 결과를 올바른 JSON/상태 코드로 변환하는지만 본다.

### `@DataJpaTest`와 대비

| | `@DataJpaTest` | `@WebMvcTest` |
|---|---|---|
| 띄우는 계층 | JPA(Repository, EntityManager) | MVC(Controller, MockMvc) |
| 협력 빈 처리 | 실제 DB(임베디드) 사용 | Service를 `@MockitoBean`으로 대체 |
| 검증 대상 | 쿼리·매핑이 맞는가 | 요청/응답·상태 코드·검증이 맞는가 |

---

## 6. 슬라이스 어노테이션 전체 목록

전부 같은 원리다 — **필요한 계층의 빈만 골라 띄운다.** 내부적으로 `@…Test`마다 "이 계층에 필요한 자동설정(AutoConfiguration)만 켜는" 필터가 걸려 있다.

| 어노테이션 | 띄우는 계층 | 주로 검증하는 것 |
|---|---|---|
| `@DataJpaTest` | JPA (Repository, EntityManager, 임베디드 DB) | 쿼리·엔티티 매핑 |
| `@WebMvcTest` | Spring MVC (Controller, MockMvc, Jackson) | 요청/응답·상태 코드·검증 |
| `@JdbcTest` | `JdbcTemplate` + `DataSource` | JPA 없이 순수 JDBC SQL |
| `@DataJdbcTest` | Spring Data JDBC Repository | Spring Data JDBC 매핑 |
| `@JsonTest` | Jackson/Gson 직렬화 빈 | 객체 ↔ JSON 변환 |
| `@RestClientTest` | `RestTemplate`/`RestClient` + `MockRestServiceServer` | 외부 API 호출 클라이언트 |
| `@WebFluxTest` | Spring WebFlux (`WebTestClient`) | 리액티브 컨트롤러 |
| `@DataMongoTest` | MongoDB Repository | Mongo 매핑·쿼리 |
| `@DataRedisTest` | Redis Repository/Template | Redis 연동 |
| `@DataR2dbcTest` | R2DBC (리액티브 RDB) | 리액티브 DB 접근 |

> 현재 프로젝트(JPA + Spring MVC) 맥락에서 실제로 쓸 건 위 3~4개. 나머지는 "이런 것도 있다" 정도.

### 슬라이스가 *아닌* 것들 (구분해서 알아두기)

- **`@SpringBootTest`** — 슬라이스의 반대. 전체 컨텍스트를 띄운다.
- **`@TestConfiguration` / `@Import`** — 슬라이스에 빠진 빈(예: `JpaAuditingConfig`)을 추가로 끼워 넣을 때. (7번에서 나오는 그것)
- **`@MockitoBean`** — 슬라이스 안에서 협력 빈을 가짜로 대체할 때. (5번 `@WebMvcTest` 예시의 Service 대체)

### 슬라이스에 보조로 붙는 것들

- **`@AutoConfigureMockMvc`** — `@SpringBootTest`에서도 `MockMvc`를 쓰고 싶을 때. (`@WebMvcTest`엔 이미 포함)
- **`@AutoConfigureTestDatabase`** — `@DataJpaTest`가 기본으로 실제 DB를 임베디드 DB로 바꾸는데, `replace = NONE`으로 그걸 꺼서 실제 DB로 테스트하고 싶을 때.

---

## 7. Mock 테스트 vs 실제 빈 테스트 — 어느 쪽을 쓸까

> "Mock 쓰는 것보다 그냥 테스트 코드 짜는 게 좋다"는 취향, 사실 일리가 있다. 둘은 **검증하는 대상이 다르다.**

### 두 방식의 차이

| | Mock 테스트 (`@MockitoBean`/Mockito) | 실제 빈 테스트 (`@DataJpaTest`, `@SpringBootTest`) |
|---|---|---|
| 협력 객체 | 가짜로 대체 (`given().willReturn()`) | 진짜 빈/DB 사용 |
| 검증 대상 | "이 클래스가 협력 객체를 **올바르게 호출**하는가" | "여러 객체가 실제로 **함께 잘 동작**하는가" |
| 속도 | 매우 빠름 (DB·컨텍스트 불필요) | 상대적으로 느림 |
| 깨지는 시점 | 호출 규약이 바뀌면 | 실제 동작이 틀어지면 |

### 핵심은 "무엇을 신뢰하고 싶은가"

- **Mock은 "약속(규약)"을 검증한다.** "컨트롤러가 입력 `1L`을 받으면 `memberService.findOne(1L)`을 호출하고, 그 결과를 JSON으로 잘 바꾼다." → Service가 실제로 뭘 하는지는 안 본다. **그래서 Service에 버그가 있어도 이 테스트는 통과한다.**
- **실제 빈 테스트는 "결과"를 검증한다.** "회원을 저장하고 조회하면 진짜로 2명이 나온다." → 매핑·쿼리·트랜잭션이 실제로 맞물려 돌아가는지를 본다. `MemberRepositoryTest`가 이쪽이다.

### 그래서 권장하는 조합

취향대로 실제 빈 테스트를 기본으로 가도 좋다. 다만 계층별로 **자연스러운 선택**이 있다:

- **Repository / 쿼리** → 실제 빈 테스트(`@DataJpaTest`). Mock으로는 "쿼리가 맞는지"를 검증할 수 없다. (Mock은 SQL을 안 돌린다.)
- **Service 단독 로직** → 둘 다 가능.
  - 분기·예외·계산 로직만 보고 싶으면 → **Mockito**(컨텍스트 없이 가장 빠름).
  - "Service + Repository가 실제로 잘 맞물리나"까지 보고 싶으면 → `@DataJpaTest`에 Service를 `@Import`해서 실제로 돌리는 게 더 믿음직하다. **이쪽이 "그냥 테스트 코드 짜는" 취향에 맞는 방향.**
- **Controller** → `@WebMvcTest` + Mock이 거의 정석. 여기서 실제 Service·DB까지 띄우면 `@SpringBootTest`에 가까워지면서 느려지고 깨지기 쉬워진다.

### 한 줄 결론

> Mock은 "빠르게, 이 클래스만 떼어내" 검증하는 도구다. 실제 동작에 대한 확신은 실제 빈 테스트가 준다.
> 둘은 경쟁이 아니라 **역할 분담**이다 — 단위는 Mock으로 빠르게, 통합은 실제 빈으로 확실하게.

---

## 8. 현재 프로젝트에서 조심할 점 — Auditing과 슬라이스 테스트

엔티티가 `BaseTimeEntity`(Auditing)를 상속하고 있다. `@EnableJpaAuditing`을 어디에 두느냐에 따라 슬라이스 테스트가 영향을 받는다.

- `@EnableJpaAuditing`을 **별도 `@Configuration` 클래스**에 두면 → `@DataJpaTest`는 그 설정 클래스를 안 읽으므로 테스트에서 `createdAt`이 `null`이 된다. 이 경우 테스트 클래스에 `@Import(JpaAuditingConfig.class)`를 붙여줘야 한다.
- `@EnableJpaAuditing`을 **메인 애플리케이션 클래스**에 두면 → `@DataJpaTest`에서는 동작하지만, 나중에 `@WebMvcTest`를 쓸 때 JPA 메타모델이 없어서 컨텍스트 로딩이 깨지는 부작용이 있다.

→ 장기적으로는 **별도 설정 클래스 + `@Import` 조합**이 더 유지보수에 유리하다.
