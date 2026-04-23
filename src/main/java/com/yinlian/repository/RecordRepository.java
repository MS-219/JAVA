package com.yinlian.repository;

import com.alibaba.fastjson.JSONObject;
import com.yinlian.model.AccessRecordEntity;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 通行记录仓库 — 数据持久化到 H2 数据库
 * 重启后数据不会丢失
 */
@Repository
public class RecordRepository {

    private static final Logger logger = LoggerFactory.getLogger(RecordRepository.class);

    @Autowired
    private AccessRecordJpaRepository jpaRepo;

    // ========== 写操作 ==========

    /**
     * 保存一条记录
     */
    public void pushRecord(JSONObject record) {
        AccessRecordEntity entity = toEntity(record);
        try {
            entity = jpaRepo.save(entity);
            record.put("__entityId", entity.getId());
            logger.debug("Record persisted with id={}", entity.getId());
        } catch (Exception e) {
            logger.error("Failed to persist record: {}", e.getMessage());
        }
    }

    /**
     * 判断是否重复记录
     */
    public boolean isDuplicate(String deviceCode, String personCode, String captureTime) {
        try {
            return jpaRepo.existsByDeviceCodeAndPersonCodeAndCaptureTime(deviceCode, personCode, captureTime);
        } catch (Exception e) {
            logger.error("isDuplicate check failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 更新记录状态
     */
    public void updateStatus(Long entityId, String status) {
        if (entityId == null) return;
        try {
            jpaRepo.updateStatusById(entityId, status);
        } catch (Exception e) {
            logger.error("updateStatus failed for id={}: {}", entityId, e.getMessage());
        }
    }

    // ========== 读操作 ==========

    /**
     * 查找单条记录
     */
    public JSONObject findRecord(Predicate<JSONObject> predicate) {
        List<AccessRecordEntity> all = jpaRepo.findAllByOrderByIdDesc();
        for (AccessRecordEntity entity : all) {
            JSONObject r = toJson(entity);
            if (predicate.test(r)) {
                return r;
            }
        }
        return null;
    }

    /**
     * 获取所有记录
     */
    public List<JSONObject> getAllRecords() {
        return jpaRepo.findAllByOrderByIdDesc()
                .stream()
                .map(this::toJson)
                .collect(Collectors.toList());
    }

    /**
     * 统计记录总数
     */
    public int countRecords() {
        return (int) jpaRepo.count();
    }

    /**
     * 获取最新的 N 条记录
     */
    public List<JSONObject> getLatestRecords(int limit) {
        List<AccessRecordEntity> all = jpaRepo.findAllByOrderByIdDesc();
        return all.stream()
                .limit(limit)
                .map(this::toJson)
                .collect(Collectors.toList());
    }

    /**
     * 按日期和设备筛选后统计
     */
    public long countFilteredRecords(String date, String deviceCode) {
        return jpaRepo.countFiltered(
                StringUtils.isBlank(date) ? null : date,
                StringUtils.isBlank(deviceCode) ? null : deviceCode
        );
    }

    /**
     * 按日期和设备筛选后获取记录
     */
    public List<JSONObject> getFilteredRecords(String date, String deviceCode, int limit) {
        List<AccessRecordEntity> filtered = jpaRepo.findFiltered(
                StringUtils.isBlank(date) ? null : date,
                StringUtils.isBlank(deviceCode) ? null : deviceCode
        );
        return filtered.stream()
                .limit(limit)
                .map(this::toJson)
                .collect(Collectors.toList());
    }

    /**
     * 按人员代码搜索
     */
    public List<JSONObject> searchByPersonCodes(List<String> personCodes, int limit) {
        if (personCodes == null || personCodes.isEmpty()) return new ArrayList<>();
        return jpaRepo.findByPersonCodeIn(personCodes)
                .stream()
                .limit(limit)
                .map(this::toJson)
                .collect(Collectors.toList());
    }

    /**
     * 按人员代码 + 日期范围搜索
     */
    public List<JSONObject> searchByPersonCodesWithDateRange(
            List<String> personCodes, String startDate, String endDate, int limit) {
        if (personCodes == null || personCodes.isEmpty()) return new ArrayList<>();
        return jpaRepo.findByPersonCodeInAndDateRange(
                        personCodes,
                        StringUtils.isBlank(startDate) ? null : startDate,
                        StringUtils.isBlank(endDate) ? null : endDate)
                .stream()
                .limit(limit)
                .map(this::toJson)
                .collect(Collectors.toList());
    }

    /**
     * 分页查询（支持日期范围）
     */
    public PageResult getPagedRecordsWithDateRange(
            String startDate, String endDate, String deviceCode, int page, int size) {
        Page<AccessRecordEntity> dbPage = jpaRepo.findPagedWithDateRange(
                StringUtils.isBlank(startDate) ? null : startDate,
                StringUtils.isBlank(endDate) ? null : endDate,
                StringUtils.isBlank(deviceCode) ? null : deviceCode,
                PageRequest.of(page, size)
        );

        PageResult pr = new PageResult();
        pr.totalElements = (int) dbPage.getTotalElements();
        pr.totalPages = dbPage.getTotalPages();
        pr.currentPage = page;
        pr.pageSize = size;
        pr.records = dbPage.getContent().stream()
                .map(this::toJson)
                .collect(Collectors.toList());
        return pr;
    }

    // ========== 转换工具方法 ==========

    /**
     * JSONObject → AccessRecordEntity
     */
    private AccessRecordEntity toEntity(JSONObject record) {
        AccessRecordEntity entity = new AccessRecordEntity();
        entity.setDeviceCode(record.getString("__deviceCode"));
        entity.setPersonCode(record.getString("__personCode"));
        entity.setCaptureTime(record.getString("__captureTime"));
        entity.setStatus(record.getString("status"));
        entity.setRawJson(record.toJSONString());
        return entity;
    }

    /**
     * AccessRecordEntity → JSONObject
     * 从 rawJson 还原完整数据，并补充 entityId
     */
    private JSONObject toJson(AccessRecordEntity entity) {
        JSONObject json;
        try {
            json = JSONObject.parseObject(entity.getRawJson());
        } catch (Exception e) {
            json = new JSONObject();
        }
        // 确保关键字段一致
        json.put("__entityId", entity.getId());
        json.put("__deviceCode", entity.getDeviceCode());
        json.put("__personCode", entity.getPersonCode());
        json.put("__captureTime", entity.getCaptureTime());
        json.put("status", entity.getStatus());
        return json;
    }

    // ========== 内部类 ==========

    public static class PageResult {
        public List<JSONObject> records = new ArrayList<>();
        public int totalElements;
        public int totalPages;
        public int currentPage;
        public int pageSize;
    }
}