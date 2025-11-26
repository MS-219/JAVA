package com.yinlian.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.yinlian.config.UnionPayConfig;
import com.yinlian.utils.DesEcbUtil;
import okhttp3.*;
import org.bouncycastle.crypto.digests.SM3Digest;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class UnionPayClient {
    private static final Logger logger = LoggerFactory.getLogger(UnionPayClient.class);

    @Autowired
    private UnionPayConfig config;

    private final OkHttpClient httpClient = new OkHttpClient();
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

    private String workingKey; // 签到后获取的工作密钥(Hex字符串形式，如果有的话)
    private byte[] workingKeyBytes; // 签到后获取的工作密钥(原始字节)

    public String lastSignString; // 用于调试：存储最后一次签名的字符串

    public String getWorkingKey() {
        return this.workingKey;
    }

    // 平台接口 - 拉取会员列表
    public JSONObject fetchMembers(int pageNo, int pageSize, String lastUpdateTime) throws Exception {
        Map<String, Object> map = buildPlatformParams("plat.member.sync");

        map.put("pageNo", String.valueOf(pageNo));
        map.put("pageSize", String.valueOf(pageSize));
        if (lastUpdateTime != null) {
            map.put("lastUpdateTime", lastUpdateTime);
        }

        // 平台签名 (SM3)
        String sign = signPlatform(map);
        map.put("sign", sign);

        return sendPlatformRequest(map);
    }

    // 平台接口 - 拉取会员卡列表
    public JSONObject fetchMemberCards(int pageNo, int pageSize, String lastUpdateTime) throws Exception {
        Map<String, Object> map = buildPlatformParams("plat.member.card.sync");
        map.put("pageNo", String.valueOf(pageNo));
        if (pageSize > 0) {
            map.put("pageSize", String.valueOf(pageSize));
        }
        if (lastUpdateTime != null && !lastUpdateTime.isEmpty()) {
            map.put("lastUpdateTime", lastUpdateTime);
        }
        String sign = signPlatform(map);
        map.put("sign", sign);
        return sendPlatformRequest(map);
    }

    // 平台接口 - 人脸信息同步
    public JSONObject fetchFaceBindings(int syncType, String startTime) throws Exception {
        Map<String, Object> map = buildPlatformParams("plat.face.sync");
        map.put("syncType", String.valueOf(syncType));
        if (syncType == 1 && startTime != null && !startTime.isEmpty()) {
            map.put("startTime", startTime);
        }
        String sign = signPlatform(map);
        map.put("sign", sign);
        return sendPlatformRequest(map);
    }

    // 平台接口 - 人脸图片下载
    public JSONObject downloadFaceImage(String cardNo) throws Exception {
        Map<String, Object> map = buildPlatformParams("plat.face.downLoad");
        map.put("cardNo", cardNo);
        String sign = signPlatform(map);
        map.put("sign", sign);
        return sendPlatformRequest(map);
    }

    // SM3 算法
    private String sm3(String text) {
        try {
            // Revert to GBK as UTF-8 didn't help
            byte[] data = text.getBytes("GBK");
            SM3Digest digest = new SM3Digest();
            digest.update(data, 0, data.length);
            byte[] hash = new byte[digest.getDigestSize()];
            digest.doFinal(hash, 0);
            String result = Hex.toHexString(hash).toUpperCase();
            logger.info("Generated Signature: {}", result); // 打印生成的签名
            return result;
        } catch (Exception e) {
            logger.error("SM3 error", e);
            throw new RuntimeException("SM3 calculation failed");
        }
    }

    private Map<String, Object> buildPlatformParams(String msgType) {
        Map<String, Object> map = new TreeMap<>();
        // 使用真实商户号
        String mchntCode = config.getMchntCode();
        if ("00000000".equals(mchntCode)) {
            logger.warn("mchntCode is 00000000, forcing to 10002856");
            mchntCode = "10002856";
        }
        map.put("mchntCode", mchntCode);
        // map.put("mchntCode", "00000000");

        map.put("msgType", msgType);
        String platformSrc = config.getPlatformMsgSrc();
        map.put("msgSrc", (platformSrc == null || platformSrc.isEmpty()) ? config.getMsgSrc() : platformSrc);
        // map.put("msgSrc", "NET_POS");

        map.put("reqTime", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        map.put("reqSsn", System.currentTimeMillis() + "" + new Random().nextInt(1000));
        return map;
    }

    // 发送平台请求
    private JSONObject sendPlatformRequest(Map<String, Object> params) throws Exception {
        String jsonBody = JSON.toJSONString(params); // 平台接口通常不需要 MapSortField，因为已经签完名了
        logger.info("Platform Request: {}", jsonBody);

        Request request = new Request.Builder()
                .url(config.getPlatformBaseUrl()) // 使用平台 Base URL
                .post(RequestBody.create(jsonBody, JSON_MEDIA_TYPE))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String respStr = response.body().string();
            logger.info("Platform Response: {}", respStr);
            return JSON.parseObject(respStr);
        }
    }

    // 平台签名 (SM3) 使用配置的 signKey - 遵循官方 SignUtil 实现
    private String signPlatform(Map<String, Object> map) throws Exception {
        // 移除空值
        removeEmptyValues(map);

        // 构造待签名字符串: key1=value1&key2=value2&...（官方格式，不含 &key=）
        StringBuilder sb = new StringBuilder();
        // map 是 TreeMap, entrySet 已经有序
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) {
                sb.append("&");
            }
            sb.append(entry.getKey()).append("=").append(entry.getValue());
            first = false;
        }

        // 官方格式：直接拼接 signKey，不加 &key=
        String originStr = sb.toString();
        String data = originStr + config.getSignKey();
        this.lastSignString = data; // 保存以供调试
        logger.info("Platform signing string: {}", data);
        return sm3(data);
    }

    // 移除Map中的空值签到
    public void posSignIn() throws Exception {
        Map<String, Object> map = buildCommonParams("pos.signIn");
        // map.put("cashierCode", config.getCashierCode()); // 移到 buildCommonParams

        // RSA加密密码
        String encryptedPwd = encryptRSA(config.getCashierPassword(), config.getRsaPublicKey());
        map.put("cashierPsd", encryptedPwd);
        // map.put("cashierPubKeyStr", config.getRsaPublicKey());

        // 签名 (使用终端主密钥)
        String mac = sign(map, config.getSecretKey());
        map.put("macSign", mac);

        // 发送请求
        JSONObject resp = sendRequest(map);

        if ("0000".equals(resp.getString("respCode"))) {
            String macKey = resp.getString("macKey");
            if (macKey != null) {
                logger.info("Received macKey: {}", macKey);
                // 尝试方案：使用 DesEcbUtil 自带的 decryptDES (单倍长) 解密
                // 如果 secretKey 是 16 字节，这里只取前 8 字节作为 Key
                byte[] secretKeyBytes = config.getSecretKey().getBytes("GBK");
                this.workingKeyBytes = DesEcbUtil.decryptDES(macKey, secretKeyBytes);

                // 打印解密后的 Key (Hex)
                this.workingKey = DesEcbUtil.bytesToHexString(this.workingKeyBytes);
                logger.info("Working Key (Hex): {}", this.workingKey);
            }
        } else {
            logger.error("Sign in failed: {} - {}", resp.getString("respCode"), resp.getString("respDesc"));
            throw new RuntimeException("Sign in failed: " + resp.getString("respDesc"));
        }
    }

    // 考勤记录上传
    public JSONObject posAttendanceSignIn(Map<String, Object> recordPayload) throws Exception {
        // 1. 检查是否需要签到
        if (this.workingKeyBytes == null) {
            logger.info("No working key, signing in first...");
            posSignIn();
        }

        // 2. 组装请求
        Map<String, Object> map = buildCommonParams("pos.attendance.signIn");
        // 恢复 parkCode 和 attendGroupCode，确保参数完整
        if (config.getParkCode() != null) {
            map.put("parkCode", config.getParkCode());
        }
        if (config.getAttendGroupCode() != null) {
            map.put("attendGroupCode", config.getAttendGroupCode());
        }

        map.putAll(recordPayload); // 合并业务参数

        // 3. 签名
        // 恢复使用工作密钥签名
        String mac;
        if (this.workingKeyBytes != null) {
            mac = sign(map, this.workingKeyBytes);
        } else {
            // 如果万一没有工作密钥，才降级用主密钥（虽然应该不会发生）
            mac = sign(map, config.getSecretKey());
        }
        map.put("macSign", mac);

        // 移除空值 (null 或 "")，与 Node.js 逻辑保持一致
        removeEmptyValues(map);

        // 4. 发送
        JSONObject resp = sendRequest(map);

        if (!"0000".equals(resp.getString("respCode"))) {
            logger.error("Upload failed: {} - {}", resp.getString("respCode"), resp.getString("respDesc"));
            // 如果是密钥过期/错误，可以考虑清除 workingKey 并重试
            if ("A001".equals(resp.getString("respCode"))) {
                this.workingKey = null;
                this.workingKeyBytes = null;
            }
            throw new RuntimeException("Upload failed: " + resp.getString("respDesc"));
        }
        return resp;
    }

    private void removeEmptyValues(Map<String, Object> map) {
        map.entrySet().removeIf(entry -> entry.getValue() == null ||
                (entry.getValue() instanceof String && ((String) entry.getValue()).isEmpty()));
    }

    // 签名方法 (使用 String 密钥)
    private String sign(Map<String, Object> map, String key) throws Exception {
        // 必须保证 macSign 为 00000000
        map.put("macSign", "00000000");
        // 移除空值再签名
        removeEmptyValues(map);

        String jsonString = JSON.toJSONString(map, SerializerFeature.MapSortField);
        logger.info("Signing JSON: {}", jsonString);
        return DesEcbUtil.getDesMac(jsonString, key);
    }

    // 签名方法 (使用 byte[] 密钥)
    private String sign(Map<String, Object> map, byte[] keyBytes) throws Exception {
        // 必须保证 macSign 为 00000000
        map.put("macSign", "00000000");
        // 移除空值再签名
        removeEmptyValues(map);

        String jsonString = JSON.toJSONString(map, SerializerFeature.MapSortField);
        logger.info("Signing JSON: {}", jsonString);

        // 直接调用 clacMac2，传入 GBK 字节流和 Key 字节流
        // DesEcbUtil.Charset 默认为 GBK
        return DesEcbUtil.clacMac2(jsonString.getBytes("GBK"), keyBytes);
    }

    // RSA 加密
    private String encryptRSA(String plainText, String publicKeyStr) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(publicKeyStr);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PublicKey publicKey = keyFactory.generatePublic(keySpec);

        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encryptedBytes = cipher.doFinal(plainText.getBytes("UTF-8"));

        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    // 构建公共参数
    private Map<String, Object> buildCommonParams(String msgType) {
        Map<String, Object> map = new TreeMap<>(); // 有序Map
        map.put("msgType", msgType);
        map.put("msgSrc", config.getMsgSrc());
        map.put("reqTime", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        map.put("termSsn", System.currentTimeMillis() + "" + new Random().nextInt(1000));
        map.put("termNo", config.getTermNo());

        String mchntCode = config.getMchntCode();
        if ("00000000".equals(mchntCode)) {
            mchntCode = "10002856";
        }
        map.put("mchntCode", mchntCode);

        // 强制补零，防御 cashierCode 为 "4" 的情况
        String cashierCode = config.getCashierCode();
        if (cashierCode != null && cashierCode.length() < 4) {
            cashierCode = String.format("%04d", Integer.parseInt(cashierCode));
        }
        map.put("cashierCode", cashierCode);

        map.put("version", config.getVersion());
        return map;
    }

    // 发送请求
    private JSONObject sendRequest(Map<String, Object> params) throws Exception {
        String jsonBody = JSON.toJSONString(params, SerializerFeature.MapSortField);
        Request request = new Request.Builder()
                .url(config.getBaseUrl())
                .post(RequestBody.create(jsonBody, JSON_MEDIA_TYPE))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String respStr = response.body().string();
            logger.info("Response: {}", respStr);
            return JSON.parseObject(respStr);
        }
    }
}
