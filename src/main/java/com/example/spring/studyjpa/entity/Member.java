package com.example.spring.studyjpa.entity;

import com.example.spring.studyjpa.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.Objects;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString
public class Member extends BaseEntity {

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
        changeDept(dept);
    }

    public void changeName(String name){
        this.name = name;
    }

    public void changeDept(Department dept){
        Objects.requireNonNull(dept, "부서는 필수입니다.");
        if(this.dept != null){
            this.dept.getMembers().remove(this);
        }
        this.dept = dept;
        dept.getMembers().add(this);
    }
}
