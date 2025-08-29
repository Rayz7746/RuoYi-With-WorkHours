package com.ruoyi.system.domain.vo;

public class WorkAttendanceQuery {
    private Long[] userIds;
    private Long[] projectIds;
    private Long[] customerIds;
    private Long[] deptIds;
    private String startTime;
    private String endTime;

    public Long[] getDeptIds() {
        return deptIds;
    }
    public void setDeptIds(Long[] deptIds) {
        this.deptIds = deptIds;
    }
    public Long[] getUserIds() {
        return userIds;
    }
    public void setUserIds(Long[] userIds) {
        this.userIds = userIds;
    }
    public Long[] getProjectIds() {
        return projectIds;
    }
    public void setProjectIds(Long[] projectIds) {
        this.projectIds = projectIds;
    }
    public Long[] getCustomerIds() {
        return customerIds;
    }
    public void setCustomerIds(Long[] customerIds) {
        this.customerIds = customerIds;
    }
    public String getStartTime() {
        return startTime;
    }
    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }
    public String getEndTime() {
        return endTime;
    }
    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }
}
