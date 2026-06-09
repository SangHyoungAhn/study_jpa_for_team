# 동적쿼리 기술 선택 가이드

JPA / Querydsl 로 처리하기 어려운 동적쿼리를 무엇으로 풀어야 하는지에 대한 정리.

---

## 결론 요약

```
Querydsl로 95%는 해결 → 안 되는 부분만 골라서:

복잡한 통계/리포트성 쿼리, SQL 튜닝 필요    →  MyBatis
간단한 동적 한두 개, 배치성 단순 SQL         →  JdbcTemplate
거의 안 씀 (정적이거나 아주 단순할 때만)      →  @Query
```

> 핵심 원칙: "JPA/Querydsl로 **안 되는** 게 아니라, **읽기 힘들어지는** 순간" 다른 도구로 넘긴다.
> 억지로 Querydsl로 다 짜다가 미궁이 되는 것보다, 그 쿼리만 SQL로 깔끔하게 빼는 게 유지보수에 낫다.

---

## 1. `@Query` (Spring Data JPA) — 동적쿼리엔 비추천

```java
// @Query는 이런 "정적인" 쿼리엔 좋다
@Query("select m from Member m where m.age > :age")
List<Member> findOlderThan(@Param("age") int age);
```

- **적합**: 정적인 JPQL / 네이티브 쿼리 하나 박아야 할 때.
- **부적합**: 동적 조건 분기. 조건 유무를 분기하려면 JPQL 문자열을 자바에서 `+`로 연결해야 함 → 오타 위험, 가독성 최악.

## 2. `JdbcTemplate` — 가볍지만 동적쿼리엔 손이 많이 감

- **장점**: 의존성 가볍고, 영속성 컨텍스트 안 거치고 바로 DB. 대량 배치/단순 조회에 빠름.
- **단점**: 동적 조건 분기를 자바 `StringBuilder`로 직접 작성. `NamedParameterJdbcTemplate`을 써도 MyBatis `<if>`만큼 깔끔하진 않음.

## 3. `MyBatis` — 복잡한 동적쿼리의 정답에 가까움

```xml
<!-- 동적쿼리는 MyBatis가 가장 읽기 좋다 -->
<select id="search" resultType="Member">
  SELECT * FROM member
  <where>
    <if test="name != null">AND name = #{name}</if>
    <if test="minAge != null">AND age >= #{minAge}</if>
  </where>
</select>
```

- `<if>`, `<choose>`, `<foreach>` 덕분에 복잡한 조건 분기 / IN절 / 통계 쿼리가 SQL과 거의 1:1로 보인다.
- SQL을 직접 통제해야 하는 튜닝 상황에서 DBA와 소통하기 좋다.
- 이 프로젝트의 `excodeMyBatis` 구조(`member.xml`, `MemberDao`)처럼 **JPA + MyBatis 공존**은 실무에서 가장 흔하고 합리적인 선택.

---

## 실무 권장 조합

| 작업 | 도구 |
|------|------|
| 기본 CRUD, 단건 조회 | **JPA (Spring Data)** |
| 일반적인 동적 검색 (목록 + 필터) | **Querydsl** |
| 복잡한 통계/리포트, 다중 조인 튜닝 | **MyBatis** |
| 대량 배치 INSERT/UPDATE, 단순 native | **JdbcTemplate** |
