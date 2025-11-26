package com.yinlian.task;

import com.alibaba.fastjson.JSONObject;
import com.yinlian.service.PlatformSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class SyncTask {

    private static final Logger logger = LoggerFactory.getLogger(SyncTask.class);

    private final PlatformSyncService platformSyncService;
    private final com.yinlian.service.DeviceHttpSyncService deviceHttpSyncService;
    private final com.yinlian.service.DeviceSyncService deviceSyncService;

    @Value("${device.ips:}")
    private java.util.List<String> deviceIps;

    @Value("${device.auto-push:false}")
    private boolean autoPush;

    public SyncTask(PlatformSyncService platformSyncService,
            com.yinlian.service.DeviceHttpSyncService deviceHttpSyncService,
            com.yinlian.service.DeviceSyncService deviceSyncService) {
        this.platformSyncService = platformSyncService;
        this.deviceHttpSyncService = deviceHttpSyncService;
        this.deviceSyncService = deviceSyncService;
    }

    // 每5分钟执行一次全量同步 (300,000 ms)
    // 使用 fixedDelay 确保上一次执行完后才开始下一次计时，避免任务堆积
    @Scheduled(fixedDelay = 300000)
    public void syncAllData() {
        logger.info("Scheduled Sync Task Started at {}", LocalDateTime.now());
        try {
            // 0. 清空 faces 文件夹，确保数据完全同步（删除后台已删除的人脸）
            logger.info("Cleaning up faces directory...");
            cleanupFacesDirectory();

            // 1. 同步所有会员
            logger.info("Starting scheduled member sync...");
            platformSyncService.syncAllMembers(null); // null 表示全量同步

            // 2. 同步人脸绑定关系 (会自动下载图片)
            logger.info("Starting scheduled face binding sync...");
            platformSyncService.syncFaceBindings(0, null); // 0=全量, null=不限时间

            // 3. HTTP 自动推送到配置的设备
            if (autoPush && deviceIps != null && !deviceIps.isEmpty()) {
                logger.info("Auto-push enabled, pushing to {} device(s)", deviceIps.size());
                for (String deviceIp : deviceIps) {
                    if (deviceIp == null || deviceIp.trim().isEmpty()) {
                        continue;
                    }
                    try {
                        logger.info("Pushing to device: {}", deviceIp);
                        JSONObject result = deviceHttpSyncService.syncAllToDevice(deviceIp);
                        logger.info("HTTP push result for {}: {}", deviceIp, result.toJSONString());
                    } catch (Exception ex) {
                        logger.error("Failed to auto-push to device " + deviceIp, ex);
                    }
                }
            } else if (autoPush) {
                logger.warn("Auto-push enabled but no device IPs configured");
            }

            // 4. MQTT 推送到活跃设备 (备用方案)
            java.util.Set<String> activeDevices = deviceSyncService.getActiveDevices();
            if (activeDevices.isEmpty()) {
                logger.info("No active devices found, skipping MQTT push.");
            } else {
                for (String devSno : activeDevices) {
                    logger.info("Pushing data to active device via MQTT: {}", devSno);
                    try {
                        deviceSyncService.pushAllMembers(devSno);
                    } catch (Exception ex) {
                        logger.error("Failed to push to device " + devSno, ex);
                    }
                }
            }

            logger.info("Scheduled Sync Task Completed Successfully");
        } catch (Exception e) {
            logger.error("Scheduled Sync Task Failed", e);
        }
    }

    /**
     * 清空 faces 文件夹，确保每次同步都是完全重新下载
     * 这样可以删除后台已经删除但本地还保留的旧人脸照片
     */
    private void cleanupFacesDirectory() {
        try {
            java.nio.file.Path facesDir = java.nio.file.Paths.get("data", "faces");
            if (java.nio.file.Files.exists(facesDir)) {
                java.nio.file.Files.walk(facesDir)
                        .filter(java.nio.file.Files::isRegularFile)
                        .forEach(file -> {
                            try {
                                java.nio.file.Files.delete(file);
                            } catch (Exception e) {
                                logger.warn("Failed to delete file: " + file, e);
                            }
                        });
                logger.info("Faces directory cleaned up successfully");
            }
        } catch (Exception e) {
            logger.error("Failed to cleanup faces directory", e);
        }
    }
}
