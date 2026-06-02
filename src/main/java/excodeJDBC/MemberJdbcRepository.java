package excodeJDBC;

import excodeJDBC.model.MemberJDBC;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;


public class MemberJdbcRepository {

    private final String url = "jdbc:h2:tcp://localhost/~/test";
    private final String user = "sa";
    private final String password = "";

    //1. save
    public void save(MemberJDBC memberJDBC){
        String sql = "INSERT INTO MEMBER (NAME, AGE, ADDRESS) VALUES (?, ?, ?)";
        Connection conn = null;
        PreparedStatement pstmt = null;

        try{
            conn = DriverManager.getConnection(url, user, password);
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, memberJDBC.getName());
            pstmt.setInt(2, memberJDBC.getAge());
            pstmt.setString(3, memberJDBC.getAddress());
            pstmt.executeUpdate();

        }catch(Exception e){
            e.printStackTrace();
        }finally{
            if(pstmt != null){
                try{
                    pstmt.close();}catch(SQLException e){}
            }
            if (conn != null) {
                try {
                    conn.close(); } catch (SQLException e) {}
            }
        }
    }

    //2.Update
    public void update(MemberJDBC memberJDBC){
        String sql = "UPDATE MEMBER SET NAME = ? , AGE= ?, ADDRESS = ? WHERE ID = ?";
        Connection conn = null;
        PreparedStatement pstmt = null;

        try{
            conn = DriverManager.getConnection(url, user, password);
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, memberJDBC.getName());
            pstmt.setInt(2, memberJDBC.getAge());
            pstmt.setString(3, memberJDBC.getAddress());
            pstmt.setLong(4, memberJDBC.getId());
            pstmt.executeUpdate();

        }catch(SQLException e){
            e.printStackTrace();
        } finally {
            if (pstmt != null) {
                try { pstmt.close(); } catch (SQLException e) { /* 무시 */ }
            }
            if (conn != null) {
                try { conn.close(); } catch (SQLException e) { /* 무시 */ }
            }
        }
    }
}
