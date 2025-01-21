package io.mosip.esignet.plugin.sunbirdrc.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mosip.esignet.api.dto.*;
import io.mosip.esignet.api.exception.KycAuthException;
import io.mosip.esignet.api.exception.KycExchangeException;
import io.mosip.esignet.api.exception.SendOtpException;
import io.mosip.esignet.api.util.ErrorConstants;
import io.mosip.kernel.signature.dto.JWTSignatureRequestDto;
import io.mosip.kernel.signature.dto.JWTSignatureResponseDto;
import io.mosip.kernel.signature.service.SignatureService;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.*;

@RunWith(MockitoJUnitRunner.class)
@Slf4j
public class SunbirdRCAuthenticaionServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private SunbirdRCAuthenticationService sunbirdRCAuthenticationService;

    @Mock
    private SignatureService signatureService;


    @Test
    public void initializeWithValidConfig_thenPass() throws KycAuthException {

        List<Map<String,String>> fieldDetailList = List.of(Map.of("id","policyNumber","type","string","format","string"));
        ReflectionTestUtils.setField(sunbirdRCAuthenticationService, "fieldDetailList", fieldDetailList);
        ReflectionTestUtils.setField(sunbirdRCAuthenticationService, "idField", "policyNumber");
        sunbirdRCAuthenticationService.initialize();

    }

    @Test
    public void initializeWithInValidConfig_thenFail() {
        try {
            sunbirdRCAuthenticationService.initialize();
        }catch (KycAuthException e){
            Assert.assertEquals("sunbird-rc authenticator field is not configured properly", e.getMessage());
        }
    }

    @Test
    public void initializeWithInValidIdField_thenFail() {
        List<Map<String,String>> fieldDetailList = List.of(Map.of("id","policyNumber","type","string","format","string"));
        ReflectionTestUtils.setField(sunbirdRCAuthenticationService, "fieldDetailList", fieldDetailList);
        ReflectionTestUtils.setField(sunbirdRCAuthenticationService, "idField", "policyNumber2");
        try {
            sunbirdRCAuthenticationService.initialize();
        }catch (KycAuthException e){
            Assert.assertEquals("Invalid configuration: individual-id-field is not available in field-details.", e.getMessage());
        }
    }

    @Test
    public void doKycAuthWithValidParams_thenPass() throws KycAuthException, IOException, NoSuchFieldException, IllegalAccessException {
        List<Map<String,String>> fieldDetailList = List.of(Map.of("id","policyNumber","type","string","format","string"),Map.of("id","fullName","type","string","format","string"));
        ReflectionTestUtils.setField(sunbirdRCAuthenticationService, "fieldDetailList", fieldDetailList);
        ReflectionTestUtils.setField(sunbirdRCAuthenticationService, "idField", "policyNumber");
        ReflectionTestUtils.setField(sunbirdRCAuthenticationService, "registrySearchUrl", "url");
        ReflectionTestUtils.setField(sunbirdRCAuthenticationService, "entityIdField", "policyNumber");
        ReflectionTestUtils.setField(sunbirdRCAuthenticationService,"objectMapper",new ObjectMapper());

        // Arrange
        String relyingPartyId = "validRelayingPartyId";
        String clientId = "validClientId";
        KycAuthDto kycAuthDto = new KycAuthDto(); // populate with valid data
        AuthChallenge authChallenge=new AuthChallenge();
        authChallenge.setFormat("string");
        authChallenge.setAuthFactorType("KBI");
        authChallenge.setChallenge("eyJmdWxsTmFtZSI6IlphaWQgU2lkZGlxdWUiLCJkb2IiOiIyMDAwLTA3LTI2In0=");

        kycAuthDto.setChallengeList(List.of(authChallenge));
        kycAuthDto.setIndividualId("000000");

        List<Map<String,Object>> responseMap=new ArrayList<>();
        Map<String,Object> map=Map.of("policyNumber","000000","dob","2000-07-26");
        responseMap.add(map);
        ResponseEntity<List<Map<String,Object>>>  responseEntity = new ResponseEntity(responseMap, HttpStatus.OK);
        Mockito.when(restTemplate.exchange(
                Mockito.any(RequestEntity.class),
                Mockito.eq(new ParameterizedTypeReference<List<Map<String,Object>>>() {})
        )).thenReturn(responseEntity);

        Map<String,String> mockChallengMap=new HashMap<>();
        mockChallengMap.put("fullName","Zaid Siddique");
        mockChallengMap.put("dob","2000-07-26");

        KycAuthResult result = sunbirdRCAuthenticationService.doKycAuth(relyingPartyId, clientId, kycAuthDto);
        Assert.assertNotNull(result);
    }

    @Test
    public void doKycAuthWithInValidChallenge_thenFail() throws IOException {

        List<Map<String,String>> fieldDetailList = List.of(Map.of("id","policyNumber","type","string","format","string"),Map.of("id","fullName","type","string","format","string"));
        ReflectionTestUtils.setField(sunbirdRCAuthenticationService, "fieldDetailList", fieldDetailList);
        ReflectionTestUtils.setField(sunbirdRCAuthenticationService, "idField", "policyNumber");
        ReflectionTestUtils.setField(sunbirdRCAuthenticationService, "registrySearchUrl", "url");
        ReflectionTestUtils.setField(sunbirdRCAuthenticationService, "entityIdField", "policyNumber");

        String relyingPartyId = "validRelayingPartyId";
        String clientId = "validClientId";
        KycAuthDto kycAuthDto = new KycAuthDto(); // populate with valid data
        AuthChallenge authChallenge=new AuthChallenge();
        authChallenge.setFormat("string");
        authChallenge.setAuthFactorType("KBI");
        authChallenge.setChallenge("eyJmdWxsTmFtZSI6IlphaWQiLCJkb2IiOiIyMDAwLTA3LTI2In0=");

        kycAuthDto.setChallengeList(List.of(authChallenge));
        kycAuthDto.setIndividualId("000000");

        Map<String,String> mockChallengMap=new HashMap<>();
        mockChallengMap.put("fullName","Zaid");
        mockChallengMap.put("dob","2000-07-26");
        Mockito.when(objectMapper.readValue(Mockito.anyString(),Mockito.eq(Map.class))).thenReturn(mockChallengMap);

        try{
            sunbirdRCAuthenticationService.doKycAuth(relyingPartyId, clientId, kycAuthDto);
            Assert.fail();
        }catch (KycAuthException e){
            Assert.assertEquals(e.getErrorCode(), ErrorConstants.AUTH_FAILED);
        }

    }


    @Test
    public void doKycAuthWithInValidResponse_thenFail() {
        List<Map<String,String>> fieldDetailList = List.of(Map.of("id","policyNumber","type","string","format","string"));
        ReflectionTestUtils.setField(sunbirdRCAuthenticationService, "fieldDetailList", fieldDetailList);
        ReflectionTestUtils.setField(sunbirdRCAuthenticationService, "idField", "policyNumber");
        ReflectionTestUtils.setField(sunbirdRCAuthenticationService, "registrySearchUrl", "url");
        ReflectionTestUtils.setField(sunbirdRCAuthenticationService, "entityIdField", "policyNumber");
        // Arrange
        String relyingPartyId = "validRelayingPartyId";
        String clientId = "validClientId";
        KycAuthDto kycAuthDto = new KycAuthDto(); // populate with valid data
        AuthChallenge authChallenge=new AuthChallenge();
        authChallenge.setFormat("string");
        authChallenge.setAuthFactorType("KBI");
        authChallenge.setChallenge("eyJmdWxsTmFtZSI6IlphaWQgU2lkZGlxdWUiLCJkb2IiOiIyMDAwLTA3LTI2In0=");

        kycAuthDto.setChallengeList(List.of(authChallenge));
        kycAuthDto.setIndividualId("000000");

        List<Map<String,Object>> responseMap=new ArrayList<>();Map<String,Object> map=Map.of("policyNumber","654321","dob","654321");
        responseMap.add(map);
        ResponseEntity<List<Map<String,Object>>>  responseEntity = new ResponseEntity(responseMap, HttpStatus.FORBIDDEN);
        Mockito.when(restTemplate.exchange(
                Mockito.any(RequestEntity.class),
                Mockito.eq(new ParameterizedTypeReference<List<Map<String,Object>>>() {})
        )).thenReturn(responseEntity);

        try{
            sunbirdRCAuthenticationService.doKycAuth(relyingPartyId, clientId, kycAuthDto);
            Assert.fail();
        }catch (KycAuthException e){
            Assert.assertEquals(e.getErrorCode(), ErrorConstants.AUTH_FAILED);
        }
    }

    @Test
    public void doKycAuthWithResponseSizeMoreThenOne_thenFail() throws IOException {
        List<Map<String,String>> fieldDetailList = List.of(Map.of("id","policyNumber","type","string","format","string"),Map.of("id","fullName","type","string","format","string"));
        ReflectionTestUtils.setField(sunbirdRCAuthenticationService, "fieldDetailList", fieldDetailList);
        ReflectionTestUtils.setField(sunbirdRCAuthenticationService, "idField", "policyNumber");
        ReflectionTestUtils.setField(sunbirdRCAuthenticationService, "registrySearchUrl", "url");
        ReflectionTestUtils.setField(sunbirdRCAuthenticationService, "entityIdField", "policyNumber");
        // Arrange
        String relyingPartyId = "validRelayingPartyId";
        String clientId = "validClientId";
        KycAuthDto kycAuthDto = new KycAuthDto(); // populate with valid data
        AuthChallenge authChallenge=new AuthChallenge();
        authChallenge.setFormat("string");
        authChallenge.setAuthFactorType("KBI");
        authChallenge.setChallenge("eyJmdWxsTmFtZSI6IlphaWQgU2lkZGlxdWUiLCJkb2IiOiIyMDAwLTA3LTI2In0=");

        kycAuthDto.setChallengeList(List.of(authChallenge));
        kycAuthDto.setIndividualId("000000");

        List<Map<String,Object>> responseList =new ArrayList<>();
        Map<String,Object> response1=Map.of("response1","654321");
        Map<String,Object> response2=Map.of("response2","654321");
        responseList.add(response1);
        responseList.add(response2);
        ResponseEntity<List<Map<String,Object>>>  responseEntity = new ResponseEntity(responseList, HttpStatus.OK);
        Mockito.when(restTemplate.exchange(
                Mockito.any(RequestEntity.class),
                Mockito.eq(new ParameterizedTypeReference<List<Map<String,Object>>>() {})
        )).thenReturn(responseEntity);

        Map<String,String> mockChallengMap=new HashMap<>();
        mockChallengMap.put("fullName","Zaid Siddique");
        mockChallengMap.put("dob","2000-07-26");
        Mockito.when(objectMapper.readValue(Mockito.anyString(),Mockito.eq(Map.class))).thenReturn(mockChallengMap);

        try{
            sunbirdRCAuthenticationService.doKycAuth(relyingPartyId, clientId, kycAuthDto);
            Assert.fail();
        }catch (KycAuthException e){
            Assert.assertEquals(e.getErrorCode(), ErrorConstants.AUTH_FAILED);
        }
    }

    @Test
    public void doKycAuthWithInValidChallengeType_thenFail() {
        String relyingPartyId = "validRelayingPartyId";
        String clientId = "validClientId";
        KycAuthDto kycAuthDto = new KycAuthDto(); // populate with valid data
        AuthChallenge authChallenge=new AuthChallenge();
        authChallenge.setFormat("string");
        authChallenge.setAuthFactorType("Bio");
        authChallenge.setChallenge("eyJmdWxsTmFtZSI6IlphaWQgU2lkZGlxdWUiLCJkb2IiOiIyMDAwLTA3LTI2In0=");

        kycAuthDto.setChallengeList(List.of(authChallenge));
        kycAuthDto.setIndividualId("000000");

        try{
            sunbirdRCAuthenticationService.doKycAuth(relyingPartyId, clientId, kycAuthDto);
            Assert.fail();
        }catch (KycAuthException e){
            Assert.assertEquals(e.getErrorCode(),"invalid_challenge_type");
        }
    }

    @Test
    public void doKycAuthWithOutChallenge_thenFail()  {
        String relyingPartyId = "validRelayingPartyId";
        String clientId = "validClientId";
        KycAuthDto kycAuthDto = new KycAuthDto(); // populate with valid data
        kycAuthDto.setIndividualId("000000");

        try{
            sunbirdRCAuthenticationService.doKycAuth(relyingPartyId, clientId, kycAuthDto);
            Assert.fail();
        }catch (KycAuthException e){
            Assert.assertEquals(e.getMessage(),ErrorConstants.AUTH_FAILED);
        }
    }

    @Test
    public void doKycExchangeWithValidParams_thenPass() throws KycExchangeException, JsonProcessingException {
        Map<String,String> oidcClaimsMapping= Map.of("name","name","email","email","phone","mobile","address","address");
        ReflectionTestUtils.setField(sunbirdRCAuthenticationService, "oidcClaimsMapping", oidcClaimsMapping);
        ReflectionTestUtils.setField(sunbirdRCAuthenticationService, "encryptKyc", false);
        String relyingPartyId = "relyingPartyId";
        String clientId = "clientId";
        KycExchangeDto kycExchangeDto = new KycExchangeDto();
        kycExchangeDto.setKycToken("kyc-token");
        kycExchangeDto.setAcceptedClaims(Arrays.asList("name", "address"));
        kycExchangeDto.setClaimsLocales(new String[]{"en"});

        Map<String, Object> registryData = new HashMap<>();
        registryData.put("name", "John");
        registryData.put("address", "address");

        Map<String, Object> expectedKyc = new HashMap<>();
        expectedKyc.put("name", "John");
        expectedKyc.put("address", "address");

        Mockito.when(restTemplate.exchange(Mockito.any(RequestEntity.class), Mockito.any(ParameterizedTypeReference.class)))
                .thenReturn(new ResponseEntity<>(registryData, HttpStatus.OK));
        String expectedPayload = "{\"name\":\"John\",\"address\":\"address\"}";
        Mockito.when(objectMapper.writeValueAsString(Mockito.any()))
                .thenReturn(expectedPayload);

        JWTSignatureResponseDto signatureResponse = new JWTSignatureResponseDto();
        signatureResponse.setJwtSignedData("signed-jwt");
        Mockito.when(signatureService.jwtSign(Mockito.any(JWTSignatureRequestDto.class))).thenReturn(signatureResponse);

        KycExchangeResult result = sunbirdRCAuthenticationService.doKycExchange(relyingPartyId, clientId, kycExchangeDto);

        Assert.assertNotNull(result);
        Assert.assertEquals("signed-jwt", result.getEncryptedKyc());
    }

    @Test
    public void doKycExchangeWithNullRegistryData_thenFail() {
        String relyingPartyId = "RP123";
        String clientId = "CLIENT123";
        KycExchangeDto kycExchangeDto = new KycExchangeDto();
        kycExchangeDto.setKycToken("kyc-token");

        Mockito.when(restTemplate.exchange(Mockito.any(RequestEntity.class), Mockito.any(ParameterizedTypeReference.class)))
                .thenReturn(new ResponseEntity<>(null, HttpStatus.OK));

        KycExchangeException exception = Assert.assertThrows(KycExchangeException.class, () ->
                sunbirdRCAuthenticationService.doKycExchange(relyingPartyId, clientId, kycExchangeDto));
        Assert.assertEquals(ErrorConstants.DATA_EXCHANGE_FAILED, exception.getMessage());
    }

    @Test
    public void sendOtpNotImplemented_thenFail() {
        try{
            sunbirdRCAuthenticationService.sendOtp("relayingPartyId","clientId",new SendOtpDto());
            Assert.fail();
        } catch (SendOtpException e) {
            Assert.assertEquals(e.getErrorCode(), ErrorConstants.NOT_IMPLEMENTED);
        }
    }

    @Test
    public void isSupportedOtpChannel_thenFail() {
        boolean result = sunbirdRCAuthenticationService.isSupportedOtpChannel("sms");
        Assert.assertFalse(result);
    }

}
