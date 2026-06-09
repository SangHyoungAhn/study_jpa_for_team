# JPA 마스터 로드맵 (교육 커리큘럼)

이 문서는 **흩어진 상세 문서들을 하나의 학습 경로로 꿰는 입구(index)** 다.
각 단계의 "핵심 포인트 + 왜 이 순서인지 + 연결 문서"만 담고, 자세한 내용은 링크된 문서로 간다.

> **교육 철학 (전 단계 공통)**
> 1. **결핍 → 처방** — 불편함을 먼저 겪게 한 뒤 해법(도구)을 준다. ("왜 쓰는지" 모르고 외우지 않게)
> 2. **단순 → 복잡** — 단방향 → 양방향, 단일키 → 복합키 순으로 토대를 쌓는다.
> 3. **항상 "왜"** — 문법(what/how)보다 그 선택의 근거(why)를 먼저 이해시킨다.

---

## 전체 로드맵

| 단계 | 주제 | 핵심 포인트 | 연결 문서 |
|---|---|---|---|
| **0** | Java 현대 문법 | `Optional`, `Stream`, `Lambda` (JPA를 위한 체력) | — |
| **1** | 환경 구축 | H2 DB 설정 + 웹 콘솔 (눈으로 보는 DB) | — |
| **2** | JPA 진화론 | JDBC → MyBatis → JPA, **왜 쓰는가** | `why-jpa.md` |
| **3** | 첫 엔티티 | `@Entity`, `@Id`, `@GeneratedValue` — 객체↔테이블 매핑 | — |
| **4** | 실습 (CRUD) | `MemberRepository`로 저장·조회 → H2에서 확인 | — |
| **5** | 연관관계 (단방향) | `@ManyToOne`, `@JoinColumn`, **LAZY** | `association-mapping-guide.md` |
| **6** | 공통 필드 자동화 | `BaseEntity`/`BaseTimeEntity`, Auditing (`@MappedSuperclass`) | `association-mapping-guide.md`(상속 개념) |
| **7** | 복합키 & 연결 엔티티 | `@EmbeddedId`, `@MapsId`, `@ManyToMany` 회피 | `postlike-composite-key-guide.md` |
| **8** | 양방향 전환 | `mappedBy`, 편의 메서드, `cascade` (필요할 때만) | `association-mapping-guide.md` |
| **9** | 전체 조망 | 도메인 ERD로 구조 정리 | `erd.md` |
| 보조 | 복잡 쿼리 분리 | JPA가 불편한 통계/튜닝은 MyBatis 병행 | `dynamic-query-guide.md` |

---

## 단계별 상세

### 0~1단계 — 준비 운동

- **0. Java 현대 문법:** `Optional`(`findById().orElseThrow()`), `Stream`(컬렉션 가공), `Lambda`. JPA 코드 곳곳에 나오므로 먼저 체력을 기른다.
- **1. 환경 구축:** H2 + 웹 콘솔. **왜 먼저?** JPA가 날리는 SQL과 테이블을 *눈으로 직접 봐야* 이후 "변경 감지가 UPDATE를 날린다" 같은 추상 개념이 실감 난다.

### 2단계 — JPA 진화론 (`why-jpa.md`)

JDBC/MyBatis의 불편함(반복 SQL, 컬럼 추가 시 전부 수정, **패러다임 불일치**) → JPA의 해법(연관관계, 생산성, 1차 캐시, **변경 감지**).
> **왜 이 단계가 중요:** 도구를 배우기 전에 "왜 이 도구가 생겼는지"를 공감해야 나머지 전부가 납득된다. 특히 **변경 감지(setName만으로 UPDATE)** 시연이 "JPA는 다르다"를 각인시키는 결정타.

### 3~4단계 — 첫 엔티티 & CRUD

- **3. 첫 엔티티:** `@Entity`/`@Id`로 객체 하나를 테이블 하나에 매핑. JPA의 가장 기본 단위.
- **4. 실습:** `MemberRepository`로 save/find → H2에서 행 확인. **여기서 영속성 컨텍스트·변경 감지**를 손으로 체험.
> **왜 연관관계보다 먼저:** 단일 엔티티 매핑과 영속성 컨텍스트가 탄탄해야, 그 응용인 연관관계가 의미를 가진다.

### 5단계 — 연관관계 단방향 (`association-mapping-guide.md`)

`Member → Department`를 `@ManyToOne` + `@JoinColumn`으로. **객체 참조 ↔ FK 번역**이 핵심.
- `@ManyToOne`은 기본값이 EAGER → **반드시 `fetch = LAZY`** (N+1 방지).
- 필수 관계는 `optional = false` + `@JoinColumn(nullable = false)`로 못 박는다.
> **왜 단방향만 먼저:** 양방향은 동기화·무한루프 비용을 동반한다. 단방향으로 "한쪽만 안다"는 결핍을 느낀 뒤 8단계에서 처방한다.

### 6단계 — 공통 필드 자동화 (BaseEntity)

`createdAt`/`updatedAt`(+`createdBy`/`updatedBy`)을 `@MappedSuperclass`로 부모에 모아 상속.
- **2단 분리:** `BaseTimeEntity`(시간) → `BaseEntity`(시간+작성자). "누가"가 의미 없는 엔티티는 시간만 상속.
- 시간은 `@CreatedDate`가 자동, **작성자는 `AuditorAware`가 있어야** 자동 기록(현재 사용자 출처를 JPA가 모르므로).
> **왜 이 자리:** 엔티티가 2개 이상(Post, Comment) 생긴 직후라야 "공통 필드 중복 제거"라는 동기가 산다.

### 7단계 — 복합키 & 연결 엔티티 (`postlike-composite-key-guide.md`)

`PostLike`를 `(memberId, postId)` 복합키로. **키 제약이 "중복 좋아요 방지" 업무 규칙을 강제**.
- `@EmbeddedId` + `@MapsId`(키 값 = FK 값 중복 제거), `PostLikeId`(식별자 묶는 그릇).
- `@ManyToMany`를 안 쓰고 연결 엔티티로 푸는 이유(연결 테이블에 속성을 붙이려고).
- `@IdClass` 방식과 비교.
> **왜 마지막 난코스:** 단일키·단순 연관관계로 기본기를 쌓은 뒤라야 복합키의 부담을 받칠 수 있다.

### 8단계 — 양방향 전환 (`association-mapping-guide.md`)

"게시글 상세 + **댓글 목록**"처럼 반대 조회가 필요해지는 순간 `@OneToMany(mappedBy)` 추가.
- 주인(FK 가진 쪽) ↔ 거울(`mappedBy`), 편의 메서드, 무한루프(`toString`/JSON) 주의.
- `cascade`/`orphanRemoval`은 **"글 삭제" 기능 만들 때** + 생명주기 소유 기준으로 설계.
> **왜 여기서:** 결핍(반대 조회 불가)을 느낀 직후 처방. **한 관계만**(Post↔Comment) 전환하고 나머진 단방향 유지 → "양방향은 선택"임을 보여줌.

### 9단계 — 전체 조망 (`erd.md`)

지금까지 만든 5개 엔티티(Member·Department·Post·Comment·PostLike)를 ERD로 한눈에. 관계·NOT NULL·상속 구조를 시각적으로 정리.

---

## 문서 지도

| 문서 | 다루는 것 | 로드맵 단계 |
|---|---|---|
| `why-jpa.md` | 왜 JPA인가 (JDBC/MyBatis 대비) | 2 |
| `association-mapping-guide.md` | 단방향/양방향, 주인·mappedBy, LAZY | 5, 8 |
| `postlike-composite-key-guide.md` | 복합키(@EmbeddedId/@MapsId/@IdClass), 연결 엔티티 | 7 |
| `erd.md` | 전체 도메인 관계도 | 9 |
| `dynamic-query-guide.md` | JPA가 불편한 복잡 쿼리 → MyBatis 병행 | 보조 |
| `implements-vs-extends.md`, `queryForObject.md` | 보조 개념 | 보조 |

---

## 한 줄 요약

> **준비(0~1) → 왜 JPA(2) → 첫 엔티티·CRUD(3~4) → 단방향 연관관계(5) → 공통 필드(6) → 복합키(7) → 양방향(8) → ERD 조망(9).**
> 매 단계 "결핍을 먼저, 처방은 나중에", 그리고 "문법보다 왜"를 지킨다.
