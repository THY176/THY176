package com.example.entity;

public class Apply {
    private Integer apply_ID;
    private Integer team_ID;
    private String apply_money;
    private String apply_type;
    private String reason;
    private String apply_time;
    private String account_name;
    private String bank;
    private String account;
    private String status;
    private String tags;
    private String processInstanceId;  // 工作流实例ID

    public Integer getApply_ID() {
        return apply_ID;
    }

    public void setApply_ID(Integer apply_ID) {
        this.apply_ID = apply_ID;
    }

    public Integer getTeam_ID() {
        return team_ID;
    }

    public void setTeam_ID(Integer team_ID) {
        this.team_ID = team_ID;
    }

    public String getApply_money() {
        return apply_money;
    }

    public void setApply_money(String apply_money) {
        this.apply_money = apply_money;
    }

    public String getApply_type() {
        return apply_type;
    }

    public void setApply_type(String apply_type) {
        this.apply_type = apply_type;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getApply_time() {
        return apply_time;
    }

    public void setApply_time(String apply_time) {
        this.apply_time = apply_time;
    }

    public String getAccount_name() {
        return account_name;
    }

    public void setAccount_name(String account_name) {
        this.account_name = account_name;
    }

    public String getBank() {
        return bank;
    }

    public void setBank(String bank) {
        this.bank = bank;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }
}