package com.example.spring.studyjpa.dto;

import com.example.spring.studyjpa.entity.Member;
import lombok.AllArgsConstructor;
import lombok.Getter;


@Getter
@AllArgsConstructor
public class MemberResponse {

    private final String name;
    private final String deptName;

    /** 엔티티 → DTO 변환 (정적 팩토리). 트랜잭션 안에서 호출해야 dept(LAZY)가 안전하게 채워진다. */
    public static MemberResponse from(Member m) {
        return new MemberResponse(m.getName(), m.getDept().getDeptName());
    }
}
