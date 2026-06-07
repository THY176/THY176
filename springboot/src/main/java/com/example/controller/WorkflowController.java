package com.example.controller;

import com.example.common.Result;
import com.example.entity.Apply;
import com.example.entity.Team;
import com.example.service.ApplyService;
import com.example.service.TeamService;
import com.example.service.WorkflowService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/workflow")
public class WorkflowController {

    @Resource
    private WorkflowService workflowService;

    @Resource
    private ApplyService applyService;

    @Resource
    private TeamService teamService;

    /**
     * 获取指导老师的待审核任务
     */
    @GetMapping("/teacher/tasks/{teacherId}")
    public Result getTeacherTasks(@PathVariable Integer teacherId) {
        List<Map<String, Object>> tasks = workflowService.getTeacherPendingTasks(teacherId);
        return Result.success(tasks);
    }

    /**
     * 获取管理员的待审核任务（按 assignee 查询）
     */
    @GetMapping("/admin/tasks/{adminId}")
    public Result getAdminTasks(@PathVariable Integer adminId) {
        List<Map<String, Object>> tasks = workflowService.getAdminPendingTasks(adminId);
        return Result.success(tasks);
    }

    /**
     * 获取待报销工作流任务
     */
    @GetMapping("/reimburse/tasks")
    public Result getReimburseTasks() {
        List<Map<String, Object>> tasks = workflowService.getReimbursePendingTasks();
        return Result.success(tasks);
    }

    /**
     * 一级审核（指导老师）
     */
    @PostMapping("/firstAudit")
    public Result firstAudit(@RequestParam String taskId,
                             @RequestParam Integer applyId,
                             @RequestParam Integer teacherId,
                             @RequestParam String teacherName,
                             @RequestParam String opinion,
                             @RequestParam boolean approved) {
        try {
            workflowService.firstAudit(taskId, applyId, teacherId, teacherName, opinion, approved);
            return Result.success(approved ? "审核通过" : "审核驳回");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("审核失败: " + e.getMessage());
        }
    }

    /**
     * 二级审核（管理员）
     */
    @PostMapping("/secondAudit")
    public Result secondAudit(@RequestParam String taskId,
                              @RequestParam Integer applyId,
                              @RequestParam Integer adminId,
                              @RequestParam String adminName,
                              @RequestParam String opinion,
                              @RequestParam boolean approved) {
        try {
            workflowService.secondAudit(taskId, applyId, adminId, adminName, opinion, approved);
            return Result.success(approved ? "审核通过" : "审核驳回");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("审核失败: " + e.getMessage());
        }
    }

    /**
     * 三级审核（管理员终审）
     */
    @PostMapping("/thirdAudit")
    public Result thirdAudit(@RequestParam String taskId,
                             @RequestParam Integer applyId,
                             @RequestParam Integer adminId,
                             @RequestParam String adminName,
                             @RequestParam String opinion,
                             @RequestParam boolean approved) {
        try {
            workflowService.thirdAudit(taskId, applyId, adminId, adminName, opinion, approved);
            return Result.success(approved ? "审核通过" : "审核驳回");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("审核失败: " + e.getMessage());
        }
    }

    /**
     * 执行报销
     */
    @PostMapping("/reimburse")
    public Result reimburse(@RequestParam String taskId,
                            @RequestParam Integer applyId,
                            @RequestParam Integer financeId,
                            @RequestParam String financeName,
                            @RequestParam String reimburseAmount) {
        try {
            workflowService.executeReimburse(taskId, applyId, financeId, financeName, reimburseAmount);
            return Result.success("报销成功");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("报销失败: " + e.getMessage());
        }
    }

    /**
     * 获取流程当前状态
     */
    @GetMapping("/status/{processInstanceId}")
    public Result getStatus(@PathVariable String processInstanceId) {
        String status = workflowService.getCurrentStatus(processInstanceId);
        return Result.success(status);
    }

    /**
     * 获取申请当前待处理任务及已分配审批人
     */
    @GetMapping("/currentTask/{applyId}")
    public Result getCurrentTask(@PathVariable Integer applyId) {
        Map<String, Object> taskInfo = workflowService.getCurrentTaskInfo(applyId);
        return Result.success(taskInfo);
    }
}
