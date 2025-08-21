package com.ruoyi.system.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.WorkAttendanceMapper;
import com.ruoyi.system.domain.WorkAttendance;
import com.ruoyi.system.service.IWorkAttendanceService;

/**
 * 考勤记录Service业务层处理
 * 
 * @author zha
 * @date 2025-08-21
 */
@Service
public class WorkAttendanceServiceImpl implements IWorkAttendanceService 
{
    @Autowired
    private WorkAttendanceMapper workAttendanceMapper;

    /**
     * 查询考勤记录
     * 
     * @param attendanceId 考勤记录主键
     * @return 考勤记录
     */
    @Override
    public WorkAttendance selectWorkAttendanceByAttendanceId(Long attendanceId)
    {
        return workAttendanceMapper.selectWorkAttendanceByAttendanceId(attendanceId);
    }

    /**
     * 查询考勤记录列表
     * 
     * @param workAttendance 考勤记录
     * @return 考勤记录
     */
    @Override
    public List<WorkAttendance> selectWorkAttendanceList(WorkAttendance workAttendance)
    {
        return workAttendanceMapper.selectWorkAttendanceList(workAttendance);
    }

    /**
     * 新增考勤记录
     * 
     * @param workAttendance 考勤记录
     * @return 结果
     */
    @Override
    public int insertWorkAttendance(WorkAttendance workAttendance)
    {
        return workAttendanceMapper.insertWorkAttendance(workAttendance);
    }

    /**
     * 修改考勤记录
     * 
     * @param workAttendance 考勤记录
     * @return 结果
     */
    @Override
    public int updateWorkAttendance(WorkAttendance workAttendance)
    {
        return workAttendanceMapper.updateWorkAttendance(workAttendance);
    }

    /**
     * 批量删除考勤记录
     * 
     * @param attendanceIds 需要删除的考勤记录主键
     * @return 结果
     */
    @Override
    public int deleteWorkAttendanceByAttendanceIds(Long[] attendanceIds)
    {
        return workAttendanceMapper.deleteWorkAttendanceByAttendanceIds(attendanceIds);
    }

    /**
     * 删除考勤记录信息
     * 
     * @param attendanceId 考勤记录主键
     * @return 结果
     */
    @Override
    public int deleteWorkAttendanceByAttendanceId(Long attendanceId)
    {
        return workAttendanceMapper.deleteWorkAttendanceByAttendanceId(attendanceId);
    }
}
