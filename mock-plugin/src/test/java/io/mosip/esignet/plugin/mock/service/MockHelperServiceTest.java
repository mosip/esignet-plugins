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
import org.junit.Assert;
import org.junit.Before;
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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class MockHelperServiceTest {


    @InjectMocks
    MockHelperService mockHelperService;


    @Mock
    RestTemplate restTemplate;

    @Mock
    SignatureService signatureService;

    private ObjectMapper objectMapper = new ObjectMapper();


    @Before
    public void setUp() throws Exception {
        // Create the map you want to set
        Map<String, List<String>> supportedKycAuthFormats = new HashMap<>();
        supportedKycAuthFormats.put("OTP", List.of("alpha-numeric"));
        supportedKycAuthFormats.put("PIN", List.of("number"));
        supportedKycAuthFormats.put("BIO", List.of("encoded-json"));
        supportedKycAuthFormats.put("WLA", List.of("jwt"));
        supportedKycAuthFormats.put("KBA", List.of("base64url-encoded-json"));
        supportedKycAuthFormats.put("PWD", List.of("alpha-numeric"));

        // Get the field
        Field field = MockHelperService.class.getDeclaredField("supportedKycAuthFormats");

        // Make the field accessible
        field.setAccessible(true);

        // Remove the final modifier
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        int modifiers = field.getModifiers();
        modifiers &= ~Modifier.FINAL; // Clear the FINAL bit
        modifiersField.setInt(field, modifiers);

        // Now you can set the field value
        field.set(null, supportedKycAuthFormats); // Setting static field
    }

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

        Assert.assertNotNull(result);
        Assert.assertEquals("test_token", result.getKycToken());
        Assert.assertEquals("partner_token", result.getPartnerSpecificUserToken());
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

        Assert.assertNotNull(result);
        Assert.assertEquals("test_token", result.getKycToken());
        Assert.assertEquals("partner_token", result.getPartnerSpecificUserToken());
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

        Assert.assertNotNull(result);
        Assert.assertEquals("test_token", result.getKycToken());
        Assert.assertEquals("partner_token", result.getPartnerSpecificUserToken());
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

        Assert.assertNotNull(result);
        Assert.assertEquals("test_token", result.getKycToken());
        Assert.assertEquals("partner_token", result.getPartnerSpecificUserToken());
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

        Assert.assertNotNull(result);
        Assert.assertEquals("test_token", result.getKycToken());
        Assert.assertEquals("partner_token", result.getPartnerSpecificUserToken());
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
            Assert.assertEquals(e.getErrorCode(),"invalid_auth_challenge");
        }
    }

    @Test
    public void doKycAuthMock_withInValidAuthChallenge_thenFail() {

        ReflectionTestUtils.setField(mockHelperService, "kycAuthUrl", "http://localhost:8080/kyc/auth");
        ReflectionTestUtils.setField(mockHelperService, "objectMapper", new ObjectMapper());

        KycAuthDto kycAuthDto = new KycAuthDto(); // Assume this is properly initialized
        AuthChallenge authChallenge = new AuthChallenge();
        authChallenge.setAuthFactorType("KBA");
        authChallenge.setChallenge("e3dq.2ef.3ww23");
        authChallenge.setFormat("jwt");
        kycAuthDto.setChallengeList(List.of(authChallenge));

        try{
            mockHelperService.doKycAuthMock("relyingPartyId", "clientId", kycAuthDto, true);
        }catch (KycAuthException e){
            Assert.assertEquals(e.getErrorCode(),"invalid_auth_challenge");
        }
    }

    @Test
    public void getUTCDateTime_withValidDetails_thenPass() {
        LocalDateTime utcDateTime = mockHelperService.getUTCDateTime();
        Assert.assertNotNull(utcDateTime);
    }

    @Test
    public void getEpochSeconds_withValidDetails_thenPass() {
        long epochSeconds = mockHelperService.getEpochSeconds();
        Assert.assertTrue(epochSeconds > 0);
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
            Assert.assertEquals(ErrorConstants.AUTH_FAILED,e.getErrorCode());
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
        Assert.assertNotNull(result);
        Assert.assertEquals(result, sendOtpResult);

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
            Assert.fail();
        }catch (SendOtpException e){
            Assert.assertEquals(ErrorConstants.SEND_OTP_FAILED,e.getErrorCode());
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
            Assert.fail();
        }catch (SendOtpException e){
            Assert.assertEquals("test_error_code",e.getErrorCode());
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
            Assert.fail();
        }catch (SendOtpException e){
            Assert.assertEquals(ErrorConstants.SEND_OTP_FAILED,e.getErrorCode());
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
        Assert.assertNotNull(requestSignature);
        Assert.assertEquals("jwtSignedData", requestSignature);

    }

    @Test
    public void isSupportedOtpChannelWithSupportedChannel_thenPass(){

        ReflectionTestUtils.setField(mockHelperService,"otpChannels",List.of("email","phone"));
        boolean isSupported= mockHelperService.isSupportedOtpChannel("email");
        Assert.assertTrue(isSupported);
    }

    @Test
    public void isSupportedOtpChannelWithUnSupportedChannel_thenFail(){

        ReflectionTestUtils.setField(mockHelperService,"otpChannels",List.of("email"));
        boolean isSupported= mockHelperService.isSupportedOtpChannel("phone");
        Assert.assertFalse(isSupported);
    }
}