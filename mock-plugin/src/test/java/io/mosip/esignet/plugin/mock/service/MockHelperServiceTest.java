package io.mosip.esignet.plugin.mock.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.mosip.esignet.api.dto.*;
import io.mosip.esignet.api.exception.KycAuthException;
import io.mosip.esignet.api.exception.SendOtpException;
import io.mosip.esignet.api.util.ErrorConstants;
import io.mosip.esignet.plugin.mock.dto.KycAuthResponseDtoV2;
import io.mosip.kernel.core.exception.ServiceError;
import io.mosip.kernel.core.http.ResponseWrapper;
import io.mosip.kernel.signature.dto.JWTSignatureResponseDto;
import io.mosip.kernel.signature.service.SignatureService;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
public class MockHelperServiceTest {


    @InjectMocks
    MockHelperService mockHelperService;


    @Mock
    RestTemplate restTemplate;

    @Mock
    SignatureService signatureService;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void doKycAuthMock_withAuthFactorAsOTP_thenPass() throws KycAuthException {

        ReflectionTestUtils.setField(mockHelperService, "kycAuthUrl", "http://localhost:8080/kyc/auth");
        ReflectionTestUtils.setField(mockHelperService, "objectMapper", new ObjectMapper());

        ResponseWrapper<KycAuthResponseDtoV2> responseWrapper = new ResponseWrapper<>();
        KycAuthResponseDtoV2 response = new KycAuthResponseDtoV2();

        Map<String,List<JsonNode>> claimMetaData=new HashMap<>();

        ObjectNode verificationDetail = objectMapper.createObjectNode();
        verificationDetail.put("trust_framework", "test_trust_framework");
        claimMetaData.put("name",List.of(verificationDetail));

        response.setClaimMetadata(claimMetaData);

        response.setAuthStatus(true);
        response.setKycToken("test_token");
        response.setPartnerSpecificUserToken("partner_token");
        responseWrapper.setResponse(response);
        ResponseEntity<ResponseWrapper<KycAuthResponseDtoV2>> responseEntity= new ResponseEntity<>(responseWrapper, HttpStatus.OK);

        Mockito.when(restTemplate.exchange(
                Mockito.any(RequestEntity.class),
                Mockito.eq(new ParameterizedTypeReference<ResponseWrapper<KycAuthResponseDtoV2>>() {
                })
        )).thenReturn(responseEntity);


        KycAuthDto kycAuthDto = new KycAuthDto(); // Assume this is properly initialized
        AuthChallenge authChallenge = new AuthChallenge();
        authChallenge.setAuthFactorType("OTP");
        authChallenge.setChallenge("123456");
        authChallenge.setFormat("alpha-numeric");
        kycAuthDto.setChallengeList(List.of(authChallenge));
        // Execute the method
        KycAuthResult result = mockHelperService.doKycAuthMock("relyingPartyId", "clientId", kycAuthDto, true);

        Assertions.assertNotNull(result);
        Assertions.assertEquals("test_token", result.getKycToken());
        Assertions.assertEquals("partner_token", result.getPartnerSpecificUserToken());
    }

    @Test
    public void doKycAuthMock_withAuthFactorAsWLA_thenPass() throws KycAuthException {
        ReflectionTestUtils.setField(mockHelperService, "kycAuthUrl", "http://localhost:8080/kyc/auth");
        ReflectionTestUtils.setField(mockHelperService, "objectMapper", new ObjectMapper());
        ResponseWrapper<KycAuthResponseDtoV2> responseWrapper = new ResponseWrapper<>();
        KycAuthResponseDtoV2 response = new KycAuthResponseDtoV2();
        Map<String,List<JsonNode>> claimMetaData=new HashMap<>();

        ObjectNode verificationDetail = objectMapper.createObjectNode();
        verificationDetail.put("trust_framework", "test_trust_framework");
        claimMetaData.put("name",List.of(verificationDetail));

        response.setClaimMetadata(claimMetaData);
        response.setAuthStatus(true);
        response.setKycToken("test_token");
        response.setPartnerSpecificUserToken("partner_token");
        responseWrapper.setResponse(response);
        ResponseEntity<ResponseWrapper<KycAuthResponseDtoV2>> responseEntity= new ResponseEntity<>(responseWrapper, HttpStatus.OK);

        Mockito.when(restTemplate.exchange(
                Mockito.any(RequestEntity.class),
                Mockito.eq(new ParameterizedTypeReference<ResponseWrapper<KycAuthResponseDtoV2>>() {
                })
        )).thenReturn(responseEntity);

        KycAuthDto kycAuthDto = new KycAuthDto();
        AuthChallenge authChallenge = new AuthChallenge();
        authChallenge.setAuthFactorType("WLA");
        authChallenge.setChallenge("validjwt");
        authChallenge.setFormat("jwt");
        kycAuthDto.setChallengeList(List.of(authChallenge));
        KycAuthResult result = mockHelperService.doKycAuthMock("relyingPartyId", "clientId", kycAuthDto, true);

        Assertions.assertNotNull(result);
        Assertions.assertEquals("test_token", result.getKycToken());
        Assertions.assertEquals("partner_token", result.getPartnerSpecificUserToken());
    }

    @Test
    public void doKycAuthMock_withAuthFactorAsPIN_thenPass() throws KycAuthException {
        ReflectionTestUtils.setField(mockHelperService, "kycAuthUrl", "http://localhost:8080/kyc/auth");
        ReflectionTestUtils.setField(mockHelperService, "objectMapper", new ObjectMapper());
        ResponseWrapper<KycAuthResponseDtoV2> responseWrapper = new ResponseWrapper<>();
        KycAuthResponseDtoV2 response = new KycAuthResponseDtoV2();
        Map<String,List<JsonNode>> claimMetaData=new HashMap<>();

        ObjectNode verificationDetail = objectMapper.createObjectNode();
        verificationDetail.put("trust_framework", "test_trust_framework");
        claimMetaData.put("name",List.of(verificationDetail));

        response.setClaimMetadata(claimMetaData);
        response.setAuthStatus(true);
        response.setKycToken("test_token");
        response.setPartnerSpecificUserToken("partner_token");
        responseWrapper.setResponse(response);
        ResponseEntity<ResponseWrapper<KycAuthResponseDtoV2>> responseEntity= new ResponseEntity<>(responseWrapper, HttpStatus.OK);

        Mockito.when(restTemplate.exchange(
                Mockito.any(RequestEntity.class),
                Mockito.eq(new ParameterizedTypeReference<ResponseWrapper<KycAuthResponseDtoV2>>() {
                })
        )).thenReturn(responseEntity);

        KycAuthDto kycAuthDto = new KycAuthDto();
        AuthChallenge authChallenge = new AuthChallenge();
        authChallenge.setAuthFactorType("PIN");
        authChallenge.setChallenge("111111");
        authChallenge.setFormat("number");
        kycAuthDto.setChallengeList(List.of(authChallenge));
        KycAuthResult result = mockHelperService.doKycAuthMock("relyingPartyId", "clientId", kycAuthDto, true);

        Assertions.assertNotNull(result);
        Assertions.assertEquals("test_token", result.getKycToken());
        Assertions.assertEquals("partner_token", result.getPartnerSpecificUserToken());
    }

    @Test
    public void doKycAuthMock_withAuthFactorAsPWD_thenPass() throws KycAuthException {
        ReflectionTestUtils.setField(mockHelperService, "kycAuthUrl", "http://localhost:8080/kyc/auth");
        ReflectionTestUtils.setField(mockHelperService, "objectMapper", new ObjectMapper());
        ResponseWrapper<KycAuthResponseDtoV2> responseWrapper = new ResponseWrapper<>();
        KycAuthResponseDtoV2 response = new KycAuthResponseDtoV2();
        Map<String,List<JsonNode>> claimMetaData=new HashMap<>();

        ObjectNode verificationDetail = objectMapper.createObjectNode();
        verificationDetail.put("trust_framework", "test_trust_framework");
        claimMetaData.put("name",List.of(verificationDetail));

        response.setClaimMetadata(claimMetaData);
        response.setAuthStatus(true);
        response.setKycToken("test_token");
        response.setPartnerSpecificUserToken("partner_token");
        responseWrapper.setResponse(response);
        ResponseEntity<ResponseWrapper<KycAuthResponseDtoV2>> responseEntity= new ResponseEntity<>(responseWrapper, HttpStatus.OK);

        Mockito.when(restTemplate.exchange(
                Mockito.any(RequestEntity.class),
                Mockito.eq(new ParameterizedTypeReference<ResponseWrapper<KycAuthResponseDtoV2>>() {
                })
        )).thenReturn(responseEntity);

        KycAuthDto kycAuthDto = new KycAuthDto();
        AuthChallenge authChallenge = new AuthChallenge();
        authChallenge.setAuthFactorType("PWD");
        authChallenge.setChallenge("Mosip@12");
        authChallenge.setFormat("alpha-numeric");
        kycAuthDto.setChallengeList(List.of(authChallenge));
        KycAuthResult result = mockHelperService.doKycAuthMock("relyingPartyId", "clientId", kycAuthDto, true);

        Assertions.assertNotNull(result);
        Assertions.assertEquals("test_token", result.getKycToken());
        Assertions.assertEquals("partner_token", result.getPartnerSpecificUserToken());
    }

    @Test
    public void doKycAuthMock_withValidAuthFactorAsBIO_thenPass() throws KycAuthException {
        ReflectionTestUtils.setField(mockHelperService, "kycAuthUrl", "http://localhost:8080/kyc/auth");
        ReflectionTestUtils.setField(mockHelperService, "objectMapper", new ObjectMapper());

        ResponseWrapper<KycAuthResponseDtoV2> responseWrapper = new ResponseWrapper<>();
        KycAuthResponseDtoV2 response = new KycAuthResponseDtoV2();

        Map<String,List<JsonNode>> claimMetaData=new HashMap<>();

        ObjectNode verificationDetail = objectMapper.createObjectNode();
        verificationDetail.put("trust_framework", "test_trust_framework");
        claimMetaData.put("name",List.of(verificationDetail));

        response.setClaimMetadata(claimMetaData);

        response.setAuthStatus(true);
        response.setKycToken("test_token");
        response.setPartnerSpecificUserToken("partner_token");
        responseWrapper.setResponse(response);
        ResponseEntity<ResponseWrapper<KycAuthResponseDtoV2>> responseEntity= new ResponseEntity<>(responseWrapper, HttpStatus.OK);

        Mockito.when(restTemplate.exchange(
                Mockito.any(RequestEntity.class),
                Mockito.eq(new ParameterizedTypeReference<ResponseWrapper<KycAuthResponseDtoV2>>() {
                })
        )).thenReturn(responseEntity);


        KycAuthDto kycAuthDto = new KycAuthDto(); // Assume this is properly initialized
        AuthChallenge authChallenge = new AuthChallenge();
        authChallenge.setAuthFactorType("BIO");
        authChallenge.setChallenge("{\"bio\":\"data\"}");
        authChallenge.setFormat("encoded-json");
        kycAuthDto.setChallengeList(List.of(authChallenge));
        // Execute the method
        KycAuthResult result = mockHelperService.doKycAuthMock("relyingPartyId", "clientId", kycAuthDto, true);

        Assertions.assertNotNull(result);
        Assertions.assertEquals("test_token", result.getKycToken());
        Assertions.assertEquals("partner_token", result.getPartnerSpecificUserToken());
    }

    @Test
    public void doKycAuthMock_withInValidAuthFactor_thenFail() {

        ReflectionTestUtils.setField(mockHelperService, "kycAuthUrl", "http://localhost:8080/kyc/auth");
        ReflectionTestUtils.setField(mockHelperService, "objectMapper", new ObjectMapper());

        KycAuthDto kycAuthDto = new KycAuthDto(); // Assume this is properly initialized
        AuthChallenge authChallenge = new AuthChallenge();
        authChallenge.setAuthFactorType("Knowledge");
        authChallenge.setChallenge("e3dq.2ef.3ww23");
        authChallenge.setFormat("alpha-numeric");
        kycAuthDto.setChallengeList(List.of(authChallenge));

        try{
            mockHelperService.doKycAuthMock("relyingPartyId", "clientId", kycAuthDto, true);
        }catch (KycAuthException e){
            Assertions.assertEquals(e.getErrorCode(),"invalid_auth_challenge");
        }
    }

    @Test
    public void doKycAuthMock_withInValidAuthChallenge_thenFail() {

        ReflectionTestUtils.setField(mockHelperService, "kycAuthUrl", "http://localhost:8080/kyc/auth");
        ReflectionTestUtils.setField(mockHelperService, "objectMapper", new ObjectMapper());

        KycAuthDto kycAuthDto = new KycAuthDto(); // Assume this is properly initialized
        AuthChallenge authChallenge = new AuthChallenge();
        authChallenge.setAuthFactorType("KBI");
        authChallenge.setChallenge("e3dq.2ef.3ww23");
        authChallenge.setFormat("jwt");
        kycAuthDto.setChallengeList(List.of(authChallenge));

        try{
            mockHelperService.doKycAuthMock("relyingPartyId", "clientId", kycAuthDto, true);
        }catch (KycAuthException e){
            Assertions.assertEquals(e.getErrorCode(),"invalid_challenge_format");
        }
    }

    @Test
    public void getUTCDateTime_withValidDetails_thenPass() {
        LocalDateTime utcDateTime = mockHelperService.getUTCDateTime();
        Assertions.assertNotNull(utcDateTime);
    }

    @Test
    public void getEpochSeconds_withValidDetails_thenPass() {
        long epochSeconds = mockHelperService.getEpochSeconds();
        Assertions.assertTrue(epochSeconds > 0);
    }

    @Test
    public void doKycAuthMock_withEmptyResponse_thenFail() throws KycAuthException {
        ReflectionTestUtils.setField(mockHelperService, "kycAuthUrl", "http://localhost:8080/kyc/auth");
        ReflectionTestUtils.setField(mockHelperService, "objectMapper", new ObjectMapper());

        ResponseWrapper<KycAuthResponseDtoV2> responseWrapper = new ResponseWrapper<>();
        responseWrapper.setResponse(null);
        ResponseEntity<ResponseWrapper<KycAuthResponseDtoV2>> responseEntity= new ResponseEntity<>(responseWrapper, HttpStatus.OK);

        Mockito.when(restTemplate.exchange(
                Mockito.any(RequestEntity.class),
                Mockito.eq(new ParameterizedTypeReference<ResponseWrapper<KycAuthResponseDtoV2>>() {
                })
        )).thenReturn(responseEntity);


        KycAuthDto kycAuthDto = new KycAuthDto(); // Assume this is properly initialized
        AuthChallenge authChallenge = new AuthChallenge();
        authChallenge.setAuthFactorType("OTP");
        authChallenge.setChallenge("123456");
        authChallenge.setFormat("alpha-numeric");
        kycAuthDto.setChallengeList(List.of(authChallenge));

        try{
            mockHelperService.doKycAuthMock("relyingPartyId", "clientId", kycAuthDto, true);
        }catch (KycAuthException e){
            Assertions.assertEquals(ErrorConstants.AUTH_FAILED,e.getErrorCode());
        }
    }


    @Test
    public void sendOtpMock_withValidDetails_thenPass() throws SendOtpException {

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        ReflectionTestUtils.setField(mockHelperService, "objectMapper", objectMapper);
        ReflectionTestUtils.setField(mockHelperService, "sendOtpUrl", "http://localhost:8080/otp/send");

        ResponseWrapper<SendOtpResult> responseWrapper = new ResponseWrapper<>();
        SendOtpResult sendOtpResult = new SendOtpResult();
        sendOtpResult.setTransactionId("test_transaction_id");
        sendOtpResult.setMaskedMobile("test_masked_mobile");
        sendOtpResult.setMaskedEmail("test_masked_email");
        responseWrapper.setResponse(sendOtpResult);
        ResponseEntity<ResponseWrapper<SendOtpResult>> responseEntity= new ResponseEntity<>(responseWrapper, HttpStatus.OK);

        Mockito.when(restTemplate.exchange(
                Mockito.any(RequestEntity.class),
                Mockito.eq(new ParameterizedTypeReference<ResponseWrapper<SendOtpResult>>() {
                })
        )).thenReturn(responseEntity);

        SendOtpResult result = mockHelperService.sendOtpMock("test_transaction_id", "individualId", List.of("mobile"), "relyingPartyId", "clientId");
        Assertions.assertNotNull(result);
        Assertions.assertEquals(result, sendOtpResult);

    }

    @Test
    public void sendOtpMock_withEmptyResponse_thenFail() throws SendOtpException {

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        ReflectionTestUtils.setField(mockHelperService, "objectMapper", objectMapper);
        ReflectionTestUtils.setField(mockHelperService, "sendOtpUrl", "http://localhost:8080/otp/send");

        ResponseWrapper<SendOtpResult> responseWrapper = new ResponseWrapper<>();
        responseWrapper.setResponse(null);
        ResponseEntity<ResponseWrapper<SendOtpResult>> responseEntity= new ResponseEntity<>(responseWrapper, HttpStatus.OK);

        Mockito.when(restTemplate.exchange(
                Mockito.any(RequestEntity.class),
                Mockito.eq(new ParameterizedTypeReference<ResponseWrapper<SendOtpResult>>() {
                })
        )).thenReturn(responseEntity);

        try{
            mockHelperService.sendOtpMock("test_transaction_id", "individualId", List.of("mobile"),"relyingPartyId", "clientId");
            Assertions.fail();
        }catch (SendOtpException e){
            Assertions.assertEquals(ErrorConstants.SEND_OTP_FAILED,e.getErrorCode());
        }
    }

    @Test
    public void sendOtpMock_withErrorInResponse_thenFail() throws SendOtpException {

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        ReflectionTestUtils.setField(mockHelperService, "objectMapper", objectMapper);
        ReflectionTestUtils.setField(mockHelperService, "sendOtpUrl", "http://localhost:8080/otp/send");

        ResponseWrapper<SendOtpResult> responseWrapper = new ResponseWrapper<>();
        responseWrapper.setErrors(List.of(new ServiceError("test_error_code","test_error_message")));
        responseWrapper.setResponse(null);
        ResponseEntity<ResponseWrapper<SendOtpResult>> responseEntity= new ResponseEntity<>(responseWrapper, HttpStatus.OK);

        Mockito.when(restTemplate.exchange(
                Mockito.any(RequestEntity.class),
                Mockito.eq(new ParameterizedTypeReference<ResponseWrapper<SendOtpResult>>() {
                })
        )).thenReturn(responseEntity);

        try{
            mockHelperService.sendOtpMock("test_transaction_id", "individualId", List.of("mobile"),"relyingPartyId", "clientId");
            Assertions.fail();
        }catch (SendOtpException e){
            Assertions.assertEquals("test_error_code",e.getErrorCode());
        }
    }


    @Test
    public void sendOtpMock_withResponseCodeAsUnAuthorized_thenFail() throws SendOtpException {


        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        ReflectionTestUtils.setField(mockHelperService, "objectMapper", objectMapper);
        ReflectionTestUtils.setField(mockHelperService, "sendOtpUrl", "http://localhost:8080/otp/send");

        ResponseWrapper<SendOtpResult> responseWrapper = new ResponseWrapper<>();
        responseWrapper.setErrors(List.of(new ServiceError("test_error_code","test_error_message")));
        responseWrapper.setResponse(null);
        ResponseEntity<ResponseWrapper<SendOtpResult>> responseEntity= new ResponseEntity<>(responseWrapper, HttpStatus.UNAUTHORIZED);

        Mockito.when(restTemplate.exchange(
                Mockito.any(RequestEntity.class),
                Mockito.eq(new ParameterizedTypeReference<ResponseWrapper<SendOtpResult>>() {
                })
        )).thenReturn(responseEntity);

        try{
            mockHelperService.sendOtpMock("test_transaction_id", "individualId", List.of("mobile"),"relyingPartyId", "clientId");
            Assertions.fail();
        }catch (SendOtpException e){
            Assertions.assertEquals(ErrorConstants.SEND_OTP_FAILED,e.getErrorCode());
        }
    }

    @Test
    public void getRequestSignatureTest() {
        String request = "request";
        JWTSignatureResponseDto jwtSignatureResponseDto = new JWTSignatureResponseDto();
        jwtSignatureResponseDto.setJwtSignedData("jwtSignedData");
        jwtSignatureResponseDto.setTimestamp(LocalDateTime.now());
        Mockito.when(signatureService.jwtSign(Mockito.any())).thenReturn(jwtSignatureResponseDto);
        String requestSignature = mockHelperService.getRequestSignature(request);
        Assertions.assertNotNull(requestSignature);
        Assertions.assertEquals("jwtSignedData", requestSignature);

    }

    @Test
    public void isSupportedOtpChannelWithSupportedChannel_thenPass(){

        ReflectionTestUtils.setField(mockHelperService,"otpChannels",List.of("email","phone"));
        boolean isSupported= mockHelperService.isSupportedOtpChannel("email");
        Assertions.assertTrue(isSupported);
    }

    @Test
    public void isSupportedOtpChannelWithUnSupportedChannel_thenFail(){

        ReflectionTestUtils.setField(mockHelperService,"otpChannels",List.of("email"));
        boolean isSupported= mockHelperService.isSupportedOtpChannel("phone");
        Assertions.assertFalse(isSupported);
    }
}