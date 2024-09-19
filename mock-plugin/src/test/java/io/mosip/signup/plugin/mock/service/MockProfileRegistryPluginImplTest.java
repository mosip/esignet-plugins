package io.mosip.signup.plugin.mock.service;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.mosip.esignet.core.dto.Error;
import io.mosip.esignet.core.dto.ResponseWrapper;
import io.mosip.signup.api.dto.ProfileDto;
import io.mosip.signup.api.dto.ProfileResult;
import io.mosip.signup.api.exception.ProfileException;
import io.mosip.signup.api.util.ProfileCreateUpdateStatus;
import io.mosip.signup.plugin.mock.dto.MockIdentityResponse;
import io.mosip.signup.plugin.mock.util.ErrorConstants;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(SpringRunner.class)
public class MockProfileRegistryPluginImplTest {


    @InjectMocks
    private MockProfileRegistryPluginImpl mockProfileRegistryPlugin;

    @Mock
    RestTemplate restTemplate;

    ObjectMapper objectMapper=new ObjectMapper();


    @Test
    public void validate_withValidActionAndProfileDto_thenPass() {

        List<String> requiredField=new ArrayList<>();
        requiredField.add("phone");
        ReflectionTestUtils.setField(mockProfileRegistryPlugin, "requiredFieldsOnCreate", requiredField);
        String action = "CREATE";
        Map<String, Object> verifiedData = new HashMap<>();
        verifiedData.put("phone","+91841987567");

        JsonNode mockIdentity = objectMapper.valueToTree(verifiedData);
        ProfileDto profileDto = new ProfileDto();
        profileDto.setIndividualId("individualId");
        profileDto.setIdentity(mockIdentity);

        mockProfileRegistryPlugin.validate(action, profileDto);
    }

    @Test
    public void createProfile_withValidRequestAndProfileDto_thenPass() throws ProfileException {
        // Arrange
        ReflectionTestUtils.setField(mockProfileRegistryPlugin, "identityEndpoint","http://localhost:8080/");
        ReflectionTestUtils.setField(mockProfileRegistryPlugin, "usernameField","individualId");
        Map<String, Object> verifiedData = new HashMap<>();
        verifiedData.put("individualId","1234567890");
        JsonNode mockIdentity = objectMapper.valueToTree(verifiedData);
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
        Assert.assertNotNull(result);
        Assert.assertEquals(result.getStatus(), "CREATED");
    }


    @Test
    public void createProfile_withInValidRequestAndProfileDto_thenFail() throws ProfileException {
        // Arrange
        ReflectionTestUtils.setField(mockProfileRegistryPlugin, "usernameField","individualId");
        Map<String,String> verifiedData=new HashMap<>();
        verifiedData.put("individualId","1234567890");
        JsonNode mockIdentity = objectMapper.valueToTree(verifiedData);
        ProfileDto profileDto = new ProfileDto();
        profileDto.setIndividualId("123456789");
        profileDto.setIdentity(mockIdentity);
        try{
            mockProfileRegistryPlugin.createProfile("requestId", profileDto);
        }catch (ProfileException e){
            Assert.assertEquals(ErrorConstants.IDENTIFIER_MISMATCH,e.getMessage());
        }
    }


    @Test
    public void getProfileCreateUpdateStatus_withValidRequestId_thenPass() throws ProfileException {
        String requestId = "requestId123";

        ProfileCreateUpdateStatus status = mockProfileRegistryPlugin.getProfileCreateUpdateStatus(requestId);

        Assert.assertEquals(status,ProfileCreateUpdateStatus.COMPLETED);
    }

    @Test
    public void getProfile_withValidIndividualId_thenPass() throws ProfileException {
        String individualId = "1234567890";
        ReflectionTestUtils.setField(mockProfileRegistryPlugin, "getIdentityEndpoint","http://localhost:8080/");

        Map<String, Object> verifiedData = new HashMap<>();
        verifiedData.put("email","123@email.com");
        verifiedData.put("password","123456");
        verifiedData.put("UIN","1234567890");
        verifiedData.put("individualId",individualId);

        JsonNode mockIdentity = objectMapper.valueToTree(verifiedData);
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
        Assert.assertNotNull(profileDto);
        Assert.assertEquals(profileDto.getIndividualId(),"1234567890");
    }

    @Test
    public void getProfile_withErrorCodeAsIndividualIdFail_thenFail() throws ProfileException {
        String individualId = "1234567890";
        ReflectionTestUtils.setField(mockProfileRegistryPlugin, "getIdentityEndpoint","http://localhost:8080/");

        Map<String, Object> verifiedData = new HashMap<>();
        verifiedData.put("email","123@email.com");
        verifiedData.put("password","123456");
        verifiedData.put("UIN","1234567890");
        verifiedData.put("individualId",individualId);

        JsonNode mockIdentity = objectMapper.valueToTree(verifiedData);
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
        Assert.assertNotNull(profileDto);
        Assert.assertEquals(profileDto.getIndividualId(),individualId);
    }


    @Test
    public void updateProfile_withVerifiedClaim_thenPass()  {

        ReflectionTestUtils.setField(mockProfileRegistryPlugin, "objectMapper",objectMapper);
        ReflectionTestUtils.setField(mockProfileRegistryPlugin, "addVerifiedClaimsEndpoint","http://localhost:8080/");
        String requestId = "req-123";
        String individualId = "ind-456";

        Map<String, Object> verifiedData = new HashMap<>();
        verifiedData.put("email","123@email.com");
        verifiedData.put("password","123456");

        Map<String,Object> verifiedClaim=new HashMap<>();
        verifiedClaim.put("verified_claims",verifiedData);

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
        Assert.assertNotNull(profileResult);
        Assert.assertEquals(profileResult.getStatus(),"UPDATED");
    }

    @Test
    public void updateProfile_withOutVerifiedClaim_thenPass()  {

        ReflectionTestUtils.setField(mockProfileRegistryPlugin, "objectMapper",objectMapper);
        ReflectionTestUtils.setField(mockProfileRegistryPlugin, "identityEndpoint","http://localhost:8080/");
        String requestId = "req-123";
        String individualId = "ind-456";

        Map<String, Object> verifiedData = new HashMap<>();
        verifiedData.put("email","123@email.com");
        verifiedData.put("password","123456");

        JsonNode mockIdentity = objectMapper.valueToTree(verifiedData);
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
        Assert.assertNotNull(profileResult);
        Assert.assertEquals(profileResult.getStatus(),"UPDATED");
    }

    @Test
    public void isMatch_withMatchingIdentityAndInputChallenge_thenPass() {
        // Arrange
        ObjectMapper objectMapper=new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        ReflectionTestUtils.setField(mockProfileRegistryPlugin, "objectMapper",objectMapper);
        Map<String, Object> verifiedData = new HashMap<>();
        verifiedData.put("email","123@email.com");
        verifiedData.put("password","123456");
        verifiedData.put("UIN","1234567890");
        JsonNode mockIdentity = objectMapper.valueToTree(verifiedData);

        boolean isMatch = mockProfileRegistryPlugin.isMatch(mockIdentity, mockIdentity);
        Assert.assertTrue(isMatch);
    }
}
