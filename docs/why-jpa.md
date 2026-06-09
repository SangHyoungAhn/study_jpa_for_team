# 왜 JPA를 쓰는가

JPA를 쓰는 이유는 "JPA가 없던 시절에 뭐가 불편했는지"를 보면 확 와닿는다.

---

## 1. JPA 이전: JDBC / MyBatis의 불편함

순수 JDBC나 MyBatis로 회원 한 명을 저장하면:

```java
// SQL을 사람이 직접 다 적어야 한다
String sql = "INSERT INTO member(name, age, email) VALUES(?, ?, ?)";
pstmt.setString(1, member.getName());
pstmt.setInt(2, member.getAge());
pstmt.setString(3, member.getEmail());
```

근본 문제 세 가지:

- **반복되는 CRUD SQL**: 테이블마다 `insert/select/update/delete`를 손으로 다 적는다.
- **컬럼 하나 추가 = 전부 수정**: `member`에 `phone`이 추가되면 관련 SQL·매핑·VO를 전부 찾아 고쳐야 한다.
- **사고방식 차이(= 패러다임 불일치)**: 자바는 "객체(참조)"로 생각하는데 DB는 "테이블(외래키)"로 생각한다. 둘은 서로 다른 언어를 쓰는 셈이라, 그 사이를 개발자가 매번 손으로 "번역"해줘야 한다. ← **가장 핵심**

---

## 2. JPA를 쓰는 진짜 이유: "객체답게" 다루기

### (1) 사고방식 차이 해결 — 연관관계

자바와 DB는 "관계"를 표현하는 방식 자체가 다르다.

```java
// 자바식 사고: Member가 Team을 "가지고" 있다
class Member {
    Long   id;
    String name;
    Team   team;   // ← 객체를 직접 참조
}
```

```sql
-- DB식 사고: MEMBER 테이블이 TEAM_ID라는 "값"을 가진다
CREATE TABLE MEMBER (
    MEMBER_ID  BIGINT,
    NAME       VARCHAR(255),
    TEAM_ID    BIGINT     -- ← 객체가 아니라 그냥 숫자(FK)
);
```

DB는 `Team` 객체를 모른다. 그냥 `TEAM_ID = 1`이라는 숫자만 안다.
그래서 JPA가 없으면, 이 "번역"을 개발자가 매번 손으로 해야 한다.

**Before — 번역을 손으로 하는 코드 (JDBC / MyBatis)**

```java
// 저장: 객체(team)에서 외래키(team_id)를 꺼내 넣는 변환을 직접
public void save(Member member) {
    String sql = "INSERT INTO MEMBER(NAME, TEAM_ID) VALUES(?, ?)";
    pstmt.setString(1, member.getName());
    pstmt.setLong(2, member.getTeam().getId());  // ← 객체 → 숫자 수동 변환
}

// 조회: 외래키(team_id)를 다시 객체(team)로 복원하는 변환을 직접
public Member find(Long memberId) {
    Member member = ...;   // SELECT * FROM MEMBER  WHERE MEMBER_ID = ?
    Team   team   = ...;   // SELECT * FROM TEAM    WHERE TEAM_ID   = ?  (또 조회)
    member.setTeam(team);  // ← 숫자 → 객체 수동 연결 (매번 반복!)
    return member;
}
```

**After — 번역을 JPA가 대신**

```java
// 매핑만 선언해두면
@Entity
class Member {
    @Id @GeneratedValue
    Long id;
    String name;

    @ManyToOne                       // "다대일 관계"라고 알려주고
    @JoinColumn(name = "TEAM_ID")    // "FK 컬럼은 TEAM_ID"라고 알려주면
    Team team;                       // 변환은 JPA가 전담
}

// 저장: 객체 그대로
member.setTeam(team);
em.persist(member);            // INSERT ... TEAM_ID ... 를 JPA가 생성

// 조회: 객체 그래프 그대로 탐색
Member member = em.find(Member.class, memberId);
Team   team   = member.getTeam();   // TEAM 조회 + 연결을 JPA가 처리
team.getName();
```

DB에는 외래키(`team_id`)지만 자바에서는 진짜 객체(`Team`)처럼 다룰 수 있다. JPA가 주는 가장 큰 가치.

### (2) SQL을 직접 안 짜도 됨 — 생산성

```java
// 이 한 줄이 INSERT SQL을 자동 생성
em.persist(member);

// 컬럼이 추가돼도 엔티티 필드만 추가하면 됨 → SQL 수정 없음
```

### (3) 1차 캐시 / 영속성 컨텍스트 — 성능 + 일관성

```java
Member m1 = em.find(Member.class, 1L);  // DB 조회
Member m2 = em.find(Member.class, 1L);  // DB 안 감 (1차 캐시에서 반환)
m1 == m2;                                // true — 같은 객체 보장
```

같은 트랜잭션 안에서 같은 데이터는 같은 객체로 보장되고, DB 조회도 줄어든다.

### (4) 변경 감지 (Dirty Checking) — update SQL이 사라짐

```java
Member member = em.find(Member.class, 1L);
member.setName("새이름");      // update 코드를 안 적었는데
// 트랜잭션 끝나면 JPA가 알아서 UPDATE SQL 실행
```

값만 바꾸면 JPA가 변경을 감지해서 알아서 `UPDATE`를 날린다. "수정 = 객체 값 변경"이라는 자바다운 코드가 된다.

---

## 3. 한 문장 요약

> **JPA는 "DB를 자바 객체처럼 다루게 해주는" 기술이다.**
> SQL 반복에서 해방되고(생산성), 객체지향답게 설계할 수 있다(유지보수).

그래서 기본 CRUD와 연관관계 탐색은 JPA로 가고, JPA가 오히려 불편해지는 복잡한 통계/튜닝 쿼리만 MyBatis로 뺀다.
(→ `dynamic-query-guide.md` 참고)

---

## 4. 짚어둘 점 — 장점은 양날의 검

위 4가지 장점은 편한 만큼 위험할 수도 있다. 특히:

- **변경 감지**: 의도치 않은 값 변경도 자동 UPDATE → 사이드이펙트
- **객체 참조(지연 로딩)**: 무심코 탐색하다 N+1 문제 발생

→ 이 부분은 이후 더 깊이 다룰 주제.
