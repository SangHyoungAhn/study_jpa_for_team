package com.example.spring.studyjpa.controller;


import com.example.spring.studyjpa.dto.MemberResponse;
import com.example.spring.studyjpa.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    // ✅ 엔티티가 아니라 DTO 를 반환 → 양방향(dept ↔ members) 직렬화 순환을 원천 차단
    @GetMapping("/members/{id}")
    public MemberResponse getMember(@PathVariable Long id) {
        return memberService.getMember(id);
    }
}
