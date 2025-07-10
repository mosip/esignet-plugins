package io.mosip.esignet.plugin.mock.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.mosip.esignet.api.dto.*;
import io.mosip.esignet.api.exception.KycAuthException;
import io.mosip.esignet.api.exception.SendOtpException;
import io.mosip.esignet.plugin.mock.dto.KycExchangeResponseDto;
import io.mosip.esignet.api.exception.KycExchangeException;
import io.mosip.esignet.api.util.ErrorConstants;
import io.mosip.esignet.plugin.mock.dto.KycExchangeRequestDtoV2;
import io.mosip.kernel.core.http.ResponseWrapper;
import io.mosip.kernel.keymanagerservice.dto.AllCertificatesDataResponseDto;
import io.mosip.kernel.keymanagerservice.dto.CertificateDataResponseDto;
import io.mosip.kernel.keymanagerservice.service.KeymanagerService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
public class MockAuthenticationServiceTest {

    @InjectMocks
    private MockAuthenticationService mockAuthenticationService;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    MockHelperService mockHelperService;

    @Mock
    KeymanagerService keymanagerService;

    @Mock
    ObjectMapper objectMapper;

    @Test
    public void doKycExchange_withValidDetails_thenPass() throws KycExchangeException {
        ReflectionTestUtils.setField(mockAuthenticationService, "kycExchangeUrl", "http://localhost:8080/kyc/exchange");
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        ReflectionTestUtils.setField(mockAuthenticationService, "objectMapper", objectMapper);

        KycExchangeDto kycExchangeDto = new KycExchangeDto();
        kycExchangeDto.setKycToken("test_token");
        kycExchangeDto.setAcceptedClaims(List.of("name","dob"));
        kycExchangeDto.setIndividualId("test_individual_id");
        kycExchangeDto.setTransactionId("test_transaction_id");
        kycExchangeDto.setClaimsLocales(new String[]{"en","fr"});

        KycExchangeResponseDto kycExchangeResponseDto = new KycExchangeResponseDto();
        kycExchangeResponseDto.setKyc("responseKyc");
        ResponseWrapper responseWrapper = new ResponseWrapper();
        responseWrapper.setResponse(kycExchangeResponseDto);
        ResponseEntity<ResponseWrapper<KycExchangeResponseDto>> responseEntity = new ResponseEntity(responseWrapper, HttpStatus.OK);
        Mockito.when(restTemplate.exchange(
                Mockito.any(RequestEntity.class),
                Mockito.eq(new ParameterizedTypeReference<ResponseWrapper<KycExchangeResponseDto>>() {
                })
        )).thenReturn(responseEntity);

        KycExchangeResult kycExchangeResult = mockAuthenticationService.doKycExchange("RP", "CL", kycExchangeDto);
        Assertions.assertEquals(kycExchangeResponseDto.getKyc(), kycExchangeResult.getEncryptedKyc());
    }

    @Test
    public void doKycExchange_withEmptyResponse_thenFail() {
        ReflectionTestUtils.setField(mockAuthenticationService, "kycExchangeUrl", "http://localhost:8080/kyc/exchange");
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        ReflectionTestUtils.setField(mockAuthenticationService, "objectMapper", objectMapper);

        KycExchangeDto kycExchangeDto = new KycExchangeDto();
        kycExchangeDto.setKycToken("test_token");
        kycExchangeDto.setAcceptedClaims(List.of("name","dob"));
        kycExchangeDto.setIndividualId("test_individual_id");
        kycExchangeDto.setTransactionId("test_transaction_id");
        kycExchangeDto.setClaimsLocales(new String[]{"en","fr"});

        ResponseWrapper responseWrapper = new ResponseWrapper();
        responseWrapper.setResponse(null);
        ResponseEntity<ResponseWrapper<KycExchangeResponseDto>> responseEntity = new ResponseEntity(responseWrapper, HttpStatus.OK);
        Mockito.when(restTemplate.exchange(
                Mockito.any(RequestEntity.class),
                Mockito.eq(new ParameterizedTypeReference<ResponseWrapper<KycExchangeResponseDto>>() {
                })
        )).thenReturn(responseEntity);
        try {
            mockAuthenticationService.doKycExchange("RP", "CL", kycExchangeDto);
            Assertions.fail();
        } catch (KycExchangeException e) {
            Assertions.assertEquals(e.getErrorCode(), ErrorConstants.DATA_EXCHANGE_FAILED);
        }
    }

    @Test
    public void doKycExchange_withInvalidDetails_thenFail() {
        ReflectionTestUtils.setField(mockAuthenticationService, "kycExchangeUrl", "http://localhost:8080/kyc/exchange");
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        ReflectionTestUtils.setField(mockAuthenticationService, "objectMapper", objectMapper);

        KycExchangeDto kycExchangeDto = new KycExchangeDto();
        kycExchangeDto.setKycToken("test_token");
        kycExchangeDto.setAcceptedClaims(List.of("name","dob"));
        kycExchangeDto.setIndividualId("test_individual_id");
        kycExchangeDto.setTransactionId("test_transaction_id");
        kycExchangeDto.setClaimsLocales(new String[]{"en","fr"});
        try {
            mockAuthenticationService.doKycExchange("RP", "CL", kycExchangeDto);
            Assertions.fail();
        }  catch (KycExchangeException e) {
            Assertions.assertEquals(e.getErrorCode(),"mock-ida-005");
        }
    }

    @Test
    public void sendOtp_withValidDetails_thenPass() throws SendOtpException {

        SendOtpDto se = new SendOtpDto();
        se.setIndividualId("individualId");
        se.setTransactionId("transactionId");
        se.setOtpChannels(Arrays.asList("email", "mobile"));
        SendOtpResult sendOtpResult = new SendOtpResult();
        sendOtpResult.setTransactionId("transactionId");
        sendOtpResult.setMaskedEmail("maskedEmail");
        sendOtpResult.setMaskedMobile("maskedMobile");
        Mockito.when(mockHelperService.sendOtpMock(se.getTransactionId(), se.getIndividualId(), se.getOtpChannels(), "relyingPartyId", "clientId")).thenReturn(sendOtpResult);
        SendOtpResult result = mockAuthenticationService.sendOtp("relyingPartyId", "clientId", se);
        Assertions.assertEquals(sendOtpResult, result);
    }

    @Test
    public void sendOtp_withInValidOtpRequest_thenFail() {
        try{
            mockAuthenticationService.sendOtp("relyingPartyId", "clientId", null);
            Assertions.fail();
        }catch (SendOtpException e){
            Assertions.assertEquals(e.getErrorCode(), "invalid_transaction_id");
        }
    }

    @Test
    public void getAllKycSigningCertificates_withValidDetails_thenPass() {

        AllCertificatesDataResponseDto allCertificatesDataResponseDto = new AllCertificatesDataResponseDto();
        allCertificatesDataResponseDto.setAllCertificates(new CertificateDataResponseDto[2]);
        CertificateDataResponseDto certificateDataResponseDto = new CertificateDataResponseDto();
        certificateDataResponseDto.setCertificateData("certificateData");
        certificateDataResponseDto.setExpiryAt(LocalDateTime.MAX);
        certificateDataResponseDto.setIssuedAt(LocalDateTime.MIN);
        certificateDataResponseDto.setKeyId("keyId");
        allCertificatesDataResponseDto.getAllCertificates()[0] = certificateDataResponseDto;
        CertificateDataResponseDto certificateDataResponseDto1 = new CertificateDataResponseDto();
        certificateDataResponseDto1.setCertificateData("certificateData1");
        certificateDataResponseDto1.setExpiryAt(LocalDateTime.MAX);
        certificateDataResponseDto1.setIssuedAt(LocalDateTime.MIN);
        certificateDataResponseDto1.setKeyId("keyId1");
        allCertificatesDataResponseDto.getAllCertificates()[1]= certificateDataResponseDto1;

        Mockito.when(keymanagerService.getAllCertificates(Mockito.anyString(),Mockito.any())).thenReturn(allCertificatesDataResponseDto);
        List<KycSigningCertificateData> allKycSigningCertificates = mockAuthenticationService.getAllKycSigningCertificates();
        Assertions.assertNotNull(allKycSigningCertificates);
        Assertions.assertEquals(allKycSigningCertificates.size(), 2);
    }

    @Test
    public void doKycAuth_withValidDetails_thenPass() throws KycAuthException {
        String relyingPartyId = "testRelyingPartyId";
        String clientId = "testClientId";
        KycAuthDto kycAuthDto = new KycAuthDto();
        KycAuthResult expectedResult = new KycAuthResult();
        Mockito.when(mockHelperService.doKycAuthMock(relyingPartyId, clientId, kycAuthDto, false))
                .thenReturn(expectedResult);
        KycAuthResult result = mockAuthenticationService.doKycAuth(relyingPartyId, clientId, false, kycAuthDto);
        Assertions.assertEquals(expectedResult, result);
    }

    @Test
    public void doVerifiedKycExchange_withValidDetails_thenPass () throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        ReflectionTestUtils.setField(mockAuthenticationService, "kycExchangeV2Url", "http://localhost:8080/kyc/exchange");
        ReflectionTestUtils.setField(mockAuthenticationService, "objectMapper", objectMapper);

        String relyingPartyId = "testRelyingPartyId";
        String clientId = "testClientId";

        VerifiedKycExchangeDto kycExchangeDto = new VerifiedKycExchangeDto();
        kycExchangeDto.setTransactionId("transactionId");
        kycExchangeDto.setKycToken("kycToken");
        kycExchangeDto.setIndividualId("individualId");
        kycExchangeDto.setClaimsLocales(new String[]{"en"});
        kycExchangeDto.setAcceptedClaimDetails(new HashMap<>());

        KycExchangeRequestDtoV2 kycExchangeRequestDtoV2 = new KycExchangeRequestDtoV2();
        kycExchangeRequestDtoV2.setTransactionId(kycExchangeDto.getTransactionId());
        kycExchangeRequestDtoV2.setKycToken(kycExchangeDto.getKycToken());
        kycExchangeRequestDtoV2.setIndividualId(kycExchangeDto.getIndividualId());
        kycExchangeRequestDtoV2.setClaimLocales(Arrays.asList(kycExchangeDto.getClaimsLocales()));
        kycExchangeRequestDtoV2.setAcceptedClaimDetail(kycExchangeDto.getAcceptedClaimDetails());

        KycExchangeResponseDto responseDto = new KycExchangeResponseDto();
        responseDto.setKyc("mockKyc");

        ResponseWrapper<KycExchangeResponseDto> responseWrapper = new ResponseWrapper<>();
        responseWrapper.setResponse(responseDto);

        ResponseEntity<ResponseWrapper<KycExchangeResponseDto>> responseEntity =
                new ResponseEntity<>(responseWrapper, HttpStatus.OK);

        Mockito.when(restTemplate.exchange(
                any(RequestEntity.class),
                any(ParameterizedTypeReference.class))
        ).thenReturn(responseEntity);
        KycExchangeResult result = mockAuthenticationService.doVerifiedKycExchange(relyingPartyId, clientId, kycExchangeDto);
        Assertions.assertEquals("mockKyc", result.getEncryptedKyc());
    }

    @Test
    public void doVerifiedKycExchange_withInvalidRequest_thenFail() {
        String relyingPartyId = "testRelyingPartyId";
        String clientId = "testClientId";
        VerifiedKycExchangeDto kycExchangeDto = new VerifiedKycExchangeDto();
        ResponseEntity<ResponseWrapper<KycExchangeResponseDto>> responseEntity =
                new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        KycExchangeException exception = Assertions.assertThrows(KycExchangeException.class, () ->
                mockAuthenticationService.doVerifiedKycExchange(relyingPartyId, clientId, kycExchangeDto)
        );
        Assertions.assertEquals("mock-ida-005", exception.getErrorCode());
    }

    @Test
    public void doVerifiedKycExchange_throwsKycExchangeException_thenFail() {
        String relyingPartyId = "testRelyingPartyId";
        String clientId = "testClientId";
        VerifiedKycExchangeDto kycExchangeDto = new VerifiedKycExchangeDto();
        KycExchangeException exception = Assertions.assertThrows(KycExchangeException.class, () ->
                mockAuthenticationService.doVerifiedKycExchange(relyingPartyId, clientId, kycExchangeDto)
        );
        Assertions.assertEquals("mock-ida-005", exception.getErrorCode());
    }

    @Test
    public void convertLangCodesToISO3LanguageCodes_withInvalidInput_thenReturnEmpty() {
        Assertions.assertTrue(mockAuthenticationService.convertLangCodesToISO3LanguageCodes(null).isEmpty());
        Assertions.assertTrue(mockAuthenticationService.convertLangCodesToISO3LanguageCodes(new String[]{}).isEmpty());
        Assertions.assertTrue(mockAuthenticationService.convertLangCodesToISO3LanguageCodes(new String[]{"", ""}).isEmpty());
        Assertions.assertTrue(mockAuthenticationService.convertLangCodesToISO3LanguageCodes(new String[]{"e1"}).isEmpty());
    }

    @Test
    public void convertLangCodesToISO3LanguageCodes_withValidInput_thenPass() {
        List<String> langCodes = mockAuthenticationService.convertLangCodesToISO3LanguageCodes(new String[]{"en", "km"});
        Assertions.assertFalse(langCodes.isEmpty());
        Assertions.assertEquals(langCodes.get(0), "eng");
        Assertions.assertEquals(langCodes.get(1), "khm");
    }
}


