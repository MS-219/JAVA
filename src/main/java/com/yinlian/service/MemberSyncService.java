package com.yinlian.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.yinlian.config.UnionPayConfig;
import com.yinlian.model.MemberCardEntity;
import com.yinlian.model.MemberEntity;
import com.yinlian.model.MemberFaceEntity;
import com.yinlian.model.SyncLogEntity;
import com.yinlian.repository.MemberCardRepository;
import com.yinlian.repository.MemberFaceRepository;
import com.yinlian.repository.MemberRepository;
import com.yinlian.repository.SyncLogRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 平台会员/卡/人脸同步持久化服务.
 */
@Service
public class MemberSyncService {

    private static final Logger logger = LoggerFactory.getLogger(MemberSyncService.class);

    private static final Path FACE_DIR = Paths.get("data", "faces");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final MemberRepository memberRepository;
    private final MemberCardRepository memberCardRepository;
    private final MemberFaceRepository memberFaceRepository;
    private final SyncLogRepository syncLogRepository;
    private final UnionPayConfig unionPayConfig;
    private final UnionPayClient unionPayClient;

    public MemberSyncService(MemberRepository memberRepository,
                             MemberCardRepository memberCardRepository,
                             MemberFaceRepository memberFaceRepository,
                             SyncLogRepository syncLogRepository,
                             UnionPayConfig unionPayConfig,
                             UnionPayClient unionPayClient) {
        this.memberRepository = memberRepository;
        this.memberCardRepository = memberCardRepository;
        this.memberFaceRepository = memberFaceRepository;
        this.syncLogRepository = syncLogRepository;
        this.unionPayConfig = unionPayConfig;
        this.unionPayClient = unionPayClient;
    }

    @Transactional
    public void saveMembers(JSONArray memberList) {
        if (memberList == null || memberList.isEmpty()) {
            logger.warn("[SYNC] 会员落库: 本页没有会员数据");
            return;
        }
        logger.info("[SYNC] 会员落库: 本页准备处理 {} 条", memberList.size());
        List<MemberEntity> entities = new ArrayList<>(memberList.size());
        for (Object obj : memberList) {
            if (!(obj instanceof JSONObject)) {
                continue;
            }
            JSONObject json = (JSONObject) obj;
            String memberCode = json.getString("memberCode");
            if (StringUtils.isBlank(memberCode)) {
                logger.warn("Skipping member with empty memberCode: {}", json);
                continue;
            }
            MemberEntity entity = memberRepository.findById(memberCode).orElseGet(MemberEntity::new);
            entity.setMemberCode(memberCode);
            entity.setMemberUniqueId(json.getString("memberUniqueId"));
            entity.setMemberName(json.getString("memberName"));
            entity.setMobileNo(json.getString("mobileNo"));
            entity.setCertNo(json.getString("certNo"));
            entity.setDepartCode(json.getString("departCode"));
            entity.setDepartName(json.getString("departName")); // Fix typo deparName -> departName
            entity.setMemberTypeName(json.getString("memberTypeName"));
            entity.setState(parseInteger(json.getString("state")));
            entity.setDeleted(parseInteger(json.getString("deleted")));
            entity.setExpiryDate(parseDate(json.getString("expiryDate")));
            entity.setRemark(json.getString("remark"));
            entity.setAge(parseInteger(json.getString("age")));
            entity.setSex(parseInteger(json.getString("sex")));
            entity.setNation(json.getString("nation"));
            entity.setHouseholdAddress(json.getString("householdAddress"));
            entity.setLiveAddress(json.getString("liveAddress"));
            entity.setCreateTime(parseDateTime(json.getString("createTime")));
            entity.setUpdateTime(parseDateTime(json.getString("updateTime")));
            entity.setRawPayload(json.toJSONString());
            entities.add(entity);

            // 同步接口 2.44 有嵌套的 memberCardList
            JSONArray memberCardList = json.getJSONArray("memberCardList");
            if (memberCardList != null) {
                saveMemberCards(memberCardList);
            }
        }
        memberRepository.saveAll(entities);
        logger.info("[SYNC] 会员落库: 成功保存 {} 条", entities.size());
    }

    @Transactional
    public void saveMemberCards(JSONArray cardList) {
        if (cardList == null || cardList.isEmpty()) {
            logger.info("[SYNC] 会员卡落库: 本页没有会员卡数据");
            return;
        }
        List<MemberCardEntity> entities = new ArrayList<>(cardList.size());
        for (Object obj : cardList) {
            if (!(obj instanceof JSONObject)) {
                continue;
            }
            JSONObject json = (JSONObject) obj;
            String cardCode = json.getString("cardCode");
            if (StringUtils.isBlank(cardCode)) {
                continue;
            }
            MemberCardEntity entity = memberCardRepository.findById(cardCode).orElseGet(MemberCardEntity::new);
            entity.setCardCode(cardCode);
            entity.setMemberCode(json.getString("memberCode"));
            entity.setCardNo(json.getString("cardNo"));
            entity.setMemberCardType(json.getString("memberCardType"));
            entity.setBalance(parseLong(json.getString("balance")));
            entity.setFaceBind(parseInteger(json.getString("faceBind")));
            entity.setLossState(parseInteger(json.getString("lossState")));
            entity.setLockState(parseInteger(json.getString("lockState")));
            entity.setEnableState(parseInteger(json.getString("enableState")));
            entity.setDeleted(parseInteger(json.getString("deleted")));
            entity.setCreateTime(parseDateTime(json.getString("createTime")));
            entity.setUpdateTime(parseDateTime(json.getString("updateTime")));
            entity.setRawPayload(json.toJSONString());
            entities.add(entity);
        }
        memberCardRepository.saveAll(entities);
        logger.info("[SYNC] 会员卡落库: 成功保存 {} 条", entities.size());
    }

    @Transactional
    public void saveFaceBindings(JSONArray bindList, JSONArray unbindList) {
        int bindCount = bindList == null ? 0 : bindList.size();
        int unbindCount = unbindList == null ? 0 : unbindList.size();
        logger.info("[SYNC] 人脸绑定落库: bind={}, unbind={}", bindCount, unbindCount);
        if (bindList != null) {
            for (Object obj : bindList) {
                String cardNo = extractCardNo(obj);
                if (StringUtils.isBlank(cardNo)) {
                    continue;
                }
                MemberFaceEntity entity = memberFaceRepository.findById(cardNo).orElseGet(MemberFaceEntity::new);
                entity.setCardNo(cardNo);
                entity.setBindingState(1);
                entity.setFaceCode(extractFaceCode(obj));
                entity.setRawPayload(obj instanceof JSONObject ? ((JSONObject) obj).toJSONString() : null);
                entity.setSyncedAt(LocalDateTime.now());
                entity.setMemberCode(resolveMemberCode(cardNo));
                memberFaceRepository.save(entity);
            }
        }
        if (unbindList != null) {
            for (Object obj : unbindList) {
                String cardNo = extractCardNo(obj);
                if (StringUtils.isBlank(cardNo)) {
                    continue;
                }
                MemberFaceEntity entity = memberFaceRepository.findById(cardNo).orElse(null);
                if (entity != null) {
                    entity.setBindingState(0);
                    entity.setImagePath(null);
                    entity.setImageHash(null);
                    entity.setSyncedAt(LocalDateTime.now());
                    memberFaceRepository.save(entity);
                }
            }
        }
        logger.info("[SYNC] 人脸绑定落库: 完成");
    }

    public void recordSyncLog(String type, int pageNo, int pageSize, String respCode, String respDesc, boolean success) {
        SyncLogEntity log = new SyncLogEntity();
        log.setSyncType(type);
        log.setPageNo(pageNo);
        log.setPageSize(pageSize);
        log.setRespCode(respCode);
        log.setRespDesc(respDesc);
        log.setSuccess(success);
        syncLogRepository.save(log);
    }

    @Transactional
    public String saveFaceImage(String cardNo, String picName, byte[] imageBytes) {
        Objects.requireNonNull(cardNo, "cardNo cannot be null");
        if (imageBytes == null || imageBytes.length == 0) {
            throw new IllegalArgumentException("imageBytes is empty");
        }
        try {
            Files.createDirectories(FACE_DIR);
            String safeName = StringUtils.isNotBlank(picName) ? picName : cardNo + ".jpg";
            Path filePath = FACE_DIR.resolve(safeName);
            Files.write(filePath, imageBytes);

            MemberFaceEntity entity = memberFaceRepository.findById(cardNo).orElseGet(MemberFaceEntity::new);
            entity.setCardNo(cardNo);
            entity.setImagePath(filePath.toAbsolutePath().toString());
            entity.setImageHash(md5Hex(imageBytes));
            entity.setMemberCode(resolveMemberCode(cardNo));
            entity.setBindingState(1);
            entity.setSyncedAt(LocalDateTime.now());
            memberFaceRepository.save(entity);

            logger.info("[SYNC] 人脸图片保存: cardNo={}, file={}", cardNo, filePath.getFileName());
            return filePath.toAbsolutePath().toString();
        } catch (IOException e) {
            throw new RuntimeException("Failed to persist face image", e);
        }
    }

    public byte[] decryptFaceImage(String base64Content) {
        if (StringUtils.isBlank(base64Content)) {
            return new byte[0];
        }
        try {
            // Clean input (remove newlines, spaces)
            String cleaned = base64Content.replaceAll("\\s+", "");

            // 文档中人脸密文可能是十六进制串，也可能是Base64
            byte[] encrypted;
            // 只有当长度是偶数且只包含Hex字符时才尝试Hex解析
            // 但为了防止Base64恰好符合Hex格式的误判，我们可以尝试优先Base64解码
            // 这里保留原逻辑，但在日志中增加调试信息
            if (cleaned.matches("^[0-9A-Fa-f]+$") && cleaned.length() % 2 == 0) {
                try {
                    encrypted = hexToBytes(cleaned);
                } catch (Exception e) {
                    // Fallback to Base64 if Hex fails (unlikely with regex match but safe)
                    encrypted = Base64.getDecoder().decode(cleaned);
                }
            } else {
                encrypted = Base64.getDecoder().decode(cleaned);
            }

            // 优先使用 POS 签到的 workingKey
            String keyStr = unionPayClient.getWorkingKey();
            String keySource = "Working Key";

            if (StringUtils.isBlank(keyStr)) {
                // 其次使用配置文件里的 encryptKey
                keyStr = unionPayConfig.getEncryptKey();
                keySource = "Config EncryptKey";
            }
            
            if (StringUtils.isBlank(keyStr)) {
                // 最后回退到 secretKey
                keyStr = unionPayConfig.getSecretKey();
                keySource = "Config SecretKey";
            }
            
            // Log key source for debugging (hide actual key in production)
            logger.info("Decrypting face image using: {}, Key: {}", keySource, keyStr);
            
            // 尝试多种解密算法组合
            byte[] decrypted = tryDecryptWithAllMethods(encrypted, keyStr);

            // 检查解密结果是否已经是图片 (JPEG: FF D8, PNG: 89 50 4E 47)
            if (isImageFile(decrypted)) {
                return decrypted;
            }

            // 如果不是图片头，尝试作为 String 进行 Base64 二次解码
            try {
                String potentialBase64 = new String(decrypted, StandardCharsets.UTF_8).trim();
                if (potentialBase64.length() > 0 && isBase64Chars(potentialBase64)) {
                    byte[] secondDecode = Base64.getDecoder().decode(potentialBase64);
                    if (isImageFile(secondDecode)) {
                        logger.info("Success: Double Base64 decoding required.");
                        return secondDecode;
                    }
                    return secondDecode; 
                }
            } catch (Exception e) {
                // Ignore
            }

            return decrypted;

        } catch (Exception e) {
            logger.error("Failed to decrypt face image. Length: " + (base64Content != null ? base64Content.length() : "null") + ", Error: " + e.toString(), e);
            return new byte[0];
        }
    }

    private byte[] tryDecryptWithAllMethods(byte[] content, String keyStr) {
        // 1. Standard 3DES (Hex Key)
        try {
            byte[] keyBytes = normalize3DesKey(keyStr);
            
            byte[] res = doDecrypt(content, keyBytes, "DESede/ECB/PKCS5Padding");
            if (isSuccess(res)) { logger.info("Success with: 3DES/ECB/PKCS5Padding (Hex Key)"); return res; }
            
            res = doDecrypt(content, keyBytes, "DESede/ECB/NoPadding");
            if (isSuccess(res)) { logger.info("Success with: 3DES/ECB/NoPadding (Hex Key)"); return res; }
        } catch (Exception e) {}

        // 2. DES (Hex Key, first 8 bytes)
        try {
            byte[] fullKey = hexToBytes(keyStr);
            byte[] desKey = new byte[8];
            System.arraycopy(fullKey, 0, desKey, 0, 8);
            
            byte[] res = doDecrypt(content, desKey, "DES/ECB/PKCS5Padding");
            if (isSuccess(res)) { logger.info("Success with: DES/ECB/PKCS5Padding (Hex Key)"); return res; }

            res = doDecrypt(content, desKey, "DES/ECB/NoPadding");
            if (isSuccess(res)) { logger.info("Success with: DES/ECB/NoPadding (Hex Key)"); return res; }
        } catch (Exception e) {}

        // 3. AES (Hex Key, 16 bytes)
        try {
            byte[] aesKey = hexToBytes(keyStr); // 32 hex chars = 16 bytes
            if (aesKey.length == 16) {
                SecretKeySpec keySpec = new SecretKeySpec(aesKey, "AES");
                Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
                cipher.init(Cipher.DECRYPT_MODE, keySpec);
                byte[] res = cipher.doFinal(content);
                if (isSuccess(res)) { logger.info("Success with: AES/ECB/PKCS5Padding (Hex Key)"); return res; }
                
                cipher = Cipher.getInstance("AES/ECB/NoPadding");
                cipher.init(Cipher.DECRYPT_MODE, keySpec);
                res = cipher.doFinal(content);
                if (isSuccess(res)) { logger.info("Success with: AES/ECB/NoPadding (Hex Key)"); return res; }
            }
        } catch (Exception e) {}

        // 4. 3DES (String Key Bytes) - This is the winner!
        try {
            byte[] rawStringBytes = keyStr.getBytes(StandardCharsets.UTF_8);
            byte[] keyBytes = new byte[24];
            System.arraycopy(rawStringBytes, 0, keyBytes, 0, Math.min(rawStringBytes.length, 24));
            
            byte[] res = doDecrypt(content, keyBytes, "DESede/ECB/PKCS5Padding");
            if (isSuccess(res)) { logger.info("Success with: 3DES/ECB/PKCS5Padding (String Key)"); return res; }
            
            res = doDecrypt(content, keyBytes, "DESede/ECB/NoPadding");
            if (isSuccess(res)) { logger.info("Success with: 3DES/ECB/NoPadding (String Key)"); return res; }
        } catch (Exception e) {}

        return new byte[0];
    }

    private boolean isSuccess(byte[] data) {
        if (isImageFile(data)) return true;
        // Check if it is Base64 encoded image
        if (data == null || data.length < 4) return false;
        // Check common Base64 headers
        // /9j/ -> JPEG
        if (data[0] == 0x2F && data[1] == 0x39 && data[2] == 0x6A && data[3] == 0x2F) return true;
        // iVBOR -> PNG
        if (data[0] == 0x69 && data[1] == 0x56 && data[2] == 0x42 && data[3] == 0x4F) return true;
        return false;
    }

    private byte[] doDecrypt(byte[] content, byte[] keyBytes, String transformation) throws Exception {
        String algo = transformation.split("/")[0];
        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, algo);
        Cipher cipher = Cipher.getInstance(transformation);
        cipher.init(Cipher.DECRYPT_MODE, keySpec);
        return cipher.doFinal(content);
    }

    private boolean isImageFile(byte[] data) {
        if (data == null || data.length < 4) return false;
        // JPEG: FF D8
        if ((data[0] & 0xFF) == 0xFF && (data[1] & 0xFF) == 0xD8) return true;
        // PNG: 89 50 4E 47
        if ((data[0] & 0xFF) == 0x89 && (data[1] & 0xFF) == 0x50 && 
            (data[2] & 0xFF) == 0x4E && (data[3] & 0xFF) == 0x47) return true;
        // BMP: 42 4D
        if ((data[0] & 0xFF) == 0x42 && (data[1] & 0xFF) == 0x4D) return true;
        return false;
    }

    private boolean isBase64Chars(String str) {
        // 允许 Base64 字符集
        for (char c : str.toCharArray()) {
            if (!((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || 
                  (c >= '0' && c <= '9') || c == '+' || c == '/' || c == '=' || Character.isWhitespace(c))) {
                return false;
            }
        }
        return true;
    }

    private byte[] normalize3DesKey(String original) {
        if (StringUtils.isBlank(original)) {
            throw new IllegalArgumentException("3DES key is empty");
        }
        byte[] raw;
        if (original.matches("^[0-9A-Fa-f]+$") && original.length() % 2 == 0) {
            raw = hexToBytes(original);
        } else {
            raw = original.getBytes(StandardCharsets.UTF_8);
        }
        if (raw.length == 24) {
            return raw;
        }
        byte[] normalized = new byte[24];
        if (raw.length >= 24) {
            System.arraycopy(raw, 0, normalized, 0, 24);
        } else if (raw.length == 16) {
            System.arraycopy(raw, 0, normalized, 0, 16);
            System.arraycopy(raw, 0, normalized, 16, 8);
        } else {
            System.arraycopy(raw, 0, normalized, 0, raw.length);
        }
        return normalized;
    }

    private byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }

    private String extractCardNo(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof JSONObject) {
            return ((JSONObject) obj).getString("cardNo");
        }
        return obj.toString();
    }

    private String extractFaceCode(Object obj) {
        if (obj instanceof JSONObject) {
            return ((JSONObject) obj).getString("faceCode");
        }
        return null;
    }

    private String md5Hex(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] md5 = digest.digest(data);
            StringBuilder sb = new StringBuilder();
            for (byte b : md5) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not found", e);
        }
    }

    private Integer parseInteger(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Long parseLong(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private LocalDate parseDate(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        try {
            return LocalDate.parse(value, DATE);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private LocalDateTime parseDateTime(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        try {
            return LocalDateTime.parse(value, DATE_TIME);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private String resolveMemberCode(String cardNo) {
        if (StringUtils.isBlank(cardNo)) {
            return null;
        }
        MemberCardEntity cardEntity = memberCardRepository.findFirstByCardNo(cardNo);
        return cardEntity != null ? cardEntity.getMemberCode() : null;
    }
}
