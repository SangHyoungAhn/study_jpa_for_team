package excodeMyBatis.dao;

import excodeMyBatis.model.Member;
import org.apache.ibatis.session.SqlSession;
import org.springframework.stereotype.Repository;

import java.util.Map;

public interface MemberDao {

    void save(Member member);
    void update(Member member);

}
