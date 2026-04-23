package com.yinlian.model;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 通行记录持久化实体
 * 存储设备上报的通行记录，重启后不丢失
 */
@Entity
@Table(name = "access_record", indexes = {
        @Index(name = "idx_device_person_time", columnList = "deviceCode,personCode,captureTime", unique = true),
        @Index(name = "idx_capture_time", columnList = "captureTime"),
        @Index(name = "idx_person_code", columnList = "personCode"),
        @Index(name = "idx_device_code", columnList = "deviceCode")
})
public class AccessRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 设备标识 (MAC地址/IP/SN) */
    @Column(name = "deviceCode", length = 128)
    private String deviceCode;

    /** 人员标识 (memberCode/userId 等) */
    @Column(name = "personCode", length = 128)
    private String personCode;

    /** 抓拍时间 (设备上报的原始时间字符串) */
    @Column(name = "captureTime", length = 64)
    private String captureTime;

    /** 上报状态: pending / success / failed */
    @Column(length = 16)
    private String status;

    /** 设备上报的完整原始 JSON 数据 */
    @Lob
    @Column(name = "raw_json")
    private String rawJson;

    /** 入库时间 */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    // ---- Getters & Setters ----

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDeviceCode() {
        return deviceCode;
    }

    public void setDeviceCode(String deviceCode) {
        this.deviceCode = deviceCode;
    }

    public String getPersonCode() {
        return personCode;
    }

    public void setPersonCode(String personCode) {
        this.personCode = personCode;
    }

    public String getCaptureTime() {
        return captureTime;
    }

    public void setCaptureTime(String captureTime) {
        this.captureTime = captureTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRawJson() {
        return rawJson;
    }

    public void setRawJson(String rawJson) {
        this.rawJson = rawJson;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
