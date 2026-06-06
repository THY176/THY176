package com.example.service;

import com.example.entity.Approve;
import com.example.mapper.ApproveMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ApproveService {

    @Resource
    private ApproveMapper approveMapper;

    public List<Approve> selectAll(Approve approve) {
        return approveMapper.selectAll(approve);
    }

    public Approve selectByapprove_ID(Integer approve_ID) {
        return approveMapper.selectByapprove_ID(approve_ID);
    }

    public PageInfo<Approve> selectPage(Approve approve, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Approve> list = approveMapper.selectAll(approve);
        return PageInfo.of(list);
    }

    public void add(Approve approve) {
        approveMapper.insert(approve);
    }

    public void update(Approve approve) {
        approveMapper.updateByapprove_ID(approve);
    }

    public void delByapprove_ID(Integer approve_ID) {
        approveMapper.delByapprove_ID(approve_ID);
    }

    public void delBatch(List<Integer> approve_IDs) {
        approveMapper.delBatch(approve_IDs);
    }

    public List<Approve> selectByApply_ID(Integer apply_ID) {
        return approveMapper.selectByApply_ID(apply_ID);
    }

    public List<Approve> selectByStatus(String status) {
        return approveMapper.selectByStatus(status);
    }

    public List<Approve> selectByTeacher_ID(Integer teacher_ID) {
        return approveMapper.selectByTeacher_ID(teacher_ID);
    }

    public List<Approve> selectByRole(String role) {
        return approveMapper.selectByRole(role);
    }

    public List<Approve> selectByApply_IDAndSequence(Integer apply_ID, Integer sequence) {
        return approveMapper.selectByApply_IDAndSequence(apply_ID, sequence);
    }
}