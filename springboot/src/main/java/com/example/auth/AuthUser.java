package com.example.auth;

public class AuthUser {
    private Integer id;
    private String role;
    private String name;

    public AuthUser() {
    }

    public AuthUser(Integer id, String role, String name) {
        this.id = id;
        this.role = role;
        this.name = name;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
