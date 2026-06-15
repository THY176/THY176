package com.example.service;

import com.example.entity.Student;
import com.example.entity.Team;
import com.example.mapper.StudentMapper;
import com.example.mapper.TeamMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class StudentService {

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
        validateForSave(student);
        if (studentMapper.selectByID(student.getID()) != null) {
            throw new IllegalArgumentException("学号已存在");
        }
        studentMapper.insert(student);
    }

    @Transactional
    public void update(Student student) {
        validateForSave(student);
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

        // 更新社团表的人数
        Team team = new Team();
        team.setTeam_ID(teamId);
        team.setNumber(count);
        int result = teamMapper.updateNumber(team);

        return count;
    }
    // 统计社团人数
    public int countByTeamId(Integer teamId) {
        return studentMapper.countByTeamId(teamId);
    }

    private void validateForSave(Student student) {
        if (student == null) {
            throw new IllegalArgumentException("成员信息不能为空");
        }
        if (student.getID() == null) {
            throw new IllegalArgumentException("学号不能为空");
        }
        if (student.getTeam_ID() == null) {
            throw new IllegalArgumentException("所属社团不能为空");
        }
        if (teamMapper.selectByteam_ID(student.getTeam_ID()) == null) {
            throw new IllegalArgumentException("所属社团不存在");
        }
        if (isBlank(student.getName())) {
            throw new IllegalArgumentException("姓名不能为空");
        }
        if (isBlank(student.getRole())) {
            throw new IllegalArgumentException("职位不能为空");
        }
        if (isBlank(student.getTele())) {
            throw new IllegalArgumentException("电话不能为空");
        }
        if (isBlank(student.getGender())) {
            throw new IllegalArgumentException("性别不能为空");
        }
        if (student.getAge() == null) {
            throw new IllegalArgumentException("年龄不能为空");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
