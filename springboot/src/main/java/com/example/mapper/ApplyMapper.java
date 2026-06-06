package com.example.mapper;

import com.example.entity.Apply;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.*;

import java.util.List;

public interface ApplyMapper {
    List<Apply> selectAll(Apply apply);


    @Select("select * from `apply` where apply.apply_ID = #{apply_ID}")
    @ResultMap("applyMap")
    Apply selectByapply_ID(Integer apply_ID);

    @Select("select * from `apply` where apply.team_ID = #{team_ID}")
    @ResultMap("applyMap")
    List<Apply> selectByTeam_ID(Integer team_ID);

    @Select("select * from `apply` where apply.status = #{status}")
    @ResultMap("applyMap")
    List<Apply> selectByStatus(String status);

    @Select("select * from `apply` where apply.apply_type = #{apply_type}")
    List<Apply> selectByApply_type(String apply_type);

    void insert(Apply apply);

    void updateByapply_ID(Apply apply);

    @Delete("delete from `apply` where apply_ID = #{apply_ID}")
    void delByapply_ID(Integer apply_ID);

    void delBatch(List<Integer> apply_IDs);
}