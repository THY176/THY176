package com.example.service;

import com.example.entity.Apply;
import com.example.entity.Approve;
import com.example.entity.Team;
import com.example.mapper.AdminMapper;
import com.example.mapper.ApplyMapper;
import com.example.mapper.ApproveMapper;
import com.example.mapper.TeamMapper;
import org.flowable.engine.*;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class WorkflowService {

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private TeamMapper teamMapper;

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private ApplyMapper applyMapper;

    @Autowired
    private ApproveMapper approveMapper;

    @Autowired
    private AdminMapper adminMapper;  // 添加这行

    /**
     * 随机获取一个管理员ID（排除指定的管理员）
     */
    private Integer getRandomAdmin(Integer excludeId) {
        List<Integer> adminIds = adminMapper.getAllAdminIds();
        if (adminIds == null || adminIds.isEmpty()) {
            throw new RuntimeException("没有可用的管理员");
        }

        // 排除指定的管理员
        List<Integer> availableAdmins = new ArrayList<>();
        for (Integer id : adminIds) {
            if (excludeId == null || !id.equals(excludeId)) {
                availableAdmins.add(id);
            }
        }

        if (availableAdmins.isEmpty()) {
            throw new RuntimeException("没有其他可用的管理员");
        }

        Random random = new Random();
        return availableAdmins.get(random.nextInt(availableAdmins.size()));
    }

    /**
     * 启动审批流程（社团提交申请）
     */
    @Transactional
    public String startProcess(Integer applyId, Integer teamId, Integer teacherId) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("teamId", String.valueOf(teamId));
        variables.put("teacherId", String.valueOf(teacherId));
        variables.put("applyId", applyId);
        variables.put("auditResult", "待审核");
        variables.put("reimburseResult", "待报销");
        // 初始设置二级审核人为空
        variables.put("secondAuditAssignee", "");
        variables.put("thirdAuditAssignee", "");

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
                "expenseApproval",
                "apply_" + applyId,
                variables
        );

        // 自动完成"提交申请"任务
        Task submitTask = taskService.createTaskQuery()
                .processInstanceId(processInstance.getId())
                .taskDefinitionKey("submitApply")
                .singleResult();

        if (submitTask != null) {
            taskService.complete(submitTask.getId());
        }

        return processInstance.getId();
    }

    /**
     * 一级审核（指导老师）
     */
    @Transactional
    public void firstAudit(String taskId, Integer applyId, Integer teacherId,
                           String teacherName, String opinion, boolean approved) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new RuntimeException("任务不存在");
        }

        // 1. 保存审核记录
        Approve approve = new Approve();
        approve.setApply_ID(applyId);
        approve.setTeacher_ID(teacherId);
        approve.setTeacher_name(teacherName);
        approve.setRole("指导老师");
        approve.setSequence(1);
        approve.setOpinion(opinion);
        approve.setStatus(approved ? "审核通过" : "审核驳回");
        approve.setApprove_time(getCurrentTime());
        approveMapper.insert(approve);

        // 2. 更新申请状态
        Apply apply = applyMapper.selectByapply_ID(applyId);
        if (apply != null) {
            apply.setStatus(approved ? "待二次审核" : "审核驳回");
            applyMapper.updateByapply_ID(apply);
        }

        // 3. 完成工作流任务
        Map<String, Object> variables = new HashMap<>();
        variables.put("auditResult", approved ? "通过" : "驳回");
        variables.put("firstOpinion", opinion);

        if (approved) {
            // 随机选择一个管理员作为二级审核人
            Integer nextAssignee = getRandomAdmin(null);
            variables.put("secondAuditAssignee", String.valueOf(nextAssignee));
        }
        taskService.complete(taskId, variables);
    }

    /**
     * 二级审核（管理员）
     */
    @Transactional
    public void secondAudit(String taskId, Integer applyId, Integer adminId,
                            String adminName, String opinion, boolean approved) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new RuntimeException("任务不存在");
        }

        // 1. 保存审核记录
        Approve approve = new Approve();
        approve.setApply_ID(applyId);
        approve.setTeacher_ID(adminId);
        approve.setTeacher_name(adminName);
        approve.setRole("管理员");
        approve.setSequence(2);
        approve.setOpinion(opinion);
        approve.setStatus(approved ? "审核通过" : "审核驳回");
        approve.setApprove_time(getCurrentTime());
        approveMapper.insert(approve);

        // 2. 更新申请状态
        Apply apply = applyMapper.selectByapply_ID(applyId);
        if (apply != null) {
            apply.setStatus(approved ? "待三次审核" : "审核驳回");
            applyMapper.updateByapply_ID(apply);
        }

        // 3. 完成工作流任务
        Map<String, Object> variables = new HashMap<>();
        variables.put("auditResult", approved ? "通过" : "驳回");
        variables.put("secondOpinion", opinion);

        if (approved) {
            // 随机选择一个管理员作为三级审核人（排除当前管理员）
            Integer nextAssignee = getRandomAdmin(adminId);
            variables.put("thirdAuditAssignee", String.valueOf(nextAssignee));
        }
        taskService.complete(taskId, variables);
    }

    /**
     * 三级审核（管理员终审）
     */
    @Transactional
    public void thirdAudit(String taskId, Integer applyId, Integer adminId,
                           String adminName, String opinion, boolean approved) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new RuntimeException("任务不存在");
        }

        // 1. 保存审核记录
        Approve approve = new Approve();
        approve.setApply_ID(applyId);
        approve.setTeacher_ID(adminId);
        approve.setTeacher_name(adminName);
        approve.setRole("管理员");
        approve.setSequence(3);
        approve.setOpinion(opinion);
        approve.setStatus(approved ? "审核通过" : "审核驳回");
        approve.setApprove_time(getCurrentTime());
        approveMapper.insert(approve);

        // 2. 更新申请状态
        Apply apply = applyMapper.selectByapply_ID(applyId);
        if (apply != null) {
            apply.setStatus(approved ? "审核通过" : "审核驳回");
            applyMapper.updateByapply_ID(apply);
        }

        // 3. 完成工作流任务
        Map<String, Object> variables = new HashMap<>();
        variables.put("auditResult", approved ? "通过" : "驳回");
        variables.put("thirdOpinion", opinion);
        taskService.complete(taskId, variables);
    }

    /**
     * 获取指导老师的待审核任务列表
     */
    /**
     * 获取指导老师的待审核任务列表
     */
    public List<Map<String, Object>> getTeacherPendingTasks(Integer teacherId) {
        List<Task> tasks = taskService.createTaskQuery()
                .taskAssignee(String.valueOf(teacherId))
                .taskDefinitionKey("firstAudit")
                .orderByTaskCreateTime().desc()
                .list();

        List<Map<String, Object>> result = new ArrayList<>();
        for (Task task : tasks) {
            Map<String, Object> taskInfo = new HashMap<>();
            taskInfo.put("taskId", task.getId());
            taskInfo.put("taskName", task.getName());
            taskInfo.put("taskDefinitionKey", task.getTaskDefinitionKey());
            taskInfo.put("createTime", task.getCreateTime());

            // 获取流程变量中的申请ID
            Integer applyId = (Integer) runtimeService.getVariable(task.getProcessInstanceId(), "applyId");
            taskInfo.put("applyId", applyId);

            // 获取完整的申请信息
            if (applyId != null) {
                Apply apply = applyMapper.selectByapply_ID(applyId);
                taskInfo.put("apply", apply);

                // 获取社团名称
                if (apply != null && apply.getTeam_ID() != null) {
                    Team team = teamMapper.selectByteam_ID(apply.getTeam_ID());
                    taskInfo.put("teamName", team != null ? team.getTeam_name() : "未知社团");
                    taskInfo.put("team_name", team != null ? team.getTeam_name() : "未知社团");
                }
            }

            result.add(taskInfo);
        }
        return result;
    }

    /**
     * 获取管理员的待审核任务列表（按 assignee 查询）
     */
    /**
     * 获取管理员的待审核任务列表（按 assignee 查询）
     */
    public List<Map<String, Object>> getAdminPendingTasks(Integer adminId) {
        List<Task> tasks = taskService.createTaskQuery()
                .taskAssignee(String.valueOf(adminId))
                .orderByTaskCreateTime().desc()
                .list();

        List<Map<String, Object>> result = new ArrayList<>();
        for (Task task : tasks) {
            Map<String, Object> taskInfo = new HashMap<>();
            taskInfo.put("taskId", task.getId());
            taskInfo.put("taskName", task.getName());
            taskInfo.put("taskDefinitionKey", task.getTaskDefinitionKey());
            taskInfo.put("createTime", task.getCreateTime());

            Integer applyId = (Integer) runtimeService.getVariable(task.getProcessInstanceId(), "applyId");
            taskInfo.put("applyId", applyId);

            if (applyId != null) {
                Apply apply = applyMapper.selectByapply_ID(applyId);
                taskInfo.put("apply", apply);
                if (apply != null && apply.getTeam_ID() != null) {
                    Team team = teamMapper.selectByteam_ID(apply.getTeam_ID());
                    taskInfo.put("teamName", team != null ? team.getTeam_name() : "未知社团");
                    taskInfo.put("team_name", team != null ? team.getTeam_name() : "未知社团");
                }
            }
            result.add(taskInfo);
        }
        return result;
    }

    /**
     * 构建任务返回结果
     */
    private List<Map<String, Object>> buildTaskResult(List<Task> tasks) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Task task : tasks) {
            Map<String, Object> taskInfo = new HashMap<>();
            taskInfo.put("taskId", task.getId());
            taskInfo.put("taskName", task.getName());
            taskInfo.put("taskDefinitionKey", task.getTaskDefinitionKey());
            taskInfo.put("createTime", task.getCreateTime());

            // 获取流程变量中的申请ID
            Integer applyId = (Integer) runtimeService.getVariable(task.getProcessInstanceId(), "applyId");
            taskInfo.put("applyId", applyId);

            // 获取完整的申请信息
            if (applyId != null) {
                Apply apply = applyMapper.selectByapply_ID(applyId);
                taskInfo.put("apply", apply);

                // 获取社团名称
                if (apply != null && apply.getTeam_ID() != null) {
                    Team team = teamMapper.selectByteam_ID(apply.getTeam_ID());
                    taskInfo.put("teamName", team != null ? team.getTeam_name() : "未知社团");
                    taskInfo.put("team_name", team != null ? team.getTeam_name() : "未知社团");
                }
            }
            result.add(taskInfo);
        }
        return result;
    }

    /**
     * 获取流程当前状态
     */
    public String getCurrentStatus(String processInstanceId) {
        Task task = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .active()
                .singleResult();

        if (task == null) {
            if (historyService.createHistoricProcessInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .finished()
                    .count() > 0) {
                return "已结束";
            }
            return "未知";
        }

        switch (task.getTaskDefinitionKey()) {
            case "firstAudit":
                return "待审核";
            case "secondAudit":
                return "待二次审核";
            case "thirdAudit":
                return "待三次审核";
            case "reimburse":
                return "待报销";
            default:
                return task.getName();
        }
    }

    /**
     * 执行报销
     */
    @Transactional
    public void executeReimburse(String taskId, Integer applyId, Integer financeId,
                                 String financeName, String reimburseAmount) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new RuntimeException("任务不存在");
        }

        Apply apply = applyMapper.selectByapply_ID(applyId);
        if (apply != null) {
            apply.setStatus("已报销");
            applyMapper.updateByapply_ID(apply);
        }

        Map<String, Object> variables = new HashMap<>();
        variables.put("reimburseResult", "完成");
        variables.put("reimburseAmount", reimburseAmount);
        taskService.complete(taskId, variables);
    }

    private String getCurrentTime() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }

}