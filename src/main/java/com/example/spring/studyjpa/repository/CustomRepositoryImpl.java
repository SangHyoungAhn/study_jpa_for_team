package com.example.spring.studyjpa.repository;

import com.example.spring.studyjpa.dto.MemberDto;

import java.util.List;

public class CustomRepositoryImpl implements CustomRepository {
    @Override
    public List<MemberDto> findMemberId() {

        return List.of();
    }
}
