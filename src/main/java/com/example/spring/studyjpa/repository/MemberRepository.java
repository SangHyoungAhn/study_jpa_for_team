package com.example.spring.studyjpa.repository;

import com.example.spring.studyjpa.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


public interface MemberRepository extends JpaRepository<Member, Long> {
}
