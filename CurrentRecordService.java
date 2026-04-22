package com.yinlian.service;

import com.alibaba.fastjson.JSONArray;
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

        // 【增强】从多个可能的字段中提取设备标识
        // 常见字段: Mac_addr, dev_sno, deviceCode, device_sn, sn, DeviceSN, deviceId,
        // terminal_id
        fp.deviceCode = StringUtils.defaultString(StringUtils.firstNonBlank(
                record.getString("Mac_addr"),
                record.getString("dev_sno"),
                record.getString("deviceCode"),
                record.getString("device_sn"),
                record.getString("DeviceSN"),
                record.getString("sn"),
                record.getString("deviceId"),
                record.getString("terminal_id"),
                record.getString("terminalId"),
                record.getString("devId")), "").trim();

        fp.personCode = StringUtils.defaultString(StringUtils.firstNonBlank(
                record.getString("userid"),
                record.getString("employee_number"),
                record.getString("person_id"),
                record.getString("userId"),
                record.getString("id"), // 添加 id 字段支持
                record.getString("icNum"),
                record.getString("ic_card")), "").trim();

        if (record.containsKey("time")) {
            fp.captureTime = record.getString("time");
        } else if (record.containsKey("capture_time")) {
            fp.captureTime = record.getString("capture_time");
        } else {
            fp.captureTime = "";
        }

        // 【调试日志】打印提取的设备标识
        logger.info("【设备识别】提取结果: deviceCode={}, personCode={}, time={}",
                fp.deviceCode.isEmpty() ? "(空)" : fp.deviceCode,
                fp.personCode.isEmpty() ? "(空)" : fp.personCode,
                fp.captureTime);

        return fp;
    }

    public boolean isDuplicate(JSONObject record) {
        Fingerprint fp = extractFingerprint(record);
        if (fp.deviceCode.isEmpty() || fp.personCode.isEmpty() || fp.captureTime.isEmpty()) {
            return false;
        }

        // 使用数据库直接查询，效率更高
        return repository.isDuplicate(fp.deviceCode, fp.personCode, fp.captureTime);
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
        } else if (StringUtils.isNotBlank(record.getString("QRcode"))
                || StringUtils.isNotBlank(record.getString("qr"))) {
            unionpayPayload.put("qrCode",
                    StringUtils.firstNonBlank(record.getString("QRcode"), record.getString("qr")));
        } else if (StringUtils.isNotBlank(record.getString("icNum"))
                || StringUtils.isNotBlank(record.getString("ic_card"))) {
            unionpayPayload.put("cardNo",
                    StringUtils.firstNonBlank(record.getString("icNum"), record.getString("ic_card")));
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

                // 更新数据库中的状态
                Long entityId = record.getLong("__entityId");
                if (entityId != null) {
                    repository.updateStatus(entityId, record.getString("status"));
                }
            }
        }
    }

    public void handleDeviceLogin(JSONObject data) {
        logger.info("Device login: {}", data);
        // Store device info if needed
    }

    @Autowired
    private com.yinlian.repository.MemberCardRepository memberCardRepository;

    @Autowired
    private com.yinlian.repository.MemberRepository memberRepository;

    public JSONObject getDashboardData() {
        JSONObject data = new JSONObject();

        // 1. Basic Stats
        int totalRecords = repository.countRecords();
        data.put("totalRecords", totalRecords);
        // ... (existing code)
        data.put("todayCount", totalRecords);

        // 2. Latest Records (Enriched)
        List<JSONObject> rawList = repository.getLatestRecords(20);
        List<JSONObject> enrichedList = new java.util.ArrayList<>();

        for (JSONObject r : rawList) {
            JSONObject item = new JSONObject();
            String personCode = r.getString("__personCode");
            String time = r.getString("__captureTime");

            item.put("time", time);
            item.put("device", r.getString("__deviceCode"));
            item.put("type", "face"); // default

            // Enrich with Name
            String displayName = "未知";
            String department = "";
            String memberType = "";
            String userType = "guest"; // guest, student, teacher

            if (StringUtils.isNotBlank(personCode)) {
                com.yinlian.model.MemberEntity member = memberRepository.findById(personCode).orElse(null);

                if (member == null) {
                    List<com.yinlian.model.MemberCardEntity> cards = memberCardRepository.findByCardNo(personCode);
                    if (cards != null && !cards.isEmpty()) {
                        String realMemberCode = cards.get(0).getMemberCode();
                        member = memberRepository.findById(realMemberCode).orElse(null);
                    }
                }

                if (member != null) {
                    displayName = member.getMemberName();
                    department = member.getDepartName();
                    memberType = member.getMemberTypeName();
                    userType = "member";
                } else {
                    displayName = "陌生人 (" + personCode + ")";
                }
            }

            item.put("name", displayName);
            item.put("department", department);
            item.put("memberType", memberType);
            item.put("userType", userType);

            // Resolve Device Name - 优先使用设备代码映射，跳过无效的默认值
            String deviceName = "";
            String devCode = r.getString("__deviceCode");

            // 1. 优先使用设备代码映射（更可靠）
            if (StringUtils.isNotBlank(devCode)) {
                deviceName = getDeviceName(devCode);
            }

            // 2. 如果代码映射返回的是"未知设备"，尝试使用设备自带名称
            if (deviceName.isEmpty() || deviceName.equals("未知设备") || deviceName.startsWith("设备 ")) {
                String rawDeviceName = r.getString("devicename");
                String rawLocation = r.getString("location");

                // 跳过无效的默认值
                if (StringUtils.isNotBlank(rawDeviceName)
                        && !rawDeviceName.equalsIgnoreCase("Terminal")
                        && !rawDeviceName.equalsIgnoreCase("Unknown")
                        && !rawDeviceName.equalsIgnoreCase("Default")) {
                    deviceName = rawDeviceName;
                } else if (StringUtils.isNotBlank(rawLocation)
                        && !rawLocation.equalsIgnoreCase("Terminal")
                        && !rawLocation.equalsIgnoreCase("Unknown")) {
                    deviceName = rawLocation;
                }
            }

            // 3. 最终兜底
            if (StringUtils.isBlank(deviceName)) {
                deviceName = "未知设备";
            }

            // 【调试】打印设备名称解析结果
            logger.debug("【大屏】设备名称解析: devCode={} -> deviceName={}", devCode, deviceName);

            item.put("deviceName", deviceName);

            // 尝试获取人脸照片 URL (兼容多种设备字段名)
            String capturePath = "";

            // 1. 先检查是否有 Base64 编码的图片 (设备协议标准字段)
            if (r.containsKey("face_base64") && StringUtils.isNotBlank(r.getString("face_base64"))) {
                String base64Data = r.getString("face_base64");
                // 如果已经是 data URI 格式，直接使用；否则添加前缀
                if (base64Data.startsWith("data:image")) {
                    capturePath = base64Data;
                } else {
                    capturePath = "data:image/jpeg;base64," + base64Data;
                }
            } else if (r.containsKey("templatePhoto") && StringUtils.isNotBlank(r.getString("templatePhoto"))) {
                String base64Data = r.getString("templatePhoto");
                if (base64Data.startsWith("data:image")) {
                    capturePath = base64Data;
                } else {
                    capturePath = "data:image/jpeg;base64," + base64Data;
                }
            } else {
                // 2. 尝试 URL 字段 (兼容其他设备)
                String[] possibleFields = { "path", "img_url", "pic", "photo", "faceImg",
                        "face_url", "picUrl", "capture_url", "image",
                        "imgPath", "facePath", "faceUrl", "photoUrl" };
                for (String field : possibleFields) {
                    if (r.containsKey(field) && StringUtils.isNotBlank(r.getString(field))) {
                        capturePath = r.getString(field);
                        break;
                    }
                }
            }

            // 用户要求：只能要这个识别记录（抓拍图）
            item.put("avatar", capturePath);

            enrichedList.add(item);
        }
        data.put("recentRecords", enrichedList);

        return data;
    }

    private String getDeviceName(String code) {
        if (StringUtils.isBlank(code))
            return "未知设备";

        // 【调试】打印原始设备代码
        logger.info("【设备映射】原始代码: {}", code);

        // 转大写便于比较
        String codeUpper = code.toUpperCase();

        // =======================================================
        // 设备MAC地址 -> 位置名称 映射表
        // 格式: 0A:0C:E7:5A:61:XX (匹配最后两位即可)
        // =======================================================

        // 大门东 (3台)
        if (code.endsWith("45") || code.contains("10.20.250.45") || codeUpper.contains("61:B2"))
            return "大门东-中间 出口"; // MAC: 0A:0C:E7:5A:61:B2
        if (code.endsWith("43") || code.contains("10.20.250.43") || codeUpper.contains("61:CD"))
            return "大门东-东侧 入口"; // MAC: 0A:0C:E7:5A:61:CD
        if (code.endsWith("44") || code.contains("10.20.250.44") || codeUpper.contains("61:A5"))
            return "大门东-西侧 入口"; // MAC: 0A:0C:E7:5A:61:A5

        // 大门西 (3台)
        if (code.endsWith("40") || code.contains("10.20.250.40") || codeUpper.contains("61:D2"))
            return "大门西-东侧 入口"; // MAC: 0A:0C:E7:5A:61:D2
        if (code.endsWith("37") || code.contains("10.20.250.37") || codeUpper.contains("61:CF"))
            return "大门西-西侧 出口"; // MAC: 0A:0C:E7:5A:61:CF
        if (code.endsWith("42") || code.contains("10.20.250.42") || codeUpper.contains("61:D5"))
            return "大门西-中间 入口"; // MAC: 0A:0C:E7:5A:61:D5

        // Fallback: 显示简短编码
        if (code.length() > 6) {
            return "设备 " + code.substring(code.length() - 6);
        }
        return "设备 " + code;
    }

    /**
     * 获取带筛选条件的大屏数据
     * 
     * @param date       日期 (yyyy-MM-dd)，为空则查今天
     * @param deviceCode 设备代码片段，为空则不筛选
     */
    public JSONObject getDashboardDataFiltered(String date, String deviceCode) {
        JSONObject data = new JSONObject();

        // 1. 统计筛选后的通行人次
        long count = repository.countFilteredRecords(date, deviceCode);
        data.put("todayCount", count);
        data.put("totalRecords", count);

        // 2. 获取筛选后的记录
        List<JSONObject> rawList = repository.getFilteredRecords(date, deviceCode, 50);
        List<JSONObject> enrichedList = new java.util.ArrayList<>();

        for (JSONObject r : rawList) {
            JSONObject item = enrichRecord(r);
            enrichedList.add(item);
        }
        data.put("recentRecords", enrichedList);

        // 3. 返回可用的设备列表 (用于前端下拉框)
        data.put("deviceList", getDeviceList());

        return data;
    }

    /**
     * 获取分页数据（支持日期范围）
     * 
     * @param startDate  开始日期 (yyyy-MM-dd)
     * @param endDate    结束日期 (yyyy-MM-dd)
     * @param deviceCode 设备代码
     * @param page       页码 (0开始)
     * @param size       每页大小
     */
    public JSONObject getDashboardDataPaged(String startDate, String endDate, String deviceCode, int page, int size) {
        JSONObject data = new JSONObject();

        // 获取分页数据（支持日期范围）
        com.yinlian.repository.RecordRepository.PageResult pageResult = repository.getPagedRecordsWithDateRange(
                startDate, endDate, deviceCode, page, size);

        // 丰富记录信息
        List<JSONObject> enrichedList = new java.util.ArrayList<>();
        for (JSONObject r : pageResult.records) {
            JSONObject item = enrichRecord(r);
            enrichedList.add(item);
        }

        data.put("recentRecords", enrichedList);
        data.put("todayCount", pageResult.totalElements);
        data.put("totalElements", pageResult.totalElements);
        data.put("totalPages", pageResult.totalPages);
        data.put("currentPage", pageResult.currentPage);
        data.put("pageSize", pageResult.pageSize);
        data.put("deviceList", getDeviceList());

        return data;
    }

    /**
     * 获取设备列表 (用于前端筛选下拉框)
     */
    public JSONArray getDeviceList() {
        JSONArray list = new JSONArray();

        // 固定的6台设备
        String[][] devices = {
                { "61:B2", "大门东-中间 出口", "10.20.250.45" },
                { "61:CD", "大门东-东侧 入口", "10.20.250.43" },
                { "61:A5", "大门东-西侧 入口", "10.20.250.44" },
                { "61:D2", "大门西-东侧 入口", "10.20.250.40" },
                { "61:CF", "大门西-西侧 出口", "10.20.250.37" },
                { "61:D5", "大门西-中间 入口", "10.20.250.42" }
        };

        for (String[] dev : devices) {
            JSONObject item = new JSONObject();
            item.put("code", dev[0]);
            item.put("name", dev[1]);
            item.put("ip", dev[2]);
            list.add(item);
        }

        return list;
    }

    /**
     * 丰富单条记录（抽取公共逻辑）
     */
    private JSONObject enrichRecord(JSONObject r) {
        JSONObject item = new JSONObject();
        String personCode = r.getString("__personCode");
        String time = r.getString("__captureTime");

        item.put("time", time);
        item.put("device", r.getString("__deviceCode"));
        item.put("type", "face");

        // Enrich with Name
        String displayName = "未知";
        String department = "";
        String memberType = "";
        String userType = "guest";

        if (StringUtils.isNotBlank(personCode)) {
            com.yinlian.model.MemberEntity member = memberRepository.findById(personCode).orElse(null);

            if (member == null) {
                List<com.yinlian.model.MemberCardEntity> cards = memberCardRepository.findByCardNo(personCode);
                if (cards != null && !cards.isEmpty()) {
                    String realMemberCode = cards.get(0).getMemberCode();
                    member = memberRepository.findById(realMemberCode).orElse(null);
                }
            }

            if (member != null) {
                displayName = member.getMemberName();
                department = member.getDepartName();
                memberType = member.getMemberTypeName();
                userType = "member";
            } else {
                displayName = "陌生人 (" + personCode + ")";
            }
        }

        item.put("name", displayName);
        item.put("department", department);
        item.put("memberType", memberType);
        item.put("userType", userType);

        // Resolve Device Name
        String deviceName = "";
        String devCode = r.getString("__deviceCode");

        if (StringUtils.isNotBlank(devCode)) {
            deviceName = getDeviceName(devCode);
        }

        if (deviceName.isEmpty() || deviceName.equals("未知设备") || deviceName.startsWith("设备 ")) {
            String rawDeviceName = r.getString("devicename");
            String rawLocation = r.getString("location");

            if (StringUtils.isNotBlank(rawDeviceName)
                    && !rawDeviceName.equalsIgnoreCase("Terminal")
                    && !rawDeviceName.equalsIgnoreCase("Unknown")
                    && !rawDeviceName.equalsIgnoreCase("Default")) {
                deviceName = rawDeviceName;
            } else if (StringUtils.isNotBlank(rawLocation)
                    && !rawLocation.equalsIgnoreCase("Terminal")
                    && !rawLocation.equalsIgnoreCase("Unknown")) {
                deviceName = rawLocation;
            }
        }

        if (StringUtils.isBlank(deviceName)) {
            deviceName = "未知设备";
        }

        item.put("deviceName", deviceName);

        // Avatar
        String capturePath = "";
        if (r.containsKey("face_base64") && StringUtils.isNotBlank(r.getString("face_base64"))) {
            String base64Data = r.getString("face_base64");
            if (base64Data.startsWith("data:image")) {
                capturePath = base64Data;
            } else {
                capturePath = "data:image/jpeg;base64," + base64Data;
            }
        } else if (r.containsKey("templatePhoto") && StringUtils.isNotBlank(r.getString("templatePhoto"))) {
            String base64Data = r.getString("templatePhoto");
            if (base64Data.startsWith("data:image")) {
                capturePath = base64Data;
            } else {
                capturePath = "data:image/jpeg;base64," + base64Data;
            }
        } else {
            String[] possibleFields = { "path", "img_url", "pic", "photo", "faceImg",
                    "face_url", "picUrl", "capture_url", "image",
                    "imgPath", "facePath", "faceUrl", "photoUrl" };
            for (String field : possibleFields) {
                if (r.containsKey(field) && StringUtils.isNotBlank(r.getString(field))) {
                    capturePath = r.getString(field);
                    break;
                }
            }
        }

        item.put("avatar", capturePath);

        return item;
    }

    /**
     * 按人名搜索通行记录（支持日期范围过滤）
     * 
     * @param name      人名关键词
     * @param startDate 开始日期 (yyyy-MM-dd)，可选
     * @param endDate   结束日期 (yyyy-MM-dd)，可选
     * @return 搜索结果
     */
    public JSONObject searchByName(String name, String startDate, String endDate) {
        JSONObject result = new JSONObject();

        if (StringUtils.isBlank(name)) {
            result.put("success", false);
            result.put("message", "请输入搜索关键词");
            result.put("records", new JSONArray());
            return result;
        }

        // 1. 先按人名模糊搜索会员
        List<com.yinlian.model.MemberEntity> members = memberRepository.findByMemberNameContaining(name);

        if (members.isEmpty()) {
            result.put("success", true);
            result.put("message", "未找到匹配的人员");
            result.put("records", new JSONArray());
            result.put("count", 0);
            return result;
        }

        // 2. 获取这些会员的 memberCode 列表
        List<String> memberCodes = new java.util.ArrayList<>();
        for (com.yinlian.model.MemberEntity m : members) {
            memberCodes.add(m.getMemberCode());
        }

        // 3. 按 memberCode + 日期范围 搜索通行记录
        List<JSONObject> rawList;
        if (StringUtils.isNotBlank(startDate) || StringUtils.isNotBlank(endDate)) {
            // 【修复】使用带日期过滤的搜索方法
            rawList = repository.searchByPersonCodesWithDateRange(memberCodes, startDate, endDate, 100);
        } else {
            // 向后兼容：无日期参数时查全部
            rawList = repository.searchByPersonCodes(memberCodes, 50);
        }

        // 4. 丰富记录信息
        JSONArray enrichedList = new JSONArray();
        for (JSONObject r : rawList) {
            JSONObject item = enrichRecord(r);
            enrichedList.add(item);
        }

        result.put("success", true);
        result.put("message", "搜索完成");
        result.put("records", enrichedList);
        result.put("count", enrichedList.size());
        result.put("matchedMembers", members.size());

        return result;
    }

    /**
     * 导出通行记录为 CSV 格式
     * 
     * @param date       日期
     * @param deviceCode 设备代码
     * @param name       人名搜索（可选）
     */
    public String exportRecordsAsCsv(String date, String deviceCode, String name) {
        StringBuilder csv = new StringBuilder();

        List<JSONObject> rawList;

        // 如果有人名搜索
        if (StringUtils.isNotBlank(name)) {
            List<com.yinlian.model.MemberEntity> members = memberRepository.findByMemberNameContaining(name);
            if (members.isEmpty()) {
                return "";
            }
            List<String> memberCodes = new java.util.ArrayList<>();
            for (com.yinlian.model.MemberEntity m : members) {
                memberCodes.add(m.getMemberCode());
            }
            rawList = repository.searchByPersonCodes(memberCodes, 1000);
        } else if (StringUtils.isBlank(date)) {
            // 如果没有日期参数，查询所有记录（最近1000条）
            rawList = repository.getLatestRecords(1000);
        } else {
            // 按日期和设备筛选
            rawList = repository.getFilteredRecords(date, deviceCode, 1000);
        }

        // 生成 CSV 数据
        for (JSONObject r : rawList) {
            JSONObject item = enrichRecord(r);

            String time = item.getString("time");
            String personName = item.getString("name");
            String department = item.getString("department");
            String deviceName = item.getString("deviceName");
            String userType = getTypeName(item.getString("userType"));

            // 从 deviceName 中拆分位置和进出方向
            String location = deviceName;
            String direction = "";
            if (deviceName != null && (deviceName.endsWith(" 入口") || deviceName.endsWith(" 出口"))) {
                int lastSpace = deviceName.lastIndexOf(' ');
                location = deviceName.substring(0, lastSpace);
                direction = deviceName.substring(lastSpace + 1);
            }

            // 转义 CSV 特殊字符
            csv.append(escapeCsv(department)).append(",");
            csv.append(escapeCsv(personName)).append(",");
            csv.append(escapeCsv(time)).append(",");
            csv.append(escapeCsv(direction)).append(",");
            csv.append(escapeCsv(location)).append(",");
            csv.append(escapeCsv(userType)).append("\n");
        }

        return csv.toString();
    }

    private String escapeCsv(String value) {
        if (value == null)
            return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private String getTypeName(String type) {
        if ("teacher".equals(type))
            return "教职工";
        if ("student".equals(type))
            return "学生";
        if ("member".equals(type))
            return "会员";
        return "访客";
    }
}
