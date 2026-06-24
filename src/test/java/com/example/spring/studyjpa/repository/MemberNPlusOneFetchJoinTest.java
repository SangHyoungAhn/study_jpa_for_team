package com.example.spring.studyjpa.repository;

import com.example.spring.studyjpa.entity.Department;
import com.example.spring.studyjpa.entity.Member;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class MemberNPlusOneFetchJoinTest {

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    TestEntityManager em;

    @Autowired
    EntityManagerFactory emf;   // 실행된 쿼리 수를 세려고 Statistics 를 꺼낸다

    @BeforeEach
    void 데이터_세팅() {
        /**
         * D1 부서1
         * D2 부서2
         * D3 부서3
         * D4 부서4
         * D5 부서5
         *
         * 회원1
         * 회원2
         * 회원3
         * 회원4
         * 회원5
         */
        for (int i = 1; i <= 5; i++) {
            Department dept = new Department("D" + i, "부서" + i);
            em.persist(dept);

            Member m = Member.builder()
                    .name("회원" + i)
                    .email("user" + i + "@test.com")
                    .dept(dept)
                    .build();
            em.persist(m);
        }

        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("NPlus1_확인")
    void NPlus1_확인() {
        System.out.println("===== findAll 실행 (쿼리 1번) =====");
        List<Member> members = memberRepository.findAll();

        System.out.println("===== 시작 (여기서 부서 조회가 N번 나간다) =====");
        for (Member m : members) {
            System.out.println(m.getName() + " / " + m.getDept().getDeptName());
        }
        System.out.println("===== 끝 — 위 로그에서 department SELECT가 회원 수만큼 찍혔는지 확인 =====");

        assertThat(members).hasSize(5);   // 데이터가 실제로 들어갔는지 확인 (테스트 자체는 통과)
    }

    @Test
    @DisplayName("FetchJoin_확인")
    void FetchJoin_확인() {
        Statistics stats = 통계_초기화();

        System.out.println("===== findAllWithDept 실행 (JOIN FETCH — 부서까지 한 방에) =====");
        List<Member> members = memberRepository.findAllWithDept();

        System.out.println("===== 루프 시작 (추가 쿼리가 안 나가야 정상) =====");
        for (Member m : members) {
            System.out.println(m.getName() + " / " + m.getDept().getDeptName());  // 이미 채워져 있음
        }

        long queryCount = stats.getPrepareStatementCount();
        System.out.println("실행된 쿼리 수 = " + queryCount + " (fetch join 이면 1)");

        assertThat(members).hasSize(5);
        assertThat(queryCount).isEqualTo(1);   // ★ 1 + N → 1 (한 방에)
    }


    private Statistics 통계_초기화() {
        Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();
        stats.setStatisticsEnabled(true);
        stats.clear();
        return stats;
    }
}
