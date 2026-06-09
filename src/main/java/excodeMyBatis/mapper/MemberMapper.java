package excodeMyBatis.mapper;

import org.apache.ibatis.annotations.Mapper;
import excodeMyBatis.model.Member;

@Mapper
public interface MemberMapper {

    void save(Member member);

    void update(Member member);

}
