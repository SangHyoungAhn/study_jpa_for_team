package vo_dto_ex;

import java.util.Objects;

public final class PasswordVO {

    private final String value;

    public PasswordVO(String value) {
        
        // 비밀번호는 8자리 이상이어야 한다
        if (value == null || value.length() < 8) {
            throw new IllegalArgumentException("비밀번호는 최소 8자리 이상이어야 합니다.");
        }
        this.value = value;
    }

    public String getValue() { return value; }

  @Override
    public boolean equals(Object o){
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;
        PasswordVO passwordVO = (PasswordVO) o;

        return Objects.equals(value, passwordVO.value);
    }

    @Override
    public int hashCode(){
        return Objects.hash(value);
    }

}
