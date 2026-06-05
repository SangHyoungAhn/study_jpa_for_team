package com.example.spring.studyjpa.vo_dto_ex;

public class Person {

    private Long id;
    private String name;

    private EmailVO email;
    private PasswordVO password;

    public Person(String name, EmailVO email, PasswordVO password){
        this.name = name;
        this.email = email;
        this.password = password;
    }

    public void changePassword(PasswordVO newPassword){
        this.password = newPassword;
    }
    
}
