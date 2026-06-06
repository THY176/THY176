package com.example.entity;

public class Approve {
    private Integer approve_ID;
    private Integer apply_ID;
    private Integer teacher_ID;
    private String teacher_name;
    private String role;
    private String opinion;
    private Integer sequence;
    private String approve_time;
    private String status;

    public Integer getApprove_ID() {
        return approve_ID;
    }

    public void setApprove_ID(Integer approve_ID) {
        this.approve_ID = approve_ID;
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

    public String getTeacher_name() {
        return teacher_name;
    }
    public void setTeacher_name(String teacher_name) {
        this.teacher_name = teacher_name;
    }
    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getOpinion() {
        return opinion;
    }

    public void setOpinion(String opinion) {
        this.opinion = opinion;
    }

    public Integer getSequence() {
        return sequence;
    }

    public void setSequence(Integer sequence) {
        this.sequence = sequence;
    }

    public String getApprove_time() {
        return approve_time;
    }

    public void setApprove_time(String approve_time) {
        this.approve_time = approve_time;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}