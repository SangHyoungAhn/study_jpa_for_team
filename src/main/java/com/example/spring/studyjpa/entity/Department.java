package com.example.spring.studyjpa.entity;


import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;


@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String deptCode;
    private String deptName;

    public Department(String deptCode, String deptName){
        this.deptCode = deptCode;
        this.deptName = deptName;
    }


}
