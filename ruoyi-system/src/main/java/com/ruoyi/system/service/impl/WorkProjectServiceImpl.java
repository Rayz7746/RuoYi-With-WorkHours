package com.ruoyi.system.service.impl;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.system.criteria.WorkProjectQueryCriteria;
import com.ruoyi.system.domain.WorkCustomer;
import com.ruoyi.system.service.IWorkCustomerService;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.WorkProjectMapper;
import com.ruoyi.system.domain.WorkProject;
import com.ruoyi.system.service.IWorkProjectService;

/**
 * 项目信息Service业务层处理
 * 
 * @author zha
 * @date 2025-08-18
 */
@Service
public class WorkProjectServiceImpl implements IWorkProjectService 
{
    @Autowired
    private WorkProjectMapper workProjectMapper;

    @Autowired
    private IWorkCustomerService workCustomerService;

    /**
     * 查询项目信息
     * 
     * @param projectId 项目信息主键
     * @return 项目信息
     */
    @Override
    public WorkProject selectWorkProjectByProjectId(Long projectId)
    {
        WorkProject workProject = workProjectMapper.selectWorkProjectByProjectId(projectId);
        if (workProject != null && workProject.getCustomerId() != null) {
            WorkCustomer customer = workCustomerService.selectWorkCustomerByCustomerId(workProject.getCustomerId());
            workProject.setCustomer(customer);
        }
        return workProject;
    }

    /**
     * 查询项目信息列表
     * 
     * @param workProject 项目信息
     * @return 项目信息
     */
    @Override
    public List<WorkProject> selectWorkProjectList(WorkProjectQueryCriteria workProject)
    {
        List<WorkProject> workProjects = workProjectMapper.selectWorkProjectList(workProject);
        Long[] customerIds = workProjects.stream().map(WorkProject::getCustomerId).toArray(Long[]::new);
        List<WorkCustomer> customers = workCustomerService.selectWorkCustomers(customerIds);
        Map<Long, WorkCustomer> customerMap = customers.stream().collect(Collectors.toMap(
                WorkCustomer::getCustomerId,      // 键的映射函数
                customer -> customer,
                (existing, replacement) -> existing
        ));
        for (WorkProject project : workProjects) {
            WorkCustomer customer = customerMap.get(project.getCustomerId());
            project.setCustomer(customer);
        }
        return workProjects;
    }

    /**
     * 新增项目信息
     * 
     * @param workProject 项目信息
     * @return 结果
     */
    @Override
    public int insertWorkProject(WorkProject workProject)
    {
        return workProjectMapper.insertWorkProject(workProject);
    }

    /**
     * 修改项目信息
     * 
     * @param workProject 项目信息
     * @return 结果
     */
    @Override
    public int updateWorkProject(WorkProject workProject)
    {
        return workProjectMapper.updateWorkProject(workProject);
    }

    /**
     * 批量删除项目信息
     * 
     * @param projectIds 需要删除的项目信息主键
     * @return 结果
     */
    @Override
    public int deleteWorkProjectByProjectIds(Long[] projectIds)
    {
        return workProjectMapper.deleteWorkProjectByProjectIds(projectIds);
    }

    /**
     * 删除项目信息信息
     * 
     * @param projectId 项目信息主键
     * @return 结果
     */
    @Override
    public int deleteWorkProjectByProjectId(Long projectId)
    {
        return workProjectMapper.deleteWorkProjectByProjectId(projectId);
    }
}
