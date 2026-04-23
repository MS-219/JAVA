package com.yinlian.repository;

import com.yinlian.model.MemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MemberRepository extends JpaRepository<MemberEntity, String> {

    MemberEntity findByMemberCode(String memberCode);

    List<MemberEntity> findByMemberNameContaining(String name);

    List<MemberEntity> findByMemberNameAndCertNoEndingWith(String memberName, String certNoSuffix);
}
