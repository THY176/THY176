package com.example.controller;

import com.example.common.Result;
import com.example.entity.Approve;
import com.example.service.ApproveService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/approve")
public class ApproveController {

    @Resource
    private ApproveService approveService;

    @GetMapping("/selectAll")
    public Result selectAll(Approve approve) {
        List<Approve> list = approveService.selectAll(approve);
        return Result.success(list);
    }

    @GetMapping("/selectByapprove_ID/{approve_ID}")
    public Result selectByapprove_ID(@PathVariable Integer approve_ID) {
        Approve approve = approveService.selectByapprove_ID(approve_ID);
        return Result.success(approve);
    }

    @GetMapping("/selectPage")
    public Result selectPage(Approve approve,
                             @RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "10") Integer pageSize) {
        PageInfo<Approve> pageInfo = approveService.selectPage(approve, pageNum, pageSize);
        return Result.success(pageInfo);
    }

    @PostMapping("/add")
    public Result add(@RequestBody Approve approve) {
        approveService.add(approve);
        return Result.success();
    }

    @PutMapping("/update")
    public Result update(@RequestBody Approve approve) {
        approveService.update(approve);
        return Result.success();
    }

    @DeleteMapping("/delByapprove_ID/{approve_ID}")
    public Result delByapprove_ID(@PathVariable Integer approve_ID) {
        approveService.delByapprove_ID(approve_ID);
        return Result.success();
    }

    @DeleteMapping("/delBatch")
    public Result delBatch(@RequestBody List<Integer> approve_IDs) {
        approveService.delBatch(approve_IDs);
        return Result.success();
    }

    @GetMapping("/selectByApply_ID/{apply_ID}")
    public Result selectByApply_ID(@PathVariable Integer apply_ID) {
        List<Approve> list = approveService.selectByApply_ID(apply_ID);
        return Result.success(list);
    }

    @GetMapping("/selectByStatus/{status}")
    public Result selectByStatus(@PathVariable String status) {
        List<Approve> list = approveService.selectByStatus(status);
        return Result.success(list);
    }

    @GetMapping("/selectByTeacher_ID/{teacher_ID}")
    public Result selectByTeacher_ID(@PathVariable Integer teacher_ID) {
        List<Approve> list = approveService.selectByTeacher_ID(teacher_ID);
        return Result.success(list);
    }

    @GetMapping("/selectByRole/{role}")
    public Result selectByRole(@PathVariable String role) {
        List<Approve> list = approveService.selectByRole(role);
        return Result.success(list);
    }

    @GetMapping("/selectByApply_IDAndSequence")
    public Result selectByApply_IDAndSequence(@RequestParam Integer apply_ID, @RequestParam Integer sequence) {
        List<Approve> list = approveService.selectByApply_IDAndSequence(apply_ID, sequence);
        return Result.success(list);
    }
}