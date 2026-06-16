package pkCompositeKeyEx;


import com.example.spring.studyjpa.entity.Member;
import com.example.spring.studyjpa.entity.Post;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

//@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@IdClass(PostLikeAnotherId.class)
public class PostLikeAnother {

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id")
    private Member member;

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id")
    private Post post;

    public PostLikeAnother(Member member, Post post){
        this.member = member;
        this.post = post;
    }
}
