# JdbcTemplate - queryForObject

## 반환 타입

| 메서드 | 반환 타입 | 결과 건수 |
|--------|-----------|-----------|
| `query()` | `List<T>` | 0개 이상 |
| `queryForObject()` | `T` (단일 객체) | 반드시 1개 |

`queryForObject`의 "Object"는 **단일 객체 하나**를 의미한다.  
설계 의도 자체가 **"결과가 반드시 1건"** 이라는 가정 하에 만들어진 메서드다.

---

## 결과 건수별 동작

| 결과 건수 | 동작 |
|-----------|------|
| 1건 | 정상 반환 |
| 0건 | `EmptyResultDataAccessException` 발생 |
| 2건 이상 | `IncorrectResultSizeDataAccessException` 발생 |

결과가 0건이거나 2건 이상이면 **"예상 밖의 상황"** 으로 간주하여 예외를 던진다.

---

## Optional로 감싸는 방법

`Optional<T>` 반환이 필요한 경우, JdbcTemplate이 직접 지원하지 않으므로  
`EmptyResultDataAccessException`을 catch해서 처리한다.

```java
@Override
public Optional<Item> findById(Long id) {
    String sql = "SELECT ID, ITEM_NAME, PRICE, QUANTITY FROM ITEM WHERE ID = ?";
    try {
        Item item = template.queryForObject(sql, itemRowMapper(), id);
        return Optional.of(item);
    } catch (EmptyResultDataAccessException e) {
        return Optional.empty(); // 결과 없으면 empty 반환
    }
}
```

---

## query() vs queryForObject() 선택 기준

- 결과가 **항상 1건**임이 보장될 때 → `queryForObject()`
- 결과가 **0건일 수도 있을 때** → `queryForObject()` + try-catch, 또는 `query()` 사용
- 결과가 **여러 건**일 때 → `query()`
