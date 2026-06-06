package com.example.service;

import com.example.entity.Reimburse;
import com.example.mapper.ReimburseMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReimburseService {

    @Resource
    private ReimburseMapper reimburseMapper;

    public List<Reimburse> selectAll(Reimburse reimburse) {
        return reimburseMapper.selectAll(reimburse);
    }

    public Reimburse selectByreimburse_ID(Integer reimburse_ID) {
        return reimburseMapper.selectByreimburse_ID(reimburse_ID);
    }

    public PageInfo<Reimburse> selectPage(Reimburse reimburse, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Reimburse> list = reimburseMapper.selectAll(reimburse);
        return PageInfo.of(list);
    }

    public void add(Reimburse reimburse) {
        reimburseMapper.insert(reimburse);
    }

    public void update(Reimburse reimburse) {
        reimburseMapper.updateByreimburse_ID(reimburse);
    }

    public void delByreimburse_ID(Integer reimburse_ID) {
        reimburseMapper.delByreimburse_ID(reimburse_ID);
    }

    public void delBatch(List<Integer> reimburse_IDs) {
        for (Integer reimburse_ID : reimburse_IDs) {
            reimburseMapper.delByreimburse_ID(reimburse_ID);
        }
    }

    public List<Reimburse> selectByApply_ID(Integer apply_ID) {
        return reimburseMapper.selectByApply_ID(apply_ID);
    }

    public List<Reimburse> selectByStatus(String status) {
        return reimburseMapper.selectByStatus(status);
    }

    public List<Reimburse> selectByTeacher_ID(Integer teacher_ID) {
        return reimburseMapper.selectByTeacher_ID(teacher_ID);
    }
}