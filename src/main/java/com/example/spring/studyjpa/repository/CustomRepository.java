package com.example.spring.studyjpa.repository;

import com.example.spring.studyjpa.dto.MemberDto;
import com.example.spring.studyjpa.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomRepository extends JpaRepository<Member, Long> {

    List<MemberDto> findMemberId();
}
