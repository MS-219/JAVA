package com.yinlian.repository;

import com.yinlian.model.RecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JpaRecordRepository extends JpaRepository<RecordEntity, Long> {

    boolean existsByDeviceCodeAndPersonCodeAndCaptureTime(String deviceCode, String personCode, String captureTime);

    List<RecordEntity> findByStatus(String status);
}
