package com.groupaccountingsystem.service.impl;

import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.dypnsapi.model.v20170525.CheckSmsVerifyCodeRequest;
import com.aliyuncs.dypnsapi.model.v20170525.CheckSmsVerifyCodeResponse;
import com.aliyuncs.dypnsapi.model.v20170525.SendSmsVerifyCodeRequest;
import com.aliyuncs.dypnsapi.model.v20170525.SendSmsVerifyCodeResponse;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.profile.DefaultProfile;
import com.groupaccountingsystem.config.AliyunSmsAuthProperties;
import com.groupaccountingsystem.service.SmsVerifyCodeService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AliyunSmsVerifyCodeServiceImpl implements SmsVerifyCodeService {
    private static final String SUCCESS_CODE = "OK";
    private static final String VERIFY_PASS = "PASS";

    private final AliyunSmsAuthProperties properties;

    public AliyunSmsVerifyCodeServiceImpl(AliyunSmsAuthProperties properties) {
        this.properties = properties;
    }

    @Override
    public void sendCode(String phoneNumber) {
        validatePhoneNumber(phoneNumber);
        properties.validate();

        SendSmsVerifyCodeRequest request = new SendSmsVerifyCodeRequest();
        request.setCountryCode(properties.getCountryCode());
        request.setPhoneNumber(phoneNumber);
        request.setSignName(properties.getSignName());
        request.setTemplateCode(properties.getTemplateCode());
        request.setTemplateParam(properties.getTemplateParam());
        request.setCodeLength(properties.getCodeLength());
        request.setValidTime(properties.getValidTime());
        request.setDuplicatePolicy(properties.getDuplicatePolicy());
        request.setInterval(properties.getInterval());
        request.setCodeType(properties.getCodeType());
        request.setReturnVerifyCode(false);
        if (StringUtils.hasText(properties.getSchemeName())) {
            request.setSchemeName(properties.getSchemeName());
        }

        try {
            SendSmsVerifyCodeResponse response = client().getAcsResponse(request);
            if (!Boolean.TRUE.equals(response.getSuccess()) || !SUCCESS_CODE.equals(response.getCode())) {
                throw new RuntimeException("SMS send failed: " + response.getMessage());
            }
        } catch (ClientException e) {
            throw new RuntimeException("SMS send failed: " + e.getErrMsg(), e);
        }
    }

    @Override
    public boolean checkCode(String phoneNumber, String verifyCode) {
        validatePhoneNumber(phoneNumber);
        if (!StringUtils.hasText(verifyCode)) {
            throw new RuntimeException("Verify code is required.");
        }
        properties.validate();

        CheckSmsVerifyCodeRequest request = new CheckSmsVerifyCodeRequest();
        request.setCountryCode(properties.getCountryCode());
        request.setPhoneNumber(phoneNumber);
        request.setVerifyCode(verifyCode);
        request.setCaseAuthPolicy(properties.getCaseAuthPolicy());
        if (StringUtils.hasText(properties.getSchemeName())) {
            request.setSchemeName(properties.getSchemeName());
        }

        try {
            CheckSmsVerifyCodeResponse response = client().getAcsResponse(request);
            if (!Boolean.TRUE.equals(response.getSuccess()) || !SUCCESS_CODE.equals(response.getCode())) {
                throw new RuntimeException("SMS verify failed: " + response.getMessage());
            }
            CheckSmsVerifyCodeResponse.Model model = response.getModel();
            return model != null && VERIFY_PASS.equals(model.getVerifyResult());
        } catch (ClientException e) {
            throw new RuntimeException("SMS verify failed: " + e.getErrMsg(), e);
        }
    }

    private IAcsClient client() {
        DefaultProfile profile = DefaultProfile.getProfile(
                properties.getRegionId(),
                properties.getAccessKeyId(),
                properties.getAccessKeySecret()
        );
        return new DefaultAcsClient(profile);
    }

    private void validatePhoneNumber(String phoneNumber) {
        if (!StringUtils.hasText(phoneNumber)) {
            throw new RuntimeException("Phone number is required.");
        }
        if (!phoneNumber.matches("^1\\d{10}$")) {
            throw new RuntimeException("Invalid phone number.");
        }
    }
}
