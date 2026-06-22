package com.example.spring.studyjpa.entity;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class PostCommentTest {

    @Test
    void 한쪽만_세팅하면_불일치가_생긴다(){
        Post post = Post.builder()
                .title("제목")
                .build();

        Comment comment = Comment.builder()
                .content("댓글")
                .build();


        comment.setPost(post);

        Assertions.assertThat(comment.getPost()).isEqualTo(post);
        Assertions.assertThat(post.getComments()).doesNotContain(comment);
    }

    @Test
    void addComment는_양쪽을_모두_연결한다(){
        Post post = Post.builder()
                .title("제목")
                .build();

        Comment comment = Comment.builder()
                .content("댓글")
                .build();

        post.addComment(comment);


        //여기서 주인은 comment, post
        Assertions.assertThat(comment.getPost()).isEqualTo(post);
        Assertions.assertThat(post.getComments()).contains(comment);
    }
}
