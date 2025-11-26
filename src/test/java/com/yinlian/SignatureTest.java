package com.yinlian;

import org.bouncycastle.crypto.digests.SM3Digest;
import org.bouncycastle.util.encoders.Hex;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;

public class SignatureTest {

    public static void main(String[] args) {
        // 模拟实际请求参数
        Map<String, Object> params = new TreeMap<>();
        params.put("mchntCode", "10002856");
        params.put("msgSrc", "XBMAUIJV");
        params.put("msgType", "plat.member.sync");
        params.put("pageNo", 1);
        params.put("pageSize", 10);
        params.put("reqSsn", "1763750534308133");
        params.put("reqTime", "2025-11-22 02:42:14");

        // 拼接签名字符串
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            sb.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
        }

        // 测试不同的 signKey
        String[] signKeys = {
                "D307FCF5D0F64392A7F0290868756501", // 配置文件中的
                "D8ABDE1EEA96138EE053F0FE83909C45" // 可能的另一个key
        };

        for (String signKey : signKeys) {
            String toSign = sb.toString() + "key=" + signKey;
            System.out.println("\n===========================================");
            System.out.println("待签名字符串: " + toSign);
            System.out.println("SignKey: " + signKey);

            String signature = sm3(toSign).toUpperCase();
            System.out.println("生成的签名: " + signature);
            System.out.println("实际的签名: 0764BF96A88ED9D7DAB058629C07984DC52E7B88C472B76D4BA858059FF89746");
            System.out.println(
                    "签名匹配: " + signature.equals("0764BF96A88ED9D7DAB058629C07984DC52E7B88C472B76D4BA858059FF89746"));
        }
    }

    private static String sm3(String text) {
        try {
            byte[] data = text.getBytes(StandardCharsets.UTF_8);
            SM3Digest digest = new SM3Digest();
            digest.update(data, 0, data.length);
            byte[] hash = new byte[digest.getDigestSize()];
            digest.doFinal(hash, 0);
            return Hex.toHexString(hash).toUpperCase();
        } catch (Exception e) {
            throw new RuntimeException("SM3 calculation failed", e);
        }
    }
}
