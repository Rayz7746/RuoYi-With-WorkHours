package com.ruoyi.system.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 项目信息对象 work_project
 * 
 * @author zha
 * @date 2025-08-18
 */
public class WorkProject extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** $column.columnComment */
    private Long projectId;

    /** 项目名称，如"AI - 人工智能" */
    @Excel(name = "项目名称", width = 30)
    private String name;

    /** 项目代码，如"AI" */
    @Excel(name = "项目代码", width = 30)
    private String code;

    /** 关联客户ID */
    @Excel(name = "关联客户ID", width = 30)
    private Long customerId;
//
    @Excel(name = "关联客户名称", targetAttr = "name", type = Excel.Type.EXPORT)
    private WorkCustomer customer;

    /** 项目负责人ID(关联sys_user.user_id) */
    @Excel(name = "项目负责人ID(关联sys_user.user_id)", width = 30)
    private Long projectManagerId;

    /** 项目描述，可为空 */
    @Excel(name = "项目描述", width = 50)
    private String description;

    /** 联系人 */
    @Excel(name = "联系人", width = 30)
    private String contactPerson;

    /** 是否默认项目，0否1是 */
    @Excel(name = "是否为默认项目", readConverterExp = "1=是,0=否", width = 30)
    private Integer isDefault;

    /** 是否可计费，0否1是 */
    @Excel(name = "是否为可计费项目", readConverterExp = "1=是,0=否", width = 30)
    private Integer isBillable;

    /** 是否激活，0否1是 */
    @Excel(name = "是否启用", readConverterExp = "1=启用,0=未启用", width = 30)
    private Integer isActive;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
//    @Excel(name = "创建时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date createdAt;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
//    @Excel(name = "更新时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date updatedAt;

    public void setProjectId(Long projectId) 
    {
        this.projectId = projectId;
    }

    public Long getProjectId() 
    {
        return projectId;
    }

    public void setName(String name) 
    {
        this.name = name;
    }

    public String getName() 
    {
        return name;
    }

    public void setCode(String code) 
    {
        this.code = code;
    }

    public String getCode() 
    {
        return code;
    }

    public void setCustomerId(Long customerId) 
    {
        this.customerId = customerId;
    }

    public Long getCustomerId() 
    {
        return customerId;
    }

    public WorkCustomer getCustomer()
    {
        return customer;
    }

    public void setCustomer(WorkCustomer customer)
    {
        this.customer = customer;
    }

    public void setProjectManagerId(Long projectManagerId) 
    {
        this.projectManagerId = projectManagerId;
    }

    public Long getProjectManagerId() 
    {
        return projectManagerId;
    }

    public void setDescription(String description) 
    {
        this.description = description;
    }

    public String getDescription() 
    {
        return description;
    }

    public void setContactPerson(String contactPerson) 
    {
        this.contactPerson = contactPerson;
    }

    public String getContactPerson() 
    {
        return contactPerson;
    }

    public void setIsDefault(Integer isDefault) 
    {
        this.isDefault = isDefault;
    }

    public Integer getIsDefault() 
    {
        return isDefault;
    }

    public void setIsBillable(Integer isBillable) 
    {
        this.isBillable = isBillable;
    }

    public Integer getIsBillable() 
    {
        return isBillable;
    }

    public void setIsActive(Integer isActive) 
    {
        this.isActive = isActive;
    }

    public Integer getIsActive() 
    {
        return isActive;
    }

    public void setCreatedAt(Date createdAt) 
    {
        this.createdAt = createdAt;
    }

    public Date getCreatedAt() 
    {
        return createdAt;
    }

    public void setUpdatedAt(Date updatedAt) 
    {
        this.updatedAt = updatedAt;
    }

    public Date getUpdatedAt() 
    {
        return updatedAt;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("projectId", getProjectId())
            .append("name", getName())
            .append("code", getCode())
            .append("customerId", getCustomerId())
            .append("projectManagerId", getProjectManagerId())
            .append("description", getDescription())
            .append("contactPerson", getContactPerson())
            .append("isDefault", getIsDefault())
            .append("isBillable", getIsBillable())
            .append("isActive", getIsActive())
            .append("createdAt", getCreatedAt())
            .append("updatedAt", getUpdatedAt())
            .append("customer", getCustomer() != null ? getCustomer().getCustomerName() : null)
            .toString();
    }
}
