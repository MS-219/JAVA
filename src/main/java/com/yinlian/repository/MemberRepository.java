package com.yinlian.repository;

import com.yinlian.model.MemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

@Repository
public interface MemberRepository extends JpaRepository<MemberEntity, String> {

    MemberEntity findByMemberCode(String memberCode);

    List<MemberEntity> findByMemberNameContaining(String name);

    List<MemberEntity> findByMemberNameAndCertNoEndingWith(String memberName, String certNoSuffix);

    @Modifying
    @Transactional
    @Query("UPDATE MemberEntity m SET m.deleted = 1 WHERE (m.deleted IS NULL OR m.deleted <> 1) AND m.memberCode NOT IN :memberCodes")
    int markMissingAsDeleted(@Param("memberCodes") Collection<String> memberCodes);
}
