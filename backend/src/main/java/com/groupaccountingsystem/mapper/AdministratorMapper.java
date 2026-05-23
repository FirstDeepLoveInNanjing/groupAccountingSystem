package com.groupaccountingsystem.mapper;

import com.groupaccountingsystem.pojo.Administrator;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AdministratorMapper {
    Administrator selectByAccountAndPassword(Administrator administrator);
}
