# PostLike — 복합키로 만드는 연결 엔티티

"좋아요"를 예시로, **연결 테이블을 복합키(`@EmbeddedId` + `@MapsId`)로 만드는 과정**을 정리한다.
관계: **한 회원이 여러 글에 좋아요, 한 글에 여러 회원이 좋아요 (다대다)** 인데,
`@ManyToMany`를 쓰지 않고 **연결 엔티티 `PostLike`** 로 직접 푼다.

> 선행: 복합키 기본기는 `composite-key-guide.md`(@EmbeddedId, 키 클래스 3대 규칙),
> `@ManyToMany`를 왜 연결 엔티티로 푸는지는 `association-mapping-guide.md` 6절 참고.

---

## 0. 왜 복합키인가 — 키가 업무 규칙을 강제한다

좋아요의 식별자는 자연스럽게 `(회원, 게시글)` 조합이다. 이걸 PK로 잡으면:

> **"한 회원이 한 글에 좋아요는 최대 한 번"** 이 키 제약으로 **자동 보장**된다.

같은 `(memberId, postId)` 조합은 PK 중복이라 DB가 두 번째 INSERT를 거부한다.
단일키 `Long id`를 썼다면 중복 행이 쌓일 수 있어 **유니크 제약을 따로** 걸어야 한다.
즉 복합키는 단순 식별을 넘어 **중복 좋아요 방지라는 업무 규칙까지 담당**한다 — 이게 복합키를 쓰는 가장 강한 명분.

---

## 1. PostLikeId — 왜 "별도 키 클래스"가 필요한가

JPA에서 `@Id`는 기본적으로 **필드 하나**다. 그런데 좋아요는 식별자가 `(memberId, postId)` **두 개**다.
식별자가 2개 이상이면 **하나의 클래스로 묶어야** 한다. 그 그릇이 `PostLikeId`.

> `MemberId`(dept+email)가 있던 것과 똑같은 이유다. "흩어진 식별자를 하나의 키로 포장하는 그릇".

키 클래스는 JPA 규칙상 반드시 3가지를 만족해야 한다.

1. **`Serializable` 구현**
2. **기본 생성자(no-arg)**
3. **`equals()` / `hashCode()` 재정의** ← 핵심. JPA가 키 동등성으로 엔티티를 식별

```java
package com.example.spring.studyjpa.entity;   // 도메인 키 → entity 패키지 (MemberId와 동일)

import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable                                          // 엔티티에 끼워 넣는 값
@Getter
@EqualsAndHashCode                                   // equals/hashCode 필수 → lombok이 생성
@NoArgsConstructor(access = AccessLevel.PROTECTED)   // JPA용 기본 생성자
public class PostLikeId implements Serializable {    // Serializable 필수

    private Long memberId;
    private Long postId;

    public PostLikeId(Long memberId, Long postId) {  // 값 채우는 생성자 (existsById 등에 사용)
        this.memberId = memberId;
        this.postId = postId;
    }
}
```

> 📍 **위치:** `PostLikeId`는 `PostLike` 전용 도메인 키이지, 전 엔티티가 공유하는 기술 베이스가 아니다.
> 그래서 `entity.base`(BaseEntity가 사는 곳)가 아니라 **`entity` 패키지**에 둔다 — `MemberId`와 같은 자리.

---

## 2. PostLike 엔티티 — `@EmbeddedId` + `@MapsId`

```java
package com.example.spring.studyjpa.entity;

import com.example.spring.studyjpa.entity.base.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostLike extends BaseTimeEntity {       // createdAt = "좋아요 누른 시각"

    @EmbeddedId
    private PostLikeId id;                            // (memberId, postId) 복합키

    @MapsId("memberId")                              // 복합키의 memberId = 이 관계의 FK
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id")                  // ⚠️ name = 필수 (아래 주의 참고)
    private Member member;

    @MapsId("postId")                                // 복합키의 postId = 이 관계의 FK
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id")
    private Post post;

    public PostLike(Member member, Post post) {
        this.member = member;
        this.post = post;
        // id는 직접 안 만든다 → @MapsId가 member/post의 id에서 자동으로 채움
    }
}
```

> ⚠️ **흔한 실수:** `@JoinColumn("member_id")` 는 **컴파일 에러**다. `@JoinColumn`은 위치 인자가 없으므로
> 반드시 **`@JoinColumn(name = "member_id")`** 처럼 속성명을 적어야 한다.

`BaseTimeEntity` 상속은 "좋아요 누른 시각(`createdAt`)"을 기록하기 위함이다.
연결 테이블에 속성(시각)이 붙기 시작하는 순간이 곧 **"`@ManyToMany`를 안 쓰고 엔티티로 만든 이유"** 가 증명되는 지점이다.

---

## 3. `@MapsId`는 왜 쓰는가 — 키 값과 FK 값이 같아서

복합키의 `memberId`/`postId` 값은 사실 `member`/`post` 관계의 **FK 값과 똑같다**.
`@MapsId`가 **없다고 가정**하면 왜 필요한지 보인다.

**Before — `@MapsId` 없이 (키와 관계를 따로 관리)**
```java
@EmbeddedId
private PostLikeId id;                 // memberId, postId 를 직접 채워야 하고

@ManyToOne @JoinColumn(name = "member_id")
private Member member;                 // member_id FK 도 따로 세팅
```
문제:
- 저장 시 `id.memberId`도 채우고 `member`도 세팅 → **같은 값을 두 번** 적음
- 둘이 어긋날 수 있음 (`id.memberId = 1` 인데 `member`는 2번?)
- 컬럼이 PK용·FK용으로 **중복 생성**될 여지

**After — `@MapsId`로 연결**
```java
@MapsId("memberId")                    // "복합키의 memberId 칸은 member 관계의 FK로 채워라"
@ManyToOne(fetch = FetchType.LAZY, optional = false)
@JoinColumn(name = "member_id")
private Member member;
```
효과:
- **`member`만 세팅하면 `id.memberId`가 자동으로 채워짐**
- `member_id` 컬럼 **하나가 PK이자 FK 역할을 동시에** 함 → 값·컬럼 중복 제거

```java
PostLike like = new PostLike(member, post);   // 관계만 줌 → @MapsId가 id 자동 채움
postLikeRepository.save(like);
```

### 둘의 역할 구분

| | 무엇 | 왜 필요 |
|---|---|---|
| `PostLikeId` | 두 식별자를 묶는 키 클래스 | 복합키는 식별자가 2개라 한 그릇으로 묶어야 함 |
| `@MapsId` | 키 칸을 관계의 FK에 연결 | 키 값 = FK 값이라, 따로 관리하는 중복·불일치 제거 |

> 한 줄: **`PostLikeId`는 "복합키를 담는 그릇", `@MapsId`는 "그 칸을 관계의 FK로 자동으로 채워주는 연결".**

---

## 4. PostLikeId를 직접 만드는 건 언제인가 — "행을 가리키는 주소표"

### 먼저 — `@Embeddable` / `@EmbeddedId` 정리

- **`@Embeddable`** (PostLikeId에) → "이 클래스는 엔티티 안에 **끼워 넣을 수 있는 값 묶음**이다." `memberId`+`postId`를 하나로 포장한 부품.
- **`@EmbeddedId`** (PostLike의 `id`에) → "그 끼워 넣은 부품을 **이 엔티티의 PK로 장착**한다."

> `@Embeddable`로 "키 부품"을 정의하고, `@EmbeddedId`로 "그 부품을 PK로 장착"한다.

### "키를 만든다"는 게 무슨 말인가 — 행을 찾는 방법에서 출발

JPA에서 특정 행을 찾을 때는 **PK 값**을 건넨다.

**단일키 (`Post`, PK = `Long id`)** — 숫자 하나면 충분
```java
postRepository.findById(5L);     // "5번 글" → 키가 그냥 숫자
postRepository.deleteById(5L);
```

**복합키 (`PostLike`, PK = `(memberId, postId)`)** — 두 값이 다 있어야 행을 가리킨다.
그런데 `findById`/`deleteById`는 **인자를 하나만** 받는다. 그래서 두 값을 **하나의 객체로 묶어** 건네야 하고, 그 객체가 `PostLikeId`다.
```java
PostLikeId key = new PostLikeId(3L, 56L);   // "회원3이 글56에 누른 좋아요"를 가리키는 주소표
postLikeRepository.findById(key);
postLikeRepository.deleteById(key);
postLikeRepository.existsById(key);
```

> **"키를 만든다" = "어떤 행인지 가리키는 주소표(PostLikeId)를 만들어 JPA에 건넨다".**
> 단일키는 숫자 하나가 주소표, 복합키는 두 값을 묶은 객체가 주소표다.

### 저장은 왜 키를 안 만들어도 되나 — 대비

| 상황 | 손에 있는 것 | 키를 직접 만드나? |
|---|---|---|
| **저장** (새 좋아요) | `member`, `post` 객체 | ❌ — `@MapsId`가 두 객체의 id에서 자동으로 채움 |
| **조회/삭제** (기존 좋아요) | `memberId`, `postId` 날것 값 | ✅ — `new PostLikeId(...)`로 주소표를 만들어 지목 |

```java
// 저장: 관계 객체가 있으니 키는 @MapsId가 채움
postLikeRepository.save(new PostLike(member, post));

// 조회/삭제: 날것 값(예: 요청으로 받은 3, 56)만 있으니 주소표를 직접 만듦
postLikeRepository.deleteById(new PostLikeId(3L, 56L));
```

저장은 진짜 `member`/`post` 객체를 들고 있어 `@MapsId`가 거기서 id를 뽑아 키를 채운다.
조회/삭제는 보통 "회원3, 글56" 같은 날것 숫자만 있고 PostLike 객체가 없어, 우리가 직접 주소표를 만들어 "이 행이요"라고 지목해야 한다.

### 그래서 생성자가 두 개씩 있다

| 클래스 | `@NoArgsConstructor` | 명시적 값 생성자 |
|---|---|---|
| `PostLike` | **필수** — JPA가 조회 시 빈 객체 생성 | `PostLike(member, post)` — 좋아요 **생성** 편의 (→ `@MapsId`가 키 채움) |
| `PostLikeId` | **필수** — JPA가 키 객체 생성 (`@Embeddable` 스펙) | `PostLikeId(memberId, postId)` — **조회/삭제** 시 주소표를 만들려고 |

- `@NoArgsConstructor`는 **두 클래스 다 필수**(JPA가 리플렉션으로 빈 객체를 만든 뒤 필드를 채우므로). 복합키와 무관하게 모든 `@Entity`/`@Embeddable`의 공통 규칙.
- 명시적 생성자는 **목적이 다르다**: `PostLike(member,post)`는 *만들 때*, `PostLikeId(memberId,postId)`는 *찾을 때/지울 때*.
- 자바 규칙상 **명시적 생성자를 적으면 컴파일러가 주던 기본 생성자가 사라지므로**, JPA용으로 `@NoArgsConstructor`를 다시 붙여 되살린다. (`composite-key-guide.md` 4절과 동일한 원리)

---

## 5. 좋아요 카운트는 어디에 있나 — "컬럼이 아니라 행을 센다"

PostLike에는 카운트 컬럼이 **없고, 없는 게 맞다.**

> 좋아요 수 = `POST_LIKE` 테이블에서 그 글의 **행 개수**. 좋아요 하나 = 행 하나.

```java
long count = postLikeRepository.countByPost_Id(5L);
// → SELECT COUNT(*) FROM post_like WHERE post_id = 5
```

**왜 컬럼으로 안 두나 (Single Source of Truth):** Post에 `likeCount` 컬럼을 따로 두면
좋아요 누를 때마다 `+1` 동기화를 해야 하고, 행 수와 어긋나는 **불일치**가 생긴다(= `dept` 이중관리와 같은 함정).

| 방식 | 장점 | 단점 |
|---|---|---|
| **행 COUNT (기본)** | 항상 정확, 불일치 없음 | 매번 쿼리 (캐싱 가능) |
| Post에 `likeCount` 컬럼 | 읽기 빠름 | 동기화 책임 + 불일치 위험 |

> 비정규화 카운터는 "좋아요 수백만 + 목록에서 매번 COUNT 부담"인 **대규모 성능 최적화**에서만.
> 교육 단계에선 **COUNT가 정답**.

---

## 6. "한 번만 가능"한 구조 + 토글 처리

`(memberId, postId)`가 PK라 **DB가 중복을 거부** → 한 회원 × 한 글 = 최대 1개.
"좋아요 / 취소"는 **행을 넣고 빼는** 토글로 구현한다.

```java
// Service — 존재 확인(리포지토리 조회)이 필요하므로 엔티티가 아니라 Service
public void toggleLike(Long memberId, Long postId) {
    PostLikeId id = new PostLikeId(memberId, postId);

    if (postLikeRepository.existsById(id)) {
        postLikeRepository.deleteById(id);                 // 이미 눌렀으면 → 취소
    } else {
        Member member = memberRepository.findById(memberId).orElseThrow();
        Post post = postRepository.findById(postId).orElseThrow();
        postLikeRepository.save(new PostLike(member, post)); // 처음이면 → 좋아요
    }
}
```

> **왜 Service인가:** `existsById`(조회)는 "바깥(리포지토리)"이 필요하다. "자기 필드만으로 끝나면 엔티티, 바깥이 필요하면 Service" 기준.
> **왜 DB 제약도 필요한가:** 앱의 `existsById` 체크는 동시 요청(따닥) 경쟁 상황에서 둘 다 통과할 수 있다.
> 그때 **복합키 PK 제약이 최후 방어선**으로 중복을 막는다 → "앱은 1차, DB 제약은 최종 방어".

---

## 7. 복합키 vs surrogate key — 연결 테이블의 두 방식

같은 연결 테이블을 `PostLike`(복합키)와 `Enrollment`(`association-mapping-guide.md`의 `Long id`)로 비교하면:

| 방식 | 중복 방지 | 다른 테이블에서 참조 | 복잡도 |
|---|---|---|---|
| 복합키 (PostLike) | 키 자체가 막음 ✅ | FK 2개가 줄줄이 따라감 | `@MapsId` 등 손이 더 감 |
| surrogate key (Enrollment) | 유니크 제약 따로 필요 | `Long id` 하나만 | 단순 |

> 판단 기준: **좋아요처럼 다른 데서 참조 안 되는 단순 연결 → 복합키도 OK.**
> **주문상품처럼 이후 다른 테이블이 참조할 가능성 → surrogate key가 편함.**

---

## 8. (비교) `@IdClass` 방식 — 복합키의 또 다른 방법

복합키는 `@EmbeddedId` 말고 **`@IdClass`** 로도 만들 수 있다. 결과(복합 PK)는 같지만 **구조가 다르다.**

**엔티티 — `@Id`를 각 관계에 직접, `@MapsId`/`@EmbeddedId` 없음**
```java
@Entity
@IdClass(PostLikeId.class)                           // 키 클래스를 여기서 지정
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostLike extends BaseTimeEntity {

    @Id                                              // @Id를 관계에 직접
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id")
    private Member member;

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id")
    private Post post;

    public PostLike(Member member, Post post) {
        this.member = member;
        this.post = post;
    }
}
```

**키 클래스 — `@Embeddable`이 아니라 평범한 클래스**
```java
public class PostLikeId implements Serializable {    // Serializable + equals/hashCode 만 필요

    private Long member;   // ⚠️ 엔티티의 필드명(member)과 같아야 함, 타입은 Member의 PK 타입(Long)
    private Long post;     // ⚠️ 엔티티의 필드명(post)과 같아야 함

    // 기본 생성자 + 값 생성자 + equals/hashCode
}
```

### `@EmbeddedId` vs `@IdClass`

| | `@EmbeddedId` (이 문서의 기본) | `@IdClass` |
|---|---|---|
| 키 위치 | 한 객체로 묶어 필드 하나(`id`) | 엔티티에 직접 펼침 (`@Id` x2) |
| 키 클래스 | `@Embeddable` 필요 | 평범한 클래스 (`Serializable`만) |
| `@MapsId` | 관계 연결 시 사용 | **불필요** (`@Id`를 관계에 직접) |
| 키 클래스 필드명 | 자유 (`memberId`, `postId`) | **엔티티 필드명과 일치 필수** (`member`, `post`) |
| 엔티티 접근 | `post.getId().getMemberId()` (중첩) | `post.getMember()` (평면) |

### 왜 이 프로젝트는 `@EmbeddedId`인가

- **일관성** — `MemberId`가 이미 `@EmbeddedId`(`@Embeddable`)다. PostLike만 `@IdClass`면 한 프로젝트에 복합키 방식이 둘로 섞인다.
- **안정성** — `@IdClass`는 키 클래스 필드명이 엔티티 필드명과 글자까지 같아야 하는데 **컴파일 시점에 안 잡힌다**(오타 시 런타임 에러). `@EmbeddedId`는 `@MapsId("memberId")`로 명시 연결이라 덜 취약하다.
- **객체지향** — 키를 하나의 의미 단위(객체)로 다루고 통째로 넘기기 좋다(`findById(key)`).

> `@IdClass`가 나은 경우: 키를 객체로 묶기보다 **평면적으로** 다루고 싶을 때, 또는 레거시 DB라 키 컬럼을 엔티티 필드처럼 직접 노출하고 싶을 때.
> 그래서 기본은 `@EmbeddedId`, `@IdClass`는 **"두 방식이 있다"는 비교 교보재**로 쓰면 좋다.

---

## 9. 한 줄 요약

- **왜 복합키:** `(memberId, postId)` PK가 "한 회원 × 한 글 = 1번"이라는 중복 방지 규칙을 강제한다.
- **`PostLikeId`:** 식별자가 2개라 묶는 그릇. 3대 규칙(`Serializable` + 기본생성자 + `equals/hashCode`). `entity` 패키지에 둔다.
- **`@MapsId`:** 키 값 = FK 값이라, 관계만 세팅하면 키가 자동으로 채워지고 컬럼·값 중복이 사라진다.
- **키를 직접 만드는 때:** 복합키 PK는 "행을 가리키는 주소표". 저장은 `@MapsId`가 채워주지만, `findById`/`deleteById`/`existsById`로 조회·삭제할 땐 `new PostLikeId(...)`로 주소표를 직접 만들어 건넨다.
- **카운트:** 컬럼이 아니라 행을 COUNT (Single Source of Truth).
- **한 번만:** PK 제약이 DB에서 강제. 토글은 Service에서 `existsById` → save/delete.
- **주의:** `@JoinColumn`은 `name =` 필수. `@JoinColumn("post_id")`는 컴파일 에러.
- **다른 방식:** `@IdClass`로도 복합키 가능(`@Id` 직접 + 평범한 키 클래스). 단 필드명 매칭이 취약하고 `MemberId`와 안 맞아, 이 프로젝트는 `@EmbeddedId` 유지 — `@IdClass`는 비교 교보재용.
