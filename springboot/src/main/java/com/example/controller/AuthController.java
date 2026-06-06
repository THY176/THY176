package com.example.controller;

import com.example.auth.AuthUser;
import com.example.auth.JwtService;
import com.example.auth.LoginRequest;
import com.example.common.Result;
import com.example.entity.Admin;
import com.example.entity.Teacher;
import com.example.entity.Team;
import com.example.service.AdminService;
import com.example.service.PasswordService;
import com.example.service.TeacherService;
import com.example.service.TeamService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    @Resource
    private TeamService teamService;

    @Resource
    private TeacherService teacherService;

    @Resource
    private AdminService adminService;

    @Resource
    private PasswordService passwordService;

    @Resource
    private JwtService jwtService;

    @PostMapping("/login")
    public Result login(@RequestBody LoginRequest loginRequest) {
        if (loginRequest.getAccount() == null || isBlank(loginRequest.getPassword()) || isBlank(loginRequest.getRole())) {
            return Result.error("400", "账号、密码和角色不能为空");
        }

        return switch (loginRequest.getRole()) {
            case "team" -> loginTeam(loginRequest);
            case "teacher" -> loginTeacher(loginRequest);
            case "admin" -> loginAdmin(loginRequest);
            default -> Result.error("400", "不支持的登录角色");
        };
    }

    private Result loginTeam(LoginRequest loginRequest) {
        Team team = teamService.selectByteam_ID(loginRequest.getAccount());
        if (team == null || !passwordService.matches(loginRequest.getPassword(), team.getPassword())) {
            return Result.error("401", "账号或密码错误");
        }
        if (!passwordService.isEncoded(team.getPassword())) {
            tryUpgradeTeamPassword(team, loginRequest.getPassword());
        }

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("role", "team");
        userInfo.put("id", team.getTeam_ID());
        userInfo.put("team_ID", team.getTeam_ID());
        userInfo.put("team_name", team.getTeam_name());
        userInfo.put("teacher_ID", team.getTeacher_ID());
        userInfo.put("number", team.getNumber());
        userInfo.put("time", team.getTime());
        return buildLoginResult(new AuthUser(team.getTeam_ID(), "team", team.getTeam_name()), userInfo);
    }

    private Result loginTeacher(LoginRequest loginRequest) {
        Teacher teacher = teacherService.selectByteacher_ID(loginRequest.getAccount());
        if (teacher == null || !passwordService.matches(loginRequest.getPassword(), teacher.getPassword())) {
            return Result.error("401", "账号或密码错误");
        }
        if (!passwordService.isEncoded(teacher.getPassword())) {
            tryUpgradeTeacherPassword(teacher, loginRequest.getPassword());
        }

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("role", "teacher");
        userInfo.put("id", teacher.getTeacher_ID());
        userInfo.put("teacher_ID", teacher.getTeacher_ID());
        userInfo.put("name", teacher.getName());
        userInfo.put("team_ID", teacher.getTeam_ID());
        userInfo.put("tele", teacher.getTele());
        userInfo.put("gender", teacher.getGender());
        userInfo.put("age", teacher.getAge());
        return buildLoginResult(new AuthUser(teacher.getTeacher_ID(), "teacher", teacher.getName()), userInfo);
    }

    private Result loginAdmin(LoginRequest loginRequest) {
        Admin admin = adminService.selectByteacher_ID(loginRequest.getAccount());
        if (admin == null || !passwordService.matches(loginRequest.getPassword(), admin.getPassword())) {
            return Result.error("401", "账号或密码错误");
        }
        if (!passwordService.isEncoded(admin.getPassword())) {
            tryUpgradeAdminPassword(admin, loginRequest.getPassword());
        }

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("role", "admin");
        userInfo.put("id", admin.getTeacher_ID());
        userInfo.put("teacher_ID", admin.getTeacher_ID());
        userInfo.put("name", admin.getName());
        userInfo.put("tele", admin.getTele());
        userInfo.put("gender", admin.getGender());
        userInfo.put("age", admin.getAge());
        return buildLoginResult(new AuthUser(admin.getTeacher_ID(), "admin", admin.getName()), userInfo);
    }

    private Result buildLoginResult(AuthUser user, Map<String, Object> userInfo) {
        Map<String, Object> data = new HashMap<>();
        data.put("token", jwtService.createToken(user));
        data.put("userInfo", userInfo);
        return Result.success(data);
    }

    private void tryUpgradeTeamPassword(Team team, String rawPassword) {
        try {
            team.setPassword(passwordService.encodeIfNeeded(rawPassword));
            teamService.update(team);
        } catch (Exception e) {
            log.warn("Failed to upgrade team password hash, team_ID={}", team.getTeam_ID(), e);
        }
    }

    private void tryUpgradeTeacherPassword(Teacher teacher, String rawPassword) {
        try {
            teacher.setPassword(passwordService.encodeIfNeeded(rawPassword));
            teacherService.update(teacher);
        } catch (Exception e) {
            log.warn("Failed to upgrade teacher password hash, teacher_ID={}", teacher.getTeacher_ID(), e);
        }
    }

    private void tryUpgradeAdminPassword(Admin admin, String rawPassword) {
        try {
            admin.setPassword(passwordService.encodeIfNeeded(rawPassword));
            adminService.update(admin);
        } catch (Exception e) {
            log.warn("Failed to upgrade admin password hash, teacher_ID={}", admin.getTeacher_ID(), e);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
