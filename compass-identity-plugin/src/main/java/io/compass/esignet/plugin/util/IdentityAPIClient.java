package io.compass.esignet.plugin.util;

import io.compass.esignet.plugin.dto.*;
import io.mosip.esignet.api.exception.SendOtpException;
import io.mosip.esignet.core.dto.RequestWrapper;
import io.mosip.esignet.core.dto.ResponseWrapper;
import io.mosip.esignet.core.exception.EsignetException;
import io.mosip.esignet.core.util.IdentityProviderUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;


import java.util.Map;

@Component
@Slf4j
public class IdentityAPIClient {

    @Value("${mosip.esignet.send-otp.endpoint}")
    private String generateChallengeUrl;

    @Value("${mosip.compass.user-info.endpoint}")
    private String userInfoUrl;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${mosip.esignet.send-notification.endpoint}")
    private String sendNotificationEndpoint;

    @Value("${mosip.esignet.client.secret}")
    private String clientSecret ;

    @Value("${mosip.compass.client.secret}")
    private String compassClientSecret ;

    @Value("${mosip.esignet.get-auth.endpoint}")
    private String getAuthTokenEndpoint;

    public String getAuthToken(String client_id,String client_secret,String grant_type)
    {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", client_id);
        body.add("client_secret", client_secret);
        body.add("grant_type", grant_type);

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(getAuthTokenEndpoint, requestEntity, Map.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                Map<String, Object> responseBody = response.getBody();
                if (responseBody != null && responseBody.containsKey("access_token")) {
                    return responseBody.get("access_token").toString();
                }
            }
        } catch (Exception e) {
            throw new EsignetException(e.getMessage());
        }
        throw new EsignetException("Error fetching auth token");
    }

    public String generateOTPChallenge(String challengeTransactionId) throws SendOtpException {
        OtpRequest otpRequest = new OtpRequest();
        otpRequest.setKey(challengeTransactionId);
        RequestWrapper<OtpRequest> restRequestWrapper = new RequestWrapper<>();
        restRequestWrapper.setRequestTime(IdentityProviderUtil.getUTCDateTime());
        restRequestWrapper.setRequest(otpRequest);
        String token = getAuthToken("mosip-signup-client", clientSecret, "client_credentials");
        if (token == null || token.isEmpty()) {
            throw new SendOtpException("Token retrieval failed");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Cookie", "Authorization="+token);

        HttpEntity<RequestWrapper<OtpRequest>> entity = new HttpEntity<>(restRequestWrapper, headers);

        try {
            ResponseWrapper<OtpResponse> responseWrapper = restTemplate
                    .exchange(generateChallengeUrl, HttpMethod.POST,
                            entity,
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


    public void sendSMSNotification(String[] mailTo,
                                    String[] mailCc,
                                    String[] mailSubject,
                                    String[] mailContent,
                                    MultipartFile[] attachments) throws SendOtpException {

        NotificationRequest notificationRequest = new NotificationRequest(mailTo, mailCc, mailSubject, mailContent, attachments);

        RequestWrapper<NotificationRequest> restRequestWrapper = new RequestWrapper<>();
        restRequestWrapper.setRequestTime(IdentityProviderUtil.getUTCDateTime());
        restRequestWrapper.setRequest(notificationRequest);
        String token = getAuthToken("mosip-signup-client", clientSecret, "client_credentials");
        if (token == null || token.isEmpty()) {
            throw new SendOtpException("Token retrieval failed");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Cookie", "Authorization="+token);

        HttpEntity<RequestWrapper<NotificationRequest>> entity = new HttpEntity<>(restRequestWrapper, headers);

        try {
            ResponseWrapper<NotificationResponse> responseWrapper = restTemplate.exchange(sendNotificationEndpoint,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<ResponseWrapper<NotificationResponse>>(){}).getBody();
            log.debug("Notification response -> {}", responseWrapper);
        } catch (RestClientException e){
            throw new SendOtpException("otp_notification_failed");
        }
    }

    public UserInfo getUserInfoByNationalUid(String nationalUid) {
        try {
            String token = getAuthToken("compass-admin", compassClientSecret, "client_credentials");
            if (token == null || token.isEmpty()) {
                throw new SendOtpException("Token retrieval failed");
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization","Bearer "+token);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<UserInfo> responseEntity = restTemplate.exchange(
                    userInfoUrl + "/{nationalUid}",
                    HttpMethod.GET,
                    entity,
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
