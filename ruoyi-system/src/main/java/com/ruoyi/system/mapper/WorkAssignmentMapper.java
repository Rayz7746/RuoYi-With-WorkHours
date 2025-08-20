package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.WorkAssignment;

/**
 * 项目任务分配关系Mapper接口
 * 
 * @author zha
 * @date 2025-08-20
 */
public interface WorkAssignmentMapper 
{
    /**
     * 查询项目任务分配关系
     * 
     * @param assignmentId 项目任务分配关系主键
     * @return 项目任务分配关系
     */
    public WorkAssignment selectWorkAssignmentByAssignmentId(Long assignmentId);

    /**
     * 查询项目任务分配关系列表
     * 
     * @param workAssignment 项目任务分配关系
     * @return 项目任务分配关系集合
     */
    public List<WorkAssignment> selectWorkAssignmentList(WorkAssignment workAssignment);

    /**
     * 新增项目任务分配关系
     * 
     * @param workAssignment 项目任务分配关系
     * @return 结果
     */
    public int insertWorkAssignment(WorkAssignment workAssignment);

    /**
     * 修改项目任务分配关系
     * 
     * @param workAssignment 项目任务分配关系
     * @return 结果
     */
    public int updateWorkAssignment(WorkAssignment workAssignment);

    /**
     * 删除项目任务分配关系
     * 
     * @param assignmentId 项目任务分配关系主键
     * @return 结果
     */
    public int deleteWorkAssignmentByAssignmentId(Long assignmentId);

    /**
     * 批量删除项目任务分配关系
     * 
     * @param assignmentIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteWorkAssignmentByAssignmentIds(Long[] assignmentIds);
}
