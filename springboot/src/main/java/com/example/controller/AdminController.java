package com.example.controller;

import com.example.common.Result;
import com.example.entity.Admin;
import com.example.service.AdminService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
public class AdminController {

    @Resource
    private AdminService adminService;

    @GetMapping("/selectAll")
    public Result selectAll(Admin admin) {
        List<Admin> list = adminService.selectAll(admin);
        list.forEach(this::clearPassword);
        return Result.success(list);
    }

    @GetMapping("/selectByteacher_ID/{teacher_ID}")
    public Result selectByteacher_ID(@PathVariable Integer teacher_ID) {
        Admin admin = adminService.selectByteacher_ID(teacher_ID);
        clearPassword(admin);
        return Result.success(admin);
    }

    @GetMapping("/selectPage")
    public Result selectPage(Admin admin,
                             @RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "10") Integer pageSize) {
        PageInfo<Admin> pageInfo = adminService.selectPage(admin, pageNum, pageSize);
        pageInfo.getList().forEach(this::clearPassword);
        return Result.success(pageInfo);
    }

    @PostMapping("/add")
    public Result add(@RequestBody Admin admin) {
        adminService.add(admin);
        return Result.success();
    }

    @PutMapping("/update")
    public Result update(@RequestBody Admin admin) {
        adminService.update(admin);
        return Result.success();
    }

    @DeleteMapping("/delByteacher_ID/{teacher_ID}")
    public Result delByteacher_ID(@PathVariable Integer teacher_ID) {
        adminService.delByteacher_ID(teacher_ID);
        return Result.success();
    }

    @DeleteMapping("/delBatch")
    public Result delBatch(@RequestBody List<Integer> teacher_IDs) {
        adminService.delBatch(teacher_IDs);
        return Result.success();
    }

    private void clearPassword(Admin admin) {
        if (admin != null) {
            admin.setPassword(null);
        }
    }
}
