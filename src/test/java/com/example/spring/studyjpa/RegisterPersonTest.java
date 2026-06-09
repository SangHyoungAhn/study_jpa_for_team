import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import vo_dto_ex.PasswordVO;

class RegisterPersonTest {
    
    @Test
    @DisplayName("��й�ȣ 4�ڸ� �Է½� ����")
    void pwRegisterTest(){
        assertThrows(IllegalArgumentException.class, () -> {
            new PasswordVO("kkkk"); 
        });
    }
}