package com.example.spring.studyjpa.repository;

import com.example.spring.studyjpa.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;


public interface MemberRepository extends JpaRepository<Member, Long> {


    /**
     * 1. 이메일로 단건 조회
     * 2. 부서ID로 멤버 조회
     * 3. 이메일로 중복 체크
     *
     * 4. 벌크 메서드 ***
     */

    Optional<Member> findByEmail(String email);
    List<Member> findByDeptId(Long departmentId);
    boolean existsByEmail(String email);

    @Modifying
    @Query("UPDATE Member m SET m.activityPoint = m.activityPoint + :amount")
    int addActivityPointToAll(@Param("amount") int amount);

}
