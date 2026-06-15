package com.example.service;

import com.example.entity.Teacher;
import com.example.mapper.TeacherMapper;
import com.example.mapper.TeamMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TeacherService {

    @Resource
    private TeacherMapper teacherMapper;

    @Resource
    private TeamMapper teamMapper;

    @Resource
    private PasswordService passwordService;

    public List<Teacher> selectAll(Teacher teacher) {
        return teacherMapper.selectAll(teacher);
    }

    public Teacher selectByteacher_ID(Integer teacher_ID) {
        return teacherMapper.selectByteacher_ID(teacher_ID);
    }

    public PageInfo<Teacher> selectPage(Teacher teacher, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Teacher> list = teacherMapper.selectAll(teacher);
        return PageInfo.of(list);
    }

    public void add(Teacher teacher) {
        validateForSave(teacher, true);
        if (teacherMapper.selectByteacher_ID(teacher.getTeacher_ID()) != null) {
            throw new IllegalArgumentException("教师工号已存在");
        }
        teacher.setPassword(passwordService.encodeIfNeeded(teacher.getPassword()));
        teacherMapper.insert(teacher);
    }

    public void update(Teacher teacher) {
        validateForSave(teacher, false);
        teacher.setPassword(passwordService.encodeIfNeeded(teacher.getPassword()));
        teacherMapper.updateByteacher_ID(teacher);
    }

    public void delByteacher_ID(Integer teacher_ID) {
        teacherMapper.delByteacher_ID(teacher_ID);
    }

    public void delBatch(List<Integer> teacher_IDs) {
        for (Integer teacher_ID : teacher_IDs) {
            teacherMapper.delByteacher_ID(teacher_ID);
        }
    }

    public List<Teacher> selectByTeam_ID(Integer team_ID) {
        return teacherMapper.selectByTeam_ID(team_ID);
    }

    private void validateForSave(Teacher teacher, boolean requirePassword) {
        if (teacher == null) {
            throw new IllegalArgumentException("教师信息不能为空");
        }
        if (teacher.getTeacher_ID() == null) {
            throw new IllegalArgumentException("教师工号不能为空");
        }
        if (teacher.getTeam_ID() != null && teamMapper.selectByteam_ID(teacher.getTeam_ID()) == null) {
            throw new IllegalArgumentException("所属社团不存在");
        }
        if (isBlank(teacher.getName())) {
            throw new IllegalArgumentException("姓名不能为空");
        }
        if (requirePassword && isBlank(teacher.getPassword())) {
            throw new IllegalArgumentException("密码不能为空");
        }
        if (isBlank(teacher.getTele())) {
            throw new IllegalArgumentException("电话不能为空");
        }
        if (isBlank(teacher.getGender())) {
            throw new IllegalArgumentException("性别不能为空");
        }
        if (teacher.getAge() == null) {
            throw new IllegalArgumentException("年龄不能为空");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
