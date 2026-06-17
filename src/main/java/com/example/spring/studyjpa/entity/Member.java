package com.example.spring.studyjpa.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name="dept_id", nullable = false)
    private Department dept;


    @Column(nullable = false)
    private int activityPoint = 0;

    @Builder
    public Member(String name, String email, Department dept){
        this.name = name;
        this.email = email;
        this.dept = dept;
    }

    public void changeName(String name){
        this.name = name;
    }
}
