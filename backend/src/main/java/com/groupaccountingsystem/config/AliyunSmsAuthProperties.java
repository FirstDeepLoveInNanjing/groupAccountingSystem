package com.groupaccountingsystem.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@ConfigurationProperties(prefix = "aliyun.sms-auth")
public class AliyunSmsAuthProperties {
    private String accessKeyId;
    private String accessKeySecret;
    private String regionId = "cn-hangzhou";
    private String countryCode = "86";
    private String signName;
    private String templateCode;
    private String templateParam = "{\"code\":\"##code##\",\"min\":\"5\"}";
    private String schemeName;
    private Long codeLength = 6L;
    private Long validTime = 300L;
    private Long duplicatePolicy = 1L;
    private Long interval = 60L;
    private Long codeType = 1L;
    private Long caseAuthPolicy = 1L;

    public void validate() {
        if (!StringUtils.hasText(accessKeyId)
                || !StringUtils.hasText(accessKeySecret)
                || !StringUtils.hasText(signName)
                || !StringUtils.hasText(templateCode)) {
            throw new IllegalStateException("Aliyun SMS auth is not configured completely.");
        }
    }

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public void setAccessKeyId(String accessKeyId) {
        this.accessKeyId = accessKeyId;
    }

    public String getAccessKeySecret() {
        return accessKeySecret;
    }

    public void setAccessKeySecret(String accessKeySecret) {
        this.accessKeySecret = accessKeySecret;
    }

    public String getRegionId() {
        return regionId;
    }

    public void setRegionId(String regionId) {
        this.regionId = regionId;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getSignName() {
        return signName;
    }

    public void setSignName(String signName) {
        this.signName = signName;
    }

    public String getTemplateCode() {
        return templateCode;
    }

    public void setTemplateCode(String templateCode) {
        this.templateCode = templateCode;
    }

    public String getTemplateParam() {
        return templateParam;
    }

    public void setTemplateParam(String templateParam) {
        this.templateParam = templateParam;
    }

    public String getSchemeName() {
        return schemeName;
    }

    public void setSchemeName(String schemeName) {
        this.schemeName = schemeName;
    }

    public Long getCodeLength() {
        return codeLength;
    }

    public void setCodeLength(Long codeLength) {
        this.codeLength = codeLength;
    }

    public Long getValidTime() {
        return validTime;
    }

    public void setValidTime(Long validTime) {
        this.validTime = validTime;
    }

    public Long getDuplicatePolicy() {
        return duplicatePolicy;
    }

    public void setDuplicatePolicy(Long duplicatePolicy) {
        this.duplicatePolicy = duplicatePolicy;
    }

    public Long getInterval() {
        return interval;
    }

    public void setInterval(Long interval) {
        this.interval = interval;
    }

    public Long getCodeType() {
        return codeType;
    }

    public void setCodeType(Long codeType) {
        this.codeType = codeType;
    }

    public Long getCaseAuthPolicy() {
        return caseAuthPolicy;
    }

    public void setCaseAuthPolicy(Long caseAuthPolicy) {
        this.caseAuthPolicy = caseAuthPolicy;
    }
}
