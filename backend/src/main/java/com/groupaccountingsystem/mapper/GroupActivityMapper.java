package com.groupaccountingsystem.mapper;

import com.groupaccountingsystem.pojo.GroupActivity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface GroupActivityMapper {
    int insert(GroupActivity groupActivity);

    int update(GroupActivity groupActivity);

    int updateSettlement(GroupActivity groupActivity);

    int updateConfirmInitiated(@Param("activityID") Integer activityID,
                               @Param("confirmInitiated") Boolean confirmInitiated);

    int delete(@Param("activityID") Integer activityID);

    GroupActivity selectById(@Param("activityID") Integer activityID);

    List<GroupActivity> selectAll();

    List<GroupActivity> search(@Param("keyword") String keyword, @Param("isSettable") Boolean isSettable);

    List<GroupActivity> searchByUser(@Param("keyword") String keyword,
                                     @Param("isSettable") Boolean isSettable,
                                     @Param("userID") Integer userID);

    List<GroupActivity> selectByCreatorID(@Param("creatorID") Integer creatorID);

    List<GroupActivity> selectByMemberID(@Param("memberID") Integer memberID);
}
