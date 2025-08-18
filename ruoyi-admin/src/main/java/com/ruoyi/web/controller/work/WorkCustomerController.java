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
import com.ruoyi.system.domain.WorkCustomer;
import com.ruoyi.system.service.IWorkCustomerService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 客户信息Controller
 * 
 * @author zha
 * @date 2025-08-18
 */
@RestController
@RequestMapping("/work/customer")
public class WorkCustomerController extends BaseController
{
    @Autowired
    private IWorkCustomerService workCustomerService;

    /**
     * 查询客户信息列表
     */
    @PreAuthorize("@ss.hasPermi('work:customer:list')")
    @GetMapping("/list")
    public TableDataInfo list(WorkCustomer workCustomer)
    {
        startPage();
        List<WorkCustomer> list = workCustomerService.selectWorkCustomerList(workCustomer);
        return getDataTable(list);
    }

    /**
     * 导出客户信息列表
     */
    @PreAuthorize("@ss.hasPermi('work:customer:export')")
    @Log(title = "客户信息", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, WorkCustomer workCustomer)
    {
        List<WorkCustomer> list = workCustomerService.selectWorkCustomerList(workCustomer);
        ExcelUtil<WorkCustomer> util = new ExcelUtil<WorkCustomer>(WorkCustomer.class);
        util.exportExcel(response, list, "客户信息数据");
    }

    /**
     * 获取客户信息详细信息
     */
    @PreAuthorize("@ss.hasPermi('work:customer:query')")
    @GetMapping(value = "/{customerId}")
    public AjaxResult getInfo(@PathVariable("customerId") Long customerId)
    {
        return success(workCustomerService.selectWorkCustomerByCustomerId(customerId));
    }

    /**
     * 新增客户信息
     */
    @PreAuthorize("@ss.hasPermi('work:customer:add')")
    @Log(title = "客户信息", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody WorkCustomer workCustomer)
    {
        return toAjax(workCustomerService.insertWorkCustomer(workCustomer));
    }

    /**
     * 修改客户信息
     */
    @PreAuthorize("@ss.hasPermi('work:customer:edit')")
    @Log(title = "客户信息", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody WorkCustomer workCustomer)
    {
        return toAjax(workCustomerService.updateWorkCustomer(workCustomer));
    }

    /**
     * 删除客户信息
     */
    @PreAuthorize("@ss.hasPermi('work:customer:remove')")
    @Log(title = "客户信息", businessType = BusinessType.DELETE)
	@DeleteMapping("/{customerIds}")
    public AjaxResult remove(@PathVariable Long[] customerIds)
    {
        return toAjax(workCustomerService.deleteWorkCustomerByCustomerIds(customerIds));
    }
}
