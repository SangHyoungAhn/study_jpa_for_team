package com.example.spring.studyjpa.entity;


import com.example.spring.studyjpa.entity.base.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostLike extends BaseTimeEntity {

    @EmbeddedId
    private PostLikeId id;

    @MapsId("memberId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name= "member_id")
    private Member member;

    @MapsId("postId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name= "post_id")
    private Post post;

    public PostLike(Member member, Post post){
        this.member = member;
        this.post = post;
    }


}
