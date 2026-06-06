package com.example.service;

import com.example.entity.Team;
import com.example.mapper.TeamMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TeamService {

    @Resource
    private TeamMapper teamMapper;

    @Resource
    private PasswordService passwordService;

    public List<Team> selectAll(Team team) {
        return teamMapper.selectAll(team);
    }

    public Team selectByteam_ID(Integer team_ID) {
        return teamMapper.selectByteam_ID(team_ID);
    }

    public PageInfo<Team> selectPage(Team team, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Team> list = teamMapper.selectAll(team);
        return PageInfo.of(list);
    }

    public void add(Team team) {
        team.setPassword(passwordService.encodeIfNeeded(team.getPassword()));
        teamMapper.insert(team);
    }

    public void update(Team team) {
        team.setPassword(passwordService.encodeIfNeeded(team.getPassword()));
        teamMapper.updateByteam_ID(team);
    }

    public void delByteam_ID(Integer team_ID) {
        teamMapper.delByteam_ID(team_ID);
    }

    public void delBatch(List<Integer> team_IDs) {
        for (Integer team_ID : team_IDs) {
            teamMapper.delByteam_ID(team_ID);
        }
    }

    public List<Team> selectByteacher_ID(Integer teacher_ID) {
        return teamMapper.selectByteacher_ID(teacher_ID);
    }

    public List<Team> selectByteam_name(String team_name) {
        return teamMapper.selectByteam_name(team_name);
    }
}
