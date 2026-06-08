package com.example.controller;

import com.example.auth.AuthContext;
import com.example.auth.AuthUser;
import com.example.common.Result;
import com.example.entity.Teacher;
import com.example.service.TeacherService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/teacher")
public class TeacherController {

    @Resource
    private TeacherService teacherService;

    @GetMapping("/selectAll")
    public Result selectAll(Teacher teacher) {
        bindTeacherQuery(teacher);
        List<Teacher> list = teacherService.selectAll(teacher);
        list.forEach(this::clearPassword);
        return Result.success(list);
    }

    @GetMapping("/selectByteacher_ID/{teacher_ID}")
    public Result selectByteacher_ID(@PathVariable Integer teacher_ID) {
        if (isTeacherUser() && !currentUser().getId().equals(teacher_ID)) {
            return forbidden();
        }
        Teacher teacher = teacherService.selectByteacher_ID(teacher_ID);
        clearPassword(teacher);
        return Result.success(teacher);
    }

    @GetMapping("/selectPage")
    public Result selectPage(Teacher teacher,
                             @RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "10") Integer pageSize) {
        bindTeacherQuery(teacher);
        PageInfo<Teacher> pageInfo = teacherService.selectPage(teacher, pageNum, pageSize);
        pageInfo.getList().forEach(this::clearPassword);
        return Result.success(pageInfo);
    }

    @PostMapping("/add")
    public Result add(@RequestBody Teacher teacher) {
        teacherService.add(teacher);
        return Result.success();
    }

    @PutMapping("/update")
    public Result update(@RequestBody Teacher teacher) {
        if (isTeacherUser()) {
            if (teacher.getTeacher_ID() != null && !currentUser().getId().equals(teacher.getTeacher_ID())) {
                return forbidden();
            }
            Teacher existing = teacherService.selectByteacher_ID(currentUser().getId());
            teacher.setTeacher_ID(currentUser().getId());
            if (existing != null) {
                teacher.setTeam_ID(existing.getTeam_ID());
            }
        }
        teacherService.update(teacher);
        return Result.success();
    }

    @DeleteMapping("/delByteacher_ID/{teacher_ID}")
    public Result delByteacher_ID(@PathVariable Integer teacher_ID) {
        teacherService.delByteacher_ID(teacher_ID);
        return Result.success();
    }

    @DeleteMapping("/delBatch")
    public Result delBatch(@RequestBody List<Integer> teacher_IDs) {
        teacherService.delBatch(teacher_IDs);
        return Result.success();
    }

    @GetMapping("/selectByTeam_ID/{team_ID}")
    public Result selectByTeam_ID(@PathVariable Integer team_ID) {
        List<Teacher> list = teacherService.selectByTeam_ID(team_ID);
        list.forEach(this::clearPassword);
        return Result.success(list);
    }

    private void clearPassword(Teacher teacher) {
        if (teacher != null) {
            teacher.setPassword(null);
        }
    }

    private void bindTeacherQuery(Teacher teacher) {
        if (isTeacherUser()) {
            teacher.setTeacher_ID(currentUser().getId());
        }
    }

    private boolean isTeacherUser() {
        AuthUser user = currentUser();
        return user != null && "teacher".equals(user.getRole());
    }

    private AuthUser currentUser() {
        return AuthContext.get();
    }

    private Result forbidden() {
        return Result.error("403", "无权操作该教师");
    }
}
