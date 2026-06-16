package com.example.spring.studyjpa.entity;

import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostLikeId implements Serializable {

    private Long memberId;
    private Long postId;

    public PostLikeId(Long memberId, Long postId){
        this.memberId = memberId;
        this.postId = postId;
    }
}
