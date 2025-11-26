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
import java.util.List;

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

        // 先清空设备上的所有白名单，确保数据完全同步
        logger.info("Clearing all whitelist on device {}...", deviceIp);
        if (!deleteAllWhiteList(deviceIp)) {
            logger.warn("Failed to clear device whitelist, but continuing with sync...");
        }

        List<MemberEntity> members = memberRepository.findAll();
        logger.info("HTTP sync to device {}: found {} members", deviceIp, members.size());
        int total = 0;
        int success = 0;
        int failed = 0;

        int missingCard = 0;
        int missingFace = 0;
        int missingImage = 0;

        for (MemberEntity member : members) {
            String memberCode = member.getMemberCode();
            if (StringUtils.isBlank(memberCode)) {
                continue;
            }
            MemberCardEntity card = memberCardRepository.findFirstByMemberCode(memberCode);
            if (card == null || StringUtils.isBlank(card.getCardNo())) {
                missingCard++;
                if (missingCard <= 5)
                    logger.warn("Skip member {}: No card found", member.getMemberName());
                continue;
            }
            MemberFaceEntity face = memberFaceRepository.findFirstByMemberCode(memberCode);
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

            total++;
            boolean ok = sendAddWhiteList(deviceIp, total, total, member, card, base64);
            if (ok) {
                success++;
            } else {
                failed++;
            }
        }

        logger.info(
                "Sync check result: Total Members: {}, Missing Card: {}, Missing Face Record: {}, Missing Image File: {}",
                members.size(), missingCard, missingFace, missingImage);

        result.put("status", failed == 0 ? "ok" : "partial");
        result.put("total", total);
        result.put("success", success);
        result.put("failed", failed);
        return result;
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
                logger.info("HTTP sync->device {} addDeviceWhiteList resp: {}", deviceIp, respStr);
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

    /**
     * 清空设备上的所有白名单
     * 
     * @param deviceIp 设备IP
     * @return true=成功, false=失败
     */
    private boolean deleteAllWhiteList(String deviceIp) {
        try {
            JSONObject body = new JSONObject();
            body.put("password", "123456");

            String url = "http://" + deviceIp + ":8091/deleteDeviceAllWhiteList";
            Request request = new Request.Builder()
                    .url(url)
                    .post(RequestBody.create(body.toJSONString(), JSON_MEDIA_TYPE))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                String respStr = response.body() != null ? response.body().string() : "";
                logger.info("Delete all whitelist response: {}", respStr);
                if (!response.isSuccessful()) {
                    return false;
                }
                JSONObject respJson = JSONObject.parseObject(respStr);
                Integer result = respJson.getInteger("result");
                return result != null && result == 0;
            }
        } catch (Exception e) {
            logger.error("Failed to delete all whitelist on device {}", deviceIp, e);
            return false;
        }
    }
}
