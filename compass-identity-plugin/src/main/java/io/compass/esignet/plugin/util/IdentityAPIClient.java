package io.compass.esignet.plugin.util;

import io.compass.esignet.plugin.dto.*;
import io.mosip.esignet.api.exception.SendOtpException;
import io.mosip.esignet.core.dto.RequestWrapper;
import io.mosip.esignet.core.dto.ResponseWrapper;
import io.mosip.esignet.core.exception.EsignetException;
import io.mosip.esignet.core.util.IdentityProviderUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class IdentityAPIClient {

    @Value("${mosip.esignet.send-otp.endpoint}")
    private String generateChallengeUrl;

    @Value("${mosip.compass.user-info.endpoint}")
    private String userInfoUrl;

    @Autowired
    @Qualifier("selfTokenRestTemplate")
    private RestTemplate selfTokenRestTemplate;

    @Autowired
    private Environment environment;

    @Value("${mosip.esignet.send-notification.endpoint}")
    private String sendNotificationEndpoint;

    @Value("{${mosip.esignet.default-language}")
    private String defaultLanguage;

    @Value("#{${mosip.esignet.sms-notification-template.encoded-langcodes}}")
    private List<String> encodedLangCodes;

    @Value("${mosip.signup.identifier.prefix:}")
    private String identifierPrefix;

    public String generateOTPChallenge(String challengeTransactionId) throws SendOtpException {
        OtpRequest otpRequest = new OtpRequest();
        otpRequest.setKey(challengeTransactionId);
        RequestWrapper<OtpRequest> restRequestWrapper = new RequestWrapper<>();
        restRequestWrapper.setRequestTime(IdentityProviderUtil.getUTCDateTime());
        restRequestWrapper.setRequest(otpRequest);

        try {
            ResponseWrapper<OtpResponse> responseWrapper = selfTokenRestTemplate
                    .exchange(generateChallengeUrl, HttpMethod.POST,
                            new HttpEntity<>(restRequestWrapper),
                            new ParameterizedTypeReference<ResponseWrapper<OtpResponse>>() {
                            })
                    .getBody();

            if (responseWrapper != null && responseWrapper.getResponse() != null &&
                    !StringUtils.isEmpty(responseWrapper.getResponse().getOtp()) &&
                    !responseWrapper.getResponse().getOtp().equals("null")) {
                return responseWrapper.getResponse().getOtp();
            }

            log.error("Generate OTP failed with response {}", responseWrapper);
            throw new SendOtpException(responseWrapper != null && !CollectionUtils.isEmpty(responseWrapper.getErrors()) ?
                    responseWrapper.getErrors().get(0).getErrorCode() : "send_otp_failed");
        } catch (SendOtpException e) {
            log.error("Endpoint {} is unreachable.", generateChallengeUrl);
            throw new SendOtpException("server_unreachable");
        }
    }


    public void sendSMSNotification
            (String number, String locale, String templateKey, Map<String, String> params) throws SendOtpException {

        locale = locale != null ? locale : defaultLanguage;

        String message = encodedLangCodes.contains(locale)?
                new String(Base64.getDecoder().decode(environment.getProperty(templateKey + "." + locale))):
                environment.getProperty(templateKey + "." + locale);

        if (params != null && message != null) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                message = message.replace(entry.getKey(), entry.getValue());
            }
        }

        NotificationRequest notificationRequest = new NotificationRequest(number.substring(identifierPrefix.length()), message);

        RequestWrapper<NotificationRequest> restRequestWrapper = new RequestWrapper<>();
        restRequestWrapper.setRequestTime(IdentityProviderUtil.getUTCDateTime());
        restRequestWrapper.setRequest(notificationRequest);

        try {
            ResponseWrapper<NotificationResponse> responseWrapper = selfTokenRestTemplate.exchange(sendNotificationEndpoint,
                    HttpMethod.POST,
                    new HttpEntity<>(restRequestWrapper),
                    new ParameterizedTypeReference<ResponseWrapper<NotificationResponse>>(){}).getBody();
            log.debug("Notification response -> {}", responseWrapper);
        } catch (RestClientException e){
            throw new SendOtpException("otp_notification_failed");
        }
    }

    @Async
    public void sendSMSNotificationAsync
            (String number, String locale, String templateKey, Map<String, String> params) throws SendOtpException {
        sendSMSNotification(number, locale, templateKey, params);
    }

    public UserInfo getUserInfoByNationalUid(String nationalUid) {
        try {
            ResponseEntity<UserInfo> responseEntity = selfTokenRestTemplate.getForEntity(
                    userInfoUrl + "/{nationalUid}",
                    UserInfo.class,
                    nationalUid
            );

            UserInfo userInfo = responseEntity.getBody();

            if (userInfo != null && !StringUtils.isEmpty(userInfo.getNationalUid())) {
                return userInfo;
            }

            log.error("Failed to fetch user info for National UID: {}. Response: {}", nationalUid, userInfo);
            throw new EsignetException("user_info_not_found");
        } catch (Exception ex) {
            log.error("Failed to fetch user info for National UID: {}. Error: {}", nationalUid, ex.getMessage(), ex);
            throw new EsignetException("user_info_fetch_failed");
        }
    }

}
