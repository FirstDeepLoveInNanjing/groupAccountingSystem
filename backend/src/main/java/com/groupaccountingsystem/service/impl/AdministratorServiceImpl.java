package com.groupaccountingsystem.service.impl;

import com.groupaccountingsystem.mapper.AdministratorMapper;
import com.groupaccountingsystem.pojo.Administrator;
import com.groupaccountingsystem.service.AdministratorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;



@Service
public class AdministratorServiceImpl implements AdministratorService {

    @Autowired
    private AdministratorMapper administratorMapper;

    @Override
    public Administrator administratorLogin(Administrator administrator) {
        return administratorMapper.selectByAccountAndPassword(administrator);
    }
}

