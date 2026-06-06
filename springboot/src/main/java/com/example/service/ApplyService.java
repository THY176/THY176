package com.example.service;

import com.example.entity.Apply;
import com.example.mapper.ApplyMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ApplyService {

    @Resource
    private ApplyMapper applyMapper;

    public List<Apply> selectAll(Apply apply) {
        return applyMapper.selectAll(apply);
    }

    public Apply selectByapply_ID(Integer apply_ID) {
        return applyMapper.selectByapply_ID(apply_ID);
    }

    public PageInfo<Apply> selectPage(Apply apply, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Apply> list = applyMapper.selectAll(apply);
        return PageInfo.of(list);
    }

    public void add(Apply apply) {
        applyMapper.insert(apply);
    }

    public void update(Apply apply) {
        System.out.println("=== ApplyService.update ===");
        System.out.println("apply_ID: " + apply.getApply_ID());
        System.out.println("processInstanceId: " + apply.getProcessInstanceId());
        applyMapper.updateByapply_ID(apply);
    }

    public void delByapply_ID(Integer apply_ID) {
        applyMapper.delByapply_ID(apply_ID);
    }

    public void delBatch(List<Integer> apply_IDs) {
        applyMapper.delBatch(apply_IDs);
    }

    public List<Apply> selectByTeam_ID(Integer team_ID) {
        return applyMapper.selectByTeam_ID(team_ID);
    }

    public List<Apply> selectByStatus(String status) {
        return applyMapper.selectByStatus(status);
    }

    public List<Apply> selectByApply_type(String apply_type) {
        return applyMapper.selectByApply_type(apply_type);
    }

    public List<Apply> selectByTag(String tag) {
        List<Apply> list = applyMapper.selectAll(new Apply());
        return list.stream()
                .filter(apply -> apply.getTags() != null && apply.getTags().contains(tag))
                .collect(Collectors.toList());
    }

    public void addTags(Integer apply_ID, String tags) {
        Apply apply = applyMapper.selectByapply_ID(apply_ID);
        String oldTags = apply.getTags();
        String newTags;
        if (oldTags == null || oldTags.isEmpty()) {
            newTags = tags;
        } else {
            newTags = oldTags + "," + tags;
        }
        apply.setTags(newTags);
        applyMapper.updateByapply_ID(apply);
    }

    public void delTags(Integer apply_ID, String tags) {
        Apply apply = applyMapper.selectByapply_ID(apply_ID);
        String oldTags = apply.getTags();
        if (oldTags != null && !oldTags.isEmpty()) {
            List<String> tagList = Arrays.asList(oldTags.split(","));
            List<String> delTagList = Arrays.asList(tags.split(","));
            String newTags = tagList.stream()
                    .filter(tag -> !delTagList.contains(tag))
                    .collect(Collectors.joining(","));
            apply.setTags(newTags.isEmpty() ? null : newTags);
            applyMapper.updateByapply_ID(apply);
        }
    }
}