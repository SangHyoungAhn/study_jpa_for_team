# JPA Auditing & `AuditorAware` 가이드

게시판 도메인의 **감사 컬럼**(`createdAt`/`updatedAt`/`createdBy`/`updatedBy`)을
직접 채우지 않고 **자동으로 기록**되게 하는 JPA Auditing 기능을 정리한다.

> 관련: 공통 필드 상속 구조 `erd.md`(짚어둘 점), `BaseEntity`/`BaseTimeEntity`.

---

## 1. 감사 컬럼이란

비즈니스 데이터(제목·내용 등)가 아니라 **"언제, 누가 만들고 바꿨는지"를 추적하는 메타데이터** 컬럼.

| 컬럼 | 의미 | 어노테이션 |
|---|---|---|
| `createdAt` | 생성 시각 | `@CreatedDate` |
| `updatedAt` | 마지막 수정 시각 | `@LastModifiedDate` |
| `createdBy` | 생성자(회원 ID) | `@CreatedBy` |
| `updatedBy` | 마지막 수정자(회원 ID) | `@LastModifiedBy` |

이 4개는 거의 모든 테이블에 반복되므로 `@MappedSuperclass` 부모 클래스에 한 번만 정의하고
**상속**해서 쓴다. 사람이 손으로 채우지 않고 **JPA가 insert/update 시점에 자동으로** 채운다.

```
BaseTimeEntity   → createdAt, updatedAt          (시간만)
   └ BaseEntity  → + createdBy, updatedBy         (작성자까지)
```

---

## 2. 기능을 켜는 부품 3가지

감사 기능은 부품 3개가 **모두 연결돼야** 동작한다. 하나라도 빠지면 컬럼이 `null`로 남는다.

### ① 필드 표시 — `@CreatedDate` 등 (엔티티)

```java
@CreatedDate
@Column(updatable = false)   // 생성 시각은 수정되면 안 되므로 update 대상에서 제외
private LocalDateTime createdAt;

@LastModifiedDate
private LocalDateTime updatedAt;
```

"이 칸은 생성/수정 정보를 담는다"고 **표시**만 한 상태.

### ② 리스너 위임 — `@EntityListeners` (엔티티)

```java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public class BaseTimeEntity { ... }
```

"이 엔티티가 저장·수정될 때 `AuditingEntityListener`가 끼어들어 위 필드를 채워라"고 **위임**.

### ③ 기능 활성화 — `@EnableJpaAuditing` (메인 클래스)

```java
@EnableJpaAuditing
@SpringBootApplication
public class StudyjpaApplication { ... }
```

①②는 "준비"일 뿐이고, **이 스위치가 있어야 리스너가 실제로 등록·작동**한다.
빠지면 `save()` 해도 `createdAt`이 계속 `null`이다.

### 동작 흐름

```
post.save() 호출
   → AuditingEntityListener가 가로챔        (② @EntityListeners)
   → 현재 시각을 createdAt/updatedAt에 채움   (① @CreatedDate / @LastModifiedDate)
   → 현재 사용자를 createdBy/updatedBy에 채움  (④ AuditorAware ← 아래 3절)
   → DB에 insert
```

---

## 3. `AuditorAware` — "지금 사용자가 누구인가"를 알려주는 부품

### 왜 시간과 다르게 별도 설정이 필요한가

- **시각**(`@CreatedDate`)은 Spring이 **시계만 보면** 알 수 있다 → 자동.
- **작성자**(`@CreatedBy`)는 "지금 로그인한 사람이 누구인지"를 Spring이 **스스로 알 수 없다.**
  그래서 "현재 사용자를 어떻게 알아내는지"를 **개발자가 알려줘야** 한다.
  그 통로가 `AuditorAware<T>` 인터페이스다. (`T`는 `createdBy` 필드 타입 = 여기선 `Long`)

`AuditorAware` 빈이 없으면 `createdBy`/`updatedBy`는 **에러 없이 그냥 `null`** 로 남는다.

### 빈 등록

```java
@Configuration
public class JpaAuditingConfig {

    @Bean
    public AuditorAware<Long> auditorProvider() {
        // 저장/수정 시점마다 호출되어 "현재 사용자 ID"를 반환한다.
        // 로그인 기능이 아직 없으면 우선 빈 값으로 둔다.
        return () -> Optional.empty();
    }
}
```

> 메인 클래스의 `@EnableJpaAuditing` 은 그대로 두고, `AuditorAware` 빈만 추가하면 된다.

### 로그인(Spring Security) 붙인 뒤

인증이 들어오면 SecurityContext에서 현재 사용자 ID를 꺼내 반환하도록 채운다.

```java
@Bean
public AuditorAware<Long> auditorProvider() {
    return () -> {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return Optional.empty();          // 비로그인 요청(배치, 익명 등)
        }
        Long memberId = ((LoginUser) auth.getPrincipal()).getId();  // 프로젝트 타입에 맞게
        return Optional.of(memberId);
    };
}
```

핵심: **이 람다는 저장/수정이 일어날 때마다 호출**된다. 그래서 "그 시점의 로그인 사용자"가
자동으로 `createdBy`/`updatedBy`에 들어간다.

---

## 4. 자주 겪는 함정

- **`@EnableJpaAuditing`을 빼먹음** → 시간·작성자 전부 `null`. (가장 흔함)
- **`AuditorAware` 빈이 없음** → 시간은 채워지는데 `createdBy`/`updatedBy`만 `null`.
- **`createdAt`에 `@Column(updatable = false)`를 안 검** → 수정할 때 생성 시각이 덮어써질 위험.
- **테스트에서 `@DataJpaTest`만 쓰고 Auditing 설정을 안 올림** → 슬라이스 테스트엔
  `@EnableJpaAuditing`이 자동 포함되지 않으므로 별도 `@Import`가 필요할 수 있다.

---

## 5. 한 줄 요약

> `@EnableJpaAuditing`은 감사 기능의 **전원 스위치**(시각 자동기록까지 완성),
> `AuditorAware`는 **"현재 사용자가 누구인지"를 알려주는 부품**(작성자 기록 완성).
> 둘이 함께 있어야 `createdAt`·`createdBy`가 모두 자동으로 채워진다.
