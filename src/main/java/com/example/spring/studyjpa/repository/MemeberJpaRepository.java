package com.example.spring.studyjpa.repository;

import com.example.spring.studyjpa.dto.MemberDto;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;


public class MemeberJpaRepository {


    private final JPAQueryFactory queryFactory;


    public MemeberJpaRepository(EntityManager entityManager) {
        this.queryFactory = new JPAQueryFactory(entityManager);
    }

    public List<MemberDto> findMemberList(){
        //return queryFactory
        //        .selectFrom

        return null;
    }

}
