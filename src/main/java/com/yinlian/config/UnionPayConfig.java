package com.yinlian.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "unionpay")
public class UnionPayConfig {
    private String baseUrl;
    private String platformBaseUrl;
    private String mchntCode;
    private String termNo;
    private String termName;
    private String storeCode;
    private String secretKey;
    private String parkCode;
    private String msgSrc;
    private String platformMsgSrc;
    private String signKey;
    private String cashierCode;
    private String cashierPassword;
    private String version;
    private String rsaPublicKey;
    private String encryptKey;
    private String attendGroupCode;

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public String getPlatformBaseUrl() { return platformBaseUrl; }
    public void setPlatformBaseUrl(String platformBaseUrl) { this.platformBaseUrl = platformBaseUrl; }

    public String getMchntCode() { return mchntCode; }
    public void setMchntCode(String mchntCode) { this.mchntCode = mchntCode; }

    public String getTermNo() { return termNo; }
    public void setTermNo(String termNo) { this.termNo = termNo; }

    public String getTermName() { return termName; }
    public void setTermName(String termName) { this.termName = termName; }

    public String getStoreCode() { return storeCode; }
    public void setStoreCode(String storeCode) { this.storeCode = storeCode; }

    public String getSecretKey() { return secretKey; }
    public void setSecretKey(String secretKey) { this.secretKey = secretKey; }

    public String getParkCode() { return parkCode; }
    public void setParkCode(String parkCode) { this.parkCode = parkCode; }

    public String getMsgSrc() { return msgSrc; }
    public void setMsgSrc(String msgSrc) { this.msgSrc = msgSrc; }

    public String getPlatformMsgSrc() { return platformMsgSrc; }
    public void setPlatformMsgSrc(String platformMsgSrc) { this.platformMsgSrc = platformMsgSrc; }

    public String getSignKey() { return signKey; }
    public void setSignKey(String signKey) { this.signKey = signKey; }

    public String getCashierCode() { return cashierCode; }
    public void setCashierCode(String cashierCode) { this.cashierCode = cashierCode; }

    public String getCashierPassword() { return cashierPassword; }
    public void setCashierPassword(String cashierPassword) { this.cashierPassword = cashierPassword; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getRsaPublicKey() { return rsaPublicKey; }
    public void setRsaPublicKey(String rsaPublicKey) { this.rsaPublicKey = rsaPublicKey; }

    public String getEncryptKey() { return encryptKey; }
    public void setEncryptKey(String encryptKey) { this.encryptKey = encryptKey; }

    public String getAttendGroupCode() { return attendGroupCode; }
    public void setAttendGroupCode(String attendGroupCode) { this.attendGroupCode = attendGroupCode; }
}
