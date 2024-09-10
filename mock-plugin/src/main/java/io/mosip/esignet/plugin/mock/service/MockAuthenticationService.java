/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.esignet.plugin.mock.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mosip.esignet.api.dto.claim.FilterCriteria;
import io.mosip.esignet.plugin.mock.dto.KycExchangeResponseDto;
import io.mosip.esignet.api.dto.*;
import io.mosip.esignet.api.exception.KycAuthException;
import io.mosip.esignet.api.exception.KycExchangeException;
import io.mosip.esignet.api.exception.SendOtpException;
import io.mosip.esignet.api.spi.Authenticator;
import io.mosip.esignet.api.util.ErrorConstants;
import io.mosip.esignet.plugin.mock.dto.KycExchangeRequestDto;
import io.mosip.esignet.plugin.mock.dto.VerifiedKycExchangeRequestDto;
import io.mosip.kernel.core.http.ResponseWrapper;
import io.mosip.kernel.core.util.StringUtils;
import io.mosip.kernel.keymanagerservice.dto.AllCertificatesDataResponseDto;
import io.mosip.kernel.keymanagerservice.dto.CertificateDataResponseDto;
import io.mosip.kernel.keymanagerservice.service.KeymanagerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.PostConstruct;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.*;


@ConditionalOnProperty(value = "mosip.esignet.integration.authenticator", havingValue = "MockAuthenticationService")
@Component
@Slf4j
public class MockAuthenticationService implements Authenticator {

    private static final String APPLICATION_ID = "MOCK_AUTHENTICATION_SERVICE";
    public static final String SEND_OTP_FAILED = "send_otp_failed";

    @Value("${mosip.esignet.mock.authenticator.kyc-exchange-url}")
    private String kycExchangeUrl;
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private KeymanagerService keymanagerService;

    @Autowired
    private MockHelperService mockHelperService;

    @Autowired
    private RestTemplate restTemplate;

    @PostConstruct
    public void initialize() {
        log.info("Started to setup MOCK IDA");
    }

    @Validated
    @Override
    public KycAuthResult doKycAuth(@NotBlank String relyingPartyId, @NotBlank String clientId,
                                   @NotNull @Valid KycAuthDto kycAuthDto) throws KycAuthException {
        return mockHelperService.doKycAuthMock(relyingPartyId, clientId, kycAuthDto,false);
    }

    @Override
    public KycExchangeResult doKycExchange(String relyingPartyId, String clientId, KycExchangeDto kycExchangeDto)
            throws KycExchangeException {
        log.info("Started to build kyc-exchange request with transactionId : {} && clientId : {}",
                kycExchangeDto.getTransactionId(), clientId);
        try {
            KycExchangeRequestDto kycExchangeRequestDto = new KycExchangeRequestDto();
            kycExchangeRequestDto.setRequestDateTime(MockHelperService.getUTCDateTime());
            kycExchangeRequestDto.setTransactionId(kycExchangeDto.getTransactionId());
            kycExchangeRequestDto.setKycToken(kycExchangeDto.getKycToken());
            kycExchangeRequestDto.setIndividualId(kycExchangeDto.getIndividualId());
            kycExchangeRequestDto.setAcceptedClaims(kycExchangeDto.getAcceptedClaims());
            kycExchangeRequestDto.setClaimLocales(Arrays.asList(kycExchangeDto.getClaimsLocales()));

            //set signature header, body and invoke kyc exchange endpoint
            String requestBody = objectMapper.writeValueAsString(kycExchangeRequestDto);
            RequestEntity requestEntity = RequestEntity
                    .post(UriComponentsBuilder.fromUriString(kycExchangeUrl).pathSegment(relyingPartyId,
                            clientId).build().toUri())
                    .contentType(MediaType.APPLICATION_JSON_UTF8)
                    .body(requestBody);
            ResponseEntity<ResponseWrapper<KycExchangeResponseDto>> responseEntity = restTemplate.exchange(requestEntity,
                    new ParameterizedTypeReference<>() {
                    });

            if (responseEntity.getStatusCode().is2xxSuccessful() && responseEntity.getBody() != null) {
                ResponseWrapper<KycExchangeResponseDto> responseWrapper = responseEntity.getBody();
                if (responseWrapper.getResponse() != null && responseWrapper.getResponse().getKyc() != null) {
                    return new KycExchangeResult(responseWrapper.getResponse().getKyc());
                }
                log.error("Errors in response received from IDA Kyc Exchange: {}", responseWrapper.getErrors());
                throw new KycExchangeException(CollectionUtils.isEmpty(responseWrapper.getErrors()) ?
                        ErrorConstants.DATA_EXCHANGE_FAILED : responseWrapper.getErrors().get(0).getErrorCode());
            }

            log.error("Error response received from IDA (Kyc-exchange) with status : {}", responseEntity.getStatusCode());
        } catch (KycExchangeException e) {
            throw e;
        } catch (Exception e) {
            log.error("IDA Kyc-exchange failed with clientId : {}", clientId, e);
        }
        throw new KycExchangeException("mock-ida-005", "Failed to build kyc data");
    }

    @Override
    public SendOtpResult sendOtp(String relyingPartyId, String clientId, SendOtpDto sendOtpDto)
            throws SendOtpException {
        if (sendOtpDto == null || StringUtils.isEmpty(sendOtpDto.getTransactionId())) {
            throw new SendOtpException("invalid_transaction_id");
        }

        return mockHelperService.sendOtpMock(sendOtpDto.getTransactionId(), sendOtpDto.getIndividualId(), sendOtpDto.getOtpChannels(), relyingPartyId, clientId);
    }

    @Override
    public boolean isSupportedOtpChannel(String channel) {
        return mockHelperService.isSupportedOtpChannel(channel);
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

    @Override
    public KycAuthResult doKycAuth(String relyingPartyId, String clientId, boolean claimsMetadataRequired, KycAuthDto kycAuthDto) throws KycAuthException {
        return mockHelperService.doKycAuthMock(relyingPartyId, clientId, kycAuthDto, claimsMetadataRequired);
    }

    @Override
    public KycExchangeResult doVerifiedKycExchange(String relyingPartyId, String clientId, VerifiedKycExchangeDto kycExchangeDto) throws KycExchangeException {
        log.info("Started to build verified kyc-exchange request with transactionId : {} && clientId : {}",
                kycExchangeDto.getTransactionId(), clientId);
        try {
            VerifiedKycExchangeRequestDto verifiedKycExchangeRequestDto = buildVerifiedKycExchangeRequestDto(kycExchangeDto);

            //set signature header, body and invoke kyc exchange endpoint
            String requestBody = objectMapper.writeValueAsString(verifiedKycExchangeRequestDto);
            RequestEntity requestEntity = RequestEntity
                    .post(UriComponentsBuilder.fromUriString(kycExchangeUrl).pathSegment(relyingPartyId,
                            clientId).build().toUri())
                    .contentType(MediaType.APPLICATION_JSON_UTF8)
                    .body(requestBody);
            ResponseEntity<ResponseWrapper<KycExchangeResponseDto>> responseEntity = restTemplate.exchange(requestEntity,
                    new ParameterizedTypeReference<>() {
                    });

            if (responseEntity.getStatusCode().is2xxSuccessful() && responseEntity.getBody() != null) {
                ResponseWrapper<KycExchangeResponseDto> responseWrapper = responseEntity.getBody();
                if (responseWrapper.getResponse() != null && responseWrapper.getResponse().getKyc() != null) {
                    return new KycExchangeResult(responseWrapper.getResponse().getKyc());
                }
                log.error("Errors in response received from IDA Kyc Exchange: {}", responseWrapper.getErrors());
                throw new KycExchangeException(CollectionUtils.isEmpty(responseWrapper.getErrors()) ?
                        ErrorConstants.DATA_EXCHANGE_FAILED : responseWrapper.getErrors().get(0).getErrorCode());
            }

            log.error("Error response received from IDA (Kyc-exchange) with status : {}", responseEntity.getStatusCode());
        } catch (KycExchangeException e) {
            throw e;
        } catch (Exception e) {
            log.error("IDA Kyc-exchange failed with clientId : {}", clientId, e);
        }
        throw new KycExchangeException("mock-ida-005", "Failed to build kyc data");
    }

    private VerifiedKycExchangeRequestDto buildVerifiedKycExchangeRequestDto(VerifiedKycExchangeDto verifiedKycExchangeDto){
        VerifiedKycExchangeRequestDto verifiedKycExchangeRequestDto = new VerifiedKycExchangeRequestDto();
        verifiedKycExchangeRequestDto.setRequestDateTime(MockHelperService.getUTCDateTime());
        verifiedKycExchangeRequestDto.setTransactionId(verifiedKycExchangeDto.getTransactionId());
        verifiedKycExchangeRequestDto.setKycToken(verifiedKycExchangeDto.getKycToken());
        verifiedKycExchangeRequestDto.setIndividualId(verifiedKycExchangeDto.getIndividualId());
        verifiedKycExchangeRequestDto.setClaimLocales(Arrays.asList(verifiedKycExchangeDto.getClaimsLocales()));

        Map<String, Object> acceptedClaims = new HashMap<>();
        //setting essential unverified claims
        for(String claim : verifiedKycExchangeDto.getAcceptedClaims()) {
            Map<String,Boolean> essential = new HashMap<>();
            essential.put("essential",true);
            acceptedClaims.put(claim,essential);
        }

        //setting essential verified claims
        Map<String, List<Map<String, Object>>> acceptedVerifiedClaims = verifiedKycExchangeDto.getAcceptedVerifiedClaims();
        //Group claims by trust framework and capture time information
        Map<String, List<Map.Entry<String, Integer>>> trustFrameWorkMap = new HashMap<>();
        /*for (Map.Entry<String, VerificationFilter> entry : acceptedVerifiedClaims.entrySet()) {
            String claimName = entry.getKey();
            FilterCriteria trustFrameworkCriteria = entry.getValue().getTrust_framework();
            FilterDateTime filterDateTime = entry.getValue().getTime();

            int maxAge = filterDateTime != null ? filterDateTime.getMax_age() : 999; // Default value for maxAge if not set

            if(trustFrameworkCriteria!=null && !StringUtils.isEmpty(trustFrameworkCriteria.getValue())){
                trustFrameWorkMap.computeIfAbsent(trustFrameworkCriteria.getValue(), k -> new ArrayList<>()).add(new AbstractMap.SimpleEntry<>(claimName, maxAge));
            }else if (trustFrameworkCriteria!=null && trustFrameworkCriteria.getValues()!=null && !trustFrameworkCriteria.getValues().isEmpty()) {
                for (String trustFramework : trustFrameworkCriteria.getValues()) {
                    trustFrameWorkMap.computeIfAbsent(trustFramework, list -> new ArrayList<>()).add(new AbstractMap.SimpleEntry<>(claimName, maxAge));
                }
            }else{
                // Handle null trust_framework separately
                trustFrameWorkMap.computeIfAbsent(null, list -> new ArrayList<>()).add(new AbstractMap.SimpleEntry<>(claimName, maxAge));
            }
        }
        // Constructing the verified_claims list
        List<Map<String, Object>> verifiedClaimsList = new ArrayList<>();
        for (Map.Entry<String, List<Map.Entry<String, Integer>>> group : trustFrameWorkMap.entrySet()) {
            String trustFramework = group.getKey();
            List<Map.Entry<String, Integer>> claimsWithMaxAge = group.getValue();

            Map<String, Object> verificationMap = new HashMap<>();
            verificationMap.put("trust_framework", trustFramework);
            verificationMap.put("max_age", claimsWithMaxAge.iterator().next().getValue());

            Map<String, Object> claimsMap = new HashMap<>();
            for (Map.Entry<String, Integer> claimWithMaxAge : claimsWithMaxAge) {
                String claimName = claimWithMaxAge.getKey();
                claimsMap.put(claimName, null); // Setting all claim values to null
            }
            Map<String, Object> finalMap = new HashMap<>();
            finalMap.put("verification", verificationMap);
            finalMap.put("claims", claimsMap);

            verifiedClaimsList.add(finalMap);
        }
        acceptedClaims.put("verified_claims",verifiedClaimsList);
        verifiedKycExchangeRequestDto.setAcceptedClaims(acceptedClaims);*/

        return verifiedKycExchangeRequestDto;
    }
}
