package com.yinlian.controller;

import com.yinlian.model.MemberCardEntity;
import com.yinlian.model.MemberEntity;
import com.yinlian.repository.MemberCardRepository;
import com.yinlian.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class DebugController {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MemberCardRepository memberCardRepository;

    @GetMapping("/debug/check-data")
    public Map<String, Object> checkData(@RequestParam(required = false) String memberCode) {
        Map<String, Object> result = new HashMap<>();

        long memberCount = memberRepository.count();
        long cardCount = memberCardRepository.count();

        result.put("totalMembers", memberCount);
        result.put("totalCards", cardCount);

        if (memberCode != null) {
            MemberEntity member = memberRepository.findById(memberCode).orElse(null);
            result.put("member", member);

            List<MemberCardEntity> cards = memberCardRepository.findByMemberCode(memberCode);
            result.put("cards", cards);
        } else {
            // Show first 5 members and their cards
            List<MemberEntity> first5 = memberRepository.findAll().subList(0, (int) Math.min(memberCount, 5));
            Map<String, Object> details = new HashMap<>();
            for (MemberEntity m : first5) {
                List<MemberCardEntity> cards = memberCardRepository.findByMemberCode(m.getMemberCode());
                details.put(m.getMemberName() + "(" + m.getMemberCode() + ")", cards);
            }
            result.put("sampleData", details);
        }

        return result;
    }
}
