package com.yinlian.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 平台端同步调度，封装分页/重试逻辑。
 */
@Service
public class PlatformSyncService {

    private static final Logger logger = LoggerFactory.getLogger(PlatformSyncService.class);
    private static final int DEFAULT_PAGE_SIZE = 50;

    private final UnionPayClient unionPayClient;
    private final MemberSyncService memberSyncService;

    public PlatformSyncService(UnionPayClient unionPayClient, MemberSyncService memberSyncService) {
        this.unionPayClient = unionPayClient;
        this.memberSyncService = memberSyncService;
    }

    public void syncAllMembers(String lastUpdateTime) throws Exception {
        logger.info("[SYNC] 平台会员同步: 开始，lastUpdateTime={}", lastUpdateTime);
        paginatePlatform("plat.member.sync", lastUpdateTime,
                pageNo -> unionPayClient.fetchMembers(pageNo, DEFAULT_PAGE_SIZE, lastUpdateTime),
                resp -> {
                    JSONArray memberList = resp.getJSONArray("memberList");
                    memberSyncService.saveMembers(memberList);
                });
        logger.info("[SYNC] 平台会员同步: 完成");
    }

    public void syncAllMemberCards(String lastUpdateTime) throws Exception {
        logger.info("[SYNC] 平台会员卡同步: 开始，lastUpdateTime={}", lastUpdateTime);
        paginatePlatform("plat.member.card.sync", lastUpdateTime,
                pageNo -> unionPayClient.fetchMemberCards(pageNo, DEFAULT_PAGE_SIZE, lastUpdateTime),
                resp -> memberSyncService.saveMemberCards(resp.getJSONArray("memberCardList")));
        logger.info("[SYNC] 平台会员卡同步: 完成");
    }

    public void syncFaceBindings(int syncType, String startTime) throws Exception {
        logger.info("[SYNC] 平台人脸绑定同步: 开始，syncType={}, startTime={}", syncType, startTime);
        JSONObject resp = unionPayClient.fetchFaceBindings(syncType, startTime);
        JSONArray bindList = resp.getJSONArray("bindList");
        JSONArray unbindList = resp.getJSONArray("unbindList");
        
        memberSyncService.saveFaceBindings(bindList, unbindList);
        
        // 自动下载图片
        if (bindList != null && !bindList.isEmpty()) {
            logger.info("Downloading face images for {} cards...", bindList.size());
            for (Object obj : bindList) {
                try {
                    // bindList里的元素可能是字符串(cardNo)或者JSONObject
                    String cardNo = null;
                    if (obj instanceof String) {
                        cardNo = (String) obj;
                    } else if (obj instanceof JSONObject) {
                        cardNo = ((JSONObject) obj).getString("cardNo");
                    }
                    
                    if (cardNo != null) {
                        downloadFaceAndStore(cardNo);
                        // 避免请求过快，稍微sleep一下
                        Thread.sleep(100); 
                    }
                } catch (Exception e) {
                    logger.error("Failed to download face for card: " + obj, e);
                    // 继续下载下一个，不中断
                }
            }
        }

        memberSyncService.recordSyncLog("plat.face.sync", 0, 0,
                resp.getString("respCode"), resp.getString("respDesc"),
                "0000".equals(resp.getString("respCode")));
        logger.info("[SYNC] 平台人脸绑定同步: 完成，bindCount={}, unbindCount={}, respCode={}",
                bindList == null ? 0 : bindList.size(),
                unbindList == null ? 0 : unbindList.size(),
                resp.getString("respCode"));
    }

    public String downloadFaceAndStore(String cardNo) throws Exception {
        JSONObject resp = unionPayClient.downloadFaceImage(cardNo);
        byte[] image = memberSyncService.decryptFaceImage(resp.getString("pic"));
        return memberSyncService.saveFaceImage(cardNo, resp.getString("picName"), image);
    }

    private void paginatePlatform(String syncType,
                                  String lastUpdateTime,
                                  PlatformPageFetcher fetcher,
                                  ResponseConsumer consumer) throws Exception {
        int pageNo = 1;
        while (true) {
            JSONObject resp = fetcher.fetch(pageNo);
            String respCode = resp.getString("respCode");
            if (!"0000".equals(respCode)) {
                memberSyncService.recordSyncLog(syncType, pageNo, DEFAULT_PAGE_SIZE,
                        respCode, resp.getString("respDesc"), false);
                throw new RuntimeException("Platform API " + syncType + " failed: " + resp.getString("respDesc"));
            }
            consumer.consume(resp);
            memberSyncService.recordSyncLog(syncType, pageNo, DEFAULT_PAGE_SIZE,
                    respCode, resp.getString("respDesc"), true);

            JSONArray list = ("plat.member.card.sync".equals(syncType))
                    ? resp.getJSONArray("memberCardList")
                    : resp.getJSONArray("memberList");
            int size = list == null ? 0 : list.size();
            logger.info("[SYNC] {} 第 {} 页同步完成，本页 {} 条，respCode={}", syncType, pageNo, size, respCode);
            if (size < DEFAULT_PAGE_SIZE) {
                break;
            }
            pageNo++;
        }
    }

    @FunctionalInterface
    private interface PlatformPageFetcher {
        JSONObject fetch(int pageNo) throws Exception;
    }

    @FunctionalInterface
    private interface ResponseConsumer {
        void consume(JSONObject resp) throws Exception;
    }
}
