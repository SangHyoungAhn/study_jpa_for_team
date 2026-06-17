package com.example.spring.studyjpa.repository;

import com.example.spring.studyjpa.entity.Department;
import com.example.spring.studyjpa.entity.Member;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.assertj.core.api.Assertions;
import org.hibernate.LazyInitializationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
public class MemberRepositoryTest {

    @Autowired
    MemberRepository memberRepository;

    @PersistenceContext
    //@PersistenceContext는 JPA 표준 어노테이션 (이 필드는 영속성컨텍스트)
    EntityManager em;

    @Test
    @DisplayName("1차 캐시 - 같은 트랜잭션에서 findById 두 번은 같은 객체를 반환한다")
    void findById_Cache(){
        //given
        Department dept = new Department("D011", "비즈테크팀");
        em.persist(dept);

        Member savedMember = memberRepository.save(
                Member.builder()
                        .name("안상형")
                        .email("shahn0718@donga.com")
                        .dept(dept)
                        .build()
        );
        Long savedMemberId = savedMember.getId();

        em.flush();
        em.clear();

        //when

        Member member1 = memberRepository.findById(savedMemberId).get();
        Member member2 = memberRepository.findById(savedMemberId).get();

        System.out.println("member1의 주소: " + System.identityHashCode(member1));
        System.out.println("member2의 주소: " + System.identityHashCode(member2));


        //then
        Assertions.assertThat(member1).isSameAs(member2);
    }

    @Test
    @DisplayName("1차 캐시 밖- 영속성 컨텍스트가 바뀌면 다른 객체가 반환된다.")
    void findById_Cache_Outside(){
        //given
        Department dept = new Department("D011", "비즈테크팀");
        em.persist(dept);

        Member savedMember = memberRepository.save(
                Member.builder()
                        .name("안상형")
                        .email("shahn0718@donga.com")
                        .dept(dept)
                        .build()
        );
        Long savedMemberId = savedMember.getId();

        em.flush();
        em.clear();

        Member m1 = memberRepository.findById(savedMemberId).get();

        em.clear();

        Member m2 = memberRepository.findById(savedMemberId).get();

        System.out.println("m1의 주소: " + System.identityHashCode(m1));
        System.out.println("m2의 주소: " + System.identityHashCode(m2));

        Assertions.assertThat(m1).isNotSameAs(m2);
        //Assertions.assertThat(m1).isSameAs(m2);

    }

    @Test
    @DisplayName("변경 감지 - save() 없이 값만 바꿔도 flush 시점에 UPDATE가 나간다.")
    void dirtChecking(){

        //given
        Department dept = new Department("D011", "비즈테크팀");
        em.persist(dept);
        Member savedMember = memberRepository.save(
                Member.builder()
                        .name("안상형")
                        .email("shahn0718@donga.com")
                        .dept(dept)
                        .build()
        );
        Long savedMemberId = savedMember.getId();

        em.flush();
        em.clear();

        //when
        Member findMember = memberRepository.findById(savedMemberId).orElseThrow();
        findMember.changeName("홍길동");

        System.out.println("===== changeName 직후 (아직 UPDATE 안 나감) =====");
        em.flush();
        System.out.println("===== flush 후 (UPDATE 나감) =====");

        //then
        em.clear();   // 1차 캐시를 비우고
        Member reloadedMember = memberRepository.findById(savedMemberId).orElseThrow();
        System.out.println("===== dB에서 새로 읽기 =====");
        assertThat(reloadedMember.getName()).isEqualTo("홍길동");

    }


    @Test
    @DisplayName("벌크 연산 - UPDATE 한 번으로 전원 활동점수 +100, 단 1차 캐시는 stale 해진다")
    void bulkUpdate_staleCache(){

        //given
        Department dept = new Department("D011",
                "비즈테크팀");
        em.persist(dept);
        Member m1 = memberRepository.save(
                Member.builder()
                        .name("안상형")
                        .email("shahn0718@donga.com")
                        .dept(dept)
                        .build()
        );
        Member m2 = memberRepository.save(
                Member.builder()
                        .name("홍길동")
                        .email("ghdrlfehd@donga.com")
                        .dept(dept)
                        .build()
        );

        //when
        int updated = memberRepository.addActivityPointToAll(100);

        Assertions.assertThat(updated).isEqualTo(2);
        Assertions.assertThat(m1.getActivityPoint()).isEqualTo(0);

        em.clear();
        Member findMember = memberRepository.findById(m1.getId()).orElseThrow();
        Assertions.assertThat(findMember.getActivityPoint()).isEqualTo(100);
    }



    @Test
    @DisplayName("LAZY - 컨텍스트가 비워진 뒤 프록시 초기화 시 LazyInitializationException")
    void lazyInit_throws(){
        //given
        Department dept = new Department("D011", "비즈테크팀");
        em.persist(dept);
        Member saved = memberRepository.save(
                Member.builder()
                        .name("안상형")
                        .email("shahn0718@donga.com")
                        .dept(dept)
                        .build()
        );

        em.flush();
        em.clear();   // 다시 조회할 때 dept가 '진짜 객체'가 아니라 'LAZY 프록시'로 들어오게

        //when
        Member member = memberRepository.findById(saved.getId()).orElseThrow();
        // member.dept = 아직 초기화 안 된 LAZY 프록시

        em.clear();   // 컨텍스트 비움 → member 준영속, 프록시가 SQL 날릴 통로 끊김

        //then
        assertThatThrownBy(() -> member.getDept().getDeptName())   // 빈 프록시의 실제 값 접근
                .isInstanceOf(LazyInitializationException.class);
    }


    @Test
    @DisplayName("LAZY 해결① - 영속 상태에서 미리 초기화하면 컨텍스트를 비워도 안전하다")
    void lazyInit_avoidedByEagerTouch(){
        //given
        Department dept = new Department("D011", "비즈테크팀");
        em.persist(dept);
        Member saved = memberRepository.save(
                Member.builder()
                        .name("안상형")
                        .email("shahn0718@donga.com")
                        .dept(dept)
                        .build()
        );

        em.flush();
        em.clear();

        //when
        Member member = memberRepository.findById(saved.getId()).orElseThrow();
        String deptName = member.getDept().getDeptName();   // 컨텍스트 살아있을 때(영속) 미리 초기화

        em.clear();   // 이제 컨텍스트를 비워 준영속으로 만들어도

        //then
        assertThat(deptName).isEqualTo("비즈테크팀");   // 이미 꺼내둔 값은 안전
        assertThatCode(() -> member.getDept().getDeptName())   // 같은 호출도 이제는 안 터진다
                .doesNotThrowAnyException();
    }


    @Test
    @DisplayName("같은 부서 멤버 2명")
    void findByDeptId_같은부서2명(){
        //given

        Department dept = new Department("D011", "비즈테크팀");
        em.persist(dept);

        memberRepository.save(
                Member.builder()
                        .name("안상형")
                        .email("shahn0718@donga.com")
                        .dept(dept)
                        .build()
        );
        memberRepository.save(
                Member.builder()
                        .name("장현수")
                        .email("wkdgustn@donga.com")
                        .dept(dept)
                        .build()
        );


        //when
        List<Member> members = memberRepository.findByDeptId(dept.getId());


        //then
        assertThat(members).hasSize(2);
    }


}
