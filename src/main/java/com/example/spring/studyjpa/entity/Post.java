package com.example.spring.studyjpa.entity;

import com.example.spring.studyjpa.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access= AccessLevel.PROTECTED)
public class Post extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Lob
    private String content;
    /**
     *   방식: @Column(length = 2000)
     *   결과: VARCHAR(2000)
     *   언제 / 왜: 수천 자 이내. 가장 단순
     *   ────────────────────────────────────────
     *   방식: @Lob
     *   결과: CLOB (문자) / BLOB (바이너리)
     *   언제 / 왜: 길이 제한 없는 대용량. DB 독립적이라 추천
     *   ────────────────────────────────────────
     *   방식: @Column(columnDefinition = "TEXT")
     *   결과: DB의 TEXT 타입 직접 지정
     *   언제 / 왜: DB 타입을 콕 집어야 할 때. DB 종속 주의
     */

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name="author_id", nullable = false)
    private Member author;

    @OneToMany(mappedBy ="post",
            cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments = new ArrayList<>();

    // 참고) 컬렉션을 '읽기 전용'으로 노출하면 getComments().add() 같은 우회를 막을 수 있다.
    //       학습용이라 지금은 꺼둔다. 활성화하려면 java.util.Collections import 추가 필요.
    // public List<Comment> getComments(){
    //     return Collections.unmodifiableList(comments);
    // }

    public void addComment(Comment comment){
        comments.add(comment);
        comment.setPost(this);
    }

    public void removeComment(Comment comment){
        comments.remove(comment);
    }

    @Builder
    public Post(String title, String content, Member author){
        this.title = title;
        this.content = content;
        this.author = author;
    }

}
