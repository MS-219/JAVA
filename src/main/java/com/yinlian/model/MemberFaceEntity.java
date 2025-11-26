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
 * 人脸绑定/照片信息（平台 2.63 / 2.62）.
 */
@Entity
@Table(name = "up_member_face")
public class MemberFaceEntity {

    @Id
    @Column(name = "card_no", length = 64)
    private String cardNo;

    @Column(name = "face_code", length = 64)
    private String faceCode;

    @Column(name = "member_code", length = 32)
    private String memberCode;

    @Column(name = "binding_state")
    private Integer bindingState;

    @Column(name = "image_path", length = 255)
    private String imagePath;

    @Column(name = "image_hash", length = 64)
    private String imageHash;

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

    public String getCardNo() {
        return cardNo;
    }

    public void setCardNo(String cardNo) {
        this.cardNo = cardNo;
    }

    public String getFaceCode() {
        return faceCode;
    }

    public void setFaceCode(String faceCode) {
        this.faceCode = faceCode;
    }

    public String getMemberCode() {
        return memberCode;
    }

    public void setMemberCode(String memberCode) {
        this.memberCode = memberCode;
    }

    public Integer getBindingState() {
        return bindingState;
    }

    public void setBindingState(Integer bindingState) {
        this.bindingState = bindingState;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getImageHash() {
        return imageHash;
    }

    public void setImageHash(String imageHash) {
        this.imageHash = imageHash;
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

