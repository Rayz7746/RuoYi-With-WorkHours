package com.ruoyi.system.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.annotation.Excels;
import com.ruoyi.common.core.domain.entity.SysUser;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 考勤记录对象 work_attendance
 * 
 * @author zha
 * @date 2025-08-21
 */
public class WorkAttendance extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 考勤记录唯一标识 */
    private Long attendanceId;

    /** 用户ID */
//    @Excel(name = "用户ID")
    private Long userId;

    @Excel(name = "用户名称", targetAttr = "nickName", type = Excel.Type.EXPORT)
    private SysUser user;

    /** 项目ID */
//    @Excel(name = "项目ID")
    private Long projectId;

    @Excels({
            @Excel(name = "客户名称", targetAttr = "customer.customerName", type = Excel.Type.EXPORT),
            @Excel(name = "项目名称", targetAttr = "name", type = Excel.Type.EXPORT)
    })
    private WorkProject project;


    /** 考勤日期（精确到天） */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "考勤日期",width = 30, dateFormat = "yyyy-MM-dd")
    private Date attendanceDate;

    /** 工作时长（小时，支持小数） */
    @Excel(name = "工作时长")
    private BigDecimal workingHours;

    /** 考勤备注 */
    @Excel(name = "考勤备注")
    private String comment;

    /** 记录创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
//    @Excel(name = "记录创建时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date createAt;

    /** 记录最后更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
//    @Excel(name = "记录最后更新时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date updateAt;

    public void setAttendanceId(Long attendanceId) 
    {
        this.attendanceId = attendanceId;
    }

    public Long getAttendanceId() 
    {
        return attendanceId;
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

    public WorkProject getProject()
    {
        return project;
    }
    public void setProject(WorkProject project)
    {
        this.project = project;
    }

    public void setProjectId(Long projectId) 
    {
        this.projectId = projectId;
    }

    public Long getProjectId() 
    {
        return projectId;
    }

    public void setAttendanceDate(Date attendanceDate) 
    {
        this.attendanceDate = attendanceDate;
    }

    public Date getAttendanceDate() 
    {
        return attendanceDate;
    }

    public void setWorkingHours(BigDecimal workingHours) 
    {
        this.workingHours = workingHours;
    }

    public BigDecimal getWorkingHours() 
    {
        return workingHours;
    }

    public void setComment(String comment) 
    {
        this.comment = comment;
    }

    public String getComment() 
    {
        return comment;
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
            .append("attendanceId", getAttendanceId())
            .append("userId", getUserId())
            .append("projectId", getProjectId())
            .append("attendanceDate", getAttendanceDate())
            .append("workingHours", getWorkingHours())
            .append("comment", getComment())
            .append("createAt", getCreateAt())
            .append("updateAt", getUpdateAt())
            .append("project", getProject() != null ? getProject().getName() : null)
            .append("user", getUser() != null ? getUser().getNickName() : null)
            .toString();
    }
}
