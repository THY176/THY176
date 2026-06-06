package com.example.controller;

import com.example.common.Result;
import com.example.entity.Reimburse;
import com.example.service.ReimburseService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reimburse")
public class ReimburseController {

    @Resource
    private ReimburseService reimburseService;

    @GetMapping("/selectAll")
    public Result selectAll(Reimburse reimburse) {
        List<Reimburse> list = reimburseService.selectAll(reimburse);
        return Result.success(list);
    }

    @GetMapping("/selectByreimburse_ID/{reimburse_ID}")
    public Result selectByreimburse_ID(@PathVariable Integer reimburse_ID) {
        Reimburse reimburse = reimburseService.selectByreimburse_ID(reimburse_ID);
        return Result.success(reimburse);
    }

    @GetMapping("/selectPage")
    public Result selectPage(Reimburse reimburse,
                             @RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "10") Integer pageSize) {
        PageInfo<Reimburse> pageInfo = reimburseService.selectPage(reimburse, pageNum, pageSize);
        return Result.success(pageInfo);
    }

    @PostMapping("/add")
    public Result add(@RequestBody Reimburse reimburse) {
        reimburseService.add(reimburse);
        return Result.success();
    }

    @PutMapping("/update")
    public Result update(@RequestBody Reimburse reimburse) {
        reimburseService.update(reimburse);
        return Result.success();
    }

    @DeleteMapping("/delByreimburse_ID/{reimburse_ID}")
    public Result delByreimburse_ID(@PathVariable Integer reimburse_ID) {
        reimburseService.delByreimburse_ID(reimburse_ID);
        return Result.success();
    }

    @DeleteMapping("/delBatch")
    public Result delBatch(@RequestBody List<Integer> reimburse_IDs) {
        reimburseService.delBatch(reimburse_IDs);
        return Result.success();
    }

    @GetMapping("/selectByApply_ID/{apply_ID}")
    public Result selectByApply_ID(@PathVariable Integer apply_ID) {
        List<Reimburse> list = reimburseService.selectByApply_ID(apply_ID);
        return Result.success(list);
    }

    @GetMapping("/selectByStatus/{status}")
    public Result selectByStatus(@PathVariable String status) {
        List<Reimburse> list = reimburseService.selectByStatus(status);
        return Result.success(list);
    }

    @GetMapping("/selectByTeacher_ID/{teacher_ID}")
    public Result selectByTeacher_ID(@PathVariable Integer teacher_ID) {
        List<Reimburse> list = reimburseService.selectByTeacher_ID(teacher_ID);
        return Result.success(list);
    }
}
