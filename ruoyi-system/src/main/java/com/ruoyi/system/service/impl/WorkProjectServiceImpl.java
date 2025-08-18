package com.ruoyi.system.service.impl;

import java.util.List;
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

    /**
     * 查询项目信息
     * 
     * @param projectId 项目信息主键
     * @return 项目信息
     */
    @Override
    public WorkProject selectWorkProjectByProjectId(Long projectId)
    {
        return workProjectMapper.selectWorkProjectByProjectId(projectId);
    }

    /**
     * 查询项目信息列表
     * 
     * @param workProject 项目信息
     * @return 项目信息
     */
    @Override
    public List<WorkProject> selectWorkProjectList(WorkProject workProject)
    {
        return workProjectMapper.selectWorkProjectList(workProject);
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
