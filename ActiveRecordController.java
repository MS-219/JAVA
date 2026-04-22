package com.yinlian.controller;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONArray;
import com.yinlian.service.DeviceSyncService;
import com.yinlian.service.DeviceHttpSyncService;
import com.yinlian.service.MemberSyncService;
import com.yinlian.service.PlatformSyncService;
import com.yinlian.service.RecordService;
import com.yinlian.service.UnionPayClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

@RestController
public class RecordController {

    private static final Logger logger = LoggerFactory.getLogger(RecordController.class);

    @Autowired
    private RecordService recordService;

    @Autowired
    private UnionPayClient unionPayClient;

    @Autowired
    private MemberSyncService memberSyncService;

    @Autowired
    private PlatformSyncService platformSyncService;

    @Autowired
    private DeviceSyncService deviceSyncService;

    @Autowired
    private DeviceHttpSyncService deviceHttpSyncService;

    @GetMapping("/test/sync-members")
    public String testSyncMembers(@RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "50") int pageSize,
            @RequestParam(required = false) String lastUpdateTime) {
        try {
            logger.info("Starting member sync test... pageNo={}, pageSize={}, lastUpdate={}",
                    pageNo, pageSize, lastUpdateTime);
            JSONObject resp = unionPayClient.fetchMembers(pageNo, pageSize, lastUpdateTime);
            memberSyncService.saveMembers(resp.getJSONArray("memberList"));
            memberSyncService.recordSyncLog("plat.member.sync", pageNo, pageSize,
                    resp.getString("respCode"), resp.getString("respDesc"),
                    "0000".equals(resp.getString("respCode")));
            logger.info("Sync response: {}", resp.toJSONString());
            // 调试：如果失败，返回签名串
            if (!"0000".equals(resp.getString("respCode"))) {
                resp.put("debug_sign_string", unionPayClient.lastSignString);
            }
            return resp.toJSONString();
        } catch (Exception e) {
            logger.error("Sync failed", e);
            return "Error: " + e.getMessage();
        }
    }

    @GetMapping("/test/sync-member-cards")
    public String testSyncMemberCards(@RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "50") int pageSize,
            @RequestParam(required = false) String lastUpdateTime) {
        try {
            logger.info("Starting member card sync test... pageNo={}, pageSize={}, lastUpdate={}",
                    pageNo, pageSize, lastUpdateTime);
            JSONObject resp = unionPayClient.fetchMemberCards(pageNo, pageSize, lastUpdateTime);
            memberSyncService.saveMemberCards(resp.getJSONArray("memberCardList"));
            memberSyncService.recordSyncLog("plat.member.card.sync", pageNo, pageSize,
                    resp.getString("respCode"), resp.getString("respDesc"),
                    "0000".equals(resp.getString("respCode")));
            logger.info("Card sync response: {}", resp.toJSONString());
            return resp.toJSONString();
        } catch (Exception e) {
            logger.error("Card sync failed", e);
            return "Error: " + e.getMessage();
        }
    }

    @GetMapping("/test/sync-face-binds")
    public String testSyncFaceBindings(@RequestParam(defaultValue = "0") int syncType,
            @RequestParam(required = false) String startTime) {
        try {
            logger.info("Starting face binding sync... syncType={}, startTime={}", syncType, startTime);
            JSONObject resp = unionPayClient.fetchFaceBindings(syncType, startTime);
            JSONArray bindList = resp.getJSONArray("bindList");
            JSONArray unbindList = resp.getJSONArray("unbindList");
            memberSyncService.saveFaceBindings(bindList, unbindList);
            memberSyncService.recordSyncLog("plat.face.sync", 0, 0,
                    resp.getString("respCode"), resp.getString("respDesc"),
                    "0000".equals(resp.getString("respCode")));
            logger.info("Face binding response: {}", resp.toJSONString());
            return resp.toJSONString();
        } catch (Exception e) {
            logger.error("Face binding sync failed", e);
            return "Error: " + e.getMessage();
        }
    }

    @GetMapping("/test/download-face")
    public String downloadFace(@RequestParam String cardNo) {
        try {
            logger.info("Downloading face for cardNo={}", cardNo);
            JSONObject resp = unionPayClient.downloadFaceImage(cardNo);
            String pic = resp.getString("pic");
            String picName = resp.getString("picName");
            byte[] imageBytes = memberSyncService.decryptFaceImage(pic);
            String storedPath = memberSyncService.saveFaceImage(cardNo, picName, imageBytes);
            JSONObject result = new JSONObject();
            result.put("storedPath", storedPath);
            result.put("picName", picName);
            result.put("respCode", resp.getString("respCode"));
            result.put("respDesc", resp.getString("respDesc"));
            return result.toJSONString();
        } catch (Exception e) {
            logger.error("Face download failed", e);
            return "Error: " + e.getMessage();
        }
    }

    @PostMapping("/admin/sync/members")
    public String syncMembers(@RequestBody(required = false) JSONObject body) {
        String lastUpdateTime = body == null ? null : body.getString("lastUpdateTime");
        try {
            platformSyncService.syncAllMembers(lastUpdateTime);
            return "{\"status\":\"ok\"}";
        } catch (Exception e) {
            logger.error("Full member sync failed", e);
            return "Error: " + e.getMessage();
        }
    }

    @PostMapping("/admin/sync/member-cards")
    public String syncMemberCards(@RequestBody(required = false) JSONObject body) {
        String lastUpdateTime = body == null ? null : body.getString("lastUpdateTime");
        try {
            platformSyncService.syncAllMemberCards(lastUpdateTime);
            return "{\"status\":\"ok\"}";
        } catch (Exception e) {
            logger.error("Full member card sync failed", e);
            return "Error: " + e.getMessage();
        }
    }

    @PostMapping("/admin/sync/face-binds")
    public String syncFaceBindings(@RequestBody(required = false) JSONObject body) {
        int syncType = body == null ? 0 : body.getIntValue("syncType");
        String startTime = body == null ? null : body.getString("startTime");
        try {
            platformSyncService.syncFaceBindings(syncType, startTime);
            return "{\"status\":\"ok\"}";
        } catch (Exception e) {
            logger.error("Face binding sync failed", e);
            return "Error: " + e.getMessage();
        }
    }

    @PostMapping("/admin/sync/face-image")
    public String syncFaceImage(@RequestBody JSONObject body) {
        String cardNo = body.getString("cardNo");
        if (cardNo == null || cardNo.isEmpty()) {
            return "Error: cardNo is required";
        }
        try {
            String path = platformSyncService.downloadFaceAndStore(cardNo);
            JSONObject result = new JSONObject();
            result.put("status", "ok");
            result.put("storedPath", path);
            return result.toJSONString();
        } catch (Exception e) {
            logger.error("Face image sync failed", e);
            return "Error: " + e.getMessage();
        }
    }

    @PostMapping("/admin/device/push-all")
    public String pushAllMembers(@RequestBody JSONObject body) {
        String topic = body.getString("topic");
        if (topic == null || topic.isEmpty()) {
            return "Error: topic is required";
        }
        boolean published = deviceSyncService.pushAllMembers(topic);
        JSONObject resp = new JSONObject();
        resp.put("status", published ? "ok" : "failed");
        return resp.toJSONString();
    }

    @PostMapping("/admin/device/http-push-all")
    public String httpPushAllMembers(@RequestBody JSONObject body) {
        String deviceIp = body.getString("deviceIp");
        JSONObject resp = deviceHttpSyncService.syncAllToDevice(deviceIp);
        return resp.toJSONString();
    }

    @PostMapping("/admin/device/http-push-member")
    public String httpPushMember(@RequestBody JSONObject body) {
        String deviceIp = body.getString("deviceIp");
        String memberCode = body.getString("memberCode");
        JSONObject resp = deviceHttpSyncService.syncMemberToDevice(deviceIp, memberCode);
        return resp.toJSONString();
    }

    @PostMapping("/device/forbidden-event")
    public String reportForbiddenEvent(@RequestBody JSONObject body) {
        deviceSyncService.recordForbiddenEvent(body);
        JSONObject resp = new JSONObject();
        resp.put("status", "ok");
        return resp.toJSONString();
    }

    @GetMapping("/device/forbidden-event")
    public Object listForbiddenEvents() {
        return deviceSyncService.listForbiddenEvents();
    }

    @PostMapping("/record/upload/online")
    public String uploadRecord(@RequestBody JSONObject body) {
        recordService.handleRecordUpload(body);
        // 返回设备需要的格式
        // 原Node.js: { res: "success" } (假设)
        JSONObject resp = new JSONObject();
        resp.put("code", 0); // 统一为 0
        resp.put("msg", "success");
        resp.put("success", true);
        return resp.toJSONString();
    }

    @PostMapping("/device/sync_person_details")
    public String getPersonDetails(@RequestBody JSONObject body) {
        logger.info("Device requesting person details: {}", body);
        JSONObject resp = deviceSyncService.getPersonDetails(body);
        return resp.toJSONString();
    }

    @PostMapping("/device/notify")
    public String deviceNotify(@RequestBody JSONObject body) {
        logger.info("Device notify: {}", body);
        String devSno = body.getString("dev_sno");
        if (devSno != null) {
            deviceSyncService.registerDevice(devSno);
        }
        
        JSONObject resp = new JSONObject();
        resp.put("code", 0);
        resp.put("msg", "OK");
        resp.put("success", true);

        // 检查是否有待发送给该设备的指令 (Piggyback)
        if (devSno != null) {
            JSONObject command = deviceSyncService.pollPendingCommand(devSno);
            if (command != null) {
                logger.info("Piggybacking command to device {}: {}", devSno, command.toJSONString());
                // 将指令内容合并到响应中
                // 通常设备协议会查看根节点的 method 字段
                resp.putAll(command);
            }
        }
        
        return resp.toJSONString();
    }

    @Value("${server.host-ip:}") 
    private String configHostIp;

    private String getHostIp() {
        if (configHostIp != null && !configHostIp.isEmpty()) {
            return configHostIp;
        }
        try {
            return java.net.InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            logger.error("Failed to get local host address", e);
            return "127.0.0.1";
        }
    }

    @PostMapping("/device/login")
    public String deviceLogin(@RequestBody JSONObject body) {
        recordService.handleDeviceLogin(body);
        String devSno = body.getString("dev_sno");
        if (devSno != null) {
            deviceSyncService.registerDevice(devSno);
        }

        JSONObject resp = new JSONObject();
        // 模拟 Node.js 的返回结构
        resp.put("code", 0); // Node.js 是 0, Java 之前是 200
        resp.put("msg", "登录成功");
        resp.put("success", true);

        resp.put("dev_sno", devSno);
        resp.put("token", "dummy_token_" + System.currentTimeMillis()); // 模拟 Token
        resp.put("expiresAt", System.currentTimeMillis() + 3600000); // 1小时后过期
        
        // 尝试告诉设备使用 HTTP 协议，停止 MQTT 重连 (虽然设备可能不听)
        resp.put("mqProtocol", "HTTP"); 

        // 绝招：让设备连上一个公网可用的 MQTT Broker，把它哄好，不再报错
        JSONObject mqInfo = new JSONObject();
        // 如果是本地运行，这里应该是本机局域网 IP
        mqInfo.put("host", getHostIp()); 
        mqInfo.put("port", 1883);
        mqInfo.put("topic", devSno);
        resp.put("mqinfo", mqInfo);

        String jsonResp = resp.toJSONString();
        logger.info("Sending login response: {}", jsonResp);
        return jsonResp;
    }
}
