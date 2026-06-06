package com.example.mapper;

import com.example.entity.Admin;
import org.apache.ibatis.annotations.*;
import java.util.List;

public interface AdminMapper {
    List<Admin> selectAll(Admin admin);

    // 删除注解，让 MyBatis 从 XML 读取
    Admin selectByteacher_ID(Integer teacher_ID);

    void insert(Admin admin);

    void updateByteacher_ID(Admin admin);

    void delByteacher_ID(Integer teacher_ID);

    void delBatch(List<Integer> teacher_IDs);
    @Select("SELECT teacher_ID FROM admin")
    List<Integer> getAllAdminIds();
}