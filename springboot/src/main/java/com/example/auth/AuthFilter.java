package com.example.auth;

import com.example.common.Result;
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

    public AuthFilter(JwtService jwtService, ObjectMapper objectMapper) {
        this.jwtService = jwtService;
        this.objectMapper = objectMapper;
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
            return path.startsWith("/teacher/")
                    || path.startsWith("/workflow/teacher/")
                    || path.equals("/workflow/firstAudit")
                    || ("GET".equals(method) && path.startsWith("/workflow/currentTask/"))
                    || ("GET".equals(method) && path.startsWith("/apply/selectByapply_ID/"))
                    || ("GET".equals(method) && path.startsWith("/approve/selectByApply_ID/"))
                    || ("GET".equals(method) && path.startsWith("/approve/selectByTeacher_ID/"));
        }

        if ("team".equals(role)) {
            return path.startsWith("/team/")
                    || path.startsWith("/student/")
                    || path.startsWith("/apply/")
                    || ("GET".equals(method) && path.startsWith("/workflow/currentTask/"))
                    || ("GET".equals(method) && path.startsWith("/approve/selectByApply_ID/"))
                    || ("GET".equals(method) && path.startsWith("/reimburse/selectByApply_ID/"));
        }

        return false;
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
