/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.compass.esignet.plugin.service;

import io.compass.esignet.plugin.dto.UserInfo;
import io.compass.esignet.plugin.util.IdentityAPIClient;
import io.mosip.esignet.api.dto.*;
import io.mosip.esignet.api.exception.KycAuthException;
import io.mosip.esignet.api.exception.KycExchangeException;
import io.mosip.esignet.api.exception.SendOtpException;
import io.mosip.esignet.api.spi.Authenticator;
import io.compass.esignet.plugin.dto.KycAuth;
import io.mosip.esignet.core.util.IdentityProviderUtil;
import io.mosip.kernel.keymanagerservice.dto.AllCertificatesDataResponseDto;
import io.mosip.kernel.keymanagerservice.dto.CertificateDataResponseDto;
import io.mosip.kernel.keymanagerservice.service.KeymanagerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.*;



@ConditionalOnProperty(value = "mosip.esignet.integration.authenticator", havingValue = "CompassAuthenticationService")
@Component
@Slf4j
public class CompassAuthenticationService implements Authenticator {

    private static final String APPLICATION_ID = "MOCK_AUTHENTICATION_SERVICE";

    private static final String SEND_OTP_SMS_NOTIFICATION_TEMPLATE_KEY = "mosip.esignet.sms-notification-template.send-otp" ;

    @Autowired
    private KeymanagerService keymanagerService;

    @Autowired
    private HelperService helperService;

    @Autowired
    private CacheService cacheService;

    @Autowired
    private IdentityAPIClient identityAPIClient;

    @Validated
    @Override
    public KycAuthResult doKycAuth(@NotBlank String relyingPartyId, @NotBlank String clientId,
                                   @NotNull @Valid KycAuthDto kycAuthDto) throws KycAuthException {

        log.info("Started to build kyc-auth request with transactionId : {} && clientId : {}",
                kycAuthDto.getTransactionId(), clientId);

        KycAuthResult kycAuthResult=null;
        for (AuthChallenge authChallenge : kycAuthDto.getChallengeList()) {
            switch (authChallenge.getAuthFactorType()) {
                case "OTP":
                    kycAuthResult = helperService.validateOtpBasedAuth(kycAuthDto.getIndividualId(), authChallenge,kycAuthDto.getTransactionId());
                    break;
                case "WLA":
                    kycAuthResult = helperService.validateWla(kycAuthDto.getIndividualId(),authChallenge);
                    break;
                default:
                    throw new KycAuthException("invalid_auth_challenge");
            }
        }
        return  kycAuthResult;
    }

    @Override
    public KycExchangeResult doKycExchange(String relyingPartyId, String clientId, KycExchangeDto kycExchangeDto)
            throws KycExchangeException {
        log.info("Started to build kyc-exchange request with transactionId : {} && clientId : {}",
                kycExchangeDto.getTransactionId(), clientId);
        try {
            KycAuth result = cacheService.getKycAuth(kycExchangeDto.getKycToken());
            if(result==null || result.getUserInfo()==null ){
                throw new KycExchangeException("compass-ida-006");
            }
            try {
                Map<String, Object> kyc = helperService.buildKycDataBasedOnPolicy(kycExchangeDto.getAcceptedClaims(),
                        result.getUserInfo());
                kyc.put("sub", result.getPartnerSpecificUserToken());

                String finalKyc= helperService.signKyc(kyc);
                KycExchangeResult kycExchangeResult = new KycExchangeResult();
                kycExchangeResult.setEncryptedKyc(finalKyc);
                return kycExchangeResult;
            } catch (Exception ex) {
                log.error("Failed to build kyc data", ex);
                throw new KycExchangeException("compass-ida-008");
            }

        } catch (KycExchangeException e) {
            throw e;
        } catch (Exception e) {
            log.error("IDA Kyc-exchange failed with clientId : {}", clientId, e);
        }
        throw new KycExchangeException("compass-ida-005", "Failed to build kyc data");
    }

    @Override
    public SendOtpResult sendOtp(String relyingPartyId, String clientId, SendOtpDto sendOtpDto)
            throws SendOtpException {
        String transactionId=sendOtpDto.getTransactionId();
        String challenge = identityAPIClient.generateOTPChallenge(transactionId);
        String challengeHash = IdentityProviderUtil.generateB64EncodedHash(IdentityProviderUtil.ALGO_SHA3_256, challenge);
        cacheService.setChallengeHash(challengeHash,transactionId);
        UserInfo userInfo=identityAPIClient.getUserInfoByNationalUid(sendOtpDto.getIndividualId());
        String email=userInfo.getEmail();
        identityAPIClient.sendSMSNotification(
                new String[]{email},
                null,
                new String[]{"subject"},
                new String[]{"message"},
                null
        );
        SendOtpResult sendOtpResult=new SendOtpResult();
        sendOtpResult.setTransactionId(transactionId);
        return sendOtpResult;
    }

    @Override
    public boolean isSupportedOtpChannel(String channel) {
        return helperService.isSupportedOtpChannel(channel);
    }

    @Override
    public List<KycSigningCertificateData> getAllKycSigningCertificates() {
        List<KycSigningCertificateData> certs = new ArrayList<>();
        AllCertificatesDataResponseDto allCertificatesDataResponseDto = keymanagerService.getAllCertificates(APPLICATION_ID,
                Optional.empty());
        for (CertificateDataResponseDto dto : allCertificatesDataResponseDto.getAllCertificates()) {
            certs.add(new KycSigningCertificateData(dto.getKeyId(), dto.getCertificateData(),
                    dto.getExpiryAt(), dto.getIssuedAt()));
        }
        return certs;
    }
}