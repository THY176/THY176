package com.example.mapper;

import com.example.entity.Teacher;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.ResultMap;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface TeacherMapper {
    List<Teacher> selectAll(Teacher teacher);

    @Select("select * from `teacher` where teacher.teacher_ID = #{teacher_ID}")
    @ResultMap("teacherMap")
    Teacher selectByteacher_ID(Integer teacher_ID);

    @Select("select * from `teacher` where teacher.team_ID = #{team_ID}")
    @ResultMap("teacherMap")
    List<Teacher> selectByTeam_ID(Integer team_ID);

    void insert(Teacher teacher);

    void updateByteacher_ID(Teacher teacher);

    @Delete("delete from `teacher` where teacher_ID = #{teacher_ID}")
    void delByteacher_ID(Integer teacher_ID);
}
