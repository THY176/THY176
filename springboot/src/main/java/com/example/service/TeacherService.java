package com.example.service;

import com.example.entity.Teacher;
import com.example.mapper.TeacherMapper;
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
        teacher.setPassword(passwordService.encodeIfNeeded(teacher.getPassword()));
        teacherMapper.insert(teacher);
    }

    public void update(Teacher teacher) {
        if (teacher == null || teacher.getTeacher_ID() == null) {
            throw new RuntimeException("教师工号不能为空");
        }

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
}
