/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.compass.esignet.plugin.service;

import com.nimbusds.jose.jwk.RSAKey;
import io.compass.esignet.plugin.dto.UserInfo;
import io.compass.esignet.plugin.util.IdentityAPIClient;
import io.mosip.esignet.api.dto.AuthChallenge;
import io.mosip.esignet.api.dto.KeyBindingResult;
import io.mosip.esignet.api.dto.SendOtpResult;
import io.mosip.esignet.api.exception.KeyBindingException;
import io.mosip.esignet.api.exception.KycAuthException;
import io.mosip.esignet.api.exception.SendOtpException;
import io.mosip.esignet.api.spi.KeyBinder;
import io.mosip.esignet.api.util.ErrorConstants;
import io.compass.esignet.plugin.dto.KycAuth;
import io.mosip.esignet.core.util.IdentityProviderUtil;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.kernel.keymanagerservice.dto.KeyPairGenerateRequestDto;
import io.mosip.kernel.keymanagerservice.dto.SignatureCertificate;
import io.mosip.kernel.keymanagerservice.service.KeymanagerService;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONObject;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.x509.X509V3CertificateGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.security.auth.x500.X500Principal;
import java.io.StringWriter;
import java.math.BigInteger;
import java.security.PrivateKey;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@ConditionalOnProperty(value = "mosip.esignet.integration.key-binder", havingValue = "CompassKeyBindingWrapperService")
@Component
@Slf4j
public class CompassKeyBindingWrapperService implements KeyBinder {

    public static final String BINDING_SERVICE_APP_ID = "MOCK_BINDING_SERVICE";

    private List<String> supportedBindAuthFactorTypes = List.of("WLA");

    @Autowired
    private KeymanagerService keymanagerService;

    @Value("${mosip.esignet.binding.key-expire-days}")
    private int expireInDays;

    @Autowired
    private HelperService helperService;

    @Autowired
    private CacheService cacheService;

    @Autowired
    private IdentityAPIClient identityAPIClient;

    @Value("${mosip.compass.email.subject}")
    private String emailSubject;

    @Value("${mosip.compass.email.content}")
    private String emailContent;

    private static final Map<String, List<String>> supportedKeyBindingFormats = new HashMap<>();

    private static final String SEND_OTP_SMS_NOTIFICATION_TEMPLATE_KEY = "mosip.esignet.sms-notification-template.send-otp" ;

    static {
        supportedKeyBindingFormats.put("WLA", List.of("jwt"));
        supportedKeyBindingFormats.put("OTP", List.of("alpha-numeric"));
    }

    @Override
    public SendOtpResult sendBindingOtp(String individualId, List<String> otpChannels,
                                        Map<String, String> requestHeaders) throws SendOtpException {
        String transactionId= "transactionId";
        String challenge = identityAPIClient.generateOTPChallenge(transactionId);
        String challengeHash = IdentityProviderUtil.generateB64EncodedHash(IdentityProviderUtil.ALGO_SHA3_256, challenge);
        cacheService.setChallengeHash(challengeHash,transactionId);
        HashMap<String, String> hashMap = new LinkedHashMap<>();
        hashMap.put("{challenge}", challenge);
        UserInfo userInfo=identityAPIClient.getUserInfoByNationalUid(individualId);
        String email=userInfo.getEmail();
        String firstName=userInfo.getFirstNamePrimary();
        identityAPIClient.sendEmailNotification(
                new String[]{email},
                null,
                new String[]{emailSubject},
                new String[]{String.format(emailContent,firstName,challenge)},
                null
        );
        SendOtpResult sendOtpResult=new SendOtpResult();
        sendOtpResult.setTransactionId(transactionId);
        return sendOtpResult;
    }

    @Override
    public KeyBindingResult doKeyBinding(String individualId, List<AuthChallenge> challengeList,
                                         Map<String, Object> publicKeyJWK, String bindAuthFactorType, Map<String, String> requestHeaders) throws KeyBindingException {
        KeyBindingResult keyBindingResult = new KeyBindingResult();

        if (!supportedBindAuthFactorTypes.contains(bindAuthFactorType)) {
            throw new KeyBindingException("invalid_bind_auth_factor_type");
        }

        KycAuth kycAuth= null;
        try {
            var kycAuthResult = helperService.validateOtpBasedAuth(individualId, challengeList.get(0), "transactionId");
            if (kycAuthResult == null || kycAuthResult.getKycToken() == null) {
                throw new KeyBindingException(ErrorConstants.KEY_BINDING_FAILED);
            }

            kycAuth = cacheService.getKycAuth(kycAuthResult.getKycToken());

        } catch (KycAuthException e) {
            throw new KeyBindingException(e.getErrorCode());
        }

        if(kycAuth==null || kycAuth.getUserInfo()==null){
            throw new KeyBindingException("compass-ida-006");
        }

        //create a signed certificate, with cn as username
        //certificate validity based on configuration
        try {
            RSAKey rsaKey = RSAKey.parse(new JSONObject(publicKeyJWK).toJSONString());
            X509V3CertificateGenerator generator = new X509V3CertificateGenerator();
            String username = kycAuth.getUserInfo().getFirstNamePrimary();
            generator.setSubjectDN(new X500Principal("CN=" + username));
            generator.setIssuerDN(new X500Principal("CN=Compass-IDA"));
            LocalDateTime notBeforeDate = DateUtils.getUTCCurrentDateTime();
            LocalDateTime notAfterDate = notBeforeDate.plus(expireInDays, ChronoUnit.DAYS);
            generator.setNotBefore(Timestamp.valueOf(notBeforeDate));
            generator.setNotAfter(Timestamp.valueOf(notAfterDate));
            generator.setPublicKey(rsaKey.toPublicKey());
            generator.setSignatureAlgorithm("SHA256WITHRSA");
            generator.setSerialNumber(new BigInteger(String.valueOf(System.currentTimeMillis())));

            setupMockBindingKey();
            SignatureCertificate signatureCertificate = keymanagerService.getSignatureCertificate(BINDING_SERVICE_APP_ID, Optional.empty(),
                    DateUtils.getUTCCurrentDateTimeString());
            PrivateKey privateKey = signatureCertificate.getCertificateEntry().getPrivateKey();
            StringWriter stringWriter = new StringWriter();
            try (JcaPEMWriter pemWriter = new JcaPEMWriter(stringWriter)) {
                pemWriter.writeObject(generator.generate(privateKey));
                pemWriter.flush();
                keyBindingResult.setCertificate(stringWriter.toString());
            }
        } catch (Exception e) {
            log.error("Failed to perform key binding", e);
            throw new RuntimeException(e);
        }
        keyBindingResult.setPartnerSpecificUserToken(individualId);
        return keyBindingResult;
    }

    private void setupMockBindingKey() {
        KeyPairGenerateRequestDto mockBindingKeyRequest = new KeyPairGenerateRequestDto();
        mockBindingKeyRequest.setApplicationId(BINDING_SERVICE_APP_ID);
        keymanagerService.generateMasterKey("CSR", mockBindingKeyRequest);
        log.info("===================== MOCK_BINDING_SERVICE KEY SETUP COMPLETED ========================");
    }

    @Override
    public List<String> getSupportedChallengeFormats(String authFactorType) {
        return supportedKeyBindingFormats.getOrDefault(authFactorType, List.of());
    }
}