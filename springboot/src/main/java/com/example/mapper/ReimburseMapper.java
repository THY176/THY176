package com.example.mapper;

import com.example.entity.Reimburse;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface ReimburseMapper {
    List<Reimburse> selectAll(Reimburse reimburse);

    @Select("select * from `reimburse` where reimburse.reimburse_ID = #{reimburse_ID}")
    Reimburse selectByreimburse_ID(Integer reimburse_ID);

    @Select("select * from `reimburse` where reimburse.apply_ID = #{apply_ID}")
    List<Reimburse> selectByApply_ID(Integer apply_ID);

    @Select("select * from `reimburse` where reimburse.status = #{status}")
    List<Reimburse> selectByStatus(String status);

    @Select("select * from `reimburse` where reimburse.teacher_ID = #{teacher_ID}")
    List<Reimburse> selectByTeacher_ID(Integer teacher_ID);

    void insert(Reimburse reimburse);

    void updateByreimburse_ID(Reimburse reimburse);

    @Delete("delete from `reimburse` where reimburse_ID = #{reimburse_ID}")
    void delByreimburse_ID(Integer reimburse_ID);
}
