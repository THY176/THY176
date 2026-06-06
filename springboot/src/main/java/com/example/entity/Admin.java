package com.example.entity;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Admin {
    @JsonProperty("teacher_ID")
    private Integer teacher_ID;
    private String name;
    private String password;
    private String tele;
    private String gender;
    private Integer age;

    public Integer getTeacher_ID() {
        return teacher_ID;
    }

    public void setTeacher_ID(Integer teacher_ID) {
        this.teacher_ID = teacher_ID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getTele() {
        return tele;
    }

    public void setTele(String tele) {
        this.tele = tele;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }
}