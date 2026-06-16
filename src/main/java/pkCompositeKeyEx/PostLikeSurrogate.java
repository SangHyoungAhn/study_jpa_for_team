package pkCompositeKeyEx;

import com.example.spring.studyjpa.entity.Member;
import com.example.spring.studyjpa.entity.Post;
import com.example.spring.studyjpa.entity.base.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

//@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        uniqueConstraints = @UniqueConstraint(
                name = "uk_member_post",
                columnNames = {"member_id", "post_id"}
        )
)
public class PostLikeSurrogate extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    public PostLikeSurrogate(Member member, Post post) {
        this.member = member;
        this.post = post;
    }
}
