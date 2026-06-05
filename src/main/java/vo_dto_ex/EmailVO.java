package com.example.spring.studyjpa.vo_dto_ex;

import java.util.Objects;

public final class EmailVO {
    
    private String value;

    public EmailVO(String value){
        if(value != null && value.isBlank()){
            throw new IllegalArgumentException("�̸����� �ʼ��Է� �׸��Դϴ�.");
        }

       if (!value.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new IllegalArgumentException("�ùٸ� �̸��� ������ �ƴմϴ�.");
        }
        this.value = value;
    }

    public String getValue(){return value;}

  @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EmailVO emailVO = (EmailVO) o;
        
        return Objects.equals(value, emailVO.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

}
