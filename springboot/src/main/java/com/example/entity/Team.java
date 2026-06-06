package com.example.entity;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Team {
    @JsonProperty("team_ID")
    private Integer team_ID;
    @JsonProperty("teacher_ID")
    private Integer teacher_ID;
    @JsonProperty("team_name")
    private String team_name;
    private String password;
    private Integer number;
    private String time;

    public Integer getTeam_ID() {
        return team_ID;
    }

    public void setTeam_ID(Integer team_ID) {
        this.team_ID = team_ID;
    }

    public Integer getTeacher_ID() {
        return teacher_ID;
    }

    public void setTeacher_ID(Integer teacher_ID) {
        this.teacher_ID = teacher_ID;
    }

    public String getTeam_name() {
        return team_name;
    }

    public void setTeam_name(String team_name) {
        this.team_name = team_name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer getNumber() {
        return number;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }
}
