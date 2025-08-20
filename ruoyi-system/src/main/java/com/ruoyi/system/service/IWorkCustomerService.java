package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.WorkCustomer;

/**
 * 客户信息Service接口
 * 
 * @author zha
 * @date 2025-08-18
 */
public interface IWorkCustomerService 
{
    /**
     * 查询客户信息
     * 
     * @param customerId 客户信息主键
     * @return 客户信息
     */
    public WorkCustomer selectWorkCustomerByCustomerId(Long customerId);

    public List<WorkCustomer> selectWorkCustomers(Long[] customerIds);

    /**
     * 查询客户信息列表
     * 
     * @param workCustomer 客户信息
     * @return 客户信息集合
     */
    public List<WorkCustomer> selectWorkCustomerList(WorkCustomer workCustomer);

    /**
     * 新增客户信息
     * 
     * @param workCustomer 客户信息
     * @return 结果
     */
    public int insertWorkCustomer(WorkCustomer workCustomer);

    /**
     * 修改客户信息
     * 
     * @param workCustomer 客户信息
     * @return 结果
     */
    public int updateWorkCustomer(WorkCustomer workCustomer);

    /**
     * 批量删除客户信息
     * 
     * @param customerIds 需要删除的客户信息主键集合
     * @return 结果
     */
    public int deleteWorkCustomerByCustomerIds(Long[] customerIds);

    /**
     * 删除客户信息信息
     * 
     * @param customerId 客户信息主键
     * @return 结果
     */
    public int deleteWorkCustomerByCustomerId(Long customerId);
}
