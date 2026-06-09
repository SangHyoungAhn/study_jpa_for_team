# JPA 입문 — 0~4단계 (주니어용)

**목표:** "왜 JPA를 쓰는지" 공감하고, 첫 엔티티를 만들어 H2에서 직접 저장·조회해 본다.
**범위:** 단일 엔티티까지. 연관관계·복합키 등 심화는 `JPA-curriculum.md`의 5단계 이후 참고.

> **이 자료의 원칙**
> - **결핍 → 처방:** 불편함을 먼저 겪고 해법을 받는다. ("왜 쓰는지" 모르고 외우지 않기)
> - **항상 "왜":** 문법보다 그 선택의 이유를 먼저 이해한다.
> - **눈으로 확인:** 추상 개념은 H2 콘솔에서 SQL·테이블을 직접 보며 익힌다.

---

## 0단계. Java 현대 문법 — 준비운동

JPA 코드 곳곳에 나오므로 먼저 익혀둔다. **왜 먼저?** 이게 익숙하지 않으면 JPA를 배우다가 문법에서 막힌다.

```java
// Optional — "값이 없을 수도 있음"을 안전하게 표현 (findById의 반환 타입)
Optional<Member> found = memberRepository.findById(1L);
Member m = found.orElseThrow();          // 없으면 예외

// Stream — 컬렉션을 선언적으로 가공
List<String> names = members.stream()
        .map(Member::getName)            // 이름만 뽑아
        .toList();

// Lambda — 동작을 값처럼 전달
members.forEach(member -> System.out.println(member.getName()));
```

> 셋의 공통점: **"어떻게(반복문 돌려라)"가 아니라 "무엇을(이름을 뽑아라)"** 을 적게 해준다. JPA도 같은 사고방식이라 손에 익혀두면 좋다.

---

## 1단계. 환경 구축 — H2 (눈으로 보는 DB)

H2는 가볍고, **웹 콘솔로 테이블/데이터를 눈으로 볼 수 있는** 메모리 DB다.

**`application.properties` 예시**
```properties
# H2 웹 콘솔 켜기
spring.h2.console.enabled=true

# 메모리 DB
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.username=sa
spring.datasource.password=

# 엔티티 보고 테이블 자동 생성 (학습용)
spring.jpa.hibernate.ddl-auto=create

# JPA가 날리는 SQL을 콘솔에 출력 (★ 학습의 핵심)
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

- 앱 실행 후 브라우저에서 **`http://localhost:8080/h2-console`** 접속 → `JDBC URL`에 `jdbc:h2:mem:testdb` 입력 → Connect.

> **왜 H2부터, 왜 콘솔을 보나:** 뒤에서 배울 "변경 감지가 UPDATE를 날린다" 같은 개념은 **SQL 로그와 테이블을 눈으로 봐야** 실감 난다. `show-sql=true`로 "내가 객체만 다뤘는데 SQL이 나가네?"를 직접 보는 게 핵심.

---

## 2단계. 왜 JPA를 쓰는가 (진화론)

JPA를 쓰는 이유는 **"JPA가 없던 시절에 뭐가 불편했는지"** 를 보면 와닿는다.

### JPA 이전 — JDBC / MyBatis의 불편함

```java
// SQL을 하나하나 다 적어줘야 한다 (JDBC)
String sql = "INSERT INTO member(name, age, email) VALUES(?, ?, ?)";
pstmt.setString(1, member.getName());
pstmt.setInt(2, member.getAge());
pstmt.setString(3, member.getEmail());
```

불편함 세 가지:
- **반복되는 CRUD SQL** — 테이블마다 `insert/select/update/delete`를 일일이 적는다.
- **컬럼 하나 추가 = 전부 수정** — `Member`에 `phone`이 추가되면 관련 SQL·매핑을 전부 찾아 고쳐야 한다.
- **패러다임 불일치** — 자바는 "객체(참조)"로, DB는 "테이블(외래키)"로 생각한다. 이 간극을 개발자가 매번 손으로 메꾼다. ← **가장 핵심**

### JPA의 해법 — "객체답게" 다루기

**(1) 패러다임 불일치 해결 — 연관관계**
```java
// JDBC 사고: team_id(FK)를 따로 들고 다니며 team을 또 조회
Long teamId = member.getTeamId();

// JPA 사고: 그냥 객체 참조
Team team = member.getTeam();          // 객체 그래프를 자연스럽게 탐색
team.getName();
```

**(2) SQL을 직접 안 짜도 됨 — 생산성**
```java
em.persist(member);                    // 이 한 줄이 INSERT SQL을 자동 생성
// 컬럼이 추가돼도 엔티티 필드만 추가하면 됨 → SQL 수정 없음
```

**(3) 1차 캐시 / 영속성 컨텍스트 — 성능 + 일관성**
```java
Member m1 = em.find(Member.class, 1L); // DB 조회
Member m2 = em.find(Member.class, 1L); // DB 안 감 (1차 캐시에서 반환)
m1 == m2;                              // true — 같은 객체 보장
```

**(4) 변경 감지 (Dirty Checking) — UPDATE가 사라짐**
```java
Member member = em.find(Member.class, 1L);
member.setName("새이름");              // update 코드를 안 적었는데
// 트랜잭션이 끝나면 JPA가 알아서 UPDATE SQL 실행
```

> **한 문장 요약:** JPA는 **"DB를 자바 객체처럼 다루게 해주는"** 기술이다. SQL 반복에서 해방되고(생산성), 객체지향답게 설계할 수 있다(유지보수).
> (더 자세히 → `why-jpa.md`)

---

## 3단계. 첫 엔티티 — `@Entity`, `@Id`

객체 하나를 테이블 하나에 매핑한다. JPA의 가장 기본 단위.

```java
package com.example.spring.studyjpa.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity                                              // ① 이 클래스 = 테이블
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)   // ② JPA용 기본 생성자
public class Member {

    @Id                                              // ③ 기본키(PK)
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // ④ PK 자동 증가
    private Long id;

    private String name;                             // ⑤ 컬럼 (기본 VARCHAR(255))
    private String email;
}
```

**포인트별 왜:**
- **① `@Entity`** — "이 클래스를 테이블로 관리하라"는 표시. 클래스명 `Member` → 테이블 `MEMBER`, 필드 → 컬럼으로 자동 매핑.
- **② `@NoArgsConstructor(PROTECTED)`** — JPA는 DB 행을 객체로 되살릴 때 *빈 객체를 만든 뒤 필드를 채운다.* 그래서 기본 생성자가 필수. `protected`는 "외부에서 함부로 빈 객체 만들지 말라"는 의도.
- **③ `@Id`** — 이 필드가 기본키. JPA는 PK로 객체를 식별한다.
- **④ `@GeneratedValue(IDENTITY)`** — PK 값을 DB의 auto_increment에 맡긴다. 우리가 id를 직접 안 넣어도 됨.
- **⑤ 필드** — 별도 어노테이션 없으면 같은 이름의 컬럼으로. (길이/제약은 `@Column`으로 조정)

> 앱을 실행하고 H2 콘솔에서 `MEMBER` 테이블이 생겼는지 확인해 본다 (`ddl-auto=create` 덕분에 자동 생성).

---

## 4단계. 실습 — Repository로 저장·조회

Spring Data JPA의 `JpaRepository`만 상속하면 기본 CRUD가 공짜로 생긴다.

```java
package com.example.spring.studyjpa.repository;

import com.example.spring.studyjpa.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {
    // 구현 코드 없이 save/findById/findAll/delete... 가 이미 제공됨
}                          // <엔티티, PK타입>
```

> **왜 구현이 없나:** Spring Data JPA가 인터페이스만 보고 **구현체를 런타임에 자동으로 만들어** 준다. 우리는 "무엇을 할지"만 선언하면 된다.

**저장·조회·수정 해보기**
```java
// 저장 — INSERT SQL 자동
Member member = new Member("김철수", "kim@x.com");
memberRepository.save(member);

// 조회 — findById는 Optional 반환 (0단계의 Optional 등장!)
Member found = memberRepository.findById(member.getId()).orElseThrow();

// 수정 — set만 했는데 UPDATE가 나간다 (변경 감지)
found.setName("김영희");
// @Transactional 안에서 트랜잭션이 끝나면 JPA가 UPDATE 자동 실행
```

**확인 포인트 (H2 콘솔 + SQL 로그):**
1. `save` 후 `MEMBER` 테이블에 행이 생겼는가? (`SELECT * FROM MEMBER`)
2. `show-sql` 로그에 `insert ...`, `select ...`, `update ...` 가 찍히는가?
3. 같은 id를 두 번 `findById` 하면 두 번째는 SELECT가 **안 나가는가?** (1차 캐시)

> **왜 이 확인이 중요:** "내가 SQL을 안 썼는데 JPA가 알아서 날린다"를 **눈으로** 보는 순간이 입문의 핵심 체험. 2단계에서 말로 들은 4가지(연관관계·생산성·1차 캐시·변경 감지)를 손으로 확인하는 단계다.

---

## 정리 & 다음 단계

| 단계 | 한 일 |
|---|---|
| 0 | `Optional`/`Stream`/`Lambda` 체력 기르기 |
| 1 | H2 + 웹 콘솔 (SQL·테이블을 눈으로) |
| 2 | 왜 JPA인가 — 패러다임 불일치, 변경 감지 |
| 3 | 첫 엔티티 `@Entity`/`@Id` |
| 4 | Repository로 CRUD + H2에서 확인 |

**다음으로 (심화):**
- **연관관계** (회원↔부서, 게시글↔댓글) → `association-mapping-guide.md`
- **공통 필드 자동화** (생성일/수정일) → BaseEntity
- **복합키 & 연결 엔티티** (좋아요) → `postlike-composite-key-guide.md`
- **전체 구조** → `erd.md`
- 전체 로드맵 → `JPA-curriculum.md`

> **한 줄 요약:** 단일 엔티티를 만들어 H2에 저장·조회하며 **"객체만 다뤘는데 SQL이 자동으로 나간다"** 를 눈으로 확인하는 것이 입문의 전부다. 나머지는 이 위에 쌓는다.
