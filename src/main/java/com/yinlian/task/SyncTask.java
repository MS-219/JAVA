package com.yinlian.task;

import com.alibaba.fastjson.JSONObject;
import com.yinlian.service.PlatformSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class SyncTask {

    private static final Logger logger = LoggerFactory.getLogger(SyncTask.class);
    private final AtomicBoolean syncRunning = new AtomicBoolean(false);

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

    @EventListener(ApplicationReadyEvent.class)
    public void triggerInitialSyncAfterStartup() {
        Thread thread = new Thread(() -> {
            try {
                Thread.sleep(15000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            logger.info("[SYNC] 启动后首次同步准备开始");
            syncAllData();
        }, "initial-sync-task");
        thread.setDaemon(true);
        thread.start();
    }

    // 每5分钟执行一次全量同步，首次调度在启动 60 秒后触发
    // 使用 fixedDelay 确保上一次执行完后才开始下一次计时，避免任务堆积
    @Scheduled(initialDelayString = "${sync.initial-delay-ms:60000}", fixedDelayString = "${sync.fixed-delay-ms:300000}")
    public void syncAllData() {
        if (!syncRunning.compareAndSet(false, true)) {
            logger.warn("[SYNC] 上一轮同步仍在执行，跳过本次触发");
            return;
        }
        LocalDateTime startedAt = LocalDateTime.now();
        logger.info("[SYNC] ==================== 定时同步开始 {} ====================", startedAt);
        try {
            // 1. 同步所有会员
            logger.info("[SYNC] 阶段1: 开始同步会员");
            platformSyncService.syncAllMembers(null); // null 表示全量同步

            // 2. 同步所有会员卡
            logger.info("[SYNC] 阶段2: 开始同步会员卡");
            platformSyncService.syncAllMemberCards(null); // null 表示全量同步

            // 3. 同步人脸绑定关系 (会自动下载图片)
            logger.info("[SYNC] 阶段3: 开始同步人脸绑定与图片");
            platformSyncService.syncFaceBindings(0, null); // 0=全量, null=不限时间

            // 4. HTTP 自动推送到配置的设备
            if (autoPush && deviceIps != null && !deviceIps.isEmpty()) {
                logger.info("[SYNC] 阶段4: 开始向 {} 台设备推送数据", deviceIps.size());
                for (String deviceIp : deviceIps) {
                    if (deviceIp == null || deviceIp.trim().isEmpty()) {
                        continue;
                    }
                    try {
                        logger.info("[SYNC] 设备 {}: 开始推送", deviceIp);
                        JSONObject result = deviceHttpSyncService.syncAllToDevice(deviceIp);
                        logger.info("[SYNC] 设备 {}: 推送结果 {}", deviceIp, result.toJSONString());
                    } catch (Exception ex) {
                        logger.error("[SYNC] 设备 {}: 推送失败", deviceIp, ex);
                    }
                }
            } else if (autoPush) {
                logger.warn("[SYNC] 阶段4: 已开启自动推送，但没有配置设备IP");
            }

            // 5. MQTT 推送到活跃设备 (备用方案)
            java.util.Set<String> activeDevices = deviceSyncService.getActiveDevices();
            if (activeDevices.isEmpty()) {
                logger.info("[SYNC] 阶段5: 没有活跃设备，跳过 MQTT 推送");
            } else {
                logger.info("[SYNC] 阶段5: 开始向 {} 个活跃设备做 MQTT 推送", activeDevices.size());
                for (String devSno : activeDevices) {
                    logger.info("[SYNC] 活跃设备 {}: 开始 MQTT 推送", devSno);
                    try {
                        deviceSyncService.pushAllMembers(devSno);
                    } catch (Exception ex) {
                        logger.error("[SYNC] 活跃设备 {}: MQTT 推送失败", devSno, ex);
                    }
                }
            }

            logger.info("[SYNC] ==================== 定时同步完成 {} ====================", LocalDateTime.now());
        } catch (Exception e) {
            logger.error("[SYNC] ==================== 定时同步失败 ====================", e);
        } finally {
            syncRunning.set(false);
        }
    }
}
