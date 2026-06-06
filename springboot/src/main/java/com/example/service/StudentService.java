package com.example.service;

import com.example.entity.Student;
import com.example.entity.Team;
import com.example.mapper.StudentMapper;
import com.example.mapper.TeamMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class StudentService {

    private static final Logger log = LoggerFactory.getLogger(StudentService.class);

    @Resource
    private StudentMapper studentMapper;

    @Resource
    private TeamMapper teamMapper;

    public List<Student> selectAll(Student student) {
        return studentMapper.selectAll(student);
    }

    public Student selectByID(Integer ID) {
        return studentMapper.selectByID(ID);
    }

    public PageInfo<Student> selectPage(Student student, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Student> list = studentMapper.selectAll(student);
        return PageInfo.of(list);
    }

    @Transactional
    public void add(Student student) {
        studentMapper.insert(student);
    }

    @Transactional
    public void update(Student student) {
        studentMapper.replace(student);
    }

    @Transactional
    public void delByID(Integer ID) {
        studentMapper.delByID(ID);
    }

    public void delBatch(List<Integer> IDs) {
        for (Integer ID : IDs) {
            studentMapper.delByID(ID);
        }
    }

    public List<Student> selectByTeam_ID(Integer team_ID) {
        return studentMapper.selectByTeam_ID(team_ID);
    }

    public List<Student> selectByRole(String role) {
        return studentMapper.selectByRole(role);
    }

    /**
     * 更新社团人数
     */
    @Transactional
    public int updateTeamNumber(Integer teamId) {
        if (teamId == null) return 0;

        // 统计该社团的成员数量
        int count = studentMapper.countByTeamId(teamId);
        System.out.println("=== updateTeamNumber ===");
        System.out.println("社团ID: " + teamId);
        System.out.println("统计到的学生数量: " + count);

        // 更新社团表的人数
        Team team = new Team();
        team.setTeam_ID(teamId);
        team.setNumber(count);
        int result = teamMapper.updateNumber(team);
        System.out.println("更新结果(影响行数): " + result);

        return count;
    }
    // 统计社团人数
    public int countByTeamId(Integer teamId) {
        return studentMapper.countByTeamId(teamId);
    }
}