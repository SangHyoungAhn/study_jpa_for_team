# implements vs extends

## 기본 개념

| 키워드 | 대상 | 의미 |
|--------|------|------|
| `implements` | 인터페이스 | 구현 ("이 규약을 내가 직접 구현할게") |
| `extends` | 클래스 | 상속 ("부모 클래스를 물려받을게") |

```java
// 구현
public class JdbcTemplateItemRepositoryV1 implements ItemRepository { ... }

// 상속
public class ArrayList extends AbstractList { ... }
```

---

## 다중 적용 가능 여부

`implements`는 여러 인터페이스를 동시에 구현 가능하지만,  
`extends`는 클래스 하나만 상속 가능 (Java는 다중 상속 불가).

```java
public class MyClass extends ParentClass implements InterfaceA, InterfaceB { ... }
```

---

## 인터페이스끼리의 extends — "확장"

```java
public interface MemberRepository extends JpaRepository<Member, Long>, MemberCustomRepository
```

인터페이스끼리 `extends`를 쓰는 건 **상속이 아니라 규약의 확장**이다.

| | 클래스 extends | 인터페이스 extends |
|---|---|---|
| 의미 | 부모 기능을 물려받음 | 규약을 합쳐서 확장 |
| 다중 가능 여부 | 불가 (1개만) | 가능 (여러 개) |
| 실제 코드 존재 | 있음 | 없음 (규약만) |

**핵심:** "이 인터페이스를 구현하는 클래스는 저 두 인터페이스의 메서드도 모두 구현해야 한다"는 규약의 합침으로 보는 것이 정확하다.
