package io.mosip.esignet.sunbirdrc.integration.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mosip.esignet.api.dto.*;
import io.mosip.esignet.api.exception.KycAuthException;
import io.mosip.esignet.api.exception.KycExchangeException;
import io.mosip.esignet.api.exception.SendOtpException;
import io.mosip.esignet.api.util.ErrorConstants;
import lombok.extern.slf4j.Slf4j;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
@Slf4j
public class SunbirdRCAuthenticaionServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private SunbirdRCAuthenticationService sunbirdRCAuthenticationService;



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
        authChallenge.setAuthFactorType("KBA");
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
        authChallenge.setAuthFactorType("KBA");
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
        authChallenge.setAuthFactorType("KBA");
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
        authChallenge.setAuthFactorType("KBA");
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
            Assert.assertEquals(e.getErrorCode(),"invalid_challenge_format");
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
    public void doKycExchangeNotImplemented_thenFail() {
        try{
            sunbirdRCAuthenticationService.doKycExchange("relyingPartyId","clientId",new KycExchangeDto());
            Assert.fail();
        } catch (KycExchangeException e) {
            Assert.assertEquals(e.getErrorCode(), ErrorConstants.NOT_IMPLEMENTED);
        }
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
