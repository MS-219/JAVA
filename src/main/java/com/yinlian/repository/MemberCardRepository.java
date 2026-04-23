package com.yinlian.repository;

import com.yinlian.model.MemberCardEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MemberCardRepository extends JpaRepository<MemberCardEntity, String> {
    List<MemberCardEntity> findByMemberCode(String memberCode);

    MemberCardEntity findFirstByCardNo(String cardNo);

    MemberCardEntity findFirstByMemberCode(String memberCode);

    List<MemberCardEntity> findByCardNo(String cardNo);
}
