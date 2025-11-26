package com.yinlian.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.yinlian.model.MemberCardEntity;
import com.yinlian.model.MemberEntity;
import com.yinlian.model.MemberFaceEntity;
import com.yinlian.repository.MemberCardRepository;
import com.yinlian.repository.MemberFaceRepository;
import com.yinlian.repository.MemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.io.File;
import org.springframework.beans.factory.annotation.Value;

/**
 * 负责把会员/卡片/人脸数据下发到设备，并缓存禁考勤事件。
 */
@Service
public class DeviceSyncService {

    private static final Logger logger = LoggerFactory.getLogger(DeviceSyncService.class);

    private final MemberRepository memberRepository;
    private final MemberCardRepository memberCardRepository;
    private final MemberFaceRepository memberFaceRepository;
    private final MqttService mqttService;

    @Value("${server.port:5389}")
    private int serverPort;

    // 不再写死 HOST_IP，而是动态获取或配置
    // private static final String HOST_IP = "47.121.128.129";
    @Value("${server.host-ip:}") 
    private String configHostIp;

    private final CopyOnWriteArrayList<JSONObject> forbiddenEvents = new CopyOnWriteArrayList<>();
    private final java.util.Set<String> activeDevices = java.util.concurrent.ConcurrentHashMap.newKeySet();
    // 存储待发送给设备的指令: Key=DevSno, Value=CommandJson
    private final java.util.concurrent.ConcurrentHashMap<String, JSONObject> pendingCommands = new java.util.concurrent.ConcurrentHashMap<>();

    public DeviceSyncService(MemberRepository memberRepository,
                             MemberCardRepository memberCardRepository,
                             MemberFaceRepository memberFaceRepository,
                             MqttService mqttService) {
        this.memberRepository = memberRepository;
        this.memberCardRepository = memberCardRepository;
        this.memberFaceRepository = memberFaceRepository;
        this.mqttService = mqttService;
    }

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

    public void registerDevice(String devSno) {
        if (devSno != null && !devSno.isEmpty()) {
            if (activeDevices.add(devSno)) {
                logger.info("New device registered: {}", devSno);
            }
        }
    }

    public java.util.Set<String> getActiveDevices() {
        return activeDevices;
    }

    /**
     * 获取并移除待发送给设备的指令（如果存在）
     */
    public JSONObject pollPendingCommand(String devSno) {
        return pendingCommands.remove(devSno);
    }

    /**
     * 构建单个会员的完整快照（含卡信息、人脸信息）。
     */
    public JSONObject buildMemberSnapshot(MemberEntity member) {
        JSONObject payload = new JSONObject();
        payload.put("memberCode", member.getMemberCode());
        payload.put("memberName", member.getMemberName());
        payload.put("mobileNo", member.getMobileNo());
        payload.put("state", member.getState());
        payload.put("deleted", member.getDeleted());
        payload.put("expiryDate", member.getExpiryDate() != null ? member.getExpiryDate().toString() : null);

        JSONArray cardArray = new JSONArray();
        List<MemberCardEntity> cards = memberCardRepository.findByMemberCode(member.getMemberCode());
        for (MemberCardEntity card : cards) {
            JSONObject cardObj = new JSONObject();
            cardObj.put("cardNo", card.getCardNo());
            cardObj.put("cardCode", card.getCardCode());
            cardObj.put("enableState", card.getEnableState());
            cardObj.put("deleted", card.getDeleted());
            cardObj.put("faceBind", card.getFaceBind());
            MemberFaceEntity faceEntity = memberFaceRepository.findFirstByMemberCode(member.getMemberCode());
            if (faceEntity != null) {
                cardObj.put("faceCode", faceEntity.getFaceCode());
                cardObj.put("faceImagePath", faceEntity.getImagePath());
            }
            cardArray.add(cardObj);
        }
        payload.put("cards", cardArray);

        return payload;
    }

    /**
     * 触发全量同步：直接通过 MQTT 推送指令给设备
     */
    public boolean pushAllMembers(String topic) {
        // topic 在这里即 devSno
        List<MemberEntity> allMembers = memberRepository.findAll();
        
        // 暂时去掉图片过滤，强行推送，以便调试
        List<MemberEntity> membersToPush = allMembers;

        int total = membersToPush.size();
        int batchSize = 1; // 极简模式：每次只推 1 个
        boolean success = true;

        logger.info("Starting sync for all members (forcing push with fallback image). Total: {}", total);

        // 临时修改：只推送前 1 个批次 (1人) 用于测试照片下发是否成功
        int testLimit = 1; 
        int loopEnd = Math.min(total, testLimit);

        for (int i = 0; i < loopEnd; i += batchSize) {
            int end = Math.min(i + batchSize, total);
            List<MemberEntity> batch = membersToPush.subList(i, end);
            List<String> personIds = batch.stream()
                    .map(MemberEntity::getMemberCode)
                    .collect(Collectors.toList());

            JSONObject message = new JSONObject();
            message.put("method", "sync_person");

            JSONObject data = new JSONObject();
            String baseUrl = "http://" + getHostIp() + ":" + serverPort;
            data.put("path", baseUrl + "/device/sync_person_details");

            JSONObject pathParams = new JSONObject();
            pathParams.put("dev_sno", topic);
            pathParams.put("limit", batchSize);
            pathParams.put("offset", i);
            pathParams.put("total", total);
            pathParams.put("person_list", personIds);
            pathParams.put("person_type", "4");

            data.put("path_params", pathParams);
            message.put("data", data);

            message.put("notify", baseUrl + "/device/notify");
            message.put("params", new JSONObject());

            logger.info("Sending sync_person command to topic {}: offset={}", topic, i);
            if (!mqttService.publish(topic, message.toJSONString())) {
                success = false;
            }
            
            // 关键修改：大幅增加延时，防止瞬间发送大量消息导致设备崩溃或断开
            try {
                Thread.sleep(1000); // 每个批次间隔 1秒
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        return success;
    }

    public JSONObject getPersonDetails(JSONObject requestBody) {
        JSONObject pathParams;
        if (requestBody.containsKey("data") && requestBody.getJSONObject("data").containsKey("path_params")) {
            pathParams = requestBody.getJSONObject("data").getJSONObject("path_params");
        } else if (requestBody.containsKey("path_params")) {
            pathParams = requestBody.getJSONObject("path_params");
        } else {
             pathParams = requestBody;
        }

        JSONArray personIds = pathParams.getJSONArray("person_list");
        if (personIds == null) personIds = new JSONArray();
        
        JSONArray personList = new JSONArray();
        String baseUrl = "http://" + getHostIp() + ":" + serverPort;

        // 兜底图片文件名 (极简测试)
        String fallbackImage = "test.jpeg";

        for (int i = 0; i < personIds.size(); i++) {
            String id = personIds.getString(i);
            MemberEntity member = memberRepository.findById(id).orElse(null);
            if (member == null) continue;

            JSONObject p = new JSONObject();
            p.put("person_id", member.getMemberCode());
            p.put("person_name", member.getMemberName());
            p.put("person_type", "4");
            p.put("sex", member.getSex() != null ? member.getSex() : 0);
            p.put("id_card", member.getCertNo());

            MemberCardEntity card = memberCardRepository.findFirstByMemberCode(id);
            if (card != null) {
                p.put("ic_card", card.getCardNo()); 
            } else {
                 p.put("ic_card", "");
            }

            MemberFaceEntity face = memberFaceRepository.findFirstByMemberCode(id);
            String imgUrl = null;
            if (face != null && face.getImagePath() != null && !face.getImagePath().isEmpty()) {
                File f = new File(face.getImagePath());
                String filename = f.getName();
                imgUrl = baseUrl + "/device/faces/" + filename;
            } else {
                // 如果没有关联图片，使用兜底图片 (仅供测试)
                logger.warn("No face record for member {}, using fallback image: {}", id, fallbackImage);
                imgUrl = baseUrl + "/device/faces/" + fallbackImage;
            }

            if (imgUrl != null) {
                JSONArray imgs = new JSONArray();
                imgs.add(imgUrl);
                p.put("templateImgUrl", imgs);
            } else {
                p.put("templateImgUrl", new JSONArray());
            }
            
            p.put("throughDateFrom", "");
            p.put("throughMomentFrom", "");
            p.put("throughDateTo", "");
            p.put("throughMomentTo", "");

            personList.add(p);
        }

        JSONObject response = new JSONObject();
        response.put("code", 0);
        response.put("msg", "OK");
        response.put("success", true);
        if (pathParams.containsKey("dev_sno")) {
            response.put("dev_sno", pathParams.getString("dev_sno"));
        }
        response.put("person_list", personList);
        return response;
    }

    /**
     * 记录禁考勤事件，后续可供平台或人工分析。
     */
    public void recordForbiddenEvent(JSONObject event) {
        event.put("receivedAt", LocalDateTime.now().toString());
        forbiddenEvents.add(event);
        logger.info("Forbidden attendance recorded: {}", event);
    }

    public List<JSONObject> listForbiddenEvents() {
        return new java.util.ArrayList<>(forbiddenEvents);
    }

    public void clearForbiddenEvents() {
        forbiddenEvents.clear();
    }
}

