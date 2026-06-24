package com.example.spring.studyjpa.dto;

import com.example.spring.studyjpa.entity.Member;

/**
 * 회원 조회 응답용 DTO (읽기 전용).
 *
 * 엔티티(Member)를 그대로 API 로 내보내면 양방향 연관(dept ↔ members) 직렬화에서
 * 무한 순환에 빠진다. 그래서 필요한 값만 담은 이 record 로 변환해 내보낸다.
 *
 * record 라서 name(), deptName() 접근자와 생성자·equals·hashCode·toString 이 자동 생성된다.
 */
public record MemberResponse(String name, String deptName) {

    /** 엔티티 → DTO 변환 (정적 팩토리). 트랜잭션 안에서 호출해야 dept(LAZY)가 안전하게 채워진다. */
    public static MemberResponse from(Member m) {
        return new MemberResponse(m.getName(), m.getDept().getDeptName());
    }
}
