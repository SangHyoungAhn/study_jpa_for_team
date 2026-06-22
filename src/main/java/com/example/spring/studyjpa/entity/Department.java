package com.example.spring.studyjpa.entity;


import com.example.spring.studyjpa.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;


@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Department extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String deptCode;
    private String deptName;

    @OneToMany(mappedBy = "dept")
    private List<Member> members = new ArrayList<>();

    public Department(String deptCode, String deptName){
        this.deptCode = deptCode;
        this.deptName = deptName;
    }

}
