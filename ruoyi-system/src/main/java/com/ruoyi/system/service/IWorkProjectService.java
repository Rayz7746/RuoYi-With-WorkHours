package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.WorkProject;

/**
 * 项目信息Service接口
 * 
 * @author zha
 * @date 2025-08-18
 */
public interface IWorkProjectService 
{
    /**
     * 查询项目信息
     * 
     * @param projectId 项目信息主键
     * @return 项目信息
     */
    public WorkProject selectWorkProjectByProjectId(Long projectId);

    /**
     * 查询项目信息列表
     * 
     * @param workProject 项目信息
     * @return 项目信息集合
     */
    public List<WorkProject> selectWorkProjectList(WorkProject workProject);

    /**
     * 新增项目信息
     * 
     * @param workProject 项目信息
     * @return 结果
     */
    public int insertWorkProject(WorkProject workProject);

    /**
     * 修改项目信息
     * 
     * @param workProject 项目信息
     * @return 结果
     */
    public int updateWorkProject(WorkProject workProject);

    /**
     * 批量删除项目信息
     * 
     * @param projectIds 需要删除的项目信息主键集合
     * @return 结果
     */
    public int deleteWorkProjectByProjectIds(Long[] projectIds);

    /**
     * 删除项目信息信息
     * 
     * @param projectId 项目信息主键
     * @return 结果
     */
    public int deleteWorkProjectByProjectId(Long projectId);
}
