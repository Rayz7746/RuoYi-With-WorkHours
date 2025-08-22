package com.ruoyi.web.controller.work;

import java.util.List;

import com.ruoyi.system.domain.WorkAssignment;
import com.ruoyi.system.service.IWorkAssignmentService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.WorkAttendance;
import com.ruoyi.system.service.IWorkAttendanceService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 考勤记录Controller
 * 
 * @author zha
 * @date 2025-08-21
 */
@RestController
@RequestMapping("/work/attendance")
public class WorkAttendanceController extends BaseController
{
    @Autowired
    private IWorkAttendanceService workAttendanceService;

    @Autowired
    private IWorkAssignmentService workAssignmentService;

    /**
     * 查询考勤记录列表
     */
    @PreAuthorize("@ss.hasPermi('work:attendance:list')")
    @GetMapping("/list")
    public TableDataInfo list(WorkAttendance workAttendance)
    {
        startPage();
        List<WorkAttendance> list = workAttendanceService.selectWorkAttendanceList(workAttendance);
        return getDataTable(list);
    }

    /**
     * 导出考勤记录列表
     */
    @PreAuthorize("@ss.hasPermi('work:attendance:export')")
    @Log(title = "考勤记录", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, WorkAttendance workAttendance)
    {
        List<WorkAttendance> list = workAttendanceService.selectWorkAttendanceList(workAttendance);
        ExcelUtil<WorkAttendance> util = new ExcelUtil<WorkAttendance>(WorkAttendance.class);
        util.exportExcel(response, list, "考勤记录数据");
    }

    /**
     * 获取考勤记录详细信息
     */
    @PreAuthorize("@ss.hasPermi('work:attendance:query')")
    @GetMapping(value = "/{attendanceId}")
    public AjaxResult getInfo(@PathVariable("attendanceId") Long attendanceId)
    {
        return success(workAttendanceService.selectWorkAttendanceByAttendanceId(attendanceId));
    }

    /**
     * 新增考勤记录
     */
    @PreAuthorize("@ss.hasPermi('work:attendance:add')")
    @Log(title = "考勤记录", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody WorkAttendance workAttendance)
    {
        return toAjax(workAttendanceService.insertWorkAttendance(workAttendance));
    }

    /**
     * 修改考勤记录
     */
    @PreAuthorize("@ss.hasPermi('work:attendance:edit')")
    @Log(title = "考勤记录", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody WorkAttendance workAttendance)
    {
        return toAjax(workAttendanceService.updateWorkAttendance(workAttendance));
    }

    /**
     * 删除考勤记录
     */
    @PreAuthorize("@ss.hasPermi('work:attendance:remove')")
    @Log(title = "考勤记录", businessType = BusinessType.DELETE)
	@DeleteMapping("/{attendanceIds}")
    public AjaxResult remove(@PathVariable Long[] attendanceIds)
    {
        return toAjax(workAttendanceService.deleteWorkAttendanceByAttendanceIds(attendanceIds));
    }
}
