package com.example.mapper;

import com.example.entity.Student;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.*;

import java.util.List;

public interface StudentMapper {
    List<Student> selectAll(Student student);

    @Select("select * from `student` where ID = #{ID}")
    @ResultMap("studentMap")
    Student selectByID(Integer ID);

    @Select("select * from `student` where team_ID = #{team_ID}")
    @ResultMap("studentMap")
    List<Student> selectByTeam_ID(Integer team_ID);

    @Select("select * from `student` where role = #{role}")
    @ResultMap("studentMap")
    List<Student> selectByRole(String role);

    void insert(Student student);

    void updateByID(Student student);
    void replace(Student student);

    @Delete("delete from `student` where ID = #{ID}")
    void delByID(Integer ID);

    @Select("select count(*) from `student` where team_ID = #{teamId}")
    int countByTeamId(@Param("teamId") Integer teamId);
}
