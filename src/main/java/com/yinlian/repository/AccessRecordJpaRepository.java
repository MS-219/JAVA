package com.yinlian.repository;

import com.yinlian.model.AccessRecordEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface AccessRecordJpaRepository extends JpaRepository<AccessRecordEntity, Long> {

    /**
     * 判断是否重复
     */
    boolean existsByDeviceCodeAndPersonCodeAndCaptureTime(String deviceCode, String personCode, String captureTime);

    /**
     * 按状态查找 (重试用)
     */
    List<AccessRecordEntity> findByStatusIn(List<String> statuses);

    /**
     * 更新状态
     */
    @Modifying
    @Transactional
    @Query("UPDATE AccessRecordEntity r SET r.status = :status WHERE r.id = :id")
    void updateStatusById(@Param("id") Long id, @Param("status") String status);

    /**
     * 按抓拍时间倒序获取最新记录
     */
    List<AccessRecordEntity> findAllByOrderByIdDesc();

    /**
     * 按设备代码筛选 + 时间模糊匹配
     */
    @Query("SELECT r FROM AccessRecordEntity r WHERE " +
            "(:date IS NULL OR r.captureTime LIKE CONCAT(:date, '%')) AND " +
            "(:deviceCode IS NULL OR r.deviceCode LIKE CONCAT('%', :deviceCode, '%')) " +
            "ORDER BY r.id DESC")
    List<AccessRecordEntity> findFiltered(@Param("date") String date, @Param("deviceCode") String deviceCode);

    /**
     * 统计筛选后的记录数
     */
    @Query("SELECT COUNT(r) FROM AccessRecordEntity r WHERE " +
            "(:date IS NULL OR r.captureTime LIKE CONCAT(:date, '%')) AND " +
            "(:deviceCode IS NULL OR r.deviceCode LIKE CONCAT('%', :deviceCode, '%'))")
    long countFiltered(@Param("date") String date, @Param("deviceCode") String deviceCode);

    /**
     * 按人员代码列表搜索
     */
    @Query("SELECT r FROM AccessRecordEntity r WHERE r.personCode IN :personCodes ORDER BY r.id DESC")
    List<AccessRecordEntity> findByPersonCodeIn(@Param("personCodes") List<String> personCodes);

    /**
     * 按人员代码列表 + 日期范围搜索
     */
    @Query("SELECT r FROM AccessRecordEntity r WHERE r.personCode IN :personCodes " +
            "AND (:startDate IS NULL OR r.captureTime >= :startDate) " +
            "AND (:endDate IS NULL OR r.captureTime <= CONCAT(:endDate, ' 23:59:59')) " +
            "ORDER BY r.id DESC")
    List<AccessRecordEntity> findByPersonCodeInAndDateRange(
            @Param("personCodes") List<String> personCodes,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate);

    /**
     * 分页查询（支持日期范围 + 设备筛选）
     */
    @Query("SELECT r FROM AccessRecordEntity r WHERE " +
            "(:startDate IS NULL OR r.captureTime >= :startDate) AND " +
            "(:endDate IS NULL OR r.captureTime <= CONCAT(:endDate, ' 23:59:59')) AND " +
            "(:deviceCode IS NULL OR r.deviceCode LIKE CONCAT('%', :deviceCode, '%')) " +
            "ORDER BY r.id DESC")
    Page<AccessRecordEntity> findPagedWithDateRange(
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            @Param("deviceCode") String deviceCode,
            Pageable pageable);

    /**
     * 统计日期范围内的记录数
     */
    @Query("SELECT COUNT(r) FROM AccessRecordEntity r WHERE " +
            "(:startDate IS NULL OR r.captureTime >= :startDate) AND " +
            "(:endDate IS NULL OR r.captureTime <= CONCAT(:endDate, ' 23:59:59')) AND " +
            "(:deviceCode IS NULL OR r.deviceCode LIKE CONCAT('%', :deviceCode, '%'))")
    long countWithDateRange(@Param("startDate") String startDate,
                            @Param("endDate") String endDate,
                            @Param("deviceCode") String deviceCode);
}
