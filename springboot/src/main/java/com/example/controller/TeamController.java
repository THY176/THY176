package com.example.controller;

import com.example.common.Result;
import com.example.entity.Team;
import com.example.service.TeamService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/team")
public class TeamController {

    @Resource
    private TeamService teamService;

    @GetMapping("/selectAll")
    public Result selectAll(Team team) {
        List<Team> list = teamService.selectAll(team);
        return Result.success(list);
    }

    @GetMapping("/selectByteam_ID/{team_ID}")
    public Result selectByteam_ID(@PathVariable Integer team_ID) {
        Team team = teamService.selectByteam_ID(team_ID);
        return Result.success(team);
    }

    @GetMapping("/selectPage")
    public Result selectPage(Team team,
                             @RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "10") Integer pageSize) {
        PageInfo<Team> pageInfo = teamService.selectPage(team, pageNum, pageSize);
        return Result.success(pageInfo);
    }

    @PostMapping("/add")
    public Result add(@RequestBody Team team) {
        teamService.add(team);
        return Result.success();
    }

    @PutMapping("/update")
    public Result update(@RequestBody Team team) {
        teamService.update(team);
        return Result.success();
    }

    @DeleteMapping("/delByteam_ID/{team_ID}")
    public Result delByteam_ID(@PathVariable Integer team_ID) {
        teamService.delByteam_ID(team_ID);
        return Result.success();
    }

    @DeleteMapping("/delBatch")
    public Result delBatch(@RequestBody List<Integer> team_IDs) {
        teamService.delBatch(team_IDs);
        return Result.success();
    }

    @GetMapping("/selectByteacher_ID/{teacher_ID}")
    public Result selectByteacher_ID(@PathVariable Integer teacher_ID) {
        List<Team> list = teamService.selectByteacher_ID(teacher_ID);
        return Result.success(list);
    }

    @GetMapping("/selectByteam_name")
    public Result selectByteam_name(@RequestParam String team_name) {
        List<Team> list = teamService.selectByteam_name(team_name);
        return Result.success(list);
    }
}
