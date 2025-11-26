package com.yinlian.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 园区会员（平台 2.44 返回数据）.
 */
@Entity
@Table(name = "up_member")
public class MemberEntity {

    @Id
    @Column(name = "member_code", length = 32, nullable = false)
    private String memberCode;

    @Column(name = "member_unique_id", length = 64)
    private String memberUniqueId;

    @Column(name = "member_name", length = 64)
    private String memberName;

    @Column(name = "mobile_no", length = 32)
    private String mobileNo;

    @Column(name = "cert_no", length = 32)
    private String certNo;

    @Column(name = "depart_code", length = 32)
    private String departCode;

    @Column(name = "depart_name", length = 64)
    private String departName;

    @Column(name = "member_type_name", length = 64)
    private String memberTypeName;

    @Column(name = "state_flag")
    private Integer state;

    @Column(name = "deleted_flag")
    private Integer deleted;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(length = 200)
    private String remark;

    @Column
    private Integer age;

    @Column
    private Integer sex;

    @Column(length = 64)
    private String nation;

    @Column(name = "household_address", length = 255)
    private String householdAddress;

    @Column(name = "live_address", length = 255)
    private String liveAddress;

    @Column(name = "created_time")
    private LocalDateTime createTime;

    @Column(name = "updated_time")
    private LocalDateTime updateTime;

    @Column(name = "synced_at")
    private LocalDateTime syncedAt;

    @Lob
    @Column(name = "raw_payload")
    private String rawPayload;

    @PrePersist
    public void onCreate() {
        if (syncedAt == null) {
            syncedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    public void onUpdate() {
        syncedAt = LocalDateTime.now();
    }

    public String getMemberCode() {
        return memberCode;
    }

    public void setMemberCode(String memberCode) {
        this.memberCode = memberCode;
    }

    public String getMemberUniqueId() {
        return memberUniqueId;
    }

    public void setMemberUniqueId(String memberUniqueId) {
        this.memberUniqueId = memberUniqueId;
    }

    public String getMemberName() {
        return memberName;
    }

    public void setMemberName(String memberName) {
        this.memberName = memberName;
    }

    public String getMobileNo() {
        return mobileNo;
    }

    public void setMobileNo(String mobileNo) {
        this.mobileNo = mobileNo;
    }

    public String getCertNo() {
        return certNo;
    }

    public void setCertNo(String certNo) {
        this.certNo = certNo;
    }

    public String getDepartCode() {
        return departCode;
    }

    public void setDepartCode(String departCode) {
        this.departCode = departCode;
    }

    public String getDepartName() {
        return departName;
    }

    public void setDepartName(String departName) {
        this.departName = departName;
    }

    public String getMemberTypeName() {
        return memberTypeName;
    }

    public void setMemberTypeName(String memberTypeName) {
        this.memberTypeName = memberTypeName;
    }

    public Integer getState() {
        return state;
    }

    public void setState(Integer state) {
        this.state = state;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public Integer getSex() {
        return sex;
    }

    public void setSex(Integer sex) {
        this.sex = sex;
    }

    public String getNation() {
        return nation;
    }

    public void setNation(String nation) {
        this.nation = nation;
    }

    public String getHouseholdAddress() {
        return householdAddress;
    }

    public void setHouseholdAddress(String householdAddress) {
        this.householdAddress = householdAddress;
    }

    public String getLiveAddress() {
        return liveAddress;
    }

    public void setLiveAddress(String liveAddress) {
        this.liveAddress = liveAddress;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public LocalDateTime getSyncedAt() {
        return syncedAt;
    }

    public void setSyncedAt(LocalDateTime syncedAt) {
        this.syncedAt = syncedAt;
    }

    public String getRawPayload() {
        return rawPayload;
    }

    public void setRawPayload(String rawPayload) {
        this.rawPayload = rawPayload;
    }
}

