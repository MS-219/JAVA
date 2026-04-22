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

    @Autowired
    private com.yinlian.service.WeChatService weChatService;

    @Autowired
    private com.yinlian.repository.MemberRepository memberRepository;

    @Autowired
    private com.yinlian.repository.MemberCardRepository memberCardRepository;

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

    @GetMapping("/dashboard/stats")
    public String getDashboardStats() {
        return recordService.getDashboardData().toJSONString();
    }

    /**
     * 【新增】支持设备和日期筛选的大屏数据接口
     * 
     * @param date   日期 (格式: yyyy-MM-dd)，可选，默认今天
     * @param device 设备代码片段 (如MAC地址后几位: 61:B2)，可选
     */
    @GetMapping("/dashboard/stats/filter")
    public String getDashboardStatsFiltered(
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String device) {
        return recordService.getDashboardDataFiltered(date, device).toJSONString();
    }

    /**
     * 【新增】获取设备列表 (用于前端筛选下拉框)
     */
    @GetMapping("/dashboard/devices")
    public String getDeviceList() {
        return recordService.getDeviceList().toJSONString();
    }

    /**
     * 【新增】按人名搜索通行记录（支持日期过滤）
     * 
     * @param name      人名关键词
     * @param startDate 开始日期 (yyyy-MM-dd)，可选
     * @param endDate   结束日期 (yyyy-MM-dd)，可选
     */
    @GetMapping("/dashboard/search")
    public String searchByName(
            @RequestParam String name,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return recordService.searchByName(name, startDate, endDate).toJSONString();
    }

    /**
     * 【新增】分页查询大屏数据
     * 
     * @param startDate 开始日期 (yyyy-MM-dd)
     * @param endDate   结束日期 (yyyy-MM-dd)
     * @param device    设备代码
     * @param page      页码 (0开始)
     * @param size      每页大小 (默认20)
     */
    @GetMapping("/dashboard/stats/paged")
    public String getDashboardStatsPaged(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String device,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return recordService.getDashboardDataPaged(startDate, endDate, device, page, size).toJSONString();
    }

    /**
     * 【新增】导出通行记录为 CSV（支持分页）
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @param device    设备代码
     * @param name      人名搜索
     * @param page      页码（可选，不传则导出全部）
     * @param size      每页大小（可选，不传则导出全部）
     */
    @GetMapping("/dashboard/export")
    public void exportRecords(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String device,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            javax.servlet.http.HttpServletResponse response) {
        try {
            response.setContentType("text/csv;charset=UTF-8");

            // 根据是否有分页参数生成不同的文件名
            String filename = "access_records";
            if (startDate != null && endDate != null) {
                filename += "_" + startDate + "_to_" + endDate;
            } else if (startDate != null) {
                filename += "_" + startDate;
            } else {
                filename += "_" + java.time.LocalDate.now().toString();
            }
            if (page != null && size != null) {
                filename += "_page" + (page + 1) + "_size" + size;
            }
            filename += ".csv";

            response.setHeader("Content-Disposition", "attachment; filename=" + filename);

            java.io.OutputStream os = response.getOutputStream();

            // 写入 BOM 以支持 Excel 正确显示中文
            os.write(new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF });

            // CSV 头
            os.write("部门,姓名,时间,进出方向,位置/设备,类型\n".getBytes("UTF-8"));

            // 获取数据（传递分页参数）
            String csvData = recordService.exportRecordsAsCsv(startDate, endDate, device, name, page, size);
            os.write(csvData.getBytes("UTF-8"));
            os.flush();
        } catch (Exception e) {
            logger.error("导出记录失败", e);
        }
    }

    @PostMapping("/record/upload/online")
    public String uploadRecord(@RequestBody JSONObject body) {
        recordService.handleRecordUpload(body);
        return handleUploadLogic(body);
    }

    // 【新增】兼容设备配置的 /device/record 路径
    @PostMapping("/device/record")
    public String uploadRecordCompat(@RequestBody JSONObject body) {
        recordService.handleRecordUpload(body);
        return handleUploadLogic(body);
    }

    // 【新增】兼容设备配置的 /device/heartbeat 路径
    @PostMapping("/device/heartbeat")
    public String deviceHeartbeat(@RequestBody JSONObject body) {
        // String devSno = body.getString("dev_sno");
        // if (devSno != null) {
        //     deviceSyncService.recordDeviceHeartbeat(devSno);
        // }

        JSONObject resp = new JSONObject();
        resp.put("result", 1);
        resp.put("success", true);
        resp.put("code", 0);
        return resp.toJSONString();
    }

    // 提取公共逻辑，方便 /device/record 复用
    private String handleUploadLogic(JSONObject body) {
        // 【新增】提取设备SN并记录上传状态
        String devSno = body.getString("dev_sno");
        if (devSno == null)
            devSno = body.getString("device_sno");
        if (devSno == null)
            devSno = body.getString("deviceSn");
        final String finalDevSno = devSno;

        // 触发微信推送 (异步执行，避免阻塞设备响应)
        try {
            new Thread(() -> {
                try {
                    // 【调试】打印设备上传的原始数据
                    logger.info("【调试】设备上传原始数据: {}", body.toJSONString());

                    // 尝试多种字段获取 memberId
                    String memberId = body.getString("person_id");
                    String time = body.getString("time");
                    String tryField = "person_id";

                    if (memberId == null || memberId.isEmpty()) {
                        memberId = body.getString("member_code");
                        tryField = "member_code";
                    }
                    if (memberId == null || memberId.isEmpty()) {
                        memberId = body.getString("userid");
                        tryField = "userid";
                    }
                    if (memberId == null || memberId.isEmpty()) {
                        memberId = body.getString("employee_number");
                        tryField = "employee_number";
                    }
                    if (memberId == null || memberId.isEmpty()) {
                        memberId = body.getString("id"); // 有些设备可能是 id
                        tryField = "id";
                    }

                    if (memberId != null && !memberId.isEmpty()) {
                        com.yinlian.model.MemberEntity member = memberRepository.findByMemberCode(memberId);

                        if (member == null) {
                            // 尝试用 memberId 当作 cardNo 查
                            java.util.List<com.yinlian.model.MemberCardEntity> cards = memberCardRepository.findByCardNo(memberId);
                            if (cards != null && !cards.isEmpty()) {
                                String realMemberCode = cards.get(0).getMemberCode();
                                member = memberRepository.findByMemberCode(realMemberCode);
                            }
                        }

                        if (member != null) {
                            // 【新增】记录设备上传状态
                            // if (finalDevSno != null) {
                            //     deviceSyncService.recordDeviceUpload(finalDevSno, member.getMemberName());
                            // }

                            String openId = member.getOpenId();
                            if (openId != null && !openId.isEmpty()) {
                                // 获取模版需要的字段
                                String cardNo = member.getMemberCode(); // 学生卡号
                                String className = member.getDepartName(); // 班级
                                String schoolName = "圣唐王府教育";

                                // 构造显示名称
                                String displayName = member.getMemberName() + " 刷脸通过";

                                logger.info("【调试】准备发送微信消息给 openId={}, studentName={}, time={}", openId,
                                        member.getMemberName(), time);

                                weChatService.sendTemplateMessage(openId, displayName, time, cardNo, className,
                                        schoolName);

                                logger.info("【成功】已发送微信通知给 {}", member.getMemberName());
                            } else {
                                logger.warn("【调试】会员 {} 未绑定服务号 (openId 为空)", member.getMemberName());
                            }
                        } else {
                            logger.warn("【调试】未找到 memberCode={} 对应的会员记录", memberId);
                        }
                    } else {
                        logger.warn("【调试】设备上传数据中没有可识别的人员ID字段");
                    }
                } catch (Exception e) {
                    logger.error("【错误】推送微信消息失败", e);
                }
            }).start();
        } catch (Exception e) {
            logger.error("【错误】启动推送线程失败", e);
        }

        JSONObject resp = new JSONObject();
        resp.put("result", 1);
        resp.put("success", true);
        resp.put("code", 0);
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
    public String deviceLogin(@RequestBody JSONObject body, javax.servlet.http.HttpServletRequest request) {
        recordService.handleDeviceLogin(body);
        String devSno = body.getString("dev_sno");
        String deviceIp = request.getRemoteAddr();
        if (devSno != null) {
            deviceSyncService.registerDevice(devSno);
            // 【新增】记录设备登录状态
            // deviceSyncService.recordDeviceLogin(devSno, deviceIp);
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

    // 【新增】设备状态查询接口
    @GetMapping("/debug/device-status")
    public String getDeviceStatus() {
        JSONObject result = new JSONObject();
        // result.put("devices", deviceSyncService.getAllDeviceStatus());
        // result.put("activeDevices", deviceSyncService.getActiveDevices());
        result.put("queryTime", java.time.LocalDateTime.now().toString());
        return result.toJSONString();
    }
}
