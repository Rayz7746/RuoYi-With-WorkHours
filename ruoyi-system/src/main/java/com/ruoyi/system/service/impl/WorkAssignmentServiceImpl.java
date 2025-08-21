package com.ruoyi.system.service.impl;

import java.util.List;

import com.ruoyi.system.domain.WorkProject;
import com.ruoyi.system.service.IWorkProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.WorkAssignmentMapper;
import com.ruoyi.system.domain.WorkAssignment;
import com.ruoyi.system.service.IWorkAssignmentService;

/**
 * 项目任务分配关系Service业务层处理
 * 
 * @author zha
 * @date 2025-08-20
 */
@Service
public class WorkAssignmentServiceImpl implements IWorkAssignmentService 
{
    @Autowired
    private WorkAssignmentMapper workAssignmentMapper;

    @Autowired
    private IWorkProjectService workProjectService;

    // 注入 WorkProjectService 用于获取项目详情
    private void append(WorkAssignment workAssignment) {
        if (workAssignment == null) {
            return;
        }
        if (workAssignment.getProjectId() != null) {
            WorkProject workProject = workProjectService.selectWorkProjectByProjectId(workAssignment.getProjectId());
            workAssignment.setProject(workProject);
        }
    }

    // 校验新增与更新的工具，同数据库规则：  UNIQUE KEY `unique_assignment` (`project_id`,`user_id`),
    private void validateWorkAssignment(WorkAssignment workAssignment) {


    }

    /**
     * 查询项目任务分配关系
     * 
     * @param assignmentId 项目任务分配关系主键
     * @return 项目任务分配关系
     */
    @Override
    public WorkAssignment selectWorkAssignmentByAssignmentId(Long assignmentId)
    {
        WorkAssignment workAssignment = workAssignmentMapper.selectWorkAssignmentByAssignmentId(assignmentId);
        append(workAssignment);
        return workAssignment;
    }

    /**
     * 查询项目任务分配关系列表
     * 
     * @param workAssignment 项目任务分配关系
     * @return 项目任务分配关系
     */
    @Override
    public List<WorkAssignment> selectWorkAssignmentList(WorkAssignment workAssignment)
    {
        List<WorkAssignment> workAssignments = workAssignmentMapper.selectWorkAssignmentList(workAssignment);
        for (WorkAssignment assignment : workAssignments) {
            append(assignment);
        }
        return workAssignments;
    }

    /**
     * 新增项目任务分配关系
     * 
     * @param workAssignment 项目任务分配关系
     * @return 结果
     */
    @Override
    public int insertWorkAssignment(WorkAssignment workAssignment)
    {
        return workAssignmentMapper.insertWorkAssignment(workAssignment);
    }

    /**
     * 修改项目任务分配关系
     * 
     * @param workAssignment 项目任务分配关系
     * @return 结果
     */
    @Override
    public int updateWorkAssignment(WorkAssignment workAssignment)
    {
        return workAssignmentMapper.updateWorkAssignment(workAssignment);
    }

    /**
     * 批量删除项目任务分配关系
     * 
     * @param assignmentIds 需要删除的项目任务分配关系主键
     * @return 结果
     */
    @Override
    public int deleteWorkAssignmentByAssignmentIds(Long[] assignmentIds)
    {
        return workAssignmentMapper.deleteWorkAssignmentByAssignmentIds(assignmentIds);
    }

    /**
     * 删除项目任务分配关系信息
     * 
     * @param assignmentId 项目任务分配关系主键
     * @return 结果
     */
    @Override
    public int deleteWorkAssignmentByAssignmentId(Long assignmentId)
    {
        return workAssignmentMapper.deleteWorkAssignmentByAssignmentId(assignmentId);
    }
}
