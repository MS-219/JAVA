package com.yinlian.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * 园区会员卡（平台 2.45 返回数据）.
 */
@Entity
@Table(name = "up_member_card")
public class MemberCardEntity {

    @Id
    @Column(name = "card_code", length = 32)
    private String cardCode;

    @Column(name = "member_code", length = 32)
    private String memberCode;

    @Column(name = "card_no", length = 64)
    private String cardNo;

    @Column(name = "member_card_type", length = 64)
    private String memberCardType;

    @Column(name = "balance")
    private Long balance;

    @Column(name = "face_bind")
    private Integer faceBind;

    @Column(name = "loss_state")
    private Integer lossState;

    @Column(name = "lock_state")
    private Integer lockState;

    @Column(name = "enable_state")
    private Integer enableState;

    @Column(name = "deleted_flag")
    private Integer deleted;

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

    public String getCardCode() {
        return cardCode;
    }

    public void setCardCode(String cardCode) {
        this.cardCode = cardCode;
    }

    public String getMemberCode() {
        return memberCode;
    }

    public void setMemberCode(String memberCode) {
        this.memberCode = memberCode;
    }

    public String getCardNo() {
        return cardNo;
    }

    public void setCardNo(String cardNo) {
        this.cardNo = cardNo;
    }

    public String getMemberCardType() {
        return memberCardType;
    }

    public void setMemberCardType(String memberCardType) {
        this.memberCardType = memberCardType;
    }

    public Long getBalance() {
        return balance;
    }

    public void setBalance(Long balance) {
        this.balance = balance;
    }

    public Integer getFaceBind() {
        return faceBind;
    }

    public void setFaceBind(Integer faceBind) {
        this.faceBind = faceBind;
    }

    public Integer getLossState() {
        return lossState;
    }

    public void setLossState(Integer lossState) {
        this.lossState = lossState;
    }

    public Integer getLockState() {
        return lockState;
    }

    public void setLockState(Integer lockState) {
        this.lockState = lockState;
    }

    public Integer getEnableState() {
        return enableState;
    }

    public void setEnableState(Integer enableState) {
        this.enableState = enableState;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
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

