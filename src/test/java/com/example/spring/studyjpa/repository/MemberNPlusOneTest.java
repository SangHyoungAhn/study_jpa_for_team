package com.example.spring.studyjpa.repository;

import com.example.spring.studyjpa.entity.Department;
import com.example.spring.studyjpa.entity.Member;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * N+1 시연용 테스트.
 *
 * 회원 목록을 findAll() 로 가져온 뒤(쿼리 1번),
 * 루프에서 각 회원의 dept(LAZY)를 건드리면 부서 조회 쿼리가 회원 수만큼(N번) 더 나간다 → 1 + N.
 *
 * 콘솔 SQL 로그(show-sql)에서
 *   select ... from member            ← 1번
 *   select ... from department where id=?  ← N번
 * 이 찍히는 걸 직접 확인하는 게 목적이다.
 */
@DataJpaTest
class MemberNPlusOneTest {

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    TestEntityManager em;

    @BeforeEach
    void 데이터_세팅() {
        // 회원마다 '서로 다른' 부서를 둔다.
        // 부서를 공유하면 1차 캐시 때문에 부서 쿼리가 distinct 개수만큼만 나가서 N+1이 약하게 보인다.
        // 1 + N(=회원 수) 을 또렷하게 보려고 회원당 부서 1개로 둔다.
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

        em.flush();   // 쌓인 INSERT를 DB로 전송
        em.clear();   // ★ 1차 캐시 비우기 → 이후 조회가 DB를 다시 읽게 (N+1 재현의 핵심)
    }

    @Test
    @DisplayName("findAll 후 dept(LAZY)를 건드리면 N+1이 발생한다")
    void N플러스1_확인() {
        System.out.println("===== findAll 실행 (쿼리 1번) =====");
        List<Member> members = memberRepository.findAll();

        System.out.println("===== 루프 시작 (여기서 부서 조회가 N번 나간다) =====");
        for (Member m : members) {
            System.out.println(m.getName() + " / " + m.getDept().getDeptName());
        }
        System.out.println("===== 끝 — 위 로그에서 department SELECT가 회원 수만큼 찍혔는지 확인 =====");

        assertThat(members).hasSize(5);   // 데이터가 실제로 들어갔는지 확인 (테스트 자체는 통과)
    }
}
