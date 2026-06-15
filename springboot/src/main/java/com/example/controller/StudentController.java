package com.example.controller;

import com.example.auth.AuthContext;
import com.example.auth.AuthUser;
import com.example.common.Result;
import com.example.entity.Student;
import com.example.service.StudentService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@RestController
@RequestMapping("/student")
public class StudentController {

    private static final Logger log = LoggerFactory.getLogger(StudentController.class);

    @Resource
    private StudentService studentService;

    @GetMapping("/selectAll")
    public Result selectAll(Student student) {
        bindTeamQuery(student);
        List<Student> list = studentService.selectAll(student);
        return Result.success(list);
    }

    @GetMapping("/selectByID/{ID}")
    public Result selectByID(@PathVariable Integer ID) {
        if (!canAccessStudent(ID)) {
            return forbidden();
        }
        Student student = studentService.selectByID(ID);
        return Result.success(student);
    }

    @GetMapping("/selectPage")
    public Result selectPage(Student student,
                             @RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "10") Integer pageSize) {
        bindTeamQuery(student);
        PageInfo<Student> pageInfo = studentService.selectPage(student, pageNum, pageSize);
        return Result.success(pageInfo);
    }

    @PostMapping("/add")
    public Result add(@RequestBody Student student) {
        bindTeamWrite(student);
        try {
            studentService.add(student);
            // 更新对应社团的人数
            if (student.getTeam_ID() != null) {
                studentService.updateTeamNumber(student.getTeam_ID());
            }
            return Result.success();
        } catch (IllegalArgumentException e) {
            return Result.error("400", e.getMessage());
        } catch (Exception e) {
            log.error("新增成员失败: ", e);
            return Result.error("新增成员失败: " + e.getMessage());
        }
    }

    @PutMapping("/update")
    public Result update(@RequestBody Student student) {
        try {
            Student oldStudent = student.getID() == null ? null : studentService.selectByID(student.getID());
            if (!canAccessStudent(student.getID())) {
                return forbidden();
            }
            if (isTeamUser()) {
                student.setTeam_ID(currentUser().getId());
            }
            studentService.update(student);
            if (oldStudent != null && oldStudent.getTeam_ID() != null) {
                studentService.updateTeamNumber(oldStudent.getTeam_ID());
            }
            if (student.getTeam_ID() != null && (oldStudent == null || !student.getTeam_ID().equals(oldStudent.getTeam_ID()))) {
                studentService.updateTeamNumber(student.getTeam_ID());
            }
            return Result.success();
        } catch (IllegalArgumentException e) {
            return Result.error("400", e.getMessage());
        } catch (Exception e) {
            log.error("更新成员失败: ", e);
            return Result.error("更新成员失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/delByID/{ID}")
    public Result delByID(@PathVariable Integer ID) {
        try {
            // 先获取成员信息
            if (!canAccessStudent(ID)) {
                return forbidden();
            }
            Student student = studentService.selectByID(ID);

            if (student == null) {
                return Result.error("成员不存在");
            }

            Integer teamId = student.getTeam_ID();

            if (teamId == null) {
                // 仍然执行删除，但不更新人数
                studentService.delByID(ID);
                return Result.success("删除成功，但该成员没有关联社团，未更新人数");
            }

            // 执行删除
            studentService.delByID(ID);

            // 更新社团人数
            studentService.updateTeamNumber(teamId);

            return Result.success();
        } catch (Exception e) {
            return Result.error("删除成员失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/delBatch")
    public Result delBatch(@RequestBody List<Integer> IDs) {
        for (Integer id : IDs) {
            if (!canAccessStudent(id)) {
                return forbidden();
            }
        }
        studentService.delBatch(IDs);
        return Result.success();
    }

    @GetMapping("/selectByTeam_ID/{team_ID}")
    public Result selectByTeam_ID(@PathVariable Integer team_ID) {
        if (isTeamUser() && !currentUser().getId().equals(team_ID)) {
            return forbidden();
        }
        List<Student> list = studentService.selectByTeam_ID(team_ID);
        return Result.success(list);
    }

    @GetMapping("/selectByRole/{role}")
    public Result selectByRole(@PathVariable String role) {
        List<Student> list = studentService.selectByRole(role);
        return Result.success(list);
    }

    private void bindTeamQuery(Student student) {
        if (isTeamUser()) {
            student.setTeam_ID(currentUser().getId());
        }
    }

    private void bindTeamWrite(Student student) {
        if (isTeamUser()) {
            student.setTeam_ID(currentUser().getId());
        }
    }

    private boolean canAccessStudent(Integer id) {
        if (!isTeamUser()) {
            return true;
        }
        if (id == null) {
            return false;
        }
        Student student = studentService.selectByID(id);
        return student != null && currentUser().getId().equals(student.getTeam_ID());
    }

    private boolean isTeamUser() {
        AuthUser user = currentUser();
        return user != null && "team".equals(user.getRole());
    }

    private AuthUser currentUser() {
        return AuthContext.get();
    }

    private Result forbidden() {
        return Result.error("403", "无权操作该成员");
    }
}
