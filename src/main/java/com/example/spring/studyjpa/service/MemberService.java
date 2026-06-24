package com.example.spring.studyjpa.service;


import com.example.spring.studyjpa.dto.MemberResponse;
import com.example.spring.studyjpa.entity.Member;
import com.example.spring.studyjpa.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;

    @Transactional
    public void save(Member member){
        memberRepository.save(member);
    }

    public Optional<Member> findById(Long id){
        return memberRepository.findById(id);
    }

    // 엔티티 → DTO 변환을 '트랜잭션 안(@Transactional readOnly)' 에서 수행
    // → m.getDept()(LAZY)가 여기서 안전하게 초기화되므로 OSIV 켜짐/꺼짐과 무관하게 동작
    public MemberResponse getMember(Long id){
        Member m = memberRepository.findById(id).orElseThrow();
        return MemberResponse.from(m);
    }

    public List<Member> findAll(){
        return memberRepository.findAll();
    }


}
