package io.mosip.signup.plugin.mock.service;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.mosip.esignet.core.dto.Error;
import io.mosip.esignet.core.dto.ResponseWrapper;
import io.mosip.signup.api.dto.ProfileDto;
import io.mosip.signup.api.dto.ProfileResult;
import io.mosip.signup.api.exception.InvalidProfileException;
import io.mosip.signup.api.exception.ProfileException;
import io.mosip.signup.api.util.ProfileCreateUpdateStatus;
import io.mosip.signup.plugin.mock.dto.MockIdentityResponse;
import io.mosip.signup.plugin.mock.util.ErrorConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
public class MockProfileRegistryPluginImplTest {


    @InjectMocks
    private MockProfileRegistryPluginImpl mockProfileRegistryPlugin;

    @Mock
    RestTemplate restTemplate;

    ObjectMapper objectMapper=new ObjectMapper();

    @BeforeEach
    public void init(){
        objectMapper.registerModule(new JavaTimeModule());
        ReflectionTestUtils.setField(mockProfileRegistryPlugin, "objectMapper",objectMapper);
    }


    @Test
    public void validate_withValidActionAndProfileDto_thenPass() throws JsonProcessingException {

        List<String> requiredField=new ArrayList<>();
        requiredField.add("phone");
        ReflectionTestUtils.setField(mockProfileRegistryPlugin, "requiredFieldsOnCreate", requiredField);
        String action = "CREATE";

        String phone="{ \"value\": \"7408001310\", \"essential\":true }";
        String verifiedClaims="[{\"verification\":{\"trust_framework\":{\"value\":\"income-tax\"}},\"claims\":{\"name\":null,\"email\":{\"essential\":0}}},{\"verification\":{\"trust_framework\":{\"value\":\"pwd\"}},\"claims\":{\"birthdate\":{\"essential\":true},\"address\":null}},{\"verification\":{\"trust_framework\":{\"value\":\"cbi\"}},\"claims\":{\"gender\":{\"essential\":true},\"email\":{\"essential\":true}}}]";
        JsonNode addressNode = objectMapper.readValue(phone, JsonNode.class);
        JsonNode verifiedClaimNode = objectMapper.readValue(verifiedClaims, JsonNode.class);

        Map<String, JsonNode> userinfoMap = new HashMap<>();
        userinfoMap.put("phone", addressNode);
        userinfoMap.put("verified_claims", verifiedClaimNode);
        JsonNode mockIdentity=objectMapper.valueToTree(userinfoMap);

        ProfileDto profileDto = new ProfileDto();
        profileDto.setIndividualId("individualId");
        profileDto.setIdentity(mockIdentity);

        mockProfileRegistryPlugin.validate(action, profileDto);
    }


    @Test
    public void validate_withInValidRequiredField_thenFail() throws JsonProcessingException {

        List<String> requiredField=new ArrayList<>();
        requiredField.add("email");
        ReflectionTestUtils.setField(mockProfileRegistryPlugin, "requiredFieldsOnCreate", requiredField);
        String action = "CREATE";

        String phone="{ \"value\": \"7408001310\", \"essential\":true }";
        String verifiedClaims="[{\"verification\":{\"trust_framework\":{\"value\":\"income-tax\"}},\"claims\":{\"name\":null,\"email\":{\"essential\":0}}},{\"verification\":{\"trust_framework\":{\"value\":\"pwd\"}},\"claims\":{\"birthdate\":{\"essential\":true},\"address\":null}},{\"verification\":{\"trust_framework\":{\"value\":\"cbi\"}},\"claims\":{\"gender\":{\"essential\":true},\"email\":{\"essential\":true}}}]";
        JsonNode addressNode = objectMapper.readValue(phone, JsonNode.class);
        JsonNode verifiedClaimNode = objectMapper.readValue(verifiedClaims, JsonNode.class);

        Map<String, JsonNode> userinfoMap = new HashMap<>();
        userinfoMap.put("phone", addressNode);
        userinfoMap.put("verified_claims", verifiedClaimNode);
        JsonNode mockIdentity=objectMapper.valueToTree(userinfoMap);

        ProfileDto profileDto = new ProfileDto();
        profileDto.setIndividualId("individualId");
        profileDto.setIdentity(mockIdentity);

        try{
            mockProfileRegistryPlugin.validate(action, profileDto);
            Assertions.fail();
        }catch (InvalidProfileException e){
            Assertions.assertEquals(e.getMessage(),"invalid_email");
        }

    }

    @Test
    public void createProfile_withValidRequestAndProfileDto_thenPass() throws ProfileException {
        // Arrange
        ReflectionTestUtils.setField(mockProfileRegistryPlugin, "identityEndpoint","http://localhost:8080/");
        ReflectionTestUtils.setField(mockProfileRegistryPlugin, "usernameField","individualId");
        Map<String, Object> identityData = new HashMap<>();
        identityData.put("individualId","1234567890");
        JsonNode mockIdentity = objectMapper.valueToTree(identityData);
        ProfileDto profileDto = new ProfileDto();
        profileDto.setIndividualId("1234567890");
        profileDto.setIdentity(mockIdentity);

        MockIdentityResponse mockIdentityResponse=new MockIdentityResponse();
        mockIdentityResponse.setStatus("CREATED");
        ResponseWrapper<MockIdentityResponse> responseWrapper = new ResponseWrapper<>();
        responseWrapper.setResponse(mockIdentityResponse);
        ResponseEntity<ResponseWrapper<MockIdentityResponse>> responseEntity=new ResponseEntity<>(responseWrapper, HttpStatus.OK);

        Mockito.when(restTemplate.exchange(
                Mockito.anyString(),
                Mockito.any(HttpMethod.class),
                Mockito.any(),
                Mockito.eq(new ParameterizedTypeReference<ResponseWrapper<MockIdentityResponse>>() {
                }))).thenReturn(responseEntity);

        ProfileResult result = mockProfileRegistryPlugin.createProfile("requestId", profileDto);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(result.getStatus(), "CREATED");
    }


    @Test
    public void createProfile_withInValidRequestAndProfileDto_thenFail() throws ProfileException {
        // Arrange
        ReflectionTestUtils.setField(mockProfileRegistryPlugin, "usernameField","individualId");
        Map<String,String> identityData=new HashMap<>();
        identityData.put("individualId","1234567890");
        JsonNode mockIdentity = objectMapper.valueToTree(identityData);
        ProfileDto profileDto = new ProfileDto();
        profileDto.setIndividualId("123456789");
        profileDto.setIdentity(mockIdentity);
        try{
            mockProfileRegistryPlugin.createProfile("requestId", profileDto);
        }catch (ProfileException e){
            Assertions.assertEquals(ErrorConstants.IDENTIFIER_MISMATCH,e.getMessage());
        }
    }


    @Test
    public void getProfileCreateUpdateStatus_withValidRequestId_thenPass() throws ProfileException {
        String requestId = "requestId123";

        ProfileCreateUpdateStatus status = mockProfileRegistryPlugin.getProfileCreateUpdateStatus(requestId);

        Assertions.assertEquals(status,ProfileCreateUpdateStatus.COMPLETED);
    }

    @Test
    public void getProfile_withValidIndividualId_thenPass() throws ProfileException {
        String individualId = "1234567890";
       ReflectionTestUtils.setField(mockProfileRegistryPlugin, "getIdentityEndpoint","http://localhost:8080/");

        Map<String, Object> identityData = new HashMap<>();
        identityData.put("email","123@email.com");
        identityData.put("password","123456");
        identityData.put("UIN","1234567890");
        identityData.put("individualId",individualId);

        JsonNode mockIdentity = objectMapper.valueToTree(identityData);
        ResponseWrapper<JsonNode> responseWrapper = new ResponseWrapper<>();
        responseWrapper.setResponse(mockIdentity);
        ResponseEntity<ResponseWrapper<JsonNode>> responseEntity=new ResponseEntity<>(responseWrapper, HttpStatus.OK);

        Mockito.when(restTemplate.exchange(
                "http://localhost:8080/"+individualId,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseWrapper<JsonNode>>() {
                })).thenReturn(responseEntity);
        ProfileDto profileDto= mockProfileRegistryPlugin.getProfile(individualId);
        Assertions.assertNotNull(profileDto);
        Assertions.assertTrue(profileDto.isActive());
    }

    @Test
    public void getProfile_withInValidIndividualId_thenFail() throws ProfileException {
        String individualId = "1234567890";
        ReflectionTestUtils.setField(mockProfileRegistryPlugin, "getIdentityEndpoint","http://localhost:8080/");

        Map<String, Object> identityData = new HashMap<>();
        identityData.put("email","123@email.com");
        identityData.put("password","123456");
        identityData.put("UIN","1234567890");
        identityData.put("individualId",individualId);

        JsonNode mockIdentity = objectMapper.valueToTree(identityData);
        ResponseWrapper<JsonNode> responseWrapper = new ResponseWrapper<>();
        responseWrapper.setResponse(mockIdentity);
        Error error = new Error();
        error.setErrorCode("invalid_individual_id");
        responseWrapper.setErrors(List.of(error));
        ResponseEntity<ResponseWrapper<JsonNode>> responseEntity=new ResponseEntity<>(responseWrapper, HttpStatus.OK);

        Mockito.when(restTemplate.exchange(
                "http://localhost:8080/"+individualId,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseWrapper<JsonNode>>() {
                })).thenReturn(responseEntity);

        ProfileDto profileDto= mockProfileRegistryPlugin.getProfile(individualId);
        Assertions.assertNotNull(profileDto);
        Assertions.assertFalse(profileDto.isActive());
        Assertions.assertEquals(profileDto.getIndividualId(),individualId);
    }


    @Test
    public void updateProfile_withVerifiedClaim_thenPass()  {

        ReflectionTestUtils.setField(mockProfileRegistryPlugin, "addVerifiedClaimsEndpoint","http://localhost:8080/");
        String requestId = "req-123";
        String individualId = "ind-456";

        Map<String, Object> identityData = new HashMap<>();
        identityData.put("email","123@email.com");
        identityData.put("password","123456");

        Map<String,Object> verifiedClaim=new HashMap<>();
        verifiedClaim.put("verified_claims",identityData);

        JsonNode mockIdentity = objectMapper.valueToTree(verifiedClaim);
        ProfileDto profileDto = new ProfileDto();
        profileDto.setIndividualId(individualId);
        profileDto.setIdentity(mockIdentity);


        MockIdentityResponse mockIdentityResponse=new MockIdentityResponse();
        mockIdentityResponse.setStatus("UPDATED");
        ResponseWrapper<MockIdentityResponse> responseWrapper = new ResponseWrapper<>();
        responseWrapper.setResponse(mockIdentityResponse);
        ResponseEntity<ResponseWrapper<MockIdentityResponse>> responseEntity=new ResponseEntity<>(responseWrapper, HttpStatus.OK);
        Mockito.when(restTemplate.exchange(
                Mockito.anyString(),
                Mockito.any(HttpMethod.class),
                Mockito.any(),
                Mockito.eq(new ParameterizedTypeReference<ResponseWrapper<MockIdentityResponse>>() {
                }))).thenReturn(responseEntity);


        ProfileResult profileResult = mockProfileRegistryPlugin.updateProfile(requestId, profileDto);
        Assertions.assertNotNull(profileResult);
        Assertions.assertEquals(profileResult.getStatus(),"UPDATED");
    }

    @Test
    public void updateProfile_withOutVerifiedClaim_thenPass()  {


        ReflectionTestUtils.setField(mockProfileRegistryPlugin, "identityEndpoint","http://localhost:8080/");
        String requestId = "req-123";
        String individualId = "ind-456";

        Map<String, Object> identityData = new HashMap<>();
        identityData.put("email","123@email.com");
        identityData.put("password","123456");

        JsonNode mockIdentity = objectMapper.valueToTree(identityData);
        ProfileDto profileDto = new ProfileDto();
        profileDto.setIndividualId(individualId);
        profileDto.setIdentity(mockIdentity);


        MockIdentityResponse mockIdentityResponse=new MockIdentityResponse();
        mockIdentityResponse.setStatus("UPDATED");
        ResponseWrapper<MockIdentityResponse> responseWrapper = new ResponseWrapper<>();
        responseWrapper.setResponse(mockIdentityResponse);
        ResponseEntity<ResponseWrapper<MockIdentityResponse>> responseEntity=new ResponseEntity<>(responseWrapper, HttpStatus.OK);
        Mockito.when(restTemplate.exchange(
                Mockito.anyString(),
                Mockito.any(HttpMethod.class),
                Mockito.any(),
                Mockito.eq(new ParameterizedTypeReference<ResponseWrapper<MockIdentityResponse>>() {
                }))).thenReturn(responseEntity);


        ProfileResult profileResult = mockProfileRegistryPlugin.updateProfile(requestId, profileDto);
        Assertions.assertNotNull(profileResult);
        Assertions.assertEquals(profileResult.getStatus(),"UPDATED");
    }

    @Test
    public void isMatch_withValidInputChallenge_thenPass() {

        Map<String, Object> identityData = new HashMap<>();
        identityData.put("email","123@email.com");
        identityData.put("password","123456");
        identityData.put("UIN","1234567890");
        JsonNode mockIdentity = objectMapper.valueToTree(identityData);
        JsonNode challengeIdentity=objectMapper.valueToTree(identityData);

        boolean isMatch = mockProfileRegistryPlugin.isMatch(mockIdentity, challengeIdentity);
        Assertions.assertTrue(isMatch);
    }

    @Test
    public void isMatch_withInValidInputChallenge_thenFail() {

        Map<String, Object> identityDataMap = new HashMap<>();
        identityDataMap.put("email","123@email.com");
        identityDataMap.put("password","123456");
        identityDataMap.put("UIN","1234567890");
        JsonNode mockIdentity = objectMapper.valueToTree(identityDataMap);

        Map<String, Object> challengeIdentityMap = new HashMap<>();
        challengeIdentityMap.put("email","1234@email.com");
        challengeIdentityMap.put("password","123456");
        challengeIdentityMap.put("UIN","1234567890");
        JsonNode challengeIdentity=objectMapper.valueToTree(challengeIdentityMap);

        boolean isMatch = mockProfileRegistryPlugin.isMatch(mockIdentity, challengeIdentity);
        Assertions.assertFalse(isMatch);
    }
}
