# JPA Checkpoint — 패러다임 불일치와 실무 매핑 전략

## 1. 패러다임 불일치란?

**패러다임 불일치(Paradigm Mismatch)** 는 **객체 지향 프로그래밍(OOP)** 과 **관계형 데이터베이스(RDB)** 가 데이터를 표현하고 다루는 방식이 근본적으로 다르다는 데서 비롯됩니다.

| 구분 | 객체 지향 | 관계형 DB |
|---|---|---|
| 핵심 목표 | 추상화, 캡슐화, 상속, 다형성 | 정규화, 집합 이론, SQL |
| 데이터 표현 | 객체(필드 + 메서드) | 행(Row)과 열(Column) |
| 연관 표현 | 참조(reference) | 외래키(Foreign Key) |
| 탐색 방식 | `.` 으로 그래프 탐색 | JOIN |
| 식별 방식 | 동일성(`==`) + 동등성(`equals`) | PK 값 |

이 두 패러다임을 메꾸기 위해 개발자가 수동으로 SQL과 객체 변환 코드를 작성하면, 결국 "데이터 변환을 위한 보일러플레이트 코드"가 비즈니스 로직보다 많아지는 문제가 발생합니다. JPA(ORM)는 이 불일치를 프레임워크 레벨에서 자동으로 해결해줍니다.

---

## 2. 5가지 불일치 문제와 JPA의 해결 방식

### 2.1 상속 (Inheritance)

#### 문제
객체는 `extends`를 통해 상속 관계를 표현할 수 있지만, RDB에는 상속 개념 자체가 없습니다.

```java
abstract class Item { Long id; String name; int price; }
class Album extends Item { String artist; }
class Movie extends Item { String director; }
class Book  extends Item { String author; }
```

위 객체 구조를 RDB에 그대로 표현할 수 없기 때문에, 슈퍼타입–서브타입 관계를 테이블로 어떻게 풀어낼지 직접 설계해야 합니다.

#### JPA의 해결
JPA는 `@Inheritance` 전략으로 객체 상속을 테이블로 매핑합니다.

| 전략 | 설명 |
|---|---|
| `SINGLE_TABLE` | 하나의 테이블에 모든 자식 컬럼을 합치고 `DTYPE`으로 구분 (기본값) |
| `JOINED` | 부모/자식 테이블을 분리, JOIN으로 조회 (정규화에 가장 가까움) |
| `TABLE_PER_CLASS` | 자식 엔티티마다 독립 테이블 (UNION 방식) |

```java
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "DTYPE")
public abstract class Item { ... }

@Entity
@DiscriminatorValue("A")
public class Album extends Item { ... }
```

개발자는 `em.find(Album.class, id)`로 객체처럼 조회하면, JPA가 적절한 JOIN/SELECT를 생성합니다.

> 📌 **실무에서는 `@Inheritance`를 잘 쓰지 않습니다.** 자세한 내용은 [§4. 실무에서의 상속 매핑](#4-실무에서의-상속-매핑) 참고.

---

### 2.2 연관관계 (Association)

#### 문제
- 객체는 **참조(reference)** 로 연관관계를 맺습니다. → 방향이 존재합니다.
- DB는 **외래키(FK)** 로 연관관계를 맺습니다. → 방향이 없고, JOIN으로 양쪽에서 접근 가능합니다.

```java
// 객체: Order는 Member를 알지만, Member는 Order를 모를 수 있음
class Order { Member member; }

// DB: ORDERS 테이블의 MEMBER_ID FK 하나로 양쪽 조회 가능
```

이 차이 때문에 개발자는 객체의 참조를 FK로, 또는 FK를 객체 참조로 변환하는 코드를 직접 작성해야 합니다.

#### JPA의 해결
`@ManyToOne`, `@OneToMany`, `@OneToOne`, `@ManyToMany`로 객체의 참조와 DB의 외래키를 매핑합니다.

```java
@Entity
public class Order {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;
}
```

- 개발자는 `order.getMember()` 같은 객체 지향적인 방식으로 접근합니다.
- JPA가 내부적으로 FK를 통한 JOIN 또는 추가 SELECT 쿼리를 생성합니다.
- 양방향 매핑이 필요한 경우 **연관관계의 주인(Owner)** 을 지정해 어느 쪽이 FK를 관리할지 결정합니다.

---

### 2.3 객체 그래프 탐색 (Graph Traversal)

#### 문제
객체는 마음껏 그래프를 탐색할 수 있습니다.
```java
order.getMember().getTeam().getName()
```

하지만 SQL은 처음 실행할 때 JOIN을 미리 결정해야 합니다. JOIN되지 않은 객체를 탐색하려고 하면 `null`이거나 `NullPointerException`이 발생할 수 있습니다. → 이로 인해 **신뢰할 수 없는 객체 그래프** 문제가 생깁니다.

또한 단순 반복 조회를 하면 **N+1 문제**가 발생합니다.

#### JPA의 해결

**(1) 지연 로딩(Lazy Loading)**
실제로 객체를 사용하는 시점에 SELECT 쿼리를 자동으로 실행합니다.

```java
@ManyToOne(fetch = FetchType.LAZY)
private Member member;

// 이 시점에 member SELECT 쿼리 실행
order.getMember().getName();
```

**(2) 즉시 로딩(Eager) / Fetch Join**
필요한 경우 JPQL의 `JOIN FETCH`로 한 번에 모든 그래프를 가져와 N+1 문제를 해결합니다.

```java
SELECT o FROM Order o JOIN FETCH o.member
```

**(3) EntityGraph**
조회 시점마다 필요한 연관 객체를 명시적으로 지정할 수 있습니다.

```java
@EntityGraph(attributePaths = {"member", "delivery"})
List<Order> findAll();
```

→ 결과적으로 개발자는 객체 그래프를 자유롭게 탐색하면서도 N+1 같은 성능 이슈를 통제할 수 있습니다.

---

### 2.4 동일성 비교 (Identity)

#### 문제
- 자바 객체: **동일성(`==`, 인스턴스 비교)** 과 **동등성(`equals()`, 값 비교)** 가 구분됩니다.
- DB: **PK 값**만으로 동일성을 판단합니다.

따라서 DB에서 같은 PK를 두 번 조회하면, 자바에서는 서로 다른 인스턴스가 만들어집니다.

```java
Member m1 = em.find(Member.class, 1L);
Member m2 = em.find(Member.class, 1L);
m1 == m2; // 일반적인 JDBC라면 false
```

#### JPA의 해결: 1차 캐시(Persistence Context)
JPA의 **영속성 컨텍스트(Persistence Context)** 는 트랜잭션 내에서 엔티티를 1차 캐시에 보관합니다. 같은 트랜잭션 내에서 같은 PK로 조회하면 **같은 인스턴스**가 반환됩니다.

```java
Member m1 = em.find(Member.class, 1L); // DB 조회 → 1차 캐시 저장
Member m2 = em.find(Member.class, 1L); // 1차 캐시에서 반환
m1 == m2; // true
```

이는 단순한 성능 개선을 넘어, **반복 가능한 읽기(Repeatable Read)** 수준의 동일성 보장을 애플리케이션 레벨에서 제공합니다.

---

### 2.5 세분성 (Granularity)

#### 문제
객체는 작은 단위로 세밀하게 쪼갤 수 있습니다.

```java
class Member {
    String name;
    Address address; // city, street, zipcode를 가진 별도의 값 객체
}
```

하지만 DB는 보통 하나의 테이블에 컬럼을 펼쳐서 표현합니다.

```
MEMBER(id, name, city, street, zipcode)
```

이를 수동으로 매핑하면 객체와 테이블 사이에 변환 코드가 계속 발생합니다.

#### JPA의 해결: `@Embeddable` / `@Embedded`
값 타입을 별도 객체로 분리하면서도 DB에서는 한 테이블로 펼쳐서 저장할 수 있습니다.

```java
@Embeddable
public class Address {
    private String city;
    private String street;
    private String zipcode;
}

@Entity
public class Member {
    @Embedded
    private Address address;
}
```

→ 객체 모델은 풍부하게 유지하면서, DB 스키마는 단순한 컬럼 구조로 저장됩니다.

---

## 3. JPA가 불일치를 해결하는 내부 메커니즘

JPA가 단순히 SQL을 생성하는 것 이상으로 "객체처럼 다룰 수 있게" 해주는 핵심 메커니즘들입니다.

### 3.1 영속성 컨텍스트 (Persistence Context)
- 엔티티를 보관하는 **논리적 저장소**.
- 엔티티의 상태: `New(비영속)`, `Managed(영속)`, `Detached(준영속)`, `Removed(삭제)`.
- 트랜잭션 단위로 동작하며, 동일성 보장, 변경 감지, 쓰기 지연 등의 기능이 모두 여기서 이루어집니다.

### 3.2 1차 캐시
- 같은 트랜잭션 내에서 동일 PK 조회 시 인스턴스를 재사용.
- DB 왕복(round trip)을 줄이고 동일성을 보장합니다.

### 3.3 변경 감지 (Dirty Checking)
- 영속 상태의 엔티티는 **스냅샷(Snapshot)** 과 함께 보관됩니다.
- 트랜잭션 커밋 시점에 현재 값과 스냅샷을 비교해 변경된 필드만 UPDATE 쿼리를 자동 생성합니다.

```java
Member m = em.find(Member.class, 1L);
m.setName("새 이름"); // setter 호출만으로 UPDATE 쿼리 자동 발생
// em.update(m) 같은 코드 불필요
```

### 3.4 쓰기 지연 (Write-Behind)
- `persist()`, `merge()`, `remove()` 호출 시 바로 SQL을 실행하지 않고 **쓰기 지연 SQL 저장소**에 모아둡니다.
- `flush()` 또는 트랜잭션 커밋 시점에 모아서 한 번에 DB로 전송 → 네트워크 비용 감소.

### 3.5 플러시 (Flush)
- 영속성 컨텍스트의 변경 내용을 DB에 동기화하는 작업.
- 자동 플러시 시점: 커밋 직전, JPQL 쿼리 실행 직전, `flush()` 직접 호출.

---

## 4. 실무에서의 상속 매핑

> "JPA에서 객체 상속은 `@Inheritance`로 많이 사용하나? 더 나은 방법은 없을까?"

결론부터 말하면, **`@Inheritance`는 생각보다 자주 쓰이지 않습니다.** 실무에서는 다음 순서로 선호도가 갈립니다.

### 4.1 `@MappedSuperclass` — 가장 많이 사용

상속이라기보다 **공통 필드 재사용** 목적입니다. 테이블은 분리되지 않고, 자식 엔티티의 컬럼으로 흡수됩니다.

```java
@MappedSuperclass
@Getter
public abstract class BaseEntity {
    @CreatedDate
    private LocalDateTime createdAt;
    @LastModifiedDate
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
}

@Entity
public class Member extends BaseEntity { ... }

@Entity
public class Order extends BaseEntity { ... }
```

- 생성일/수정일 같은 **감사(Auditing) 필드**를 모든 엔티티에서 재사용할 때 사실상 표준
- 테이블에는 상속 흔적이 없음 → DB 설계가 깔끔
- 다형성 쿼리(`SELECT b FROM BaseEntity b`) 불가능 → 어차피 안 함

### 4.2 합성(Composition) — 그 다음으로 권장

객체 지향 원칙인 **"상속보다 합성(Composition over Inheritance)"** 을 JPA에서도 동일하게 적용합니다. → [§5. 합성으로 풀어내는 실전 예제](#5-합성으로-풀어내는-실전-예제) 참고.

### 4.3 `@Inheritance` — 특정 상황에서만

진짜 상속 매핑은 다음 조건이 모두 충족될 때만 사용합니다.

| 조건 | 설명 |
|---|---|
| 진정한 IS-A 관계 | Album **is a** Item, Movie **is a** Item |
| 다형성 쿼리 필요 | "모든 Item 조회"가 빈번한 비즈니스 요구 |
| 서브 타입이 안정적 | 새로운 타입이 자주 추가되지 않음 |

이 조건에 맞을 때 전략 선택:

- **`SINGLE_TABLE`** (기본값) — 성능 최고, NULL 컬럼 다수 발생. 서브타입 컬럼이 적을 때 추천
- **`JOINED`** — 정규화에 가장 충실, JOIN 비용 발생. **실무에서 굳이 상속을 쓴다면 이게 가장 많이 선택됨**
- **`TABLE_PER_CLASS`** — 추천하지 않음 (UNION 쿼리, 식별자 관리 어려움)

### 4.4 실무 사용 빈도 (체감)

```
@MappedSuperclass            ████████████████████  매우 빈번
합성 / 독립 엔티티           ████████████████      매우 빈번
@Inheritance(JOINED)         ████                  가끔
@Inheritance(SINGLE_TABLE)   ██                    드물게
@Inheritance(TABLE_PER_CLASS) ▏                    거의 없음
```

> 김영한 강의 / 실무자 일반적 권장사항:
> "객체 상속 관계를 DB 테이블 매핑에 사용할 때는 신중해야 한다. 단순한 경우가 아니면 **상속을 사용하지 말고 합성으로 풀어내는 것**이 유지보수에 유리하다."

---

## 5. 합성으로 풀어내는 실전 예제

같은 도메인(Item / Album / Movie / Book)을 세 가지 방식으로 비교합니다.

### 5.1 출발점: 상속 방식

```java
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Item {
    @Id @GeneratedValue
    private Long id;
    private String name;
    private int price;
}

@Entity
public class Album extends Item { private String artist; }

@Entity
public class Movie extends Item { private String director; private String actor; }

@Entity
public class Book extends Item { private String author; private String isbn; }
```

문제 제기: Album이 정말 Item을 **"상속"** 해야 하나? Item의 정체성을 **"가지고 있는"** 것 아닐까?

---

### 5.2 방법 A. `@Embeddable`로 값 타입 분리 (가장 가벼움)

공통 속성을 **값 객체(Value Object)** 로 추출해 각 엔티티가 "포함"하게 합니다.

```java
@Embeddable
@Getter @Setter
public class ItemInfo {
    private String name;
    private int price;
    private int stockQuantity;
}

@Entity
public class Album {
    @Id @GeneratedValue
    private Long id;

    @Embedded
    private ItemInfo itemInfo;  // 합성

    private String artist;
}

@Entity
public class Movie {
    @Id @GeneratedValue
    private Long id;

    @Embedded
    private ItemInfo itemInfo;  // 합성

    private String director;
    private String actor;
}
```

**테이블 구조**
```
ALBUM(id, name, price, stock_quantity, artist)
MOVIE(id, name, price, stock_quantity, director, actor)
BOOK (id, name, price, stock_quantity, author, isbn)
```

- 각 엔티티가 독립 테이블 → 단순함
- `ItemInfo`는 재사용 가능한 값 타입
- 다형성 쿼리 불가능, 그러나 보통 필요 없음

---

### 5.3 방법 B. 독립 엔티티 + `@ManyToOne` (관계로 풀기)

"카테고리"나 "상품 분류"가 필요한 경우 별도 엔티티로 분리합니다.

```java
@Entity
public class Category {
    @Id @GeneratedValue
    private Long id;
    private String name; // "음반", "영화", "책"
}

@Entity
public class Product {
    @Id @GeneratedValue
    private Long id;
    private String name;
    private int price;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;
}

@Entity
public class Album {
    @Id @GeneratedValue
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;  // 합성

    private String artist;
}
```

- **"상품으로서의 측면"** 과 **"앨범으로서의 측면"** 이 명확히 분리
- 상품 정보 변경이 Product 한 곳에서만 일어남
- `productRepository.findAll()`로 전체 상품 조회 가능 (다형성 대체)

---

### 5.4 방법 C. 단일 엔티티 + 옵셔널 필드 (실용주의)

서브 타입이 적고 필드 차이도 적다면, **그냥 한 테이블에 다 넣는** 방식이 가장 단순합니다.

```java
@Entity
public class Item {
    @Id @GeneratedValue
    private Long id;

    private String name;
    private int price;

    @Enumerated(EnumType.STRING)
    private ItemType type;  // ALBUM, MOVIE, BOOK

    private String artist;    // Album일 때만
    private String director;  // Movie일 때만
    private String author;    // Book일 때만
}

public enum ItemType { ALBUM, MOVIE, BOOK }
```

- `@Inheritance(SINGLE_TABLE)`와 결과 테이블은 같지만, 코드가 훨씬 단순
- 타입 분기는 enum + 서비스 레이어에서 처리
- **실무에서 이 방식이 가장 흔합니다** — "오버엔지니어링하지 말자"

---

### 5.5 선택 기준

| 상황 | 추천 방식 |
|---|---|
| 공통 속성을 여러 엔티티에서 재사용하고 싶음 | **A. `@Embeddable`** |
| 공통 속성이 독립적인 라이프사이클을 가짐 (예: 상품 정보가 별도로 관리됨) | **B. 독립 엔티티 + 관계** |
| 타입 차이가 작고 단순함, 빠르게 만들고 싶음 | **C. 단일 엔티티 + enum** |
| 진짜 다형성 쿼리가 비즈니스 요구 | `@Inheritance(JOINED)` |

---

## 6. 합성 방식의 전체 흐름 (저장 → 조회 → 사용)

방법 A(`@Embeddable`)를 기반으로, 실제 코드 흐름을 보여줍니다.

### 6.1 값 객체 정의

```java
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ItemInfo {
    private String name;
    private int price;
    private int stockQuantity;

    public ItemInfo(String name, int price, int stockQuantity) {
        this.name = name;
        this.price = price;
        this.stockQuantity = stockQuantity;
    }

    // 비즈니스 로직도 값 객체에 응집
    public void removeStock(int quantity) {
        int restStock = this.stockQuantity - quantity;
        if (restStock < 0) {
            throw new IllegalStateException("재고가 부족합니다.");
        }
        this.stockQuantity = restStock;
    }

    public void addStock(int quantity) {
        this.stockQuantity += quantity;
    }
}
```

값 객체에 **관련 비즈니스 로직**을 같이 두는 것이 핵심입니다. (도메인 응집도 ↑)

### 6.2 엔티티 정의

```java
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Album {

    @Id @GeneratedValue
    @Column(name = "album_id")
    private Long id;

    @Embedded
    private ItemInfo itemInfo;

    private String artist;
    private String etc;

    public Album(ItemInfo itemInfo, String artist, String etc) {
        this.itemInfo = itemInfo;
        this.artist = artist;
        this.etc = etc;
    }

    // 위임 메서드 (선택)
    public void removeStock(int quantity) {
        this.itemInfo.removeStock(quantity);
    }
}
```

### 6.3 Repository — 일반 JPA처럼 사용

```java
public interface AlbumRepository extends JpaRepository<Album, Long> {

    // ItemInfo의 필드도 자연스럽게 쿼리 가능
    List<Album> findByItemInfoNameContaining(String keyword);

    @Query("select a from Album a where a.itemInfo.price <= :maxPrice")
    List<Album> findCheaperThan(@Param("maxPrice") int maxPrice);
}
```

`@Embedded`이기 때문에 별도 테이블이 없고, 쿼리 시 **`album.name`** 컬럼으로 매핑됩니다.

### 6.4 서비스 레이어

```java
@Service
@RequiredArgsConstructor
@Transactional
public class AlbumService {

    private final AlbumRepository albumRepository;

    public Long createAlbum(String name, int price, int stock, String artist) {
        ItemInfo info = new ItemInfo(name, price, stock);
        Album album = new Album(info, artist, null);
        return albumRepository.save(album).getId();
    }

    public void order(Long albumId, int quantity) {
        Album album = albumRepository.findById(albumId)
                .orElseThrow(() -> new IllegalArgumentException("앨범 없음"));

        album.removeStock(quantity);  // 변경 감지로 자동 UPDATE
    }
}
```

---

## 7. 다형성 쿼리가 필요할 땐?

합성으로 풀었을 때 자주 듣는 반박: **"그럼 전체 아이템을 한 번에 조회하려면?"**

### 7.1 방법 1. 별도 통합 뷰 테이블 / Materialized View

```sql
CREATE VIEW v_items AS
  SELECT 'ALBUM' AS type, id, name, price FROM album
  UNION ALL
  SELECT 'MOVIE' AS type, id, name, price FROM movie
  UNION ALL
  SELECT 'BOOK'  AS type, id, name, price FROM book;
```

읽기 전용 엔티티로 매핑하면 통합 조회가 가능합니다.

### 7.2 방법 2. 검색 전용 DTO + UNION 쿼리

JPQL의 한계 때문에 보통 **네이티브 쿼리** 또는 **QueryDSL UNION**을 사용합니다.

```java
public List<ItemSummary> searchAllItems(String keyword) {
    String sql = """
        SELECT 'ALBUM' as type, id, name, price FROM album WHERE name LIKE :kw
        UNION ALL
        SELECT 'MOVIE' as type, id, name, price FROM movie WHERE name LIKE :kw
        UNION ALL
        SELECT 'BOOK'  as type, id, name, price FROM book  WHERE name LIKE :kw
        """;
    return em.createNativeQuery(sql, ItemSummary.class)
             .setParameter("kw", "%" + keyword + "%")
             .getResultList();
}
```

### 7.3 방법 3. 검색 인덱스 분리 (실무 정답에 가까움)

- Elasticsearch / OpenSearch 같은 별도 검색 엔진에 통합 인덱스 구성
- "전체 상품 통합 검색"은 어차피 RDB JOIN으로 풀기엔 부담이 크기 때문에, 큰 서비스에서는 거의 이 방식

---

## 8. 합성 방식의 함정과 주의점

### 8.1 값 객체는 **불변(Immutable)** 으로 설계하는 게 안전

```java
@Embeddable
public class Address {
    private String city;
    private String street;
    private String zipcode;

    protected Address() {}

    public Address(String city, String street, String zipcode) {
        this.city = city;
        this.street = street;
        this.zipcode = zipcode;
    }
    // setter 없음! 변경하려면 새 객체로 교체
}
```

값 객체는 **공유**될 수 있어, setter로 수정하면 다른 엔티티의 값까지 같이 바뀌는 버그가 생깁니다.

### 8.2 `@Embeddable` 안에서 연관관계 사용 시 주의

`@Embeddable` 내부에 `@ManyToOne` 같은 연관관계를 두는 것은 가능하지만, **소유권/생명주기**가 모호해지기 쉽습니다. 그럴 거면 차라리 별도 엔티티로 빼는 게 낫습니다.

### 8.3 컬럼명 충돌

같은 `@Embeddable`을 한 엔티티에서 두 번 쓸 때 `@AttributeOverrides`가 필요합니다.

```java
@Entity
public class Member {
    @Embedded
    private Address homeAddress;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "city", column = @Column(name = "work_city")),
        @AttributeOverride(name = "street", column = @Column(name = "work_street")),
        @AttributeOverride(name = "zipcode", column = @Column(name = "work_zipcode"))
    })
    private Address workAddress;
}
```

---

## 9. 상속 vs 합성 최종 비교

```
[상속 방식]                          [합성 방식]
┌────────────┐                       ┌──────────────┐
│   Item     │                       │  ItemInfo    │ ← 값 객체
│ (abstract) │                       │ (Embeddable) │
└─────┬──────┘                       └──────┬───────┘
      │ extends                             │ has-a
  ┌───┴───┬───────┐                  ┌──────┴──────┬──────┐
  ▼       ▼       ▼                  ▼             ▼      ▼
Album   Movie   Book                Album         Movie  Book
                                    (포함)        (포함) (포함)
```

| 관점 | 상속 | 합성 |
|---|---|---|
| 결합도 | 강함 (부모 변경이 자식 전부에 영향) | 약함 (값 객체 변경 영향 제한적) |
| 다형성 쿼리 | 자연스러움 | 별도 처리 필요 |
| 테이블 설계 | 복잡 (전략에 따라 JOIN 또는 SINGLE) | 단순 (각 엔티티별 평탄한 테이블) |
| 변경 유연성 | 낮음 | 높음 |
| 학습 비용 | 높음 (전략 선택, 동작 이해 필요) | 낮음 |

---

## 10. 정리

### 패러다임 불일치 ↔ JPA 해결

| 패러다임 불일치 | JPA의 해결 |
|---|---|
| 상속 | `@Inheritance` 전략(SINGLE_TABLE / JOINED / TABLE_PER_CLASS) — 단, 실무에선 `@MappedSuperclass`/합성 우선 |
| 연관관계 | `@ManyToOne`, `@OneToMany` 등 + 연관관계의 주인 개념 |
| 그래프 탐색 / N+1 | 지연 로딩 + Fetch Join + EntityGraph |
| 동일성 비교 | 영속성 컨텍스트의 1차 캐시 |
| 세분성 | `@Embeddable` / `@Embedded` 값 타입 매핑 |

### 핵심 메시지

1. JPA는 단순히 "SQL을 자동으로 만들어주는 도구"가 아니라, **객체 지향 모델을 유지한 채 RDB와 상호작용할 수 있게 해주는 패러다임 어댑터**입니다. 영속성 컨텍스트, 변경 감지, 쓰기 지연 등의 메커니즘이 그 핵심에 있습니다.

2. **"is-a"가 아니라 "has-a"로 표현할 수 있다면, 상속보다 합성**이 거의 항상 더 유연하고 유지보수하기 좋습니다. JPA에서도 동일한 OOP 원칙이 적용됩니다.

3. 합성이 만능은 아니지만, **"의심스러우면 합성부터 시도하라"** 가 현대 도메인 모델링의 기본 자세입니다. JPA는 도구일 뿐, 핵심은 **객체 모델을 단순하고 변경 가능하게 유지하는 것**입니다.
