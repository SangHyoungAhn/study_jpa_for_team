# @Modifying 벌크 연산과 `@Transactional(REQUIRES_NEW)` — 위험과 권장 패턴

> 4주차 문서 530~532줄의 "가장 견고한 패턴 = 벌크를 `REQUIRES_NEW` 로 격리 + 플래그 ON"
> 문장에 대한 보충. `REQUIRES_NEW` 는 한 문제(clear의 부작용)를 풀어주는 대신
> **여러 위험을 새로 들여온다.** 상황별 예시 코드로 정리한다.

전부 4단계 벌크 메서드(`addActivityPointToAll`)를 기준으로 한다.

[TOC]

---

## 상황 1. 커넥션 풀 셀프 데드락

```java
// 풀 크기를 일부러 작게: spring.datasource.hikari.maximum-pool-size=10

@Service
public class EventService {

    private final PointBulkService pointBulkService;

    @Transactional                                   // ① 바깥 TX 시작 → 커넥션 C1 점유
    public void runEvent() {
        // ... 여기서 이미 C1을 쥐고 있다 ...
        pointBulkService.addPointToAll(100);         // ② REQUIRES_NEW → C2를 추가로 요청
    }                                                //    C1은 suspend 상태로 *반납 안 됨*
}

@Service
public class PointBulkService {
    @Transactional(propagation = Propagation.REQUIRES_NEW)  // 새 트랜잭션 = 새 커넥션 필요
    public void addPointToAll(int amount) {
        memberRepository.addActivityPointToAll(amount);
    }
}
```

**왜 터지나:** `REQUIRES_NEW` 는 바깥 트랜잭션을 *중단(suspend)* 하지만, **중단된 바깥은 자기 커넥션을 반납하지 않고 계속 쥔다.** 즉 요청 1건이 `C1 + C2` 두 개를 동시에 점유한다. `runEvent` 가 **동시에 10건** 들어오면 → 10개 요청이 각자 C1을 쥔 채 C2를 기다림 → 풀에 남은 커넥션 0 → 아무도 진행 못 함.

```
요청1: C1점유 → C2대기 ┐
요청2: C2점유 → C2대기 │  풀(10개) 전부 C1들로 소진
...                    ├─ 남은 커넥션 없음 → 전원 timeout까지 멈춤
요청10:C10점유→ C2대기 ┘
```

> DB 락 데드락이 아니라 **애플리케이션 쪽 커넥션 고갈**이라 DB 로그엔 안 찍힌다.
> 트래픽이 몰리는 순간에만 전체가 굳어서 재현·추적이 가장 까다로운 유형.

---

## 상황 2. DB 행 락 데드락 — 바깥과 안쪽이 같은 행을 건드림

```java
@Service
public class MemberService {

    private final PointBulkService pointBulkService;

    @Transactional
    public void promote(Long id) {
        Member m = memberRepository.findById(id).orElseThrow();
        m.addActivityPoint(50);          // ① 1번 회원 행 변경
        memberRepository.flush();        // ② 강제 flush → 1번 행 X락 점유 (commit 전이라 락 유지)

        pointBulkService.addPointToAll(100);  // ③ REQUIRES_NEW: 전체 UPDATE에 1번 행도 포함
    }                                          //    → 1번 행 락을 기다림. 근데 그 락은 바깥(suspend)이 쥠
}
```

**왜 터지나:**
- 안쪽 벌크: 1번 행을 UPDATE하려고 **바깥이 쥔 X락**을 기다림
- 바깥: 안쪽이 끝나야 재개되는데, 안쪽은 영원히 대기

→ 교착. DB의 lock wait timeout으로 한쪽이 깨지며 예외. **"벌크는 전체 행을 건드린다"** 와 **"바깥도 그중 일부를 이미 만졌다"** 가 겹치면 언제든 발생.

---

## 상황 3. 원자성이 깨진다 — 롤백이 안 따라옴

```java
@Service
public class SettlementService {

    private final PointBulkService pointBulkService;

    @Transactional
    public void settle() {
        pointBulkService.addPointToAll(100);   // ① REQUIRES_NEW → 여기서 *이미 commit* 됨

        if (somethingWrong()) {
            throw new IllegalStateException();  // ② 바깥 TX 롤백
        }
    }
}
```

```
정상 기대:  ①과 ②가 한 묶음 → 실패하면 둘 다 없던 일
실제 결과:  ① 벌크는 이미 커밋되어 DB에 남음 / ②만 롤백 → 포인트만 올라간 채 정합성 깨짐
```

**왜:** `REQUIRES_NEW` 는 독립 커밋이라 바깥 롤백의 영향을 안 받는다. 버그가 아니라 **"이 벌크는 바깥과 별개 거래다"라고 선언한 것.** (감사 로그처럼 "실패해도 남아야 하는" 데이터엔 오히려 이게 정답)

---

## 상황 4. `flushAutomatically` 가 REQUIRES_NEW에선 무력

```java
@Service
public class MemberService {

    private final PointBulkService pointBulkService;

    @Transactional
    public void update() {
        Member m = memberRepository.findById(1L).orElseThrow();
        m.addActivityPoint(50);          // ① 바깥 *컨텍스트*에만 기록 (아직 DB 반영 X, commit 전)

        pointBulkService.addPointToAll(100);  // ② REQUIRES_NEW: 별도 컨텍스트 + 별도 커넥션
    }
}

@Service
public class PointBulkService {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Modifying(flushAutomatically = true)   // ← 안쪽 컨텍스트를 flush. 근데 바깥의 +50은 안쪽에 *없다*
    @Query("UPDATE Member m SET m.activityPoint = m.activityPoint + :amount")
    int addPointToAll(@Param("amount") int amount);
}
```

**왜 무력한가:**
- `flushAutomatically` 는 **벌크가 도는 컨텍스트(=안쪽)** 의 dirty를 flush한다. 그런데 바깥의 `+50` 은 **바깥 컨텍스트**에 있으니 flush 대상이 아님.
- 설령 바깥이 flush했어도 **commit 전**이라, `READ_COMMITTED` 격리에서 안쪽 트랜잭션은 그 값을 **못 본다.**

```
바깥: activityPoint +50 (uncommitted)
안쪽: UPDATE ... +100  ← 바깥의 +50을 못 본 "옛날 값" 기준으로 실행
결과: +50이 반영 안 되거나 덮어써질 수 있음
```

> 같은 트랜잭션(REQUIRES_NEW 없음)이었다면 `flushAutomatically` 가 의도대로 동작했을 텐데,
> **격리하는 순간 이 플래그의 의미가 사라지는** 게 핵심.
> 반면 `clearAutomatically` 는 격리와 궁합이 좋다(clear가 안쪽 컨텍스트만 비움).

---

## 상황 5. 프록시 셀프 인보케이션 — REQUIRES_NEW가 조용히 무시됨

```java
@Service
public class MemberService {

    @Transactional
    public void update() {
        addPointToAll(100);   // ❌ this.addPointToAll() — 프록시를 안 거침
    }                          //    → REQUIRES_NEW 무시, 그냥 바깥 트랜잭션에 합류

    @Transactional(propagation = Propagation.REQUIRES_NEW)  // 붙어있어도 적용 안 됨!
    public void addPointToAll(int amount) {
        memberRepository.addActivityPointToAll(amount);
    }
}
```

**왜:** `@Transactional` 은 스프링이 만든 **프록시**가 메서드를 가로채서 동작한다. 같은 객체 안에서 `this.method()` 로 부르면 프록시를 안 거치므로 어노테이션이 **전부 무시**된다.

```java
// ✅ 해결: 다른 빈으로 분리해서 주입받아 호출 → 프록시 경유
@Service
public class MemberService {
    private final PointBulkService pointBulkService;   // 별도 빈

    @Transactional
    public void update() {
        pointBulkService.addPointToAll(100);   // 프록시 경유 → REQUIRES_NEW 적용됨
    }
}
```

---

## 권장 패턴 (좋은 순서대로)

### ✅ 패턴 A — 구조로 푼다 (제일 권장): 벌크를 *맨 마지막*에

```java
@Service
public class MemberService {

    @Transactional
    public void monthlyReset() {
        // ... 앞에서 조회/수정 다 끝냄 ...

        memberRepository.addActivityPointToAll(100);  // 맨 마지막 작업
        // 이후 이 데이터를 다시 안 읽음 → 컨텍스트가 곧 소멸
    }   // 트랜잭션 끝 → 컨텍스트 소멸. stale 엔티티를 쓸 일이 없다
}
```

**왜 좋은가:** `REQUIRES_NEW` 도, `clearAutomatically` 도 필요 없다. stale 문제는 "벌크 후에 옛날 캐시를 다시 읽을 때" 생기는데, **다시 안 읽으면 문제 자체가 없다.** 데드락·원자성 위험 0. (반대로 **맨 처음**, 엔티티 로딩 전에 두는 것도 같은 효과)

### ✅ 패턴 B — 같은 트랜잭션 + `clearAutomatically` 만

벌크 후 같은 트랜잭션에서 **다시 조회해야 할 때:**

```java
@Service
public class MemberService {

    @Transactional
    public void process() {
        memberRepository.addActivityPointToAll(100);   // 벌크 (DB 직행)

        // ↓ 다시 읽기 — clearAutomatically로 캐시를 비웠으니 DB에서 최신값 조회
        Member m = memberRepository.findById(1L).orElseThrow();
        System.out.println(m.getActivityPoint());      // 최신값 보장
    }
}

// Repository
@Modifying(clearAutomatically = true)   // 벌크 실행 후 컨텍스트 비움
@Query("UPDATE Member m SET m.activityPoint = m.activityPoint + :amount")
int addActivityPointToAll(@Param("amount") int amount);
```

**왜 좋은가:** `REQUIRES_NEW` 가 없으니 커넥션 2개·독립 커밋·프록시 함정이 전부 없다. 단, **벌크 *전*에 들고 있던 다른 엔티티는 전부 detach**되니, 벌크 이전에 로딩해 둔 엔티티를 이후에 안 만진다는 전제가 필요하다. (만진다면 패턴 A로 순서를 바꾸는 게 낫다.)

### ⚠️ 패턴 C — 정말 "독립 커밋"이 필요할 때만 REQUIRES_NEW

"바깥이 실패해도 이 기록은 **반드시 남아야 한다**"(감사 로그, 실패 카운터 등):

```java
@Service
public class AuditService {

    // (a) 별도 빈으로 분리 → 프록시 경유 (상황 5 회피)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordAttempt(Long memberId) {
        // (d) 롤백돼도 남는 게 *의도*임을 명시
        // 바깥 트랜잭션 성패와 무관하게 시도 이력은 보존되어야 함
        auditRepository.insertAttemptLog(memberId);
        // (c) 바깥이 만지는 행과 겹치지 않는 테이블만 건드림 (상황 2 회피)
    }
}
```

```properties
# (b) 커넥션 풀을 동시성보다 충분히 크게 (상황 1 회피)
spring.datasource.hikari.maximum-pool-size=20
```

**왜 이때만:** `REQUIRES_NEW` 의 진짜 용도는 "stale 캐시 방지"가 아니라 **"바깥과 운명을 분리한다"** 이다. 그 분리가 *목적*일 때만 (a)~(d) 4종 세트와 함께 쓴다.

---

## 한 줄 요약

> stale 캐시 때문에 격리가 필요하다고 느껴지면 보통은 **패턴 A(순서 바꾸기)나 B(clear만)로 충분**하고,
> `REQUIRES_NEW` 는 **"독립 커밋이 *기능 요구사항*일 때"만** 꺼내는 카드다.
> 데드락(커넥션 풀·DB 락)·원자성 깨짐·flush 무력화·프록시 함정까지 보면,
> 문서의 "가장 견고한 패턴"은 오히려 **"최후의 수단"** 에 가깝다.
