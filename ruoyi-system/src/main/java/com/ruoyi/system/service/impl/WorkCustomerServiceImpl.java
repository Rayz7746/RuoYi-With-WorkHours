package com.ruoyi.system.service.impl;

import java.util.*;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.WorkCustomerMapper;
import com.ruoyi.system.domain.WorkCustomer;
import com.ruoyi.system.service.IWorkCustomerService;

/**
 * 客户信息Service业务层处理
 * 
 * @author zha
 * @date 2025-08-18
 */
@Service
public class WorkCustomerServiceImpl implements IWorkCustomerService 
{
    @Autowired
    private WorkCustomerMapper workCustomerMapper;

    /**
     * 查询客户信息
     * 
     * @param customerId 客户信息主键
     * @return 客户信息
     */
    @Override
    public WorkCustomer selectWorkCustomerByCustomerId(Long customerId)
    {
        return workCustomerMapper.selectWorkCustomerByCustomerId(customerId);
    }

    @Override
    public List<WorkCustomer> selectWorkCustomers(Long[] customerIds) {
        if (customerIds == null) {
            return Collections.emptyList();
        }

        return Arrays.stream(customerIds)
                .map(workCustomerMapper::selectWorkCustomerByCustomerId)
                .collect(Collectors.toList()); // 不移除 null
    }



    /**
     * 查询客户信息列表
     * 
     * @param workCustomer 客户信息
     * @return 客户信息
     */
    @Override
    public List<WorkCustomer> selectWorkCustomerList(WorkCustomer workCustomer)
    {
        return workCustomerMapper.selectWorkCustomerList(workCustomer);
    }

    /**
     * 新增客户信息
     * 
     * @param workCustomer 客户信息
     * @return 结果
     */
    @Override
    public int insertWorkCustomer(WorkCustomer workCustomer)
    {
        return workCustomerMapper.insertWorkCustomer(workCustomer);
    }

    /**
     * 修改客户信息
     * 
     * @param workCustomer 客户信息
     * @return 结果
     */
    @Override
    public int updateWorkCustomer(WorkCustomer workCustomer)
    {
        return workCustomerMapper.updateWorkCustomer(workCustomer);
    }

    /**
     * 批量删除客户信息
     * 
     * @param customerIds 需要删除的客户信息主键
     * @return 结果
     */
    @Override
    public int deleteWorkCustomerByCustomerIds(Long[] customerIds)
    {
        return workCustomerMapper.deleteWorkCustomerByCustomerIds(customerIds);
    }

    /**
     * 删除客户信息信息
     * 
     * @param customerId 客户信息主键
     * @return 结果
     */
    @Override
    public int deleteWorkCustomerByCustomerId(Long customerId)
    {
        return workCustomerMapper.deleteWorkCustomerByCustomerId(customerId);
    }
}
