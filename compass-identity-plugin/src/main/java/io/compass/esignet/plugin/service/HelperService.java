package io.compass.esignet.plugin.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.compass.esignet.plugin.dto.UserInfo;
import io.mosip.esignet.api.dto.*;
import io.mosip.esignet.api.exception.KycAuthException;
import io.mosip.esignet.api.exception.KycExchangeException;
import io.mosip.esignet.api.spi.KeyBindingValidator;
import io.mosip.esignet.api.util.ErrorConstants;
import io.compass.esignet.plugin.util.IdentityAPIClient;
import io.mosip.esignet.core.util.IdentityProviderUtil;
import io.mosip.kernel.signature.dto.JWTSignatureRequestDto;
import io.mosip.kernel.signature.dto.JWTSignatureResponseDto;
import io.mosip.kernel.signature.service.SignatureService;
import io.compass.esignet.plugin.dto.KycAuth;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;


@Component
@Slf4j
public class HelperService {

    public static final String ALGO_SHA3_256 = "SHA3-256";

    private static final Base64.Encoder urlSafeEncoder = Base64.getUrlEncoder().withoutPadding();

    public static final String APPLICATION_ID = "OIDC_SERVICE";

    @Value("${mosip.esignet.compass.authenticator.otp-channels:email,phone}")
    private List<String> otpChannels;

    @Value("${mosip.esignet.compass.authenticator.ida.otp-value:111111}")
    private String otpValue;

    @Autowired
    private CacheService cacheService;

    @Autowired
    private SignatureService signatureService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private IdentityAPIClient identityAPIClient;

    @Autowired
    private KeyBindingValidator keyBindingValidator;

    public static String b64Encode(String value) {
        return urlSafeEncoder.encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    public boolean isSupportedOtpChannel(String channel) {
        return channel != null && otpChannels.contains(channel.toLowerCase());
    }

    public KycAuthResult validateOtpBasedAuth(String individualId, AuthChallenge authChallenge, String transactionId) throws KycAuthException {
        try {
                String challengeHash = IdentityProviderUtil.generateB64EncodedHash(IdentityProviderUtil.ALGO_SHA3_256, authChallenge.getChallenge());
                String storedHash = cacheService.getChallengeHash(transactionId);

                if (storedHash != null && storedHash.equals(challengeHash)) {
                    String kycToken = generateB64EncodedHash(ALGO_SHA3_256, UUID.randomUUID().toString());
                    UserInfo userInfo = identityAPIClient.getUserInfoByNationalUid(individualId);
                    KycAuthResult kycAuthResult = new KycAuthResult();
                    kycAuthResult.setKycToken(kycToken);
                    kycAuthResult.setPartnerSpecificUserToken(individualId);
                    cacheService.setKycAuth(kycToken,new KycAuth(kycToken, individualId, LocalDateTime.now(ZoneOffset.UTC), "transactionId",
                            individualId
                            , userInfo
                    ));
                    return kycAuthResult;
                }
        }  catch (Exception e) {
            log.error("Failed to do the Authentication",e);
            throw new KycAuthException(ErrorConstants.AUTH_FAILED );
        }
        throw new KycAuthException(ErrorConstants.AUTH_FAILED);
    }

    public KycAuthResult validateWla(String individualId, AuthChallenge authChallenge) throws KycAuthException {
        KycAuthResult  kycAuthResult= new KycAuthResult();

        try {

            BindingAuthResult bindingAuthResult = keyBindingValidator.validateBindingAuth("transactionId",
                    individualId, List.of(authChallenge));
            if(bindingAuthResult == null)
                throw new KycAuthException(ErrorConstants.AUTH_FAILED );

            UserInfo userInfo = identityAPIClient.getUserInfoByNationalUid(individualId);

            if(userInfo !=null) {
                String kycToken = generateB64EncodedHash(ALGO_SHA3_256, UUID.randomUUID().toString());
                    kycAuthResult.setKycToken(kycToken);
                    kycAuthResult.setPartnerSpecificUserToken(individualId);
                    cacheService.setKycAuth(kycToken,new KycAuth(kycToken, individualId, LocalDateTime.now(ZoneOffset.UTC), "transactionId",
                            individualId
                            , userInfo
                    ));
                    return kycAuthResult;
            }
        } catch (Exception e) {
            log.error("Failed to do the Authentication ",e);
            throw new KycAuthException(ErrorConstants.AUTH_FAILED );
        }
        throw new KycAuthException(ErrorConstants.AUTH_FAILED );
    }

    public KycAuthResult validateOtpForWlaBasedAuth(String individualId, AuthChallenge authChallenge, String transactionId) throws KycAuthException {
        KycAuthResult kycAuthResult = new KycAuthResult();
        if (authChallenge.getAuthFactorType().equals("OTP") && authChallenge.getFormat().equals("alpha-numeric")) {

            if (authChallenge.getChallenge().equals(otpValue)) {
                String kycToken = generateB64EncodedHash(ALGO_SHA3_256, UUID.randomUUID().toString());
                kycAuthResult.setKycToken(kycToken);
                kycAuthResult.setPartnerSpecificUserToken(individualId);
                UserInfo userInfo = identityAPIClient.getUserInfoByNationalUid(individualId);
                cacheService.setKycAuth(kycToken, new KycAuth(kycToken, kycToken, LocalDateTime.now(ZoneOffset.UTC), transactionId,
                        individualId
                        ,userInfo
                ));
                return kycAuthResult;
            } else {
                throw new KycAuthException(ErrorConstants.AUTH_FAILED);
            }
        }
        return kycAuthResult;
    }

    private String generateB64EncodedHash(String algorithm, String value) throws KycAuthException {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return urlSafeEncoder.encodeToString(hash);
        } catch (NoSuchAlgorithmException ex) {
            log.error("Invalid algorithm : {}", algorithm, ex);
            throw new KycAuthException("invalid_algorithm");
        }
    }

    public Map<String, Object> buildKycDataBasedOnPolicy(List<String> claims, UserInfo userInfo) throws KycExchangeException {
        Map<String, Object> kyc = new HashMap<>();
        for (String claim : claims) {
            switch (claim) {
                case "userInfoId":
                    if (userInfo.getUserInfoId() != null) {
                        kyc.put("userInfoId", userInfo.getFirstNamePrimary());
                    }
                    break;
                case "birthdate":
                    if (userInfo.getDateOfBirth() != null) {
                        kyc.put("birthdate", userInfo.getDateOfBirth());
                    }
                    break;
                case "birthCountry":
                    if (userInfo.getBirthCountry() != null) {
                        kyc.put("birthCountry", userInfo.getBirthCountry());
                    }
                    break;
                case "cardAccessNumber":
                    if (userInfo.getCardAccessNumber() != null) {
                        kyc.put("cardAccessNumber", userInfo.getCardAccessNumber());
                    }
                    break;
                case "email":
                    if (userInfo.getEmail() != null) {
                        kyc.put("email", userInfo.getEmail());
                    }
                    break;
                case "picture":
                    if (userInfo.getFaceImageColor() != null) {
                        kyc.put("picture", "data:image/jpeg;base64," + userInfo.getFaceImageColor());
                    }
                    break;
                case "gender":
                    if (userInfo.getGender() != null) {
                        kyc.put("gender", userInfo.getGender());
                    }
                    break;
                case "first_name":
                    kyc.put("first_name", userInfo.getFirstNamePrimary());
                    break;
                case "last_name":
                    kyc.put("last_name", userInfo.getLastNameSecondary());
                    break;
                case "nationalUid":
                    if (userInfo.getNationalUid() != null) {
                        kyc.put("nationalUid", userInfo.getNationalUid());
                    }
                    break;
                case "nationality":
                    if (userInfo.getNationality() != null) {
                        kyc.put("nationality", userInfo.getNationality());
                    }
                    break;
                case "compassId":
                    if (userInfo.getCompassId() != null) {
                        kyc.put("compassId", userInfo.getCompassId());
                    }
                    break;
                case "issuanceDate":
                    if (userInfo.getIssuanceDate() != null) {
                        kyc.put("issuanceDate", userInfo.getIssuanceDate());
                    }
                    break;
            }
        }
        return kyc;
    }

    public String signKyc(Map<String, Object> kyc) throws JsonProcessingException {
        String payload = objectMapper.writeValueAsString(kyc);
        JWTSignatureRequestDto jwtSignatureRequestDto = new JWTSignatureRequestDto();
        jwtSignatureRequestDto.setApplicationId(APPLICATION_ID);
        jwtSignatureRequestDto.setReferenceId("");
        jwtSignatureRequestDto.setIncludePayload(true);
        jwtSignatureRequestDto.setIncludeCertificate(false);
        jwtSignatureRequestDto.setDataToSign(b64Encode(payload));
        jwtSignatureRequestDto.setIncludeCertHash(false);
        JWTSignatureResponseDto responseDto = signatureService.jwtSign(jwtSignatureRequestDto);
        return responseDto.getJwtSignedData();
    }

}
