package com.ruoyi.system.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.entity.SysUser;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 项目任务分配关系对象 work_assignment
 * 
 * @author zha
 * @date 2025-08-20
 */
public class WorkAssignment extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 分配记录的唯一标识 */
    private Long assignmentId;

    /** 关联work_project表的project_id */
//    @Excel(name = "关联work_project表的project_id")
    private Long projectId;

    @Excel(name = "项目名称", targetAttr = "name", type = Excel.Type.EXPORT)
    private WorkProject project;

    /** 关联sys_user表的user_id */
//    @Excel(name = "关联sys_user表的user_id")
    private Long userId;

    @Excel(name = "项目相关人", targetAttr = "nickName", type = Excel.Type.EXPORT)
    private SysUser user;

    /** 用户在项目中的角色 */
    @Excel(name = "角色")
    private String role;

    /** 分配是否有效：0-无效，1-有效 */
    @Excel(name = "分配是否有效",readConverterExp = "1=是,0=否")
    private Integer isActiveAssignment;

    /** 任务开始日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "任务开始日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date dateStart;

    /** 任务结束日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "任务结束日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date dateEnd;

    /** 记录创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
//    @Excel(name = "记录创建时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date createAt;

    /** 记录最后更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
//    @Excel(name = "记录最后更新时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date updateAt;

    public void setAssignmentId(Long assignmentId) 
    {
        this.assignmentId = assignmentId;
    }

    public Long getAssignmentId() 
    {
        return assignmentId;
    }

    public void setProjectId(Long projectId) 
    {
        this.projectId = projectId;
    }

    public Long getProjectId() 
    {
        return projectId;
    }

    public WorkProject getProject()
    {
        return project;
    }
    public void setProject(WorkProject project)
    {
        this.project = project;
    }

    public void setUserId(Long userId) 
    {
        this.userId = userId;
    }

    public Long getUserId() 
    {
        return userId;
    }

    public SysUser getUser()
    {
        return user;
    }
    public void setUser(SysUser user)
    {
        this.user = user;
    }

    public void setRole(String role) 
    {
        this.role = role;
    }

    public String getRole() 
    {
        return role;
    }

    public void setIsActiveAssignment(Integer isActiveAssignment) 
    {
        this.isActiveAssignment = isActiveAssignment;
    }

    public Integer getIsActiveAssignment() 
    {
        return isActiveAssignment;
    }

    public void setDateStart(Date dateStart) 
    {
        this.dateStart = dateStart;
    }

    public Date getDateStart() 
    {
        return dateStart;
    }

    public void setDateEnd(Date dateEnd) 
    {
        this.dateEnd = dateEnd;
    }

    public Date getDateEnd() 
    {
        return dateEnd;
    }

    public void setCreateAt(Date createAt) 
    {
        this.createAt = createAt;
    }

    public Date getCreateAt() 
    {
        return createAt;
    }

    public void setUpdateAt(Date updateAt) 
    {
        this.updateAt = updateAt;
    }

    public Date getUpdateAt() 
    {
        return updateAt;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("assignmentId", getAssignmentId())
            .append("projectId", getProjectId())
            .append("userId", getUserId())
            .append("role", getRole())
            .append("isActiveAssignment", getIsActiveAssignment())
            .append("dateStart", getDateStart())
            .append("dateEnd", getDateEnd())
            .append("createAt", getCreateAt())
            .append("updateAt", getUpdateAt())
            .append("project", getProject() != null ? getProject().getName() : null)
            .append("user", getUser() != null ? getUser().getNickName() : null)
            .toString();
    }
}
