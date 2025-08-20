package com.ruoyi.web.controller.work;

import java.util.List;
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
import com.ruoyi.system.domain.WorkAssignment;
import com.ruoyi.system.service.IWorkAssignmentService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 项目任务分配关系Controller
 * 
 * @author zha
 * @date 2025-08-20
 */
@RestController
@RequestMapping("/work/assignment")
public class WorkAssignmentController extends BaseController
{
    @Autowired
    private IWorkAssignmentService workAssignmentService;

    /**
     * 查询项目任务分配关系列表
     */
    @PreAuthorize("@ss.hasPermi('work:assignment:list')")
    @GetMapping("/list")
    public TableDataInfo list(WorkAssignment workAssignment)
    {
        startPage();
        List<WorkAssignment> list = workAssignmentService.selectWorkAssignmentList(workAssignment);
        return getDataTable(list);
    }

    /**
     * 导出项目任务分配关系列表
     */
    @PreAuthorize("@ss.hasPermi('work:assignment:export')")
    @Log(title = "项目任务分配关系", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, WorkAssignment workAssignment)
    {
        List<WorkAssignment> list = workAssignmentService.selectWorkAssignmentList(workAssignment);
        ExcelUtil<WorkAssignment> util = new ExcelUtil<WorkAssignment>(WorkAssignment.class);
        util.exportExcel(response, list, "项目任务分配关系数据");
    }

    /**
     * 获取项目任务分配关系详细信息
     */
    @PreAuthorize("@ss.hasPermi('work:assignment:query')")
    @GetMapping(value = "/{assignmentId}")
    public AjaxResult getInfo(@PathVariable("assignmentId") Long assignmentId)
    {
        return success(workAssignmentService.selectWorkAssignmentByAssignmentId(assignmentId));
    }

    /**
     * 新增项目任务分配关系
     */
    @PreAuthorize("@ss.hasPermi('work:assignment:add')")
    @Log(title = "项目任务分配关系", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody WorkAssignment workAssignment)
    {
        return toAjax(workAssignmentService.insertWorkAssignment(workAssignment));
    }

    /**
     * 修改项目任务分配关系
     */
    @PreAuthorize("@ss.hasPermi('work:assignment:edit')")
    @Log(title = "项目任务分配关系", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody WorkAssignment workAssignment)
    {
        return toAjax(workAssignmentService.updateWorkAssignment(workAssignment));
    }

    /**
     * 删除项目任务分配关系
     */
    @PreAuthorize("@ss.hasPermi('work:assignment:remove')")
    @Log(title = "项目任务分配关系", businessType = BusinessType.DELETE)
	@DeleteMapping("/{assignmentIds}")
    public AjaxResult remove(@PathVariable Long[] assignmentIds)
    {
        return toAjax(workAssignmentService.deleteWorkAssignmentByAssignmentIds(assignmentIds));
    }
}
