package com.example.entity;

public class Reimburse {
    private Integer reimburse_ID;
    private Integer apply_ID;
    private Integer teacher_ID;
    private String money;
    private String time;
    private String status;

    public Integer getReimburse_ID() {
        return reimburse_ID;
    }

    public void setReimburse_ID(Integer reimburse_ID) {
        this.reimburse_ID = reimburse_ID;
    }

    public Integer getApply_ID() {
        return apply_ID;
    }

    public void setApply_ID(Integer apply_ID) {
        this.apply_ID = apply_ID;
    }

    public Integer getTeacher_ID() {
        return teacher_ID;
    }

    public void setTeacher_ID(Integer teacher_ID) {
        this.teacher_ID = teacher_ID;
    }

    public String getMoney() {
        return money;
    }

    public void setMoney(String money) {
        this.money = money;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}