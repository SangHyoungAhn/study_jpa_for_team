package com.example.spring.studyjpa.controller;

import com.example.spring.studyjpa.dto.MemberDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class HelloRestController {
    
    @GetMapping("/page/resthello")
    public String viewRestHello(){
        return "hello";
    }

    @GetMapping("/api/v1/member/search")
    public String searchMembers(@RequestParam String dept, @RequestParam(required=false) String name){
        return dept + "팀의" + name + "님을 검색합니다.";
    }

    @GetMapping("/api/v1/member/{id}")
    public String getMember(@PathVariable("id") Long memberId){
        return memberId + "번 사원 정보를 가져옵니다.";
    }


    @PostMapping("/api/v1/members")
    public String join(@RequestBody MemberDto dto) {
        return dto.getName() + " 사원을 등록합니다.";
    }

    @PostMapping("/api/v2/members")
    public ResponseEntity<MemberDto> joinMember(@RequestBody MemberDto requestDto) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(requestDto);
    }

    @PostMapping("/api/v3/members")
    public ResponseEntity<String> joinMemberV3(@RequestBody MemberDto requestDto) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(requestDto.getName() + "정보가 등록되었습니다.");
    }
}
