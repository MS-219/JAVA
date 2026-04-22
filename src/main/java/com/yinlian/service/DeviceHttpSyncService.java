package com.yinlian.service;

import com.alibaba.fastjson.JSONObject;
import com.yinlian.model.MemberCardEntity;
import com.yinlian.model.MemberEntity;
import com.yinlian.model.MemberFaceEntity;
import com.yinlian.repository.MemberCardRepository;
import com.yinlian.repository.MemberFaceRepository;
import com.yinlian.repository.MemberRepository;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class DeviceHttpSyncService {

    private static final Logger logger = LoggerFactory.getLogger(DeviceHttpSyncService.class);
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

    private final MemberRepository memberRepository;
    private final MemberCardRepository memberCardRepository;
    private final MemberFaceRepository memberFaceRepository;
    private final OkHttpClient httpClient = new OkHttpClient();

    public DeviceHttpSyncService(MemberRepository memberRepository,
            MemberCardRepository memberCardRepository,
            MemberFaceRepository memberFaceRepository) {
        this.memberRepository = memberRepository;
        this.memberCardRepository = memberCardRepository;
        this.memberFaceRepository = memberFaceRepository;
    }

    public JSONObject syncAllToDevice(String deviceIp) {
        JSONObject result = new JSONObject();
        if (StringUtils.isBlank(deviceIp)) {
            result.put("status", "failed");
            result.put("message", "deviceIp is blank");
            return result;
        }

        List<MemberEntity> members = memberRepository.findAll();
        logger.info("[SYNC] 设备 {}: 开始校准白名单，本地会员总数 {}", deviceIp, members.size());
        Set<String> desiredMemberCodes = new HashSet<>();
        int total = 0;
        int success = 0;
        int failed = 0;
        int deleted = 0;
        int deleteFailed = 0;

        int missingCard = 0;
        int missingFace = 0;
        int missingImage = 0;

        for (MemberEntity member : members) {
            String memberCode = member.getMemberCode();
            if (StringUtils.isBlank(memberCode)) {
                continue;
            }
            if (Integer.valueOf(1).equals(member.getDeleted())) {
                continue;
            }
            MemberCardEntity card = memberCardRepository.findFirstByMemberCode(memberCode);
            if (card == null || StringUtils.isBlank(card.getCardNo())) {
                missingCard++;
                if (missingCard <= 5)
                    logger.warn("Skip member {}: No card found", member.getMemberName());
                continue;
            }
            MemberFaceEntity face = findFaceForCard(memberCode, card.getCardNo());
            if (face == null || StringUtils.isBlank(face.getImagePath())) {
                missingFace++;
                if (missingFace <= 5)
                    logger.warn("Skip member {}: No face record", member.getMemberName());
                continue;
            }

            String base64 = loadImageBase64(face.getImagePath());
            if (base64 == null) {
                missingImage++;
                if (missingImage <= 5)
                    logger.warn("Skip member {}: Image file missing at {}", member.getMemberName(),
                            face.getImagePath());
                continue;
            }

            desiredMemberCodes.add(memberCode);
            total++;
            boolean ok = sendAddWhiteList(deviceIp, total, total, member, card, base64);
            if (ok) {
                success++;
            } else {
                failed++;
            }
        }

        Set<String> deviceMemberCodes = fetchDeviceWhiteListIds(deviceIp);
        for (String existingCode : deviceMemberCodes) {
            if (StringUtils.isBlank(existingCode) || desiredMemberCodes.contains(existingCode)) {
                continue;
            }
            if (deleteWhiteListEntry(deviceIp, existingCode)) {
                deleted++;
            } else {
                deleteFailed++;
            }
        }

        logger.info(
                "[SYNC] 设备 {}: 推送完成，候选总数={}, 成功新增/覆盖={}, 失败={}, 缺卡={}, 缺人脸={}, 缺图片={}, 已删除={}, 删除失败={}",
                deviceIp, total, success, failed, missingCard, missingFace, missingImage, deleted, deleteFailed);

        result.put("status", failed == 0 && deleteFailed == 0 ? "ok" : "partial");
        result.put("total", total);
        result.put("success", success);
        result.put("failed", failed);
        result.put("deleted", deleted);
        result.put("deleteFailed", deleteFailed);
        return result;
    }

    public JSONObject syncMemberToDevice(String deviceIp, String memberCode) {
        JSONObject result = new JSONObject();
        if (StringUtils.isBlank(deviceIp)) {
            result.put("status", "failed");
            result.put("message", "deviceIp is blank");
            return result;
        }
        if (StringUtils.isBlank(memberCode)) {
            result.put("status", "failed");
            result.put("message", "memberCode is blank");
            return result;
        }

        MemberEntity member = memberRepository.findById(memberCode).orElse(null);
        if (member == null || Integer.valueOf(1).equals(member.getDeleted())) {
            result.put("status", "failed");
            result.put("message", "member not found or deleted");
            return result;
        }
        MemberCardEntity card = memberCardRepository.findFirstByMemberCode(memberCode);
        if (card == null || StringUtils.isBlank(card.getCardNo())) {
            result.put("status", "failed");
            result.put("message", "card not found");
            return result;
        }
        MemberFaceEntity face = findFaceForCard(memberCode, card.getCardNo());
        if (face == null || StringUtils.isBlank(face.getImagePath())) {
            result.put("status", "failed");
            result.put("message", "face image not found");
            return result;
        }
        String base64 = loadImageBase64(face.getImagePath());
        if (base64 == null) {
            result.put("status", "failed");
            result.put("message", "face image file missing or empty");
            return result;
        }

        boolean ok = sendAddWhiteList(deviceIp, 1, 1, member, card, base64);
        result.put("status", ok ? "ok" : "failed");
        result.put("deviceIp", deviceIp);
        result.put("memberCode", memberCode);
        result.put("memberName", member.getMemberName());
        result.put("cardNo", card.getCardNo());
        return result;
    }

    private MemberFaceEntity findFaceForCard(String memberCode, String cardNo) {
        MemberFaceEntity face = memberFaceRepository.findFirstByMemberCode(memberCode);
        if (face != null) {
            return face;
        }
        if (StringUtils.isBlank(cardNo)) {
            return null;
        }
        face = memberFaceRepository.findById(cardNo).orElse(null);
        if (face != null && StringUtils.isBlank(face.getMemberCode())) {
            face.setMemberCode(memberCode);
            memberFaceRepository.save(face);
            logger.info("[SYNC] Backfilled memberCode {} for cardNo {}", memberCode, cardNo);
        }
        return face;
    }

    private boolean sendAddWhiteList(String deviceIp,
            int totalnum,
            int currentnum,
            MemberEntity member,
            MemberCardEntity card,
            String faceBase64) {
        try {
            JSONObject data = new JSONObject();
            data.put("usertype", "white");
            data.put("employee_number", member.getMemberCode());
            data.put("name", member.getMemberName());
            data.put("idno", member.getCertNo());
            data.put("icno", card.getCardNo());
            data.put("company", member.getDepartName());
            data.put("department", member.getDepartName());
            data.put("peoplestartdate", "2020-01-01");
            data.put("peopleenddate", "2035-12-31");
            data.put("passAlgo", false);
            data.put("TimeGroupId", 0);
            data.put("SpecialGroupId", 0);
            data.put("register_base64", faceBase64);

            JSONObject body = new JSONObject();
            body.put("password", "123456");
            body.put("totalnum", totalnum);
            body.put("currentnum", currentnum);
            body.put("data", data);

            String url = "http://" + deviceIp + ":8091/addDeviceWhiteList";
            Request request = new Request.Builder()
                    .url(url)
                    .post(RequestBody.create(body.toJSONString(), JSON_MEDIA_TYPE))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                String respStr = response.body() != null ? response.body().string() : "";
                logger.info("[SYNC] 设备 {}: 下发人员 {} 响应 {}", deviceIp, member.getMemberCode(), respStr);
                if (!response.isSuccessful()) {
                    return false;
                }
                JSONObject respJson = JSONObject.parseObject(respStr);
                Integer result = respJson.getInteger("result");
                return result != null && result == 0;
            }
        } catch (Exception e) {
            logger.error("Failed to sync member {} to device {}", member.getMemberCode(), deviceIp, e);
            return false;
        }
    }

    private String loadImageBase64(String imagePath) {
        try {
            Path path = Paths.get(imagePath);
            if (!Files.exists(path)) {
                logger.warn("Face image not found: {}", imagePath);
                return null;
            }
            byte[] bytes = Files.readAllBytes(path);
            if (bytes.length == 0) {
                logger.warn("Face image empty: {}", imagePath);
                return null;
            }
            return Base64.getEncoder().encodeToString(bytes);
        } catch (IOException e) {
            logger.error("Failed to read face image: {}", imagePath, e);
            return null;
        }
    }

    private boolean deleteWhiteListEntry(String deviceIp, String memberCode) {
        try {
            JSONObject data = new JSONObject();
            data.put("employee_number", memberCode);
            data.put("usertype", "white");

            JSONObject body = new JSONObject();
            body.put("password", "123456");
            body.put("data", data);

            String url = "http://" + deviceIp + ":8091/deleteDeviceWhiteList";
            Request request = new Request.Builder()
                    .url(url)
                    .post(RequestBody.create(body.toJSONString(), JSON_MEDIA_TYPE))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                String respStr = response.body() != null ? response.body().string() : "";
                logger.info("[SYNC] 设备 {}: 删除人员 {} 响应 {}", deviceIp, memberCode, respStr);
                if (!response.isSuccessful()) {
                    return false;
                }
                JSONObject respJson = JSONObject.parseObject(respStr);
                Integer result = respJson.getInteger("result");
                return result != null && result == 0;
            }
        } catch (Exception e) {
            logger.error("Failed to delete member {} on device {}", memberCode, deviceIp, e);
            return false;
        }
    }

    private Set<String> fetchDeviceWhiteListIds(String deviceIp) {
        try {
            JSONObject body = new JSONObject();
            body.put("password", "123456");

            String url = "http://" + deviceIp + ":8091/getAllDeviceIdWhiteList";
            Request request = new Request.Builder()
                    .url(url)
                    .post(RequestBody.create(body.toJSONString(), JSON_MEDIA_TYPE))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                String respStr = response.body() != null ? response.body().string() : "";
                logger.info("[SYNC] 设备 {}: 读取现有白名单响应 {}", deviceIp, respStr);
                if (!response.isSuccessful()) {
                    return Collections.emptySet();
                }

                JSONObject respJson = JSONObject.parseObject(respStr);
                if (respJson == null || respJson.getInteger("result") == null || respJson.getInteger("result") != 0) {
                    return Collections.emptySet();
                }

                Set<String> ids = new HashSet<>();
                JSONObject data = respJson.getJSONObject("data");
                if (data != null && data.getJSONArray("idNumList") != null) {
                    for (Object obj : data.getJSONArray("idNumList")) {
                        if (obj != null) {
                            ids.add(String.valueOf(obj));
                        }
                    }
                    return ids;
                }

                if (respJson.getJSONArray("idNumList") != null) {
                    for (Object obj : respJson.getJSONArray("idNumList")) {
                        if (obj != null) {
                            ids.add(String.valueOf(obj));
                        }
                    }
                }
                return ids;
            }
        } catch (Exception e) {
            logger.error("Failed to fetch whitelist ids from device {}", deviceIp, e);
            return Collections.emptySet();
        }
    }
}
