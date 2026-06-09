package com.example.spring.studyjpa.entity.base;


import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;


@Getter
@MappedSuperclass
public class BaseEntity extends BaseTimeEntity {

    //Long으로 설정한 이유는 ID값을 받기위해서

    @CreatedBy
    @Column(updatable = false)
    private Long createdBy;

    @LastModifiedBy
    private Long updatedBy;
}
