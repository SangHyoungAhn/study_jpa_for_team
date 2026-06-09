# 연관관계 매핑 — 단방향에서 양방향까지

`Member`(회원)와 `Department`(부서)를 예시로, 연관관계를 **단방향으로 시작해서 필요할 때 양방향으로 확장**하는 과정을 정리한다.
예시 관계: **회원 여러 명이 한 부서에 속한다 (다대일, N:1)**.

> 선행 개념은 `why-jpa.md`(객체 참조 ↔ 외래키 번역) 참고.

---

## 0. 먼저 — 설계 판단: 문자열 `dept` vs `Department` 엔티티

연관관계를 만들기 전에, 애초에 부서 정보를 어떻게 들 것인지부터 정해야 한다.

**Before — 문자열로 이중 관리 (나쁨)**
```java
public class Member {
    private String dept;   // "개발팀" 이라는 문자열
    private Team   team;   // Team(teamName="개발팀") 객체
    // ↑ 같은 사실("이 회원의 부서")이 두 군데에 → 불일치 위험
}
```

문제는 **데이터 불일치**다. 부서명이 바뀔 때 한쪽만 고치면 두 값이 서로 다른 부서를 가리키는 모순이 생긴다.

```java
member.setTeam(newTeam);   // team은 바꿨는데
// member.dept 는 그대로 "개발팀" ← 깜빡하면 어긋남
```

**After — 부서 정보는 `Department`가 단독 책임 (좋음)**
```java
public class Member {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dept_id")
    private Department dept;   // 부서 정보는 여기를 타고 들어감
}
```

핵심 원칙은 **"하나의 사실은 한 곳에만 (Single Source of Truth)"**.

| 항목 | 문자열 이중 관리 | `Department` 엔티티로 일원화 |
|---|---|---|
| 데이터 일관성 | 불일치 가능 ❌ | 항상 일치 ✅ |
| 부서명 변경 | 모든 `Member.dept` 수정 | `Department` 한 줄만 수정 |
| 부서 정보 확장(팀장, 위치 등) | 둘 곳이 없음 | `Department`에 필드 추가 |

> 단, **"부서(dept) 안에 여러 팀(team)이 있다"** 처럼 둘이 다른 축이면 둘 다 유지하되 각각을 엔티티로 분리한다.
> 여기서는 `dept = 부서`로 보고 `Department` 하나로 통합한다.

---

## 1. 1단계 — 단방향 (Member → Department)

여기서 멈춰도 **완성된 설계**다. 양방향은 "다음 선택지"이지 의무가 아니다.

**`Member.java` — 주인(owning side)**
```java
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;

    @ManyToOne(fetch = FetchType.LAZY)   // N:1, 지연 로딩
    @JoinColumn(name = "dept_id")        // MEMBER 테이블에 dept_id FK 컬럼 생성
    private Department dept;
}
```

**`Department.java` — 1단계에서는 회원 목록이 "없음"**
```java
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Department {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String deptCode;
    private String deptName;
    // ← members 리스트를 의도적으로 비워둔다. "부서는 아직 자기 회원을 모른다"
}
```

`@JoinColumn(name = "dept_id")` 한 줄이 **객체 참조 ↔ FK 번역**의 핵심이다.
"MEMBER 테이블에 `dept_id` FK 컬럼을 만들고, 거기에 Department의 PK를 저장하라"는 뜻.

```
MEMBER 테이블:  id | name | email | dept_id(FK)   ← FK가 여기 있음
DEPARTMENT 테이블:  id | deptCode | deptName       ← FK 없음
```

### 단방향의 결핍

```java
member.getDept().getDeptName();   // ✅ 회원 → 부서 조회 가능
department.???                    // ❌ 부서 → 회원 목록은 못 가져옴
```

한쪽만 안다 = 단방향. **부서에서 회원 목록을 보고 싶다는 요구가 생기면** 그때 양방향으로 간다.

---

## 2. 2단계 — 양방향으로 확장

부서 화면에서 소속 회원 목록을 보여줘야 하는 등, **반대 방향 조회가 실제로 필요해질 때만** 추가한다.

**`Department.java` — 이 시점에 `@OneToMany` 추가**
```java
@OneToMany(mappedBy = "dept")   // 주인은 Member.dept, 나(Department)는 거울
private List<Member> members = new ArrayList<>();
```

이제 거꾸로 조회된다.
```java
Department biztech = departmentRepository.findById(1L).get();
List<Member> members = biztech.getMembers();   // 비즈테크팀 소속 회원 전부
```

### 누가 주인인가 — FK를 가진 쪽

"Many라서 주인"이 아니라 **"FK 컬럼을 들고 있는 쪽이라서 주인"**이고, FK는 DB 구조상 항상 N 쪽 테이블에 생긴다. 그래서 결과적으로 `@ManyToOne`(Member) 쪽이 주인이 된다.

| | `Member.dept` | `Department.members` |
|---|---|---|
| 어노테이션 | `@ManyToOne` + `@JoinColumn` | `@OneToMany(mappedBy)` |
| 역할 | **주인** — FK를 실제로 관리 | **거울** — 읽기 전용 |
| DB 저장/변경 | 이쪽을 통해야 반영됨 | 이쪽만 바꾸면 반영 안 됨 |

### `mappedBy`에 들어가는 값 = 클래스 이름 ❌, 주인의 필드 이름 ⭕

```java
// Member.java
private Department dept;   // ← 필드 이름이 dept
//                ^^^^
```
```java
// Department.java
@OneToMany(mappedBy = "dept")   // ← 클래스(Department)가 아니라 Member의 '필드 이름'을 가리킴
```

타입은 `Department`지만 `mappedBy`가 따라가는 건 **필드 이름 `dept`**.
필드 이름을 `myDept`로 바꾸면 `mappedBy = "myDept"`가 된다 — 그래서 클래스 이름이 아니다.

> 일부러 `mappedBy = "Department"`(클래스 이름)를 넣으면 부팅 시 이런 에러가 난다:
> `mappedBy reference an unknown target entity property: ...Member.Department`
> "Member에 `Department`라는 **프로퍼티(필드)**가 없다" → mappedBy가 필드를 찾는다는 증거.

---

## 3. 양방향의 대가 — 공짜가 아니다

양방향을 켜면 같은 관계를 가리키는 참조가 두 개가 되고, 그 둘을 개발자가 맞춰줄 책임이 생긴다.

### (1) 거울 쪽만 바꾸면 DB에 반영 안 됨

```java
// ❌ 거울(Department)에만 추가 → DB의 dept_id 는 null
department.getMembers().add(member);

// ✅ 주인(Member.dept)을 세팅해야 DB에 반영됨
member.setDept(department);
```

### (2) 연관관계 편의 메서드 — 양방향의 필수 마무리

주인만 세팅하면 DB는 맞지만, 메모리상 `department.getMembers()`에는 그 회원이 안 들어있는 **객체 불일치**가 생긴다. 그래서 양쪽을 한 번에 맞추는 메서드를 만든다.

```java
// Member.java
public void changeDept(Department dept) {
    this.dept = dept;               // 주인 세팅 (DB 반영용)
    dept.getMembers().add(this);    // 거울도 동기화 (객체 일관성용)
}
```

> 핵심 분리: **DB 일관성(주인)** 과 **객체 일관성(양쪽 동기화)** 은 별개의 문제다.

### (3) 무한루프 — `toString` / JSON 직렬화

`Member.toString()` → dept 출력 → dept가 members 출력 → 그 member가 또 dept 출력… → **StackOverflow**.
그래서 양방향에서는 `@ToString(exclude = "dept")`로 연관 필드를 빼거나, JSON 직렬화 시 `@JsonIgnore`를 건다.

---

## 4. 양방향이 무조건 옳은가? → 아니다

판단 기준은 하나다.

> **"반대 방향 조회(`department.getMembers()`)가 실제로 코드에서 필요한가?"**

| 상황 | 선택 |
|---|---|
| 회원에서 부서만 조회하면 됨 (`member.getDept()`) | **단방향으로 끝.** 양방향 불필요 |
| 부서 화면에서 소속 회원 목록을 보여줘야 함 | 양방향 추가 |

반대 방향 조회는 사실 양방향 없이 쿼리로도 해결된다.
```java
memberRepository.findByDept(department);   // 양방향 없이도 "부서의 회원들" 조회
```

> 실무 원칙: **단방향으로 시작한다. 양방향은 "반대 조회가 실제로 필요할 때"만 `@OneToMany(mappedBy)`를 얹는다.**
> 양방향을 나중에 추가해도 주인(`Member.dept`)은 그대로라서 기존 코드가 안 깨진다.

---

## 5. `fetch = LAZY` — 양방향과 무관하게 항상 명시

`@ManyToOne`은 **기본값이 `EAGER`(즉시 로딩)**다. 단방향이든 양방향이든, 두면 회원을 조회할 때마다 부서까지 자동으로 같이 조회한다.

### 왜 문제인가 — N+1

```java
List<Member> members = memberRepository.findAll();   // 회원 100명
// EAGER면:
//   1) 회원 100명 SELECT        ← 쿼리 1번
//   2) 각 회원의 dept SELECT     ← 쿼리 100번
//   = 총 1 + 100 = 101번 (N+1)
```

부서를 쓰지도 않는데 100번을 더 날린다. `LAZY`면 회원 조회는 쿼리 1번, 부서는 실제 `getDept()` 호출 시에만 조회한다.

### 실무 규칙

> **`@ManyToOne`, `@OneToOne` → 기본값이 EAGER → 반드시 `fetch = LAZY` 명시.**
> **`@OneToMany`, `@ManyToMany` → 기본값이 이미 LAZY → 그대로 둬도 됨.**

이 비대칭(`Member.dept`엔 LAZY를 적고 `Department.members`엔 안 적는 이유) 때문에 "왜 어떤 건 적고 어떤 건 안 적지?"라는 혼란이 생기는데, 기본값 차이로 설명된다.

---

## 6. `@ManyToMany`는 왜 비추천인가

다대다는 관계형 DB에서 테이블 둘로 직접 표현이 안 돼서, JPA가 **연결 테이블(join table)** 을 자동 생성한다.

```java
@ManyToMany
private List<Course> courses;
// → MEMBER_COURSE 연결 테이블 자동 생성 (member_id, course_id 두 FK만 있음)
```

**문제: 연결 테이블에 속성을 못 붙인다.**

```
MEMBER_COURSE (자동 생성)
member_id | course_id
   1      |    10
   ↑ "수강신청일", "성적", "수강상태" 를 넣을 칸이 없음 ❌
```

실무에선 이런 관계 자체의 속성이 거의 항상 필요하다. 그래서 **연결 테이블을 직접 엔티티로 승격**하고 `@ManyToOne` 두 개로 푼다.

```java
@Entity
public class Enrollment {   // "수강신청"이라는 관계를 엔티티로
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

    private LocalDateTime enrolledAt;   // ★ 관계의 속성을 자유롭게 추가
    private int score;
    private String status;
}
```

`@ManyToMany` 하나 → `Member 1:N Enrollment N:1 Course` 두 개의 다대일로 바뀐다.

| | `@ManyToMany` | 연결 엔티티 (`@ManyToOne` x2) |
|---|---|---|
| 연결 테이블 | 자동 생성, 숨겨짐 | 내가 만든 엔티티 = 직접 제어 |
| 속성 추가 | 불가능 ❌ | 자유롭게 가능 ✅ |
| 실무 사용 | 거의 안 씀 | **이게 정석** |

---

## 7. 한 줄 요약

- **설계:** 같은 사실은 한 곳에만 → 부서는 문자열이 아니라 `Department` 엔티티로.
- **방향:** 단방향(`@ManyToOne`)으로 시작. 반대 조회가 실제로 필요할 때만 양방향(`@OneToMany(mappedBy)`) 추가.
- **주인:** FK를 가진 쪽(`@ManyToOne`)이 주인. 거울(`mappedBy`)은 읽기 전용 → DB 저장은 주인을 통해야 한다.
- **`mappedBy`:** 클래스 이름이 아니라 **주인 엔티티의 필드 이름**.
- **양방향의 대가:** 동기화 책임(편의 메서드) + 무한루프(`toString`/JSON) → 안 쓸 거면 켜지 마라.
- **fetch:** `@ManyToOne`/`@OneToOne`은 EAGER가 기본 → 항상 `LAZY` 명시 (N+1 방지).
- **다대다:** `@ManyToMany`는 연결 테이블에 속성을 못 붙임 → 연결 엔티티 + `@ManyToOne` 두 개로 푼다.
