package com.example.controller;

import com.example.auth.AuthContext;
import com.example.auth.AuthUser;
import com.example.common.Result;
import com.example.entity.Apply;
import com.example.entity.Team;
import com.example.service.ApplyService;
import com.example.service.TeamService;
import com.example.service.WorkflowService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/apply")
public class ApplyController {

    @Resource
    private ApplyService applyService;

    @Resource
    private TeamService teamService;

    @Autowired
    private WorkflowService workflowService;

    @GetMapping("/selectAll")
    public Result selectAll(Apply apply) {
        bindTeamQuery(apply);
        List<Apply> list = applyService.selectAll(apply);
        return Result.success(list);
    }

    @GetMapping("/selectByapply_ID/{apply_ID}")
    public Result selectByapply_ID(@PathVariable Integer apply_ID) {
        if (!canAccessApply(apply_ID)) {
            return forbidden();
        }
        Apply apply = applyService.selectByapply_ID(apply_ID);
        return Result.success(apply);
    }

    @GetMapping("/selectPage")
    public Result selectPage(Apply apply,
                             @RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "10") Integer pageSize) {
        bindTeamQuery(apply);
        PageInfo<Apply> pageInfo = applyService.selectPage(apply, pageNum, pageSize);
        return Result.success(pageInfo);
    }

    @PostMapping("/add")
    public Result add(@RequestBody Apply apply) {
        bindTeamWrite(apply);
        applyService.add(apply);
        return Result.success();
    }

    @PutMapping("/update")
    public Result update(@RequestBody Apply apply) {
        try {
            if (!canAccessApply(apply.getApply_ID())) {
                return forbidden();
            }
            if (isTeamUser()) {
                apply.setTeam_ID(currentUser().getId());
                apply.setStatus(null);
                apply.setProcessInstanceId(null);
            }
            applyService.update(apply);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/forceReject")
    public Result forceReject(@RequestParam Integer applyId,
                              @RequestParam Integer adminId,
                              @RequestParam String adminName,
                              @RequestParam(required = false) String opinion) {
        try {
            applyService.forceReject(applyId, adminId, adminName, opinion);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/delByapply_ID/{apply_ID}")
    public Result delByapply_ID(@PathVariable Integer apply_ID) {
        if (!canAccessApply(apply_ID)) {
            return forbidden();
        }
        applyService.delByapply_ID(apply_ID);
        return Result.success();
    }

    @DeleteMapping("/delBatch")
    public Result delBatch(@RequestBody List<Integer> apply_IDs) {
        for (Integer applyId : apply_IDs) {
            if (!canAccessApply(applyId)) {
                return forbidden();
            }
        }
        applyService.delBatch(apply_IDs);
        return Result.success();
    }

    @GetMapping("/selectByTeam_ID/{team_ID}")
    public Result selectByTeam_ID(@PathVariable Integer team_ID) {
        if (isTeamUser() && !currentUser().getId().equals(team_ID)) {
            return forbidden();
        }
        List<Apply> list = applyService.selectByTeam_ID(team_ID);
        return Result.success(list);
    }

    @GetMapping("/selectByStatus/{status}")
    public Result selectByStatus(@PathVariable String status) {
        List<Apply> list = applyService.selectByStatus(status);
        return Result.success(list);
    }

    @GetMapping("/selectByApply_type/{apply_type}")
    public Result selectByApply_type(@PathVariable String apply_type) {
        List<Apply> list = applyService.selectByApply_type(apply_type);
        return Result.success(list);
    }

    @GetMapping("/selectByTag")
    public Result selectByTag(@RequestParam String tag) {
        List<Apply> list = applyService.selectByTag(tag);
        return Result.success(list);
    }

    @PostMapping("/addTags")
    public Result addTags(@RequestParam Integer apply_ID, @RequestParam String tags) {
        if (!canAccessApply(apply_ID)) {
            return forbidden();
        }
        applyService.addTags(apply_ID, tags);
        return Result.success();
    }

    @DeleteMapping("/delTags")
    public Result delTags(@RequestParam Integer apply_ID, @RequestParam String tags) {
        if (!canAccessApply(apply_ID)) {
            return forbidden();
        }
        applyService.delTags(apply_ID, tags);
        return Result.success();
    }

    @GetMapping("/getTags/{apply_ID}")
    public Result getTags(@PathVariable Integer apply_ID) {
        if (!canAccessApply(apply_ID)) {
            return forbidden();
        }
        Apply apply = applyService.selectByapply_ID(apply_ID);
        String tags = apply.getTags();
        List<String> tagList = (tags == null || tags.isEmpty())
                ? java.util.Arrays.asList()
                : java.util.Arrays.asList(tags.split(","));
        return Result.success(tagList);
    }

    /**
     * 提交申请（启动工作流）
     */
    @PostMapping("/submit")
    public Result submit(@RequestBody Apply apply) {
        try {
            // 1. 保存申请
            bindTeamWrite(apply);
            apply.setApply_time(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            apply.setStatus("待审核");
            applyService.add(apply);

            Integer applyId = apply.getApply_ID();

            // 2. 获取社团的指导老师
            Team team = teamService.selectByteam_ID(apply.getTeam_ID());
            Integer teacherId = team != null ? team.getTeacher_ID() : null;

            if (teacherId == null) {
                return Result.error("该社团未分配指导老师");
            }

            // 3. 启动工作流
            String processInstanceId = workflowService.startProcess(
                    applyId,
                    apply.getTeam_ID(),
                    teacherId
            );

            // 4. 只更新 process_instance_id（不更新其他字段）
            Apply updateApply = new Apply();
            updateApply.setApply_ID(applyId);
            updateApply.setProcessInstanceId(processInstanceId);
            applyService.updateFromWorkflow(updateApply);

            return Result.success();
        } catch (Exception e) {
            return Result.error("提交失败: " + e.getMessage());
        }
    }
    /**
     * 为已有草稿启动工作流
     */
    @PostMapping("/startWorkflow")
    public Result startWorkflow(@RequestParam Integer applyId) {
        try {
            Apply apply = applyService.selectByapply_ID(applyId);
            if (!canAccessApply(applyId)) {
                return forbidden();
            }
            if (apply == null) {
                return Result.error("申请不存在");
            }

            // 获取社团的指导老师
            Team team = teamService.selectByteam_ID(apply.getTeam_ID());
            Integer teacherId = team != null ? team.getTeacher_ID() : null;

            if (teacherId == null) {
                return Result.error("该社团未分配指导老师");
            }

            // 启动工作流
            String processInstanceId = workflowService.startProcess(
                    applyId,
                    apply.getTeam_ID(),
                    teacherId
            );

            // 更新状态和流程实例ID
            Apply updateApply = new Apply();
            updateApply.setApply_ID(applyId);
            updateApply.setStatus("待审核");
            updateApply.setProcessInstanceId(processInstanceId);
            applyService.updateFromWorkflow(updateApply);

            return Result.success();
        } catch (Exception e) {
            return Result.error("启动工作流失败: " + e.getMessage());
        }
    }

    @PostMapping("/resubmit")
    @Transactional
    public Result resubmit(@RequestBody Apply apply) {
        try {
            Apply existing = applyService.selectByapply_ID(apply.getApply_ID());
            if (!canAccessApply(apply.getApply_ID())) {
                return forbidden();
            }
            if (existing == null) {
                return Result.error("申请不存在");
            }

            if (isTeamUser()) {
                apply.setTeam_ID(existing.getTeam_ID());
            }
            apply.setStatus(null);
            apply.setProcessInstanceId(null);
            applyService.update(apply);
            workflowService.resubmitApply(apply.getApply_ID(), existing.getTeam_ID());

            return Result.success();
        } catch (Exception e) {
            return Result.error("重新提交失败: " + e.getMessage());
        }
    }

    private void bindTeamQuery(Apply apply) {
        if (isTeamUser()) {
            apply.setTeam_ID(currentUser().getId());
        }
    }

    private void bindTeamWrite(Apply apply) {
        if (isTeamUser()) {
            apply.setTeam_ID(currentUser().getId());
        }
    }

    private boolean canAccessApply(Integer applyId) {
        if (!isTeamUser()) {
            return true;
        }
        if (applyId == null) {
            return false;
        }
        Apply apply = applyService.selectByapply_ID(applyId);
        return apply != null && currentUser().getId().equals(apply.getTeam_ID());
    }

    private boolean isTeamUser() {
        AuthUser user = currentUser();
        return user != null && "team".equals(user.getRole());
    }

    private AuthUser currentUser() {
        return AuthContext.get();
    }

    private Result forbidden() {
        return Result.error("403", "无权操作该申请");
    }

}
