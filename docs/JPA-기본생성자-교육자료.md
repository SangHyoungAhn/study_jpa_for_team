# JPA는 왜 인자 없는 생성자가 필요할까? (주니어 교육 자료)

> **교육 순서 팁:** "리플렉션이란~"으로 시작하면 졸립니다.
> **에러 한번 내보기 → 왜 났지? → 아하!** 순서로 가야 기억에 남아요.

---

## 1단계: 비유로 "왜 빈 객체부터?"를 먼저 이해시키기

> 택배 상자를 생각해보세요. JPA(Hibernate)는 DB에서 데이터를 꺼내올 때,
> **먼저 빈 상자를 하나 만들고 → 거기에 물건(필드 값)을 하나씩 담는** 방식으로 일해요.

여기서 자연스럽게 이런 의문이 생깁니다:

> "근데 JPA는 우리가 만든 `Member` 클래스가 뭔지 어떻게 알고 상자를 만들죠?"

→ 이 의문이 생겨야 다음 설명이 와닿습니다.

---

## 2단계: "JPA는 너의 클래스를 미리 모른다"

주니어가 평소 쓰는 코드와 대비시키는 게 핵심입니다.

**우리가 평소 쓰는 방식 (컴파일 시점에 다 정해짐):**
```java
Member m = new Member("홍길동", 20); // Member가 뭔지 코드에 박혀 있음
```

**JPA의 입장 (런타임에야 알게 됨):**
```java
// Hibernate 라이브러리는 Member라는 이름조차 모릅니다.
// 여러분이 @Entity를 붙여서 "이거 관리해줘"라고 알려준 거예요.
// 그래서 런타임에 "이름만 보고" 객체를 만드는 기술 = 리플렉션(Reflection)
```

> **한 줄 정리:** Hibernate는 라이브러리라서, 여러분이 앞으로 만들 클래스를
> 미리 알 수가 없어요. 그래서 런타임에 알아내서 만드는 '리플렉션'을 씁니다.

---

## 3단계: 그래서 "인자 없는 생성자"가 필요하다

여기서는 **일부러 에러를 내보게** 하는 게 제일 좋습니다.

**Before — 기본 생성자가 없는 엔티티:**
```java
@Entity
public class Member {
    @Id @GeneratedValue
    private Long id;
    private String name;

    public Member(String name) {  // 인자 있는 생성자만 있음
        this.name = name;
    }
}
```

→ 실행하면 이런 에러가 납니다 (직접 보게 하기):
```
org.hibernate.InstantiationException: No default constructor for entity: Member
```

> JPA는 빈 상자를 만들려고 `new Member()`를 호출하려는데,
> 그런 생성자가 없으니 못 만들겠다고 화내는 거예요.

**After — 기본 생성자 추가:**
```java
@Entity
public class Member {
    @Id @GeneratedValue
    private Long id;
    private String name;

    protected Member() {}  // ← JPA가 빈 객체 만들 때 쓰는 생성자

    public Member(String name) {
        this.name = name;
    }
}
```

> **왜 필요한가 (정리)**
> - JPA는 리플렉션으로 빈 객체부터 만든 뒤 필드 값을 하나씩 채웁니다.
> - 지연 로딩(Lazy)을 위한 프록시 객체도 원본 클래스를 상속해서 만들면서
>   인자 없는 생성자를 호출합니다.
> - 결국 인자 없는 생성자가 없으면 JPA가 객체를 만들 수단이 없습니다.

---

## 4단계: "왜 public이 아니라 protected냐"

주니어가 제일 헷갈려하는 부분입니다.
**"열어두면 무슨 나쁜 일이 생기는지"**를 보여주는 게 효과적이에요.

**만약 public이면 — 동료가 이런 코드를 짤 수 있게 됨:**
```java
Member m = new Member();   // 이름도 없고 아무것도 없는 빈 회원
memberRepository.save(m);  // name이 null인 채로 DB에 저장됨! 💥
```

> 기본 생성자를 public으로 열어두면, **필수 값이 텅 빈 불완전한 객체**를
> 아무 데서나 만들 수 있어요. 이런 객체가 DB에 들어가면 데이터가 망가집니다.

**protected로 두면:**
```java
Member m = new Member();   // ❌ 컴파일 에러! 외부에서 호출 불가

// 객체는 반드시 의도된 통로로만 만들게 강제됨
Member m = Member.create("홍길동");   // ✅ 정적 팩토리 메서드
Member m = new Member("홍길동", 20);  // ✅ 의도된 생성자
```

> **`protected`는 절묘한 위치예요.**
> - JPA(프록시)는 같은 동네(상속 관계)라서 호출할 수 있고,
> - 바깥 코드는 못 건드립니다.
>
> 즉, **'JPA만 쓰라고 만든 뒷문'**인 셈이에요.

`private`이 안 되는 이유도 한 줄:

> `private`은 자식(프록시)이 부모 생성자를 못 부르거든요.
> 그래서 상속 기반인 지연 로딩 프록시가 깨집니다. 그래서 딱 `protected`.

> **실무 연결:** 롬복 `@NoArgsConstructor(access = AccessLevel.PROTECTED)`가
> 방금 손으로 쓴 `protected Member() {}`를 자동 생성해주는 것입니다.

---

## 5단계: 한 장으로 정리하는 표

칠판/슬라이드에 이거 하나만 남기면 됩니다.

| 질문 | 답 | 왜? |
|------|-----|------|
| JPA는 어떻게 객체를 만들까? | 빈 객체 만들고 → 값 채움 (리플렉션) | 라이브러리라 여러분 클래스를 미리 모르니까 |
| 왜 인자 없는 생성자가 필요? | JPA가 `new Member()`로 빈 상자를 만들어야 하니까 | 없으면 만들 수단 자체가 없음 |
| 왜 public 아닌 protected? | JPA는 쓰되, 외부는 막으려고 | 빈 객체가 아무 데서나 생기는 걸 방지 |

---

## 6단계: "엔티티에 메서드(비즈니스 로직) 넣어도 되나?"

주니어가 자주 듣는 말 — *"엔티티에 비즈니스 메서드 넣지 마라"*.
**결론부터: 그건 절대 규칙이 아니고, 오히려 의견이 갈리는 주제입니다.**

### 사실은 정반대 의견이 두 개 있다

**진영 A — 빈약한 도메인 모델 (Anemic Domain Model)**
- 엔티티는 데이터만 담는 그릇 (필드 + getter/setter)
- 모든 비즈니스 로직은 Service에 둔다

```java
// 엔티티는 데이터만, 로직은 서비스가 끌고 다님
member.setPoint(member.getPoint() - amount);  // 엔티티는 수동적
```

**진영 B — 풍부한 도메인 모델 (Rich Domain Model, DDD)**
- 엔티티가 자기 데이터에 대한 규칙을 스스로 가진다
- Martin Fowler는 오히려 진영 A를 **안티패턴**이라 부름

```java
@Entity
public class Member {
    private int point;

    // 자기 상태에 대한 규칙을 스스로 책임짐
    public void usePoint(int amount) {
        if (this.point < amount) throw new IllegalStateException("포인트 부족");
        this.point -= amount;
    }
}
```

> "엔티티에 비즈니스 메서드 넣지 마라"는 **한쪽 진영의 주장일 뿐**,
> 보편 진리가 아니에요. 많은 DDD/좋은 코드 책은 오히려 B를 권장합니다.

### 그럼 거의 합의된 "진짜 관례"는 두 가지

**① Setter는 만들지 마라 (이게 진짜 강한 관례)**
```java
// ❌ 무분별한 setter — 누가 언제 왜 바꿨는지 알 수 없음
member.setPoint(9999);
member.setName("");

// ✅ 의도가 드러나는 메서드로
member.usePoint(500);
member.changeName("홍길동");
```
→ 우리가 만든 `register`, `changeTitle`이 다 이 맥락.
"메서드를 넣지 마라"가 아니라 **"setter 대신 의도가 드러나는 메서드를 넣어라"**가 정확한 표현.

**② 외부 의존이 필요한 로직은 엔티티에 넣지 마라**
```java
@Entity
public class Member {

    // ✅ OK — 자기 데이터만으로 완결되는 로직
    public void usePoint(int amount) {
        if (this.point < amount) throw new IllegalStateException();
        this.point -= amount;
    }

    // ❌ NO — Repository / 외부 API / 메일 발송 등 외부 의존
    public void join() {
        memberRepository.save(this);   // 엔티티가 Repository를 알면 안 됨
        emailService.sendWelcome();    // 외부 시스템 의존 → Service의 일
    }
}
```

> **기준:** 그 로직이 **자기 자신의 상태만으로 완결되면 엔티티에**,
> **다른 객체(Repository/Service/외부 API)를 끌어와야 하면 Service에** 둔다.

### 정리 표

| 흔히 듣는 말 | 정확한 의미 |
|---|---|
| "엔티티에 비즈니스 메서드 넣지 마" | ❌ 절대 규칙 아님. 진영 갈림 (DDD는 오히려 권장) |
| "Setter 만들지 마" | ✅ 거의 합의된 관례 — 의도 드러나는 메서드로 대체 |
| "엔티티에 로직 넣지 마" | △ 외부 의존(Repository/외부 API) 필요한 로직만 빼라는 뜻 |

> **결론**
> - 자기 상태를 다루는 메서드(`usePoint`, `changeTitle`, `register`) → 엔티티에 넣는 게 오히려 좋은 코드 (응집도↑)
> - Repository/외부 시스템이 필요한 흐름 → Service에
> - 무분별한 setter → 이건 진짜로 피해라

---

## 7단계: 회원가입을 넣는다면 — "생성 통로" 만들기

지금까지는 **JPA가 조회할 때 쓰는** `@NoArgsConstructor`(protected)만 봤습니다.
그런데 회원가입처럼 **내 코드가 직접 객체를 만들어 저장(INSERT)** 하려면,
**값을 넣어 만드는 "생성 통로"가 따로 필요**합니다.

> 헷갈리기 쉬운 포인트: **`@NoArgsConstructor`는 빈 객체만 만들어요** (필드가 다 null).
> JPA 조회 전용이라, 내 코드가 "값 채운 객체"를 만드는 용도로는 못 씁니다.
> 그래서 생성 통로는 **별도로** 둬야 합니다.

### Before — 현재 `Member` (생성 통로 없음)

```java
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String email;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dept_id", nullable = false)
    private Department dept;   // 부서는 필수(NOT NULL)
}
```
→ 이 상태에선 `new Member(...)`도 막혀 있고(setter도 없음), **코드에서 회원을 만들 방법이 없습니다.**
   (지금은 "JPA가 읽기만 하는" 엔티티인 셈)

### After — 정적 팩토리 메서드로 생성 통로 추가 (권장)

```java
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)  // JPA 조회 전용 (그대로 유지)
public class Member {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String email;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dept_id", nullable = false)
    private Department dept;

    // ① 진짜 생성자는 private으로 숨김 → 바깥에서 new 못 함
    private Member(String name, String email, Department dept) {
        this.name = name;
        this.email = email;
        this.dept = dept;
    }

    // ② "회원가입" 의도가 드러나는 입구 + 검증을 한곳에 모음
    public static Member register(String name, String email, Department dept) {
        if (name == null || name.isBlank())   throw new IllegalArgumentException("이름 필수");
        if (email == null || !email.contains("@")) throw new IllegalArgumentException("이메일 형식 오류");
        if (dept == null)                      throw new IllegalArgumentException("부서 필수");
        return new Member(name, email, dept);
    }
}
```

### 실제 사용 (Service)

```java
@Transactional
public Long join(String name, String email, Long deptId) {
    Department dept = departmentRepository.findById(deptId)
            .orElseThrow(() -> new IllegalArgumentException("부서 없음"));

    Member member = Member.register(name, email, dept);  // ← 생성 통로 사용
    memberRepository.save(member);                        // INSERT
    return member.getId();
}
```

### 생성 통로는 형태가 자유 — 본인 선택

```java
// 방법 1) public 생성자 (가장 단순, 검증 불필요할 때)
public Member(String name, String email, Department dept) { ... }
new Member(name, email, dept);

// 방법 2) 정적 팩토리 메서드 (의도 드러남 + 검증 — 위 예시)
Member.register(name, email, dept);

// 방법 3) 빌더
Member.builder().name(name).email(email).dept(dept).build();
```

> **핵심 정리**
> - `@NoArgsConstructor`(protected) = **JPA 조회용** (모든 엔티티 공통 필요)
> - 생성 통로(생성자/팩토리/빌더) = **내 코드가 INSERT할 때만** 추가
> - 그래서 `Member`에 생성 통로가 없던 건 규칙 때문이 아니라 **아직 회원가입 기능이 없어서** —
>   기능이 생기면 그때 추가하면 됩니다.
> - 단순하면 public 생성자, 검증·의도 표현이 필요하면 정적 팩토리 — **둘 다 정답**입니다.

> 참고: **UPDATE는 생성 통로가 아니라 변경 메서드**(`changeName` 등)로 처리하고,
> JPA의 변경 감지(Dirty Checking)가 UPDATE SQL을 자동으로 만들어 줍니다. (3단계·6단계와 연결)
