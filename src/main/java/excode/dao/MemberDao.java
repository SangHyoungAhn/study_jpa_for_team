package excode.dao;

import excode.model.Member;
import org.apache.ibatis.session.SqlSession;
import org.springframework.stereotype.Repository;

import java.util.Map;

@Repository
public class MemberDao {

    private final SqlSession sqlSession;

    public MemberDao(SqlSession sqlSession){
        this.sqlSession = sqlSession;
    }


    //findMember
    public Member findMember(String name, int age){
        return sqlSession.selectOne("member.findByNameAndAge", Map.of("name", name, "age", age));
    }

    public void updateAddress(Long id, String address){
        sqlSession.update("member.updateAddress",Map.of("id", id, "address", address));
    }
}
