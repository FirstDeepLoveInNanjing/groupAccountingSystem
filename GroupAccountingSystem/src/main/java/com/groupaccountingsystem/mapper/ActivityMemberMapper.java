package com.groupaccountingsystem.mapper;

import com.groupaccountingsystem.pojo.ActivityMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ActivityMemberMapper {
    int insert(ActivityMember activityMember);

    int delete(@Param("activityID") Integer activityID, @Param("memberID") Integer memberID);

    ActivityMember selectOne(@Param("activityID") Integer activityID, @Param("memberID") Integer memberID);

    List<ActivityMember> selectByActivityID(@Param("activityID") Integer activityID);

    List<ActivityMember> selectByMemberID(@Param("memberID") Integer memberID);

    int updateConfirmAttendStatus(@Param("activityID") Integer activityID,
                                  @Param("memberID") Integer memberID,
                                  @Param("confirmAttendStatus") String confirmAttendStatus);
}
