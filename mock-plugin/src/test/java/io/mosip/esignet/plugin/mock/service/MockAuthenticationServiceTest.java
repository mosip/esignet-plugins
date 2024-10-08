package io.mosip.esignet.plugin.mock.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.mosip.esignet.api.dto.*;
import io.mosip.esignet.api.exception.KycAuthException;
import io.mosip.esignet.api.exception.SendOtpException;
import io.mosip.esignet.plugin.mock.dto.KycExchangeResponseDto;
import io.mosip.esignet.api.exception.KycExchangeException;
import io.mosip.esignet.api.util.ErrorConstants;
import io.mosip.esignet.plugin.mock.dto.VerifiedKycExchangeRequestDto;
import io.mosip.kernel.core.http.ResponseWrapper;
import io.mosip.kernel.keymanagerservice.dto.AllCertificatesDataResponseDto;
import io.mosip.kernel.keymanagerservice.dto.CertificateDataResponseDto;
import io.mosip.kernel.keymanagerservice.service.KeymanagerService;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import static org.mockito.ArgumentMatchers.any;

@RunWith(MockitoJUnitRunner.class)
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

    /*@Test
    public void doVerifiedKycExchange_withValidDetails_thenPass() throws KycExchangeException {
        ReflectionTestUtils.setField(mockAuthenticationService, "kycExchangeUrl", "http://localhost:8080/kyc/exchange");
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        ReflectionTestUtils.setField(mockAuthenticationService, "objectMapper", objectMapper);


        VerifiedKycExchangeDto verifiedKycExchangeDto = new VerifiedKycExchangeDto();
        verifiedKycExchangeDto.setKycToken("kycToken");
        verifiedKycExchangeDto.setAcceptedClaims(Arrays.asList("name", "gender"));
        verifiedKycExchangeDto.setClaimsLocales(new String[]{"eng", "hin"});



        Map<String, VerificationFilter> acceptedVerifiedClaims=new HashMap<>();

        VerificationFilter verificationFilterForName= new VerificationFilter();
        FilterCriteria trustFrameWorkCriteria = new FilterCriteria();
        trustFrameWorkCriteria.setValues(List.of("PWD", "Income-tax"));
        FilterDateTime filterDateTime = new FilterDateTime();
        filterDateTime.setMax_age(1000);
        verificationFilterForName.setTime(filterDateTime);
        verificationFilterForName.setTrust_framework(trustFrameWorkCriteria);

        VerificationFilter verificationFilterForEmail= new VerificationFilter();
        FilterCriteria trustFrameworkForEmail = new FilterCriteria();
        trustFrameworkForEmail.setValues(List.of("PWD"));
        filterDateTime.setMax_age(500);
        verificationFilterForEmail.setTime(filterDateTime);
        verificationFilterForEmail.setTrust_framework(trustFrameworkForEmail);


        acceptedVerifiedClaims.put("name", verificationFilterForName);
        acceptedVerifiedClaims.put("email", verificationFilterForEmail);

        verifiedKycExchangeDto.setAcceptedVerifiedClaims(acceptedVerifiedClaims);
        KycExchangeResponseDto kycExchangeResponseDto = new KycExchangeResponseDto();
        kycExchangeResponseDto.setKyc("responseKyc");
        ResponseWrapper responseWrapper = new ResponseWrapper();
        responseWrapper.setResponse(kycExchangeResponseDto);
        ResponseEntity<ResponseWrapper<KycExchangeResponseDto>> responseEntity  = new ResponseEntity(responseWrapper, HttpStatus.OK);
        Mockito.when(restTemplate.exchange(
                Mockito.any(RequestEntity.class),
                Mockito.eq(new ParameterizedTypeReference<ResponseWrapper<KycExchangeResponseDto>>() {
                })
        )).thenReturn(responseEntity);

        KycExchangeResult kycExchangeResult = mockAuthenticationService.doVerifiedKycExchange("RP", "CL", verifiedKycExchangeDto);
        Assert.assertEquals(kycExchangeResponseDto.getKyc(), kycExchangeResult.getEncryptedKyc());

    }

    @Test
    public void doVerifiedKycExchange_withEmptyResponse_thenFail() {
        ReflectionTestUtils.setField(mockAuthenticationService, "kycExchangeUrl", "http://localhost:8080/kyc/exchange");
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        ReflectionTestUtils.setField(mockAuthenticationService, "objectMapper", objectMapper);

        VerifiedKycExchangeDto verifiedKycExchangeDto = new VerifiedKycExchangeDto();
        verifiedKycExchangeDto.setKycToken("kycToken");
        verifiedKycExchangeDto.setAcceptedClaims(Arrays.asList("name", "gender"));
        verifiedKycExchangeDto.setClaimsLocales(new String[]{"eng", "hin"});



        Map<String, VerificationFilter> acceptedVerifiedClaims=new HashMap<>();

        VerificationFilter verificationFilterForName= new VerificationFilter();
        FilterCriteria trustFrameWorkCriteria = new FilterCriteria();
        trustFrameWorkCriteria.setValues(List.of("PWD", "Income-tax"));
        FilterDateTime filterDateTime = new FilterDateTime();
        filterDateTime.setMax_age(1000);
        verificationFilterForName.setTime(filterDateTime);
        verificationFilterForName.setTrust_framework(trustFrameWorkCriteria);

        VerificationFilter verificationFilterForEmail= new VerificationFilter();
        FilterCriteria trustFrameworkForEmail = new FilterCriteria();
        trustFrameworkForEmail.setValues(List.of("PWD"));
        filterDateTime.setMax_age(500);
        verificationFilterForEmail.setTime(filterDateTime);
        verificationFilterForEmail.setTrust_framework(trustFrameworkForEmail);


        acceptedVerifiedClaims.put("name", verificationFilterForName);
        acceptedVerifiedClaims.put("email", verificationFilterForEmail);

        verifiedKycExchangeDto.setAcceptedVerifiedClaims(acceptedVerifiedClaims);
        ResponseWrapper responseWrapper = new ResponseWrapper();
        responseWrapper.setResponse(null);
        ResponseEntity<ResponseWrapper<KycExchangeResponseDto>> responseEntity = new ResponseEntity(responseWrapper, HttpStatus.OK);
        Mockito.when(restTemplate.exchange(
                Mockito.any(RequestEntity.class),
                Mockito.eq(new ParameterizedTypeReference<ResponseWrapper<KycExchangeResponseDto>>() {
                })
        )).thenReturn(responseEntity);
        try {
            mockAuthenticationService.doVerifiedKycExchange("RP", "CL", verifiedKycExchangeDto);
            Assert.fail();
        } catch (KycExchangeException e) {
            Assert.assertEquals(e.getErrorCode(), ErrorConstants.DATA_EXCHANGE_FAILED);
        }
    }

    @Test
    public void doVerifiedKycExchange_withInvalidDetails_thenFail() {
        ReflectionTestUtils.setField(mockAuthenticationService, "kycExchangeUrl", "http://localhost:8080/kyc/exchange");
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        ReflectionTestUtils.setField(mockAuthenticationService, "objectMapper", objectMapper);

        VerifiedKycExchangeDto verifiedKycExchangeDto = new VerifiedKycExchangeDto();
        verifiedKycExchangeDto.setKycToken("kycToken");
        verifiedKycExchangeDto.setAcceptedClaims(Arrays.asList("name", "gender"));
        verifiedKycExchangeDto.setClaimsLocales(new String[]{"eng", "hin"});



        Map<String, VerificationFilter> acceptedVerifiedClaims=new HashMap<>();

        VerificationFilter verificationFilterForName= new VerificationFilter();
        FilterCriteria trustFrameWorkCriteria = new FilterCriteria();
        trustFrameWorkCriteria.setValues(List.of("PWD", "Income-tax"));
        FilterDateTime filterDateTime = new FilterDateTime();
        filterDateTime.setMax_age(1000);
        verificationFilterForName.setTime(filterDateTime);
        verificationFilterForName.setTrust_framework(trustFrameWorkCriteria);

        VerificationFilter verificationFilterForEmail= new VerificationFilter();
        FilterCriteria trustFrameworkForEmail = new FilterCriteria();
        trustFrameworkForEmail.setValues(List.of("PWD"));
        filterDateTime.setMax_age(500);
        verificationFilterForEmail.setTime(filterDateTime);
        verificationFilterForEmail.setTrust_framework(trustFrameworkForEmail);


        acceptedVerifiedClaims.put("name", verificationFilterForName);
        acceptedVerifiedClaims.put("email", verificationFilterForEmail);

        verifiedKycExchangeDto.setAcceptedVerifiedClaims(acceptedVerifiedClaims);

        try {
            mockAuthenticationService.doVerifiedKycExchange("RP", "CL", verifiedKycExchangeDto);
            Assert.fail();
        } catch (KycExchangeException e) {
            Assert.assertEquals(e.getErrorCode(),"mock-ida-005");
        }
    }*/

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
        kycExchangeDto.setUserInfoResponseType("JWT");
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
        Assert.assertEquals(kycExchangeResponseDto.getKyc(), kycExchangeResult.getEncryptedKyc());
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
        kycExchangeDto.setUserInfoResponseType("JWT");
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
            Assert.fail();
        } catch (KycExchangeException e) {
            Assert.assertEquals(e.getErrorCode(), ErrorConstants.DATA_EXCHANGE_FAILED);
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
        kycExchangeDto.setUserInfoResponseType("JWT");
        kycExchangeDto.setClaimsLocales(new String[]{"en","fr"});
        try {
            mockAuthenticationService.doKycExchange("RP", "CL", kycExchangeDto);
            Assert.fail();
        }  catch (KycExchangeException e) {
            Assert.assertEquals(e.getErrorCode(),"mock-ida-005");
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
        Assert.assertEquals(sendOtpResult, result);
    }

    @Test
    public void sendOtp_withInValidDetails_thenFail() throws SendOtpException {

        SendOtpDto se = new SendOtpDto();
        se.setTransactionId("transactionId");
        se.setOtpChannels(Arrays.asList("email", "mobile"));
        SendOtpResult sendOtpResult = new SendOtpResult();
        sendOtpResult.setTransactionId("transactionId");
        sendOtpResult.setMaskedEmail("maskedEmail");
        sendOtpResult.setMaskedMobile("maskedMobile");
        Mockito.when(mockHelperService.sendOtpMock(se.getTransactionId(), se.getIndividualId(), se.getOtpChannels(), "relyingPartyId", "clientId")).thenReturn(sendOtpResult);
        try{
            mockAuthenticationService.sendOtp("relyingPartyId", "clientId", se);
        }catch (SendOtpException e){
            Assert.assertEquals(e.getErrorCode(), "invalid_transaction_id");
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
        Assert.assertNotNull(allKycSigningCertificates);
        Assert.assertEquals(allKycSigningCertificates.size(), 2);
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
        Assert.assertEquals(expectedResult, result);
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

        VerifiedKycExchangeRequestDto verifiedRequestDto = new VerifiedKycExchangeRequestDto();
        verifiedRequestDto.setTransactionId(kycExchangeDto.getTransactionId());
        verifiedRequestDto.setKycToken(kycExchangeDto.getKycToken());
        verifiedRequestDto.setIndividualId(kycExchangeDto.getIndividualId());
        verifiedRequestDto.setClaimLocales(Arrays.asList(kycExchangeDto.getClaimsLocales()));
        verifiedRequestDto.setAcceptedClaimDetail(kycExchangeDto.getAcceptedClaimDetails());

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
        Assert.assertEquals("mockKyc", result.getEncryptedKyc());
    }

    @Test
    public void doVerifiedKycExchange_withInvalidRequest_thenFail() {
        String relyingPartyId = "testRelyingPartyId";
        String clientId = "testClientId";
        VerifiedKycExchangeDto kycExchangeDto = new VerifiedKycExchangeDto();
        ResponseEntity<ResponseWrapper<KycExchangeResponseDto>> responseEntity =
                new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        KycExchangeException exception = Assert.assertThrows(KycExchangeException.class, () ->
                mockAuthenticationService.doVerifiedKycExchange(relyingPartyId, clientId, kycExchangeDto)
        );
        Assert.assertEquals("mock-ida-005", exception.getErrorCode());
    }

    @Test
    public void doVerifiedKycExchange_throwsKycExchangeException_thenFail() {
        String relyingPartyId = "testRelyingPartyId";
        String clientId = "testClientId";
        VerifiedKycExchangeDto kycExchangeDto = new VerifiedKycExchangeDto();
        KycExchangeException exception = Assert.assertThrows(KycExchangeException.class, () ->
                mockAuthenticationService.doVerifiedKycExchange(relyingPartyId, clientId, kycExchangeDto)
        );
        Assert.assertEquals("mock-ida-005", exception.getErrorCode());
    }
}


