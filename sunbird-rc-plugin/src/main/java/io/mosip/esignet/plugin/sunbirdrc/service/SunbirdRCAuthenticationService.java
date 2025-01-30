/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.esignet.plugin.sunbirdrc.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.RSAKey;
import io.mosip.esignet.api.dto.*;
import io.mosip.esignet.api.exception.KycAuthException;
import io.mosip.esignet.api.exception.KycExchangeException;
import io.mosip.esignet.api.exception.SendOtpException;
import io.mosip.esignet.api.spi.Authenticator;
import io.mosip.esignet.api.util.ErrorConstants;
import io.mosip.esignet.plugin.sunbirdrc.dto.RegistrySearchRequestDto;
import io.mosip.kernel.keymanagerservice.dto.AllCertificatesDataResponseDto;
import io.mosip.kernel.keymanagerservice.dto.CertificateDataResponseDto;
import io.mosip.kernel.keymanagerservice.service.KeymanagerService;
import io.mosip.kernel.signature.dto.JWTSignatureRequestDto;
import io.mosip.kernel.signature.dto.JWTSignatureResponseDto;
import io.mosip.kernel.signature.service.SignatureService;
import lombok.extern.slf4j.Slf4j;
import org.jose4j.jwe.ContentEncryptionAlgorithmIdentifiers;
import org.jose4j.jwe.JsonWebEncryption;
import org.jose4j.jwe.KeyManagementAlgorithmIdentifiers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.PostConstruct;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.nio.charset.StandardCharsets;
import java.util.*;


@ConditionalOnProperty(value = "mosip.esignet.integration.authenticator", havingValue = "SunbirdRCAuthenticationService")
@Component
@Slf4j
public class SunbirdRCAuthenticationService implements Authenticator {

    private final String FILTER_EQUALS_OPERATOR="eq";

    private final String FIELD_ID_KEY="id";

    @Value("#{${mosip.esignet.authenticator.sunbird-rc.auth-factor.kbi.field-details}}")
    private List<Map<String,String>> fieldDetailList;

    @Value("${mosip.esignet.authenticator.sunbird-rc.auth-factor.kbi.registry-search-url}")
    private String registrySearchUrl;

    @Value("${mosip.esignet.authenticator.sunbird-rc.auth-factor.kbi.individual-id-field}")
    private String idField;

    @Value("${mosip.esignet.authenticator.sunbird-rc.kbi.entity-id-field}")
    private String entityIdField;

    @Value("${mosip.esignet.authenticator.sunbird-rc.kyc.encrypt:false}")
    private boolean encryptKyc;

    @Value("${mosip.esignet.authenticator.sunbird-rc.registry-get-url}")
    private String registryUrl;

    @Value("#{${mosip.esignet.authenticator.sunbird-rc.identity-openid-claims-mapping}}")
    private Map<String,String> oidcClaimsMapping;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private KeymanagerService keymanagerService;

    @Autowired
    private SignatureService signatureService;

    private static final Base64.Encoder urlSafeEncoder = Base64.getUrlEncoder().withoutPadding();

    public static final String APPLICATION_ID = "OIDC_PARTNER";


    @PostConstruct
    public void initialize() throws KycAuthException {
        log.info("Started to setup Sunbird-RC Authenticator");
        boolean individualIdFieldIsValid = false;
        if(fieldDetailList==null || fieldDetailList.isEmpty()){
            log.error("Invalid configuration for field-details");
            throw new KycAuthException("sunbird-rc authenticator field is not configured properly");
        }
        for (Map<String, String> field : fieldDetailList) {
            if (field.containsKey(FIELD_ID_KEY) && field.get(FIELD_ID_KEY).equals(idField)) {
                individualIdFieldIsValid = true;
                break;
            }
        }
        if (!individualIdFieldIsValid) {
            log.error("Invalid configuration: The 'individual-id-field' '{}' is not available in 'field-details'.", idField);
            throw new KycAuthException("Invalid configuration: individual-id-field is not available in field-details.");
        }
    }

    @Validated
    @Override
    public KycAuthResult doKycAuth(@NotBlank String relyingPartyId, @NotBlank String clientId,
                                   @NotNull @Valid KycAuthDto kycAuthDto) throws KycAuthException {

        log.info("Started to build kyc-auth request with transactionId : {} && clientId : {}",
                kycAuthDto.getTransactionId(), clientId);
        try {
            Optional<AuthChallenge> optionalAuthChallenge = kycAuthDto.getChallengeList().stream()
                    .filter(authChallenge -> Objects.equals(authChallenge.getAuthFactorType(), "KBI"))
                    .findFirst();
            if (optionalAuthChallenge.isPresent()) {
                return validateKnowledgeBasedAuth(kycAuthDto.getIndividualId(), optionalAuthChallenge.get());
            }
            else{
                throw new KycAuthException("invalid_challenge_type");
            }
        } catch (KycAuthException e) {
            throw e;
        } catch (Exception e) {
            log.error("KYC-auth failed with transactionId : {} && clientId : {}", kycAuthDto.getTransactionId(),
                    clientId, e);
        }
        throw new KycAuthException(ErrorConstants.AUTH_FAILED);
    }

    @Override
    public KycExchangeResult doKycExchange(String relyingPartyId, String clientId, KycExchangeDto kycExchangeDto)
            throws KycExchangeException {
        String kycToken=kycExchangeDto.getKycToken();
        Map<String,Object> responseRegistryMap;
        try {
            kycExchangeDto.setAcceptedClaims(
                    Optional.ofNullable(kycExchangeDto.getAcceptedClaims())
                            .map(claims -> new ArrayList<>(new LinkedHashSet<>(claims)))
                            .orElse(new ArrayList<>())
            );
            responseRegistryMap =fetchRegistryObject(registryUrl+ kycToken);
            if (responseRegistryMap == null) {
                throw new KycExchangeException(ErrorConstants.DATA_EXCHANGE_FAILED);
            }
            Map<String, Object> kyc = buildKycDataBasedOnPolicy(responseRegistryMap,
                    kycExchangeDto.getAcceptedClaims(), kycExchangeDto.getClaimsLocales());
            kyc.put("sub", kycExchangeDto.getIndividualId());
            String finalKyc = this.encryptKyc ? getJWE(relyingPartyId, signKyc(kyc)) : signKyc(kyc);
            KycExchangeResult kycExchangeResult = new KycExchangeResult();
            kycExchangeResult.setEncryptedKyc(finalKyc);
            return kycExchangeResult;
        } catch (Exception e) {
            log.error("KYC exchange failed", e);
            throw new KycExchangeException(ErrorConstants.DATA_EXCHANGE_FAILED);
        }
    }

    private Map<String,Object> fetchRegistryObject(String entityUrl) throws KycExchangeException {
        RequestEntity requestEntity = RequestEntity
                .get(UriComponentsBuilder.fromUriString(entityUrl).build().toUri()).build();
        ResponseEntity<Map<String,Object>> responseEntity = restTemplate.exchange(requestEntity,
                new ParameterizedTypeReference<Map<String,Object>>() {});
        if (responseEntity.getStatusCode().is2xxSuccessful() && responseEntity.getBody() != null) {
            return responseEntity.getBody();
        }else {
            log.error("Sunbird service is not running. Status Code: " ,responseEntity.getStatusCode());
            throw new KycExchangeException(ErrorConstants.DATA_EXCHANGE_FAILED);
        }
    }

    public Map<String, Object> buildKycDataBasedOnPolicy(Map<String, Object> credentialSubject, List<String> claims, String[] locales) {
        Map<String, Object> kyc = new HashMap<>();
        for (String claim : claims) {
            if (oidcClaimsMapping.containsKey(claim)) {
                String mappedKey = oidcClaimsMapping.get(claim);
                if (credentialSubject.containsKey(mappedKey)) {
                    Object value = credentialSubject.get(mappedKey);
                    kyc.put(claim, value);
                }
            }
        }
        return kyc;
    }

    private String getJWE(String relyingPartyId, String signedJwt) throws Exception {
        JsonWebEncryption jsonWebEncryption = new JsonWebEncryption();
        jsonWebEncryption.setAlgorithmHeaderValue(KeyManagementAlgorithmIdentifiers.RSA_OAEP_256);
        jsonWebEncryption.setEncryptionMethodHeaderParameter(ContentEncryptionAlgorithmIdentifiers.AES_256_GCM);
        jsonWebEncryption.setPayload(signedJwt);
        jsonWebEncryption.setContentTypeHeaderValue("JWT");
        RSAKey rsaKey = getRelyingPartyPublicKey(relyingPartyId);
        jsonWebEncryption.setKey(rsaKey.toPublicKey());
        jsonWebEncryption.setKeyIdHeaderValue(rsaKey.getKeyID());
        return jsonWebEncryption.getCompactSerialization();
    }

    private RSAKey getRelyingPartyPublicKey(String relyingPartyId) throws KycExchangeException {
        //TODO where to store relying-party public key
        throw new KycExchangeException(ErrorConstants.DATA_EXCHANGE_FAILED);
    }

    private String signKyc(Map<String, Object> kyc) throws JsonProcessingException {
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

    public static String b64Encode(String value) {
        return urlSafeEncoder.encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public SendOtpResult sendOtp(String relyingPartyId, String clientId, SendOtpDto sendOtpDto)
            throws SendOtpException {
        throw new SendOtpException(ErrorConstants.NOT_IMPLEMENTED);
        }

    @Override
    public boolean isSupportedOtpChannel(String channel) {
        return false;
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

    private KycAuthResult validateKnowledgeBasedAuth(String individualId, AuthChallenge authChallenge) throws KycAuthException {

        KycAuthResult  kycAuthResult= new KycAuthResult();
        RegistrySearchRequestDto registrySearchRequestDto =new RegistrySearchRequestDto();
        String encodedChallenge=authChallenge.getChallenge();

        byte[] decodedBytes = Base64.getUrlDecoder().decode(encodedChallenge);
        String challenge = new String(decodedBytes, StandardCharsets.UTF_8);

        try {
            registrySearchRequestDto =createRegistrySearchRequestDto(challenge,individualId);
            String requestBody = objectMapper.writeValueAsString(registrySearchRequestDto);
            RequestEntity requestEntity = RequestEntity
                    .post(UriComponentsBuilder.fromUriString(registrySearchUrl).build().toUri())
                    .contentType(MediaType.APPLICATION_JSON_UTF8)
                    .body(requestBody);
            ResponseEntity<List<Map<String,Object>>> responseEntity = restTemplate.exchange(requestEntity,
                    new ParameterizedTypeReference<List<Map<String,Object>>>() {});
            if (responseEntity.getStatusCode().is2xxSuccessful() && responseEntity.getBody() != null) {
                List<Map<String,Object>> responseList = responseEntity.getBody();
                if(responseList != null && responseList.size()==1){
                    //TODO  This need to be removed since it can contain PII
                    log.debug("getting response {}", responseEntity);
                    kycAuthResult.setKycToken((String)responseList.get(0).get(entityIdField));
                    kycAuthResult.setPartnerSpecificUserToken((String)responseList.get(0).get(entityIdField));
                    return kycAuthResult;
                }else{
                    log.error("Registry search returns more than one match, so authentication is considered as failed. Result size: " + responseList.size());
                    throw new KycAuthException(ErrorConstants.AUTH_FAILED );
                }
            }else {
                log.error("Sunbird service is not running. Status Code: " ,responseEntity.getStatusCode());
                throw new KycAuthException(ErrorConstants.AUTH_FAILED);
            }

        } catch (Exception e) {
            log.error("Failed to do the Authentication: {}",e);
            throw new KycAuthException(ErrorConstants.AUTH_FAILED );
        }
    }

    private RegistrySearchRequestDto createRegistrySearchRequestDto(String challenge, String individualId) throws KycAuthException, JsonProcessingException {
        RegistrySearchRequestDto registrySearchRequestDto =new RegistrySearchRequestDto();
        registrySearchRequestDto.setLimit(2);
        registrySearchRequestDto.setOffset(0);
        Map<String,Map<String,String>> filter=new HashMap<>();

        Map<String, String> challengeMap = objectMapper.readValue(challenge, Map.class);


        for(Map<String,String> fieldDetailMap: fieldDetailList) {
            Map<String,String> hashMap=new HashMap<>();
            if(!StringUtils.isEmpty(idField) && fieldDetailMap.get(FIELD_ID_KEY).equals(idField)){
                hashMap.put(FILTER_EQUALS_OPERATOR,individualId);
            }else{
                if(!challengeMap.containsKey(fieldDetailMap.get(FIELD_ID_KEY)))
                {
                    log.error("Field '{}' is missing in the challenge.", fieldDetailMap.get(FIELD_ID_KEY));
                    throw new KycAuthException(ErrorConstants.AUTH_FAILED );
                }
                hashMap.put(FILTER_EQUALS_OPERATOR,challengeMap.get(fieldDetailMap.get(FIELD_ID_KEY)));
            }
            filter.put(fieldDetailMap.get(FIELD_ID_KEY),hashMap);
        }
        registrySearchRequestDto.setFilters(filter);
        return registrySearchRequestDto;
    }
}
