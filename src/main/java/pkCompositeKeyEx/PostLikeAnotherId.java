package pkCompositeKeyEx;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostLikeAnotherId implements Serializable {

    private Long member;
    private Long post;

    public PostLikeAnotherId(Long member, Long post) {
        this.member = member;
        this.post = post;
    }
}
