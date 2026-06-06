package com.example.mapper;

import com.example.entity.Team;
import org.apache.ibatis.annotations.*;
import java.util.List;

public interface TeamMapper {
    List<Team> selectAll(Team team);

    Team selectByteam_ID(Integer team_ID);

    List<Team> selectByteacher_ID(Integer teacher_ID);

    List<Team> selectByteam_name(String team_name);

    void insert(Team team);

    void updateByteam_ID(Team team);

    void delByteam_ID(Integer team_ID);

    int updateNumber(Team team);
}