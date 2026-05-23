package com.groupaccountingsystem.mapper;

import com.groupaccountingsystem.pojo.Collect;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CollectMapper {
    int insert(Collect collect);

    int update(Collect collect);

    Collect selectByActivityID(@Param("activityID") Integer activityID);

    int updateCollectStatus(@Param("activityID") Integer activityID, @Param("collectStatus") String collectStatus);
}
