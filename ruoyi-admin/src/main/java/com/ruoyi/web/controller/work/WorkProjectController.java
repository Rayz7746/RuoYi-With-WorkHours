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
import com.ruoyi.system.domain.WorkProject;
import com.ruoyi.system.service.IWorkProjectService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 项目信息Controller
 * 
 * @author zha
 * @date 2025-08-18
 */
@RestController
@RequestMapping("/work/project")
public class WorkProjectController extends BaseController
{
    @Autowired
    private IWorkProjectService workProjectService;

    /**
     * 查询项目信息列表
     */
    @PreAuthorize("@ss.hasPermi('work:project:list')")
    @GetMapping("/list")
    public TableDataInfo list(WorkProject workProject)
    {
        startPage();
        List<WorkProject> list = workProjectService.selectWorkProjectList(workProject);
        return getDataTable(list);
    }

    /**
     * 导出项目信息列表
     */
    @PreAuthorize("@ss.hasPermi('work:project:export')")
    @Log(title = "项目信息", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, WorkProject workProject)
    {
        List<WorkProject> list = workProjectService.selectWorkProjectList(workProject);
        ExcelUtil<WorkProject> util = new ExcelUtil<WorkProject>(WorkProject.class);
        util.exportExcel(response, list, "项目信息数据");
    }

    /**
     * 获取项目信息详细信息
     */
    @PreAuthorize("@ss.hasPermi('work:project:query')")
    @GetMapping(value = "/{projectId}")
    public AjaxResult getInfo(@PathVariable("projectId") Long projectId)
    {
        return success(workProjectService.selectWorkProjectByProjectId(projectId));
    }

    /**
     * 新增项目信息
     */
    @PreAuthorize("@ss.hasPermi('work:project:add')")
    @Log(title = "项目信息", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody WorkProject workProject)
    {
        return toAjax(workProjectService.insertWorkProject(workProject));
    }

    /**
     * 修改项目信息
     */
    @PreAuthorize("@ss.hasPermi('work:project:edit')")
    @Log(title = "项目信息", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody WorkProject workProject)
    {
        return toAjax(workProjectService.updateWorkProject(workProject));
    }

    /**
     * 删除项目信息
     */
    @PreAuthorize("@ss.hasPermi('work:project:remove')")
    @Log(title = "项目信息", businessType = BusinessType.DELETE)
	@DeleteMapping("/{projectIds}")
    public AjaxResult remove(@PathVariable Long[] projectIds)
    {
        return toAjax(workProjectService.deleteWorkProjectByProjectIds(projectIds));
    }
}
