package com.yinlian.repository;

import com.yinlian.model.MemberFaceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MemberFaceRepository extends JpaRepository<MemberFaceEntity, String> {
    MemberFaceEntity findFirstByMemberCode(String memberCode);
}

