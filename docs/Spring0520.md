- Entity class 에서 @ToString 쓸 때, 필드값으로 사용하기 

---

```java
public class JPAQuery{

 @Autowired
 EntityManager em;

 JPAQueryFactory queryFactory;
 //동시성 문제 (멀티쓰레드 호환가능) 따라서, 필드로 빼서 사용하자.
 
 public void test(){
 	queryFactory = new JPAQueryFactory(em);
 }
}
```

Q 클래스 인스턴스를 사용하는 2가지 `방법`

---

```java
//1
QMember m = new QMember("m");

//2
QMember member = QMember.member;

//1 & 2 m의 위치에 member
Member findMember = queryFactory.select(m)
					.from(m)
					.where(m.username.eq("member1"))
					.fetchOne();

//1 & 2 둘다 지저분하다.

import static QMember.*;
Member findMember = queryFactory.select(member)
  				.from(member)
  				.where(member.username.eq("member1"))
  				.fetchOne();
```

대부분 static import 를 **사용**

---

```java
Member findMember = queryFactory
				.selectFrom(member) //Select와 from 결합 가능
				.where(member.username.eq("member1"))
								.and(member.age.eq(10))) //and & or 도 가능
				.fetchOne();
				
```

```java
=
member.username.eq("member1")
!=
member.username.ne("member1")
!=
member.username.eq("member1").not()

member.username().isNotNull() // is not null

member.age.in(10,20)
member.age.notIn(10,20)
member.age.between(10,30)

>=
member.age.goe(30)
>
member.age.gt(30)
<=
member.age.loe(30)
<
member.age.lt(30)

like 검색
member.username.like("member%")
like 'member%' 검색
member.username.contains("member")
member.username.startsWith("member")
```

```java
Member findMember = queryFactory
				.selectFrom(member) //Select와 from 결합 가능
				.where(member.username.eq("member1"))
								.and(member.age.eq(10))) //and & or 도 가능
				.fetchOne();
				
				
Member findMember = queryFactory
				.selectFrom(member) 
				.where(
						member.username.eq("member1"),
						member.age.eq(10)
				)
				.fetchOne();
```

---

- Fetch() 리스트 조회, 데이터 없으면 빈 리스트 반환
- fetchOne() 단 건 조회.
  - 결과가 없으면 : null
  - 결과가 둘 이상이면: com.querydsl.core.NonUniqueResultException

- fetchFrist(): limit(1).fetchOne()
- fetchResults(): 페이징 정보 포함.  Total count 쿼리 추가 실행
- fetchCount(): count 쿼리로 변경해서 count 수 조회

```java

queryFactory
          .selectFrom(member)
          .fetchFirst();
      //  .limit(1).fetchOne();

QueryResults<Member> results = queryFactory
        .selectFrom(member)
        .fetchResults();
long totalResult = results.getTotal();
List<Member> resultMember = results.getResults();
//results.getResults() 해야 데이터가 나오고
//getTotal() 전체 데이터를 알려준다.

long total = queryFactory
        .selectFrom(member)
        .fetchCount();
//count 쿼리라고 생각하면된다.
```

---

- Tuple 은 현업에서 잘 사용하지 않는다.

---

- 기본조인

  - 조인의 기본 문법은 첫번째 파라미터에 조인 대상을 지정하고, 두번째 파라미터에 

    별칭으로 사용할 Q 타입을 지정하면된다.

  - **<u>연관관계 없어도 조인이 가능하다 (세타조인)</u>**
  - LeftOuterJoin , RightOuterJoin 같은 
    **<u>외부조인은 on절 사용으로 해결</u>**** (세타조인 제약조건)

```java
List<Member> basicJoinResult = queryFactory
        .selectFrom(member)
        .join(member.team, team)
        .where(team.name.eq("teamA"))
        .fetch();
        
List<Member> thetaJoinResult = queryFactory
        .select(member)
        .from(member, team)
        .where(member.username.eq(team.name))
        .fetch();

```

- ON 절
  -  조인 대상 필터링
  - 연관관계 없는 엔티티 외부조인 (세타조인의 제약조건을 해결하는 방법)

```java
List<Tuple> result = queryFactory
        .select(member, team)
        .from(member)
        .leftJoin(member.team, team)
        //.on(team.name.eq("teamA"))
        .where(team.name.eq("teamA"))
        .fetch();
```

- On 절을 활용해 조인 대상을 필터링 할 때, 외부조인이 아니라 내부조인 (innerJoin) 을
  사용하면, where 절에서 필터링 하는 것과 기능이 동일하다.

  따라서 on 절을 활용한 조인 대상 필터링을 사용할때, 내부조인이면 익숙한 where절
  정말 외부조인이 필요한 경우에만 이 기능을 사용하자.

```java
// when
List<Tuple> result = queryFactory
        .select(member, team )
        .from(member)
        .leftJoin(team).on(member.username.eq(team.name))

  /**
   * select member1, team
   * from Member member1
   * left join Team team with member1.username = team.name
   */
  
  //원래는 .leftJoin(member.team, team)
  //										.on(member.username.eq(team.name))
  // ID가 매칭이되는게 들어가는것이고 member.team이 빠지면 그냥 필터링 on 기준
  /**
   * select member1, team
   * from Member member1
   *   left join member1.team as team 
   *     	with member1.username = team.name
   */
        .fetch();
```

---

Fetchjoin할때,

```java
@PersistenceUnit
EntityManagerFactory emf;

@Test
@DisplayName("fetchJoinNo")
void fetchJoinNo() {
  // given
  em.flush();
  em.clear();
  //Fetchjoin 테스트할때, 한번 데이터 정리해주는 게 좋다.


  // when
  Member member1 = queryFactory
          .selectFrom(member)
          .where(member.username.eq("member1"))
          .fetchOne();
  //FetchType.LAZY 인 경우, Team과 관계가 @ManyToOne 인 경우 다 가지고 오지 않는다.


  // then
  boolean loaded = emf.getPersistenceUnitUtil().isLoaded(member1.getTeam());
  assertThat(loaded).as("페치 조인 미적용").isFalse();
}

@Test
@DisplayName("fetchJoinYes")
void fetchJoinYes() {
  // given
  em.flush();
  em.clear();

  // when
  Member member1 = queryFactory
          .selectFrom(member)
          .join(member.team, team).fetchJoin()
          .where(member.username.eq("member1"))
          .fetchOne();

  // then
  boolean loaded = emf.getPersistenceUnitUtil().isLoaded(member1.getTeam());
  assertThat(loaded).as("페치 조인 적용").isTrue();
```

---

- 서브쿼리(where 절)

```java
@Test
@DisplayName("subQuery")
void subQuery() {
  // given

  //바깥에 있는 내용과 겹치면 안됨 ***
  QMember memberSub = QMember.member;

  // when
  List<Member> result = queryFactory
          .selectFrom(member)
          .where(member.age.eq(
                  JPAExpressions.select(memberSub.age.max())
                          .from(memberSub)

          ))
          .fetch();

  // then
  assertThat(result).extracting("age")
          .containsExactly(40);
}

@Test
@DisplayName("subQuery2")
void subQuery2GOE() {
  // given
  QMember memberSub = QMember.member;

  // when
  List<Member> queryResults = queryFactory.selectFrom(member)
          .where(member.age.goe(
                  JPAExpressions.select(memberSub.age.avg())
                          .from(memberSub)
          )).fetch();

  // then
  assertThat(queryResults).extracting("age")
          .containsExactly(30,40);
}

@Test
@DisplayName("subQueryIn")
void subQueryIn() {
  // given
  QMember memberSub = QMember.member;

  // when
  List<Member> queryResults = queryFactory.selectFrom(member)
          .where(member.age.in(
                  JPAExpressions
                          .select(memberSub.age)
                          .from(memberSub)
                          .where(memberSub.age.gt(10))
          ))
          .fetch();

  // then
  assertThat(queryResults).extracting("age")
          .containsExactly(20,30,40);
}
```

---

- 서브쿼리(Select 절)

```java
@Test
@DisplayName("selectSubQuery")
void selectSubQuery() {
  // given
  QMember memberSub = QMember.member;

  // when
  List<Tuple> result = queryFactory.select(member.username,
                  JPAExpressions
                          .select(memberSub.age.avg())
                          .from(memberSub))
          .from(member)
          .fetch();

  // then
  for (Tuple tuple : result) {
      System.out.println("tuple=" + tuple);
  }

}
```

- **JPA 사용하면 서브쿼리 한계**
  - **From 절에 서브쿼리(인라인 뷰)는 지원하지 않는다.** 
  - **JPA JPQL, Querydsl 지원하지 않는다.**
  - **<u>nativeSQL</u>** 을 사용한다. 

---

####  **case 문**

```java
@Test
@DisplayName("basicCase")
void basicCase() {
  // given

  // when
  List<String> result = queryFactory
                          .select(member.age
                                  .when(10).then("열살")
                                  .when(20).then("스무살")
                                  .otherwise("노인"))
                          .from(member)
                          .fetch();

  // then
  for (String s : result) {
      System.out.println("result=" + s);
  }
}

@Test
@DisplayName("complexCase")
void complexCase() {
  // given


  // when
  List<String> result = queryFactory
          .select(new CaseBuilder()
                  .when(member.age.between(0, 20)).then("0~20살")
                  .when(member.age.between(21, 30)).then("21~30살")
                  .otherwise("기타"))
          .from(member)
          .fetch();
  // then
  for(String s : result){
      System.out.println("result=" + s);
  }
}
```

---

- **concat**
  - **<u>stringValue() 쓸일이 많다.</u>**

```java
@Test
@DisplayName("concat")
void concat() {
  // given


  // when
  //{username}_{age}
  List<String> result = queryFactory
          .select(member.username.concat("_").concat(member.age.stringValue()))
          .from(member)
          .where(member.username.eq("member1"))
          .fetch();


  // then
  for (String s : result) {
      System.out.println("s = " + s);
  }
}
```

---

- Projection (select 대상 지정) 대상이 하나일때,

  - Projection 대상이 하나면 타입을 명확하게 지정할 수 있음

    Ex) select(member.username) 인 경우, List<String>

  - 프로젝트 대상이 둘 이상이면 튜플이나 DTO로 조회

```java
@Test
@DisplayName("projectionOne")
void projectionOne() {
  // given


  // when
  List<String> result = queryFactory
          .select(member.username)
          .from(member)
          .fetch();

  // then
  for (String s : result) {
      System.out.println("s = " + s);
  }
}
```

- Projection 대상이 두개 이상일때,
  - Tuple 일때, (com.querydsl.core)
  - service 나 controller 계층까지 넘어가지는 않는게 좋은 설계

```java
@Test
@DisplayName("tupleProjection")
void tupleProjection() {
  // given


  // when
  List<Tuple> result = queryFactory
          .select(member.username, member.age)
          .from(member)
          .fetch();

  // then
  for (Tuple tuple : result) {
      String username = tuple.get(member.username);
      Integer userage = tuple.get(member.age);
      System.out.println("username = " + username);
      System.out.println("userage = " + userage);
  }
}
```

- <u>**Projection 이 DTO 일때. (매우중요)**</u>
  1) new Operation 형태 
     - DTO의 패키지 이름을 다 적어줘야해서 지저분함
     - 생성자 방식만 지원 (setter 통해서, 값을 바꾸거나 할 수 없음)

```java  
//new Operation 형태인 경우

@Test
@DisplayName("findDtoByJPQL")
void findDtoByJPQL() {
  // given


  // when 
  // 파일의 경로를 다 입력해줘야한다.
  List<MemberDto> resultList = em.createQuery("select new study.querydsl.dto.MemberDto(m.username, m.age) from Member m ", MemberDto.class)
          .getResultList();
  // then
  for (MemberDto memberDto : resultList) {
      System.out.println("memberDto = " + memberDto);
  }
}
```

**2. Querydsl 빈 생성(Bean population)**

- 프로퍼티 접근방법

  ** getter, setter 만들어줘야됨 (@Data 가 있어서, 별도로 만들어주지 않는거임)

```java
@Test
@DisplayName("findDtoByQueryDslSetter")
void findDtoByQueryDslSetter() {
  //프로퍼티 접근방법
  // given

  // when
  List<MemberDto> result = queryFactory
          .select(Projections.bean(MemberDto.class,
                  member.username,
                  member.age))
          .from(member)
          .fetch();

  // then
  for (MemberDto memberDto : result) {
      System.out.println("memberDto = " + memberDto);
  }
}
```

- 필드 접근 방법

  ** 바로 값이 필드에 들어가는 거임

```java
@Test
@DisplayName("findDtoByQuerydslField")
void findDtoByQuerydslField() {
  // given


  // when
  List<MemberDto> result = queryFactory
          .select(Projections.fields(MemberDto.class,
                  member.username,
                  member.age))
          .from(member)
          .fetch();

  // then
  for (MemberDto memberDto : result) {
      System.out.println("memberDto = " + memberDto);
  }
}
```

```java
@Test
@DisplayName("findUserDtoByQuerydslField")
void findUserDtoByQuerydslField() {
  // given


  // when
  // 필드값이 안맞으면, 그냥 null 로 들어감
  // 그걸 방지하기 위해서 member.username.as("name")

  List<UserDto> result = queryFactory
          .select(Projections.fields(UserDto.class,
                  member.username,
                  member.username.as("name"),
                  member.age))
          .from(member)
          .fetch();

  // then
  for (UserDto userDto : result) {
      System.out.println("memberDto = " + userDto);
  }
}  

@Test
@DisplayName("findUserDtoByQuerydslField")
void findUserDtoByQuerydslField() {
    // given
    QMember memberSub = new QMember(member);

    // when
    List<UserDto> result = queryFactory
            .select(Projections.fields(UserDto.class,
                    member.username.as("name"),

                    ExpressionUtils.as(JPAExpressions
                            .select(memberSub.age.max())
                            .from(memberSub),"age"),
                    member.age))
            .from(member)
            .fetch();

    // then
    for (UserDto userDto : result) {
        System.out.println("memberDto = " + userDto);
    }
}
```

- 생성자 접근방법

** 생성자 타입이 맞아야됨

```java
public Member(String username, int age){
	this.username = username;
	this.age = age;
}
```



```java
@Test
@DisplayName("findDtoByConstructor")
void findDtoByConstructor() {
  // given
  // 타입이 맞아야됩니다.

  // when
  List<MemberDto> result = queryFactory
          .select(Projections.constructor(MemberDto.class,
                  member.username,
                  member.age))
          .from(member)
          .fetch();

  // then

}
```

---

- @QueryProjection

  - QMeberDto 파일이 존재한다고, 가정

  - 위의 findDtoByConstructor 는 생성자는 컴파일 오류가 아니라, 실행시 오류가 발생

  - 이를 방지해서 @QueryProjection 사용하면 위의 에러를 방지해줌 **(실용주의적 관점)**

  - **단, Q파일 생성하는 단점** 

  - **의존관계에 대한 문제 (기존에는 Querydsl 에 대한 라이브러리 의존성 X)**

    **여러 레이어에 대해서 사용하게 됨**

  - **Querydsl 에 의존적으로 사용** 

```java
@Data
public class MemberDto {
    private String username;
    private int age;

    public MemberDto(){}

    @QueryProjection // 이 선제작업이 필요함
    public MemberDto(String username, int age){
        this.username = username;
        this.age = age;
    }
}
```

```java
@Test
@DisplayName("findByDTOProjection")
void findByDTOProjection() {
  // given

  // when
  queryFactory
          .select(new QMemberDto(member.username, member.age))
          .from(member)
          .fetch();
  // then
	}
}
```

---

- 동적쿼리를 해결하는 두가지 방식
  - BooleanBuilder
  - Where 다중 파라미터 사용

```java
@Test
@DisplayName("dynamicQueryBooleanBuilder")
void dynamicQueryBooleanBuilder() {
    // given
    String usernameParam = "member1";
    Integer ageParam = 10;
    // when
    List<Member> result = searchMember1(usernameParam, ageParam);

    // then
    Assertions.assertThat(result.size()).isEqualTo(1);
}

private List<Member> searchMember1(String usernameCond, Integer ageCond) {

    BooleanBuilder builder = new BooleanBuilder();
    if(usernameCond != null){
        builder.and(member.username.eq(usernameCond));
    }
    if(ageCond != null){
        builder.and(member.age.eq(ageCond));
    }

    return queryFactory
                .selectFrom(member)
                .where(builder)
                .fetch();
}
```

```java
//실무에서 주로 사용하는 방법
@Test
@DisplayName("dynamicQueryWhere")
void dynamicQueryWhere() {
    // given
    String usernameParam = "member1";
    Integer ageParam = 10;
    // when
    List<Member> result = searchMember2(usernameParam, ageParam);

    // then
    Assertions.assertThat(result.size()).isEqualTo(1);
}

private List<Member> searchMember2(String usernameCond, Integer ageCond) {

    return queryFactory
                .selectFrom(member)
                .where(usernameEq(usernameCond), ageEq(ageCond))
                .fetch();
}

private Predicate usernameEq(String usernameCond) {
    if(usernameCond == null){ return null;}
    return member.username.eq(usernameCond);
}
//삼항연산자로 사용 (현업)
private Predicate ageEq(Integer ageCond) {
    return ageCond == null ? null : member.member.age.eq(ageCond);
}
```

```java
private List<Member> searchMember2(String usernameCond, Integer ageCond) {

    return queryFactory
                .selectFrom(member)
                .where(allEq(usernameCond, ageCond))
                .fetch();
}

private BooleanExpression usernameEq(String usernameCond) {
    if(usernameCond == null){ return null;}
    return member.username.eq(usernameCond);
}

private BooleanExpression ageEq(Integer ageCond) {
    return ageCond == null ? null : member.member.age.eq(ageCond);
}

private BooleanExpression allEq(String usernameCond, Integer ageCond) {
    return usernameEq(usernameCond).and(ageEq(ageCond));
}

//조립이 가능하다 BooleanExpression

```

---

수정, 삭제 배치 쿼리

- 쿼리 한번으로 대량 데이터 수정

- Bulk 연산 조심해야되는것

  - **<u>영속성 컨텍스트와 db의 상태는 달라짐 (아래 주석 참고)</u>**

  - <u>**영속성 컨텍스트가 가지고 있는것을 보여줌 (DB 내용이 다르더라도 영속성 컨텍스트가**</u>

    <u>**존재하면 DB에서 가져온 데이터 버림)**</u>

```java

//member1 = 10 -> DB 비회원
//member2 = 20 -> DB 비회원
//member3 = 30 -> DB 유지
//member4 = 40 -> DB 유지
//영속성 컨텍스트 무시 (둘의 상태는 안맞음)
@Test
@DisplayName("bulkUpdate")
void bulkUpdate() {
  // given


  // when
  long count = queryFactory
          .update(member)
          .set(member.username, "비회원")
          .where(member.age.lt(28))
          .execute();
  //count는 영향을 받은 row 수 

  //bulk연산이 들어가도 해야된다.
  em.flush();
  em.clear();
  
  // then
  queryFactory.select

}
```

```java
@Test
@DisplayName("bulkAdd")
void bulkAdd() {
  // given


  // when
  long count = queryFactory
          .update(member)
          .set(member.age, member.age.add(1))
          .execute();

  em.flush();
  em.clear();

  // then
  List<Member> result = queryFactory.selectFrom(member)
          .fetch();
  for (Member member1 : result) {
      System.out.println("member1 = " + member1);
  }
}
```

```java
@Test
@DisplayName("bulkDelete")
void bulkDelete() {
    // given


    // when
    queryFactory
            .delete(member)
            .where(member.age.gt(18))
            .execute();


    // then
    List<Member> result = queryFactory.selectFrom(member).fetch();
    for (Member member1 : result) {
        System.out.println("member1 = " + member1);

    }
}
//delete는 별도로 em.flush() / em.clear() 할 필요 없음
```

---

```java
@Bean
JPAQueryFactory jpaQueryFactory(EntityManager em){
	return new JPAQueryFactory(em);
}
--------------------------------------------------------
//위에 @Bean 에 등록되어있기때문에, 아래에 내용 확인 가능 
private final EntityManager em;
private final JPAQueryFactory queryFactory;

public MemberJpaRepository(EntityManager em, JPAQueryFactory queryFactory){
  this.em = em;
  this.queryFactory = queryFactory;
}
---------------------------------------------------------
@Repository
@RequiredArgsConstructor
  
//위에 @Bean 에 등록되어있기때문에, 아래에 내용 확인 가능
private final EntityManage em;
private final JPAQueryFactory queryFactory;

```

```java
private final EntityManager em;
private final JPAQueryFactory queryFactory;

public MemberJpaRepository(EntityManager em){
  this.em = em;
  this.queryFactory = new JPAQueryFactory(em);
}
```

---



```java
public List<MemberTeamDto> searchByBuilder(MemberSearchCondition condition){

    BooleanBuilder builder = new BooleanBuilder();
    if(hasText(condition.getUsername())){
        builder.and(member.username.eq(condition.getUsername()));
    }
    if(hasText(condition.getTeamName())){
        builder.and(team.name.eq(condition.getTeamName()));
    }
    if(condition.getAgeGoe() != null){
        builder.and(member.age.eq(condition.getAgeGoe()));
    }
    if(condition.getAgeLoe() != null){
        builder.and(member.age.eq(condition.getAgeLoe()));
    }

    return queryFactory
            .select(new QMemberTeamDto(
                    member.id.as("memberId"),
                    member.username,
                    member.age,
                    team.id.as("teamId"),
                    team.name))
            .from(member)
            .leftJoin(member.team, team)
            .where(builder)
            .fetch();
}
```



---

```java
public List<MemberTeamDto> search(MemberSearchCondition condition){
  return queryFactory
          .select(new QMemberTeamDto(
                  member.id.as("memberId"),
                  member.username,
                  member.age,
                  team.id.as("teamId"),
                  team.name.as("teamName")
          ))
          .from(member)
          .leftJoin(member.team, team)
          .where(
                  usernameEq(condition.getUsername()),
                  teamNameEq(condition.getTeamName()),
                  ageGoe(condition.getAgeGoe()),
                  ageLoe(condition.getAgeLoe())
                  )
          .fetch();
}

private BooleanExpression usernameEq(String username) {
  return hasText(username) ? member.username.eq(username) : null;
}

private BooleanExpression teamNameEq(String teamName) {
  return hasText(teamName) ? team.name.eq(teamName) : null;
}

private BooleanExpression ageGoe(Integer ageGoe) {
  return ageGoe != null ? member.age.goe(ageGoe) :  null;
}

private BooleanExpression ageLoe(Integer ageLoe) {
  return ageLoe != null ? member.age.loe(ageLoe) : null;
}

//코드 재사용성이 뛰어남 (메서드)
//BooleanExpression
```

---

- 사용자 정의 리포지토리 사용법
  - 사용자 정의 인터페이스 생성
  - 사용자 구현체 생성 ~+Impl
  - 스프링 데이터 Repository에 사용자 정의 인터페이스 상속

```java
package study.querydsl.repository;

import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;

import java.util.List;

public interface MemberRepositoryCustom  {
    List<MemberTeamDto> search(MemberSearchCondition condition);
}


```

```java
package study.querydsl.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.dto.QMemberTeamDto;

import java.util.List;

import static org.springframework.util.StringUtils.hasText;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

public class MemberRepositoryImpl implements MemberRepositoryCustom{

    private final JPAQueryFactory queryFactory;

    public MemberRepositoryImpl(EntityManager em) {
        this.queryFactory = new JPAQueryFactory(em);
    }

    @Override
    public List<MemberTeamDto> search(MemberSearchCondition condition) {
        return queryFactory
                .select(new QMemberTeamDto(
                        member.id.as("memberId"),
                        member.username,
                        member.age,
                        team.id.as("teamId"),
                        team.name.as("teamName")
                ))
                .from(member)
                .leftJoin(member.team, team)
                .where(
                        usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                )
                .fetch();
    }
    private BooleanExpression usernameEq(String username) { return hasText(username) ? member.username.eq(username) : null; }

    private BooleanExpression teamNameEq(String teamName) {
        return hasText(teamName) ? team.name.eq(teamName) : null;
    }

    private BooleanExpression ageGoe(Integer ageGoe) {
        return ageGoe != null ? member.age.goe(ageGoe) :  null;
    }

    private BooleanExpression ageLoe(Integer ageLoe) {
        return ageLoe != null ? member.age.loe(ageLoe) : null;
    }

}

```

```java
package study.querydsl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import study.querydsl.entity.Member;

import java.util.List;

public interface MemberRepository extends JpaRepository<Member, Long>, MemberRepositoryCustom {
    //select m from Member m where m.username = :username
    List<Member> findByUsername(String username);
}

```

- 조회 쿼리가 너무 복잡한 경우,
  - QueryRepository 별도로 생성

```java
@Repository
public class MemberQueryRepository {
    
    private final JPAQueryFactory queryFactory;
    
    public MemberQueryRepository(EntityManager em){
        this.queryFactory = new JPAQueryFactory(em);
    }
}
```

---

- Paging

```java
public Page<MemberTeamDto> searchPageSimple(MemberSearchCondition condition, Pageable pageable) {
    QueryResults<MemberTeamDto> results = queryFactory
            .select(new QMemberTeamDto(
                    member.id.as("memberId"),
                    member.username,
                    member.age,
                    team.id.as("teamId"),
                    team.name.as("teamName")
            ))
            .from(member)
            .leftJoin(member.team, team)
            .where(
                    usernameEq(condition.getUsername()),
                    teamNameEq(condition.getTeamName()),
                    ageGoe(condition.getAgeGoe()),
                    ageLoe(condition.getAgeLoe())
            )
            .offset(pageable.getOffset()) //몇번째부터 시작할꺼야?
            .limit(pageable.getPageSize()) //한번 조회할때, 몇개까지 조회할꺼야?
            .fetchResults();

    List<MemberTeamDto> content = results.getResults();
    long total = results.getTotal();
    return new PageImpl<>(content, pageable, total);
}
```

```java
@Override
public Page<MemberTeamDto> searchPageComplex(MemberSearchCondition condition, Pageable pageable) {
    List<MemberTeamDto> content = queryFactory
            .select(new QMemberTeamDto(
                    member.id.as("memberId"),
                    member.username,
                    member.age,
                    team.id.as("teamId"),
                    team.name.as("teamName")
            ))
            .from(member)
            .leftJoin(member.team, team)
            .where(
                    usernameEq(condition.getUsername()),
                    teamNameEq(condition.getTeamName()),
                    ageGoe(condition.getAgeGoe()),
                    ageLoe(condition.getAgeLoe())
            )
            .offset(pageable.getOffset()) //몇번째부터 시작할꺼야?
            .limit(pageable.getPageSize()) //한번 조회할때, 몇개까지 조회할꺼야?
            .fetch();

        //totalCount용 쿼리를 또 작성
        long total = queryFactory
                .select(member)
                .from(member)
                .leftJoin(member.team, team)
                .where(
                        usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                )
                .fetchCount();

        return new PageImpl<>(content,pageable, total);
}
```

---

-  count 쿼리가 생략 가능한 경우 생략해서 처리
  - 페이지 시작이면서, 컨텐츠 사이즈가 페이지 사이즈 보다 작을 때
  - 마지막 페이지일때 (offset+컨텐츠 사이즈를 더해서 전체사이즈 구함)

```java
@Override
public Page<MemberTeamDto> searchPageComplex(MemberSearchCondition condition, Pageable pageable) {
    List<MemberTeamDto> content = queryFactory
            .select(new QMemberTeamDto(
                    member.id.as("memberId"),
                    member.username,
                    member.age,
                    team.id.as("teamId"),
                    team.name.as("teamName")
            ))
            .from(member)
            .leftJoin(member.team, team)
            .where(
                    usernameEq(condition.getUsername()),
                    teamNameEq(condition.getTeamName()),
                    ageGoe(condition.getAgeGoe()),
                    ageLoe(condition.getAgeLoe())
            )
            .offset(pageable.getOffset()) //몇번째부터 시작할꺼야?
            .limit(pageable.getPageSize()) //한번 조회할때, 몇개까지 조회할꺼야?
            .fetch();

        //totalCount용 쿼리를 또 작성
        JPAQuery<Member> countQuery = queryFactory
                .select(member)
                .from(member)
                .leftJoin(member.team, team)
                .where(
                        usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                );



    return PageableExecutionUtils.getPage(content,pageable,()->countQuery.fetchCount());
```

---

정렬은 조건이 조금만 복잡해져도 `Pageable`의 `Sort`기능을 사용하기 어렵다.

루트 엔티티 범위를 넘어가는 동적 정렬 기능이 필요하면 스프링 데이터 페이징이 제공하는 

`Sort` 를 사용하기 보다는 파라미터를 받아서 직접 처리하는 것을 권장한다.

---

스프링데이터 JPA가 제공하는 Querydsl 기능

- 실무 환경에서 사용하기에는 많이 부족하다.

QuerydslPredicateExecutor 인터페이스 지원 기능

Querydsl Web 지원

QuerydslRepositorySupport 추상클래스

Paging 할때는 괜찮은데, `Sort` 에 대해서는 명확하게 지원하지 않는다.

---

Querydsl 지원클래스 직접 만들어보기