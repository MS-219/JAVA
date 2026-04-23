package com.yinlian.model;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "access_record", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"device_code", "person_code", "capture_time"})
})
public class RecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_code", length = 128)
    private String deviceCode;

    @Column(name = "person_code", length = 128)
    private String personCode;

    @Column(name = "capture_time", length = 32)
    private String captureTime;

    @Column(name = "status", length = 16)
    private String status;

    @Lob
    @Column(name = "raw_json")
    private String rawJson;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDeviceCode() { return deviceCode; }
    public void setDeviceCode(String deviceCode) { this.deviceCode = deviceCode; }

    public String getPersonCode() { return personCode; }
    public void setPersonCode(String personCode) { this.personCode = personCode; }

    public String getCaptureTime() { return captureTime; }
    public void setCaptureTime(String captureTime) { this.captureTime = captureTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRawJson() { return rawJson; }
    public void setRawJson(String rawJson) { this.rawJson = rawJson; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
