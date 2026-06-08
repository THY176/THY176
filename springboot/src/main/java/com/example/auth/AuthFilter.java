package com.example.auth;

import com.example.common.Result;
import com.example.entity.Apply;
import com.example.entity.Student;
import com.example.entity.Team;
import com.example.mapper.ApplyMapper;
import com.example.mapper.StudentMapper;
import com.example.mapper.TeamMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class AuthFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final ObjectMapper objectMapper;
    private final ApplyMapper applyMapper;
    private final StudentMapper studentMapper;
    private final TeamMapper teamMapper;

    public AuthFilter(JwtService jwtService,
                      ObjectMapper objectMapper,
                      ApplyMapper applyMapper,
                      StudentMapper studentMapper,
                      TeamMapper teamMapper) {
        this.jwtService = jwtService;
        this.objectMapper = objectMapper;
        this.applyMapper = applyMapper;
        this.studentMapper = studentMapper;
        this.teamMapper = teamMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (isPublicRequest(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        AuthUser user = jwtService.verifyToken(request.getHeader("Authorization"));
        if (user == null) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "401", "登录已失效，请重新登录");
            return;
        }

        if (!isAllowed(user, request)) {
            writeError(response, HttpServletResponse.SC_FORBIDDEN, "403", "无权访问该接口");
            return;
        }

        try {
            AuthContext.set(user);
            filterChain.doFilter(request, response);
        } finally {
            AuthContext.clear();
        }
    }

    private boolean isPublicRequest(HttpServletRequest request) {
        String path = request.getRequestURI();
        return "OPTIONS".equalsIgnoreCase(request.getMethod())
                || path.equals("/auth/login")
                || path.equals("/error");
    }

    private boolean isAllowed(AuthUser user, HttpServletRequest request) {
        String role = user.getRole();
        String path = request.getRequestURI();
        String method = request.getMethod();

        if ("admin".equals(role)) {
            return true;
        }

        if ("teacher".equals(role)) {
            return isTeacherAllowed(user, request, path, method);
        }

        if ("team".equals(role)) {
            return isTeamAllowed(user, request, path, method);
        }

        return false;
    }

    private boolean isTeacherAllowed(AuthUser user, HttpServletRequest request, String path, String method) {
        if ("GET".equals(method) && path.startsWith("/teacher/selectByteacher_ID/")) {
            return isCurrentUser(user, pathIdAfterPrefix(path, "/teacher/selectByteacher_ID/"));
        }
        if ("PUT".equals(method) && path.equals("/teacher/update")) {
            return true;
        }
        if ("GET".equals(method) && path.startsWith("/workflow/teacher/tasks/")) {
            return isCurrentUser(user, pathIdAfterPrefix(path, "/workflow/teacher/tasks/"));
        }
        if ("POST".equals(method) && path.equals("/workflow/firstAudit")) {
            return isCurrentUser(user, parseInteger(request.getParameter("teacherId")));
        }
        if ("GET".equals(method) && path.startsWith("/workflow/currentTask/")) {
            return isTeacherApply(user, pathIdAfterPrefix(path, "/workflow/currentTask/"));
        }
        if ("GET".equals(method) && path.startsWith("/apply/selectByapply_ID/")) {
            return isTeacherApply(user, pathIdAfterPrefix(path, "/apply/selectByapply_ID/"));
        }
        if ("GET".equals(method) && path.startsWith("/approve/selectByApply_ID/")) {
            return isTeacherApply(user, pathIdAfterPrefix(path, "/approve/selectByApply_ID/"));
        }
        if ("GET".equals(method) && path.startsWith("/approve/selectByTeacher_ID/")) {
            return isCurrentUser(user, pathIdAfterPrefix(path, "/approve/selectByTeacher_ID/"));
        }
        return false;
    }

    private boolean isTeamAllowed(AuthUser user, HttpServletRequest request, String path, String method) {
        if ("GET".equals(method) && path.startsWith("/team/selectByteam_ID/")) {
            return isCurrentUser(user, pathIdAfterPrefix(path, "/team/selectByteam_ID/"));
        }
        if ("GET".equals(method) && (path.equals("/team/selectAll") || path.equals("/team/selectPage"))) {
            return true;
        }
        if ("PUT".equals(method) && path.equals("/team/update")) {
            return true;
        }

        if ("GET".equals(method) && (path.equals("/apply/selectAll") || path.equals("/apply/selectPage"))) {
            return true;
        }
        if ("POST".equals(method) && (path.equals("/apply/add") || path.equals("/apply/submit"))) {
            return true;
        }
        if ("PUT".equals(method) && path.equals("/apply/update")) {
            return true;
        }
        if ("POST".equals(method) && path.equals("/apply/resubmit")) {
            return true;
        }
        if ("POST".equals(method) && path.equals("/apply/startWorkflow")) {
            return isOwnApply(user, parseInteger(request.getParameter("applyId")));
        }
        if ("DELETE".equals(method) && path.startsWith("/apply/delByapply_ID/")) {
            return isOwnApply(user, pathIdAfterPrefix(path, "/apply/delByapply_ID/"));
        }
        if ("GET".equals(method) && path.startsWith("/apply/selectByapply_ID/")) {
            return isOwnApply(user, pathIdAfterPrefix(path, "/apply/selectByapply_ID/"));
        }
        if ("GET".equals(method) && path.startsWith("/apply/selectByTeam_ID/")) {
            return isCurrentUser(user, pathIdAfterPrefix(path, "/apply/selectByTeam_ID/"));
        }

        if ("GET".equals(method) && (path.equals("/student/selectAll") || path.equals("/student/selectPage"))) {
            return true;
        }
        if ("POST".equals(method) && path.equals("/student/add")) {
            return true;
        }
        if ("PUT".equals(method) && path.equals("/student/update")) {
            return true;
        }
        if ("GET".equals(method) && path.startsWith("/student/selectByID/")) {
            return isOwnStudent(user, pathIdAfterPrefix(path, "/student/selectByID/"));
        }
        if ("DELETE".equals(method) && path.startsWith("/student/delByID/")) {
            return isOwnStudent(user, pathIdAfterPrefix(path, "/student/delByID/"));
        }
        if ("GET".equals(method) && path.startsWith("/student/selectByTeam_ID/")) {
            return isCurrentUser(user, pathIdAfterPrefix(path, "/student/selectByTeam_ID/"));
        }

        if ("GET".equals(method) && path.startsWith("/workflow/currentTask/")) {
            return isOwnApply(user, pathIdAfterPrefix(path, "/workflow/currentTask/"));
        }
        if ("GET".equals(method) && path.startsWith("/approve/selectByApply_ID/")) {
            return isOwnApply(user, pathIdAfterPrefix(path, "/approve/selectByApply_ID/"));
        }
        if ("GET".equals(method) && path.startsWith("/reimburse/selectByApply_ID/")) {
            return isOwnApply(user, pathIdAfterPrefix(path, "/reimburse/selectByApply_ID/"));
        }
        return false;
    }

    private boolean isCurrentUser(AuthUser user, Integer id) {
        return id != null && id.equals(user.getId());
    }

    private boolean isOwnApply(AuthUser user, Integer applyId) {
        if (applyId == null) {
            return false;
        }
        Apply apply = applyMapper.selectByapply_ID(applyId);
        return apply != null && isCurrentUser(user, apply.getTeam_ID());
    }

    private boolean isTeacherApply(AuthUser user, Integer applyId) {
        if (applyId == null) {
            return false;
        }
        Apply apply = applyMapper.selectByapply_ID(applyId);
        if (apply == null || apply.getTeam_ID() == null) {
            return false;
        }
        Team team = teamMapper.selectByteam_ID(apply.getTeam_ID());
        return team != null && isCurrentUser(user, team.getTeacher_ID());
    }

    private boolean isOwnStudent(AuthUser user, Integer studentId) {
        if (studentId == null) {
            return false;
        }
        Student student = studentMapper.selectByID(studentId);
        return student != null && isCurrentUser(user, student.getTeam_ID());
    }

    private Integer pathIdAfterPrefix(String path, String prefix) {
        if (!path.startsWith(prefix)) {
            return null;
        }
        return parseInteger(path.substring(prefix.length()));
    }

    private Integer parseInteger(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void writeError(HttpServletResponse response, int status, String code, String message) throws IOException {
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Headers", "*");
        response.setHeader("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(Result.error(code, message)));
    }
}
