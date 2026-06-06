package com.example.service;

import com.example.entity.Admin;
import com.example.mapper.AdminMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminService {

    @Resource
    private AdminMapper adminMapper;

    public List<Admin> selectAll(Admin admin) {
        return adminMapper.selectAll(admin);
    }

    public Admin selectByteacher_ID(Integer teacher_ID) {
        return adminMapper.selectByteacher_ID(teacher_ID);
    }

    public PageInfo<Admin> selectPage(Admin admin, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Admin> list = adminMapper.selectAll(admin);
        return PageInfo.of(list);
    }

    public void add(Admin admin) {
        adminMapper.insert(admin);
    }

    public void update(Admin admin) {
        adminMapper.updateByteacher_ID(admin);
    }

    public void delByteacher_ID(Integer teacher_ID) {
        adminMapper.delByteacher_ID(teacher_ID);
    }

    public void delBatch(List<Integer> teacher_IDs) {
        adminMapper.delBatch(teacher_IDs);
    }
}