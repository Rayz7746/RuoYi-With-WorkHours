package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.WorkAttendance;
import com.ruoyi.system.domain.vo.WorkAttendanceQuery;

/**
 * 考勤记录Mapper接口
 * 
 * @author zha
 * @date 2025-08-21
 */
public interface WorkAttendanceMapper 
{
    /**
     * 查询考勤记录
     * 
     * @param attendanceId 考勤记录主键
     * @return 考勤记录
     */
    public WorkAttendance selectWorkAttendanceByAttendanceId(Long attendanceId);

    /**
     * 查询考勤记录列表
     * 
     * @param workAttendance 考勤记录
     * @return 考勤记录集合
     */
    public List<WorkAttendance> selectWorkAttendanceList(WorkAttendance workAttendance);

    public List<WorkAttendance> selectWorkAttendanceListArray(WorkAttendanceQuery workAttendanceQuery);

    /**
     * 新增考勤记录
     * 
     * @param workAttendance 考勤记录
     * @return 结果
     */
    public int insertWorkAttendance(WorkAttendance workAttendance);

    /**
     * 修改考勤记录
     * 
     * @param workAttendance 考勤记录
     * @return 结果
     */
    public int updateWorkAttendance(WorkAttendance workAttendance);

    /**
     * 删除考勤记录
     * 
     * @param attendanceId 考勤记录主键
     * @return 结果
     */
    public int deleteWorkAttendanceByAttendanceId(Long attendanceId);

    /**
     * 批量删除考勤记录
     * 
     * @param attendanceIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteWorkAttendanceByAttendanceIds(Long[] attendanceIds);
}
