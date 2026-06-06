package com.example.mapper;

import com.example.entity.Approve;
import org.apache.ibatis.annotations.*;
import java.util.List;

public interface ApproveMapper {
    List<Approve> selectAll(Approve approve);

    // 删除 @Select 和 @Results，让 MyBatis 从 XML 中读取
    Approve selectByapprove_ID(Integer approve_ID);

    // 这个方法已在 ApproveMapper.xml 中定义
    List<Approve> selectByApply_ID(Integer apply_ID);

    // 删除 @Select 和 @ResultMap
    List<Approve> selectByStatus(String status);

    // 删除 @Select 和 @ResultMap
    List<Approve> selectByTeacher_ID(Integer teacher_ID);

    // 删除 @Select 和 @ResultMap
    List<Approve> selectByRole(String role);

    // 删除 @Select 和 @ResultMap
    List<Approve> selectByApply_IDAndSequence(@Param("apply_ID") Integer apply_ID, @Param("sequence") Integer sequence);

    void insert(Approve approve);

    void updateByapprove_ID(Approve approve);

    @Delete("DELETE FROM `approve` WHERE approve_ID = #{approve_ID}")
    void delByapprove_ID(Integer approve_ID);

    void delBatch(List<Integer> approve_IDs);
}