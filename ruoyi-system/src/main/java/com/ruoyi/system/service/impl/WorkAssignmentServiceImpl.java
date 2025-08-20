package com.ruoyi.system.service.impl;

import java.util.List;
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

    /**
     * 查询项目任务分配关系
     * 
     * @param assignmentId 项目任务分配关系主键
     * @return 项目任务分配关系
     */
    @Override
    public WorkAssignment selectWorkAssignmentByAssignmentId(Long assignmentId)
    {
        return workAssignmentMapper.selectWorkAssignmentByAssignmentId(assignmentId);
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
        return workAssignmentMapper.selectWorkAssignmentList(workAssignment);
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
