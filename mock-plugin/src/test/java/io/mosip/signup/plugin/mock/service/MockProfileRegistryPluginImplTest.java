package io.mosip.signup.plugin.mock.service;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
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

import static org.mockito.Mockito.mock;

@RunWith(MockitoJUnitRunner.class)
public class MockProfileRegistryPluginImplTest {


    @InjectMocks
    private MockProfileRegistryPluginImpl mockProfileRegistryPlugin;

    @Mock
    RestTemplate restTemplate;

    ObjectMapper objectMapper=new ObjectMapper();

    @Before
    public void init(){
        objectMapper.registerModule(new JavaTimeModule());
        ReflectionTestUtils.setField(mockProfileRegistryPlugin, "objectMapper",objectMapper);
    }

    String IDENTITY_SCHEMA = "{\n" +
            "  \"$schema\": \"https://json-schema.org/draft/2020-12/schema\",\n" +
            "  \"type\": \"object\",\n" +
            "  \"$defs\": {\n" +
            "    \"langField\": {\n" +
            "      \"type\": \"array\",\n" +
            "      \"items\": {\n" +
            "        \"type\": \"object\",\n" +
            "        \"properties\": {\n" +
            "          \"language\": {\n" +
            "            \"type\": \"string\"\n" +
            "          },\n" +
            "          \"value\": {\n" +
            "            \"type\": \"string\"\n" +
            "          }\n" +
            "        },\n" +
            "        \"required\": [\n" +
            "          \"language\",\n" +
            "          \"value\"\n" +
            "        ],\n" +
            "        \"additionalProperties\": false\n" +
            "      }\n" +
            "    }\n" +
            "  },\n" +
            "  \"properties\": {\n" +
            "    \"individualId\": {\n" +
            "      \"type\": \"string\",\n" +
            "      \"pattern\": \"\\\\S\"\n" +
            "    },\n" +
            "    \"fullName\": {\n" +
            "      \"allOf\": [\n" +
            "        { \"$ref\": \"#/$defs/langField\" },\n" +
            "        {\n" +
            "          \"items\": {\n" +
            "            \"properties\": {\n" +
            "              \"value\": {\n" +
            "                \"pattern\": \"^(?=.*[^\\\\s])^(?:[a-zA-ZÀ-ÿ\\\\s]{1,40}|[ء-ي\\\\s٩ٱ-ڿﹰ-\\uFEFF\\u0600-ۿ]{1,40}|[ក-\\u17FF᧠-᧿ᨀ-\\u1A9F ]{1,40})$\"\n" +
            "              },\n" +
            "              \"language\": {\n" +
            "                \"type\": \"string\",\n" +
            "                \"enum\": [\"eng\",\"fra\",\"ara\"]\n" +
            "              }\n" +
            "            }\n" +
            "          }\n" +
            "        }\n" +
            "      ]\n" +
            "    },\n" +
            "    \"preferredLang\": {\n" +
            "        \"type\": \"string\",\n" +
            "        \"enum\": [\"eng\",\"fra\",\"ara\"],\n" +
            "        \"nullable\": true\n" +
            "    },\n" +
            "    \"phone\": {\n" +
            "      \"type\": \"string\",\n" +
            "      \"pattern\": \"^\\\\+[1-9]\\\\d{8,13}$\"\n" +
            "    },\n" +
            "    \"password\": {\n" +
            "      \"type\": \"string\",\n" +
            "      \"pattern\": \"^[A-Za-z__1-9]{6,9}\\\\d{1}$\"\n" +
            "    }\n" +
            "  },\n" +
            "  \"required\": [\n" +
            "    \"individualId\",\n" +
            "    \"fullName\",\n" +
            "    \"phone\",\n" +
            "    \"password\"\n" +
            "  ],\n" +
            "  \"additionalProperties\": false\n" +
            "}";


    @Test
    public void validate_withValidActionAndProfileDto_thenPass() throws JsonProcessingException {
        ReflectionTestUtils.setField(mockProfileRegistryPlugin, "identitySchemaEndpoint", "http://localhost:8080/");
        String action = "CREATE";

        ResponseWrapper<JsonNode> wrapper = new ResponseWrapper<>();
        wrapper.setResponse(objectMapper.readTree(IDENTITY_SCHEMA));
        ResponseEntity<ResponseWrapper<JsonNode>> responseEntity=new ResponseEntity<>(wrapper, HttpStatus.OK);
        Mockito.when(restTemplate.exchange(
                Mockito.eq("http://localhost:8080/"),
                Mockito.any(HttpMethod.class),
                Mockito.any(),
                Mockito.eq(new ParameterizedTypeReference<ResponseWrapper<JsonNode>>() {
                }))).thenReturn(responseEntity);

        String userinfo = "{\"individualId\" : \"1234567890\",\"phone\" : \"+9134567890\", \"fullName\": [{\"value\": \"John Doe\", \"language\": \"eng\"}], \"preferredLang\": \"eng\", \"password\": \"pas_swo3\"}";
        JsonNode mockIdentity=objectMapper.readTree(userinfo);

        ProfileDto profileDto = new ProfileDto();
        profileDto.setIndividualId("1234567890");
        profileDto.setIdentity(mockIdentity);
        mockProfileRegistryPlugin.validate(action, profileDto);
    }


    @Test
    public void validate_withInvalidRequiredField_thenFail() throws JsonProcessingException {
        ReflectionTestUtils.setField(mockProfileRegistryPlugin, "identitySchemaEndpoint", "http://localhost:8080/");
        String action = "CREATE";

        ResponseWrapper<JsonNode> wrapper = new ResponseWrapper<>();
        wrapper.setResponse(objectMapper.readTree(IDENTITY_SCHEMA));
        ResponseEntity<ResponseWrapper<JsonNode>> responseEntity=new ResponseEntity<>(wrapper, HttpStatus.OK);
        Mockito.when(restTemplate.exchange(
                Mockito.eq("http://localhost:8080/"),
                Mockito.any(HttpMethod.class),
                Mockito.any(),
                Mockito.eq(new ParameterizedTypeReference<ResponseWrapper<JsonNode>>() {
                }))).thenReturn(responseEntity);

        String userinfo = "{\"individualId\" : \"1234567890\", \"fullName\": [{\"value\": \"John Doe\", \"language\": \"eng\"}], \"preferredLang\": \"eng\", \"password\": \"pas_swo3\"}";
        JsonNode mockIdentity=objectMapper.readTree(userinfo);

        ProfileDto profileDto = new ProfileDto();
        profileDto.setIndividualId("1234567890");
        profileDto.setIdentity(mockIdentity);

        try{
            mockProfileRegistryPlugin.validate(action, profileDto);
            Assert.fail();
        }catch (InvalidProfileException e){
            Assert.assertEquals("invalid_phone", e.getErrorCode());
        }
    }

    @Test
    public void validate_withInvalidFieldValue_thenFail() throws JsonProcessingException {
        ReflectionTestUtils.setField(mockProfileRegistryPlugin, "identitySchemaEndpoint", "http://localhost:8080/");
        String action = "CREATE";

        ResponseWrapper<JsonNode> wrapper = new ResponseWrapper<>();
        wrapper.setResponse(objectMapper.readTree(IDENTITY_SCHEMA));
        ResponseEntity<ResponseWrapper<JsonNode>> responseEntity=new ResponseEntity<>(wrapper, HttpStatus.OK);
        Mockito.when(restTemplate.exchange(
                Mockito.eq("http://localhost:8080/"),
                Mockito.any(HttpMethod.class),
                Mockito.any(),
                Mockito.eq(new ParameterizedTypeReference<ResponseWrapper<JsonNode>>() {
                }))).thenReturn(responseEntity);

        String userinfo = "{\"individualId\" : \"1234567890\", \"fullName\": [{\"value\": \"John Doe\", \"language\": \"eng\"}], \"preferredLang\": \"khm\", \"password\": \"pas_swo3\"}";
        JsonNode mockIdentity=objectMapper.readTree(userinfo);
        ProfileDto profileDto = new ProfileDto();
        profileDto.setIndividualId("1234567890");
        profileDto.setIdentity(mockIdentity);

        try{
            mockProfileRegistryPlugin.validate(action, profileDto);
            Assert.fail();
        }catch (InvalidProfileException e) {
            Assert.assertEquals("invalid_preferredlang", e.getErrorCode());
        }
    }

    @Test
    public void validate_patternNotMatching_thenFail() throws JsonProcessingException {
        ReflectionTestUtils.setField(mockProfileRegistryPlugin, "identitySchemaEndpoint", "http://localhost:8080/");
        String action = "CREATE";

        ResponseWrapper<JsonNode> wrapper = new ResponseWrapper<>();
        wrapper.setResponse(objectMapper.readTree(IDENTITY_SCHEMA));
        ResponseEntity<ResponseWrapper<JsonNode>> responseEntity=new ResponseEntity<>(wrapper, HttpStatus.OK);
        Mockito.when(restTemplate.exchange(
                Mockito.eq("http://localhost:8080/"),
                Mockito.any(HttpMethod.class),
                Mockito.any(),
                Mockito.eq(new ParameterizedTypeReference<ResponseWrapper<JsonNode>>() {
                }))).thenReturn(responseEntity);

        String userinfo = "{\"individualId\" : \"1234567890\",\"phone\" : \"+0134567890\", \"fullName\": [{\"value\": \"John Doe\", \"language\": \"eng\"}], \"preferredLang\": \"eng\", \"password\": \"pas_swo3\"}";
        JsonNode mockIdentity=objectMapper.readTree(userinfo);
        ProfileDto profileDto = new ProfileDto();
        profileDto.setIndividualId("1234567890");
        profileDto.setIdentity(mockIdentity);

        try{
            mockProfileRegistryPlugin.validate(action, profileDto);
            Assert.fail();
        }catch (InvalidProfileException e) {
            Assert.assertEquals("invalid_phone", e.getErrorCode());
        }
    }

    @Test
    public void validate_withInvalidUpdateData_thenFail() throws JsonProcessingException {
        ReflectionTestUtils.setField(mockProfileRegistryPlugin, "identitySchemaEndpoint", "http://localhost:8080/");
        String action = "UPDATE";

        ResponseWrapper<JsonNode> wrapper = new ResponseWrapper<>();
        wrapper.setResponse(objectMapper.readTree(IDENTITY_SCHEMA));
        ResponseEntity<ResponseWrapper<JsonNode>> responseEntity=new ResponseEntity<>(wrapper, HttpStatus.OK);
        Mockito.when(restTemplate.exchange(
                Mockito.eq("http://localhost:8080/"),
                Mockito.any(HttpMethod.class),
                Mockito.any(),
                Mockito.eq(new ParameterizedTypeReference<ResponseWrapper<JsonNode>>() {
                }))).thenReturn(responseEntity);

        String userinfo = "{\"individualId\": \"1234567890\", \"password\": \"@password123\"}";
        JsonNode mockIdentity=objectMapper.readTree(userinfo);
        ProfileDto profileDto = new ProfileDto();
        profileDto.setIndividualId("1234567890");
        profileDto.setIdentity(mockIdentity);

        try{
            mockProfileRegistryPlugin.validate(action, profileDto);
            Assert.fail();
        }catch (InvalidProfileException e) {
            Assert.assertEquals("invalid_password", e.getErrorCode());
        }
    }

    @Test
    public void createProfile_withValidRequestAndProfileDto_thenPass() throws ProfileException {
        // Arrange
        ReflectionTestUtils.setField(mockProfileRegistryPlugin, "identityEndpoint","http://localhost:8080/");
        ReflectionTestUtils.setField(mockProfileRegistryPlugin, "identifierField","individualId");
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
        Assert.assertNotNull(result);
        Assert.assertEquals(result.getStatus(), "CREATED");
    }


    @Test
    public void createProfile_withInValidRequestAndProfileDto_thenFail() throws ProfileException {
        // Arrange
        ReflectionTestUtils.setField(mockProfileRegistryPlugin, "identifierField","individualId");
        Map<String,String> identityData=new HashMap<>();
        identityData.put("individualId","1234567890");
        JsonNode mockIdentity = objectMapper.valueToTree(identityData);
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
    public void createProfile_withFacePhoto_thenPass() throws ProfileException {
        // Arrange
        ReflectionTestUtils.setField(mockProfileRegistryPlugin, "identityEndpoint","http://localhost:8080/");
        ReflectionTestUtils.setField(mockProfileRegistryPlugin, "identifierField","individualId");
        ReflectionTestUtils.setField(mockProfileRegistryPlugin, "faceBiometricFieldName","encodedPhoto");
        ReflectionTestUtils.setField(mockProfileRegistryPlugin, "faceBiometricValuePrefix","data:image/jpeg;base64,");

        ObjectNode mockIdentity = mock(ObjectNode.class);
        Mockito.when(mockIdentity.get("individualId")).thenReturn(objectMapper.valueToTree("1234567890"));
        Mockito.when(mockIdentity.hasNonNull("encodedPhoto")).thenReturn(true);
        ObjectNode encodedPhotoNode = mock(ObjectNode.class);
        Mockito.when(mockIdentity.get("encodedPhoto")).thenReturn(encodedPhotoNode)
                .thenReturn(encodedPhotoNode);
        Mockito.when(encodedPhotoNode.hasNonNull("value")).thenReturn(true);
        Mockito.when(encodedPhotoNode.get("value")).thenReturn(objectMapper.valueToTree("encodedPhotoString"))
                .thenReturn(objectMapper.valueToTree("encodedPhotoString"));
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
        Mockito.verify(mockIdentity, Mockito.times(1)).put("individualId",
                "1234567890");
        Mockito.verify(mockIdentity, Mockito.times(1)).put("encodedPhoto",
                "data:image/jpeg;base64,encodedPhotoString");
        Assert.assertNotNull(result);
        Assert.assertEquals(result.getStatus(), "CREATED");
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
        Assert.assertNotNull(profileDto);
        Assert.assertTrue(profileDto.isActive());
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
        Assert.assertNotNull(profileDto);
        Assert.assertFalse(profileDto.isActive());
        Assert.assertEquals(profileDto.getIndividualId(),individualId);
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
        Assert.assertNotNull(profileResult);
        Assert.assertEquals(profileResult.getStatus(),"UPDATED");
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
        Assert.assertNotNull(profileResult);
        Assert.assertEquals(profileResult.getStatus(),"UPDATED");
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
        Assert.assertTrue(isMatch);
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
        Assert.assertFalse(isMatch);
    }
}
