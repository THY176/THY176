package com.example.service;

import com.example.entity.Apply;
import com.example.entity.Approve;
import com.example.mapper.ApplyMapper;
import com.example.mapper.ApproveMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.flowable.engine.RuntimeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ApplyService {

    @Resource
    private ApplyMapper applyMapper;

    @Resource
    private ApproveMapper approveMapper;

    @Resource
    private RuntimeService runtimeService;

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
        validateStatusUpdate(apply);
        applyMapper.updateByapply_ID(apply);
    }

    public void updateFromWorkflow(Apply apply) {
        applyMapper.updateByapply_ID(apply);
    }

    @Transactional
    public void forceReject(Integer applyId, Integer adminId, String adminName, String opinion) {
        Apply existing = applyMapper.selectByapply_ID(applyId);
        if (existing == null) {
            throw new RuntimeException("申请不存在");
        }
        if (hasActiveProcess(existing)) {
            runtimeService.deleteProcessInstance(
                    existing.getProcessInstanceId(),
                    "管理员强制驳回：" + existing.getStatus() + " -> 审核驳回"
            );
        }

        Apply updateApply = new Apply();
        updateApply.setApply_ID(applyId);
        updateApply.setStatus("审核驳回");
        applyMapper.updateByapply_ID(updateApply);

        Approve approve = new Approve();
        approve.setApply_ID(applyId);
        approve.setTeacher_ID(adminId);
        approve.setTeacher_name(adminName);
        approve.setRole("管理员强制驳回");
        approve.setSequence(99);
        approve.setOpinion((opinion == null || opinion.isBlank()) ? "管理员强制驳回" : opinion);
        approve.setStatus("审核驳回");
        approve.setApprove_time(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        approveMapper.insert(approve);
    }

    private void validateStatusUpdate(Apply apply) {
        if (apply == null || apply.getApply_ID() == null || apply.getStatus() == null) {
            return;
        }

        Apply existing = applyMapper.selectByapply_ID(apply.getApply_ID());
        if (existing == null || !hasActiveProcess(existing)) {
            return;
        }

        String oldStatus = existing.getStatus();
        String newStatus = apply.getStatus();
        if (oldStatus != null && !oldStatus.equals(newStatus)) {
            throw new RuntimeException("申请存在进行中的工作流任务，状态变更必须通过工作流接口完成");
        }
    }

    private boolean hasActiveProcess(Apply apply) {
        String processInstanceId = apply.getProcessInstanceId();
        if (processInstanceId == null || processInstanceId.isBlank()) {
            return false;
        }
        return runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .active()
                .count() > 0;
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
