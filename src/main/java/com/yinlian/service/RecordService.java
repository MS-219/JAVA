package com.yinlian.service;

import com.alibaba.fastjson.JSONObject;
import com.yinlian.repository.RecordRepository;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class RecordService {
    private static final Logger logger = LoggerFactory.getLogger(RecordService.class);

    @Autowired
    private UnionPayClient unionPayClient;
    
    @Autowired
    private RecordRepository repository;

    // Fingerprint helper class
    private static class Fingerprint {
        String deviceCode;
        String personCode;
        String captureTime;
    }

    private Fingerprint extractFingerprint(JSONObject record) {
        Fingerprint fp = new Fingerprint();
        // 使用 StringUtils.defaultString 避免 trim() 空指针
        fp.deviceCode = StringUtils.defaultString(StringUtils.firstNonBlank(
            record.getString("Mac_addr"), 
            record.getString("dev_sno"), 
            record.getString("deviceCode")
        ), "").trim();
        
        fp.personCode = StringUtils.defaultString(StringUtils.firstNonBlank(
            record.getString("userid"), 
            record.getString("employee_number"), 
            record.getString("person_id"), 
            record.getString("userId"),
            record.getString("id"), // 添加 id 字段支持
            record.getString("icNum"), 
            record.getString("ic_card")
        ), "").trim();
        
        if (record.containsKey("time")) {
            fp.captureTime = record.getString("time");
        } else if (record.containsKey("capture_time")) {
            fp.captureTime = record.getString("capture_time");
        } else {
            fp.captureTime = "";
        }
        return fp;
    }

    public boolean isDuplicate(JSONObject record) {
        Fingerprint fp = extractFingerprint(record);
        if (fp.deviceCode.isEmpty() || fp.personCode.isEmpty() || fp.captureTime.isEmpty()) {
            return false;
        }
        
        return repository.findRecord(r -> 
            Objects.equals(r.getString("__deviceCode"), fp.deviceCode) &&
            Objects.equals(r.getString("__personCode"), fp.personCode) &&
            Objects.equals(r.getString("__captureTime"), fp.captureTime)
        ) != null;
    }

    public void handleRecordUpload(JSONObject record) {
        logger.info("Received record: {}", record);
        
        if (isDuplicate(record)) {
            logger.warn("Duplicate record ignored");
            return;
        }

        Fingerprint fp = extractFingerprint(record);
        record.put("__deviceCode", fp.deviceCode);
        record.put("__personCode", fp.personCode);
        record.put("__captureTime", fp.captureTime);
        record.put("status", "pending");
        
        repository.pushRecord(record);
        
        uploadToUnionPay(record);
    }

    private void uploadToUnionPay(JSONObject record) {
        try {
            Map<String, Object> unionpayPayload = buildUnionPayPayload(record);
            unionPayClient.posAttendanceSignIn(unionpayPayload);
            
            record.put("status", "success");
            logger.info("Record uploaded successfully");
        } catch (Exception e) {
            record.put("status", "failed");
            logger.error("Handle record failed", e);
        }
    }

    private Map<String, Object> buildUnionPayPayload(JSONObject record) {
        Map<String, Object> unionpayPayload = new HashMap<>();
        String personCode = record.getString("__personCode");
        
        if (!personCode.isEmpty()) {
            unionpayPayload.put("faceCode", personCode);
        } else if (StringUtils.isNotBlank(record.getString("QRcode")) || StringUtils.isNotBlank(record.getString("qr"))) {
            unionpayPayload.put("qrCode", StringUtils.firstNonBlank(record.getString("QRcode"), record.getString("qr")));
        } else if (StringUtils.isNotBlank(record.getString("icNum")) || StringUtils.isNotBlank(record.getString("ic_card"))) {
            unionpayPayload.put("cardNo", StringUtils.firstNonBlank(record.getString("icNum"), record.getString("ic_card")));
        } else {
            String devCode = record.getString("__deviceCode");
            unionpayPayload.put("faceCode", !devCode.isEmpty() ? devCode : "unknown");
        }
        return unionpayPayload;
    }
    
    // Retry task (every 60s)
    @Scheduled(fixedRate = 60000)
    public void replayFailedRecords() {
        List<JSONObject> all = repository.getAllRecords();
        for (JSONObject record : all) {
            String status = record.getString("status");
            if ("failed".equals(status) || "pending".equals(status)) {
                logger.info("Replaying record...");
                uploadToUnionPay(record);
            }
        }
    }

    public void handleDeviceLogin(JSONObject data) {
        logger.info("Device login: {}", data);
        // Store device info if needed
    }
}
