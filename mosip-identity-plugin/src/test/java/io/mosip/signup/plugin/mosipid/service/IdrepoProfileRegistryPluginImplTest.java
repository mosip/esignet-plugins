package io.mosip.signup.plugin.mosipid.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.mosip.signup.api.dto.ProfileDto;
import io.mosip.signup.api.dto.ProfileResult;
import io.mosip.signup.api.exception.InvalidProfileException;
import io.mosip.signup.api.exception.ProfileException;
import io.mosip.signup.api.util.ProfileCreateUpdateStatus;
import io.mosip.signup.plugin.mosipid.dto.*;
import io.mosip.signup.plugin.mosipid.dto.Error;
import io.mosip.signup.plugin.mosipid.util.ProfileCacheService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@RunWith(MockitoJUnitRunner.class)
public class IdrepoProfileRegistryPluginImplTest {

    @InjectMocks
    private IdrepoProfileRegistryPluginImpl idrepoProfileRegistryPlugin;

    @Mock
    private ProfileCacheService profileCacheService;

    @Mock
    private RestTemplate restTemplate;

    private  ObjectMapper objectMapper;

    private static final String schemaSchemaJson="{\"$schema\":\"http://json-schema.org/draft-07/schema#\",\"description\":\"Identity schema for sign up\",\"additionalProperties\":false,\"title\":\"signup identity\",\"type\":\"object\",\"definitions\":{\"simpleType\":{\"uniqueItems\":true,\"additionalItems\":false,\"type\":\"array\",\"items\":{\"additionalProperties\":false,\"type\":\"object\",\"required\":[\"language\",\"value\"],\"properties\":{\"language\":{\"type\":\"string\"},\"value\":{\"type\":\"string\"}}}},\"documentType\":{\"additionalProperties\":false,\"type\":\"object\",\"properties\":{\"format\":{\"type\":\"string\"},\"type\":{\"type\":\"string\"},\"value\":{\"type\":\"string\"}}},\"biometricsType\":{\"additionalProperties\":false,\"type\":\"object\",\"properties\":{\"format\":{\"type\":\"string\"},\"version\":{\"type\":\"number\",\"minimum\":0},\"value\":{\"type\":\"string\"}}},\"hashType\":{\"additionalProperties\":false,\"type\":\"object\",\"properties\":{\"hash\":{\"type\":\"string\"},\"salt\":{\"type\":\"string\"}}}},\"properties\":{\"identity\":{\"additionalProperties\":false,\"type\":\"object\",\"required\":[\"IDSchemaVersion\",\"phone\"],\"properties\":{\"UIN\":{\"bioAttributes\":[],\"fieldCategory\":\"none\",\"format\":\"none\",\"type\":\"string\",\"fieldType\":\"default\"},\"IDSchemaVersion\":{\"bioAttributes\":[],\"fieldCategory\":\"none\",\"format\":\"none\",\"type\":\"number\",\"fieldType\":\"default\",\"minimum\":0},\"selectedHandles\":{\"fieldCategory\":\"none\",\"format\":\"none\",\"type\":\"array\",\"items\":{\"type\":\"string\"},\"fieldType\":\"default\"},\"fullName\":{\"bioAttributes\":[],\"validators\":[{\"validator\":\"^(.{3,50})$\",\"arguments\":[],\"type\":\"regex\"}],\"fieldCategory\":\"pvt\",\"format\":\"none\",\"fieldType\":\"default\",\"$ref\":\"#/definitions/simpleType\"},\"phone\":{\"bioAttributes\":[],\"validators\":[{\"validator\":\"^[+]91([0-9]{8,9})$\",\"arguments\":[],\"type\":\"regex\"}],\"fieldCategory\":\"pvt\",\"format\":\"none\",\"type\":\"string\",\"fieldType\":\"default\",\"requiredOn\":\"\",\"handle\":true},\"password\":{\"bioAttributes\":[],\"validators\":[],\"fieldCategory\":\"pvt\",\"format\":\"none\",\"fieldType\":\"default\",\"$ref\":\"#/definitions/hashType\"},\"preferredLang\":{\"bioAttributes\":[],\"validators\":[{\"validator\":\"(^eng$)\",\"arguments\":[],\"type\":\"regex\"}],\"fieldCategory\":\"pvt\",\"format\":\"none\",\"fieldType\":\"default\",\"type\":\"string\"},\"registrationType\":{\"bioAttributes\":[],\"validators\":[{\"validator\":\"^L[1-2]{1}$\",\"arguments\":[],\"type\":\"regex\"}],\"fieldCategory\":\"pvt\",\"format\":\"none\",\"fieldType\":\"default\",\"type\":\"string\"},\"phoneVerified\":{\"bioAttributes\":[],\"validators\":[],\"fieldCategory\":\"pvt\",\"format\":\"none\",\"fieldType\":\"default\",\"type\":\"boolean\"},\"updatedAt\":{\"bioAttributes\":[],\"validators\":[],\"fieldCategory\":\"pvt\",\"format\":\"none\",\"fieldType\":\"default\",\"type\":\"number\"}}}}}";


    @Before
    public void beforeEach(){

        objectMapper=new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        List<String> defaultSelectedHandles = new ArrayList<>();
        defaultSelectedHandles.add("email");
        ReflectionTestUtils.setField(idrepoProfileRegistryPlugin, "defaultSelectedHandles",defaultSelectedHandles);
        ReflectionTestUtils.setField(idrepoProfileRegistryPlugin, "objectMapper",objectMapper);
        ReflectionTestUtils.setField(idrepoProfileRegistryPlugin, "getUinEndpoint","http://localhost:8080/identity/v1/uin");
        ReflectionTestUtils.setField(idrepoProfileRegistryPlugin, "schemaUrl","http://localhost:8080/identity/v1/schema/");
        ReflectionTestUtils.setField(idrepoProfileRegistryPlugin, "identityEndpoint","http://localhost:8080/identity/v1/identity/");
        ReflectionTestUtils.setField(idrepoProfileRegistryPlugin, "generateHashEndpoint","http://localhost:8080/identity/v1/identity/genereateHash/");
        ReflectionTestUtils.setField(idrepoProfileRegistryPlugin, "getIdentityEndpoint","http://localhost:8080/identity/v1/identity/");
        ReflectionTestUtils.setField(idrepoProfileRegistryPlugin, "getIdentityEndpointMethod", "POST");
        ReflectionTestUtils.setField(idrepoProfileRegistryPlugin, "getStatusEndpoint","http://localhost:8080/identity/v1/identity/");
        ReflectionTestUtils.setField(idrepoProfileRegistryPlugin, "dynamicFieldsBaseUrl","http://mock/api/dynamicFields");
        ReflectionTestUtils.setField(idrepoProfileRegistryPlugin, "docTypesAndCategoryBaseUrl", "http://mock/api/docTypes");
        ReflectionTestUtils.setField(idrepoProfileRegistryPlugin, "mandatoryLanguages", List.of("en"));
        ReflectionTestUtils.setField(idrepoProfileRegistryPlugin, "optionalLanguages", List.of("fr","ar"));
    }

    @Test
    public void validate_withValidProfile_thenPass()  {

        String individualId = "ind-456";

        Map<String, Object> identityData = new HashMap<>();
        identityData.put("phone","+91841987567");

        JsonNode mockIdentity = objectMapper.valueToTree(identityData);
        ProfileDto profileDto = new ProfileDto();
        profileDto.setIndividualId(individualId);
        profileDto.setIdentity(mockIdentity);

        ResponseWrapper<SchemaResponse> responseWrapper = new ResponseWrapper<>();
        SchemaResponse schemaResponse = new SchemaResponse();
        schemaResponse.setIdVersion(0.0);
        schemaResponse.setSchemaJson(schemaSchemaJson);
        responseWrapper.setResponse(schemaResponse);
        ResponseEntity<ResponseWrapper<SchemaResponse>> responseEntity2=new ResponseEntity<>(responseWrapper, HttpStatus.OK);
        Mockito.when(restTemplate.exchange(
                "http://localhost:8080/identity/v1/schema/"+0.0,  // Matches any URL string
                HttpMethod.GET,  // Matches any HTTP method
                null,  // Matches any HttpEntity
                new ParameterizedTypeReference<ResponseWrapper<SchemaResponse>>() {}
        )).thenReturn(responseEntity2);
        idrepoProfileRegistryPlugin.validate("CREATE", profileDto);

    }

    @Test
    public void validate_withValidProfileContainingArrayDataType_thenPass()  {
        String individualId = "ind-456";

        Map<String, Object> identityData = new HashMap<>();
        SimpleType [] simpleTypesArray=new SimpleType[1];
        SimpleType simpleType=new SimpleType();
        simpleType.setLanguage("en");
        simpleType.setValue("John Doe");
        simpleTypesArray[0]=simpleType;
        identityData.put("phone","+91841987567");
        identityData.put("fullName",simpleTypesArray);

        JsonNode mockIdentity = objectMapper.valueToTree(identityData);
        ProfileDto profileDto = new ProfileDto();
        profileDto.setIndividualId(individualId);
        profileDto.setIdentity(mockIdentity);

        ResponseWrapper<SchemaResponse> responseWrapper = new ResponseWrapper<>();
        SchemaResponse schemaResponse = new SchemaResponse();
        schemaResponse.setIdVersion(0.0);
        schemaResponse.setSchemaJson(schemaSchemaJson);
        responseWrapper.setResponse(schemaResponse);
        ResponseEntity<ResponseWrapper<SchemaResponse>> responseEntity2=new ResponseEntity<>(responseWrapper, HttpStatus.OK);
        Mockito.when(restTemplate.exchange(
                "http://localhost:8080/identity/v1/schema/"+0.0,  // Matches any URL string
                HttpMethod.GET,  // Matches any HTTP method
                null,  // Matches any HttpEntity
                new ParameterizedTypeReference<ResponseWrapper<SchemaResponse>>() {}
        )).thenReturn(responseEntity2);
        idrepoProfileRegistryPlugin.validate("CREATE", profileDto);
    }

    @Test
    public void validate_withInvalidProfile_thenFail()  {

        String individualId = "ind-456";

        Map<String, Object> identityData = new HashMap<>();
        JsonNode mockIdentity = objectMapper.valueToTree(identityData);
        ProfileDto profileDto = new ProfileDto();
        profileDto.setIndividualId(individualId);
        profileDto.setIdentity(mockIdentity);

        ResponseWrapper<SchemaResponse> responseWrapper = new ResponseWrapper<>();
        SchemaResponse schemaResponse = new SchemaResponse();
        schemaResponse.setIdVersion(0.0);
        schemaResponse.setSchemaJson(schemaSchemaJson);
        responseWrapper.setResponse(schemaResponse);
        ResponseEntity<ResponseWrapper<SchemaResponse>> responseEntity=new ResponseEntity<>(responseWrapper, HttpStatus.OK);
        Mockito.when(restTemplate.exchange(
                "http://localhost:8080/identity/v1/schema/"+0.0,  // Matches any URL string
                HttpMethod.GET,  // Matches any HTTP method
                null,  // Matches any HttpEntity
                new ParameterizedTypeReference<ResponseWrapper<SchemaResponse>>() {}
        )).thenReturn(responseEntity);
        try{
            idrepoProfileRegistryPlugin.validate("CREATE", profileDto);
        }catch (InvalidProfileException e){
            Assert.assertEquals(e.getErrorCode(),"invalid_phone");
        }
    }

    @Test
    public void createProfile_withValidProfileDetails_thenPass()  {
        String requestId = "req-123";
        String individualId = "ind-456";

        JsonNode mockIdentity = createIdentity();
        ProfileDto profileDto = new ProfileDto();
        profileDto.setIndividualId(individualId);
        profileDto.setIdentity(mockIdentity);

        ResponseWrapper<UINResponse> responseWrapper = new ResponseWrapper<>();
        UINResponse uinResponse = new UINResponse();
        uinResponse.setUIN("1234567890");
        responseWrapper.setResponse(uinResponse);
        ResponseEntity<ResponseWrapper<UINResponse>> responseEntity = new ResponseEntity<>(responseWrapper, HttpStatus.OK);

        Mockito.when(profileCacheService.setHandleRequestIds(Mockito.anyString(),Mockito.anyList())).thenReturn(null);
        Mockito.when(restTemplate.exchange(
                "http://localhost:8080/identity/v1/uin",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseWrapper<UINResponse>>() {}
        )).thenReturn(responseEntity);

        ResponseWrapper<SchemaResponse> responseWrapper2 = new ResponseWrapper<>();
        SchemaResponse schemaResponse = new SchemaResponse();
        schemaResponse.setIdVersion(0.0);
        schemaResponse.setSchemaJson(schemaSchemaJson);
        responseWrapper2.setResponse(schemaResponse);
        ResponseEntity<ResponseWrapper<SchemaResponse>> responseEntity2=new ResponseEntity<>(responseWrapper2, HttpStatus.OK);
        Mockito.when(restTemplate.exchange(
                "http://localhost:8080/identity/v1/schema/"+0.0,  // Matches any URL string
                HttpMethod.GET,  // Matches any HTTP method
                null,  // Matches any HttpEntity
                new ParameterizedTypeReference<ResponseWrapper<SchemaResponse>>() {}
        )).thenReturn(responseEntity2);

        ResponseWrapper<IdentityResponse> responseWrapper3 = new ResponseWrapper<>();
        IdentityResponse identityResponse = new IdentityResponse();
        identityResponse.setStatus("SUCCESS");
        responseWrapper3.setResponse(identityResponse);
        ResponseEntity<ResponseWrapper<IdentityResponse>> responseEntity3=new ResponseEntity<>(responseWrapper3, HttpStatus.OK);

        Mockito.when(restTemplate.exchange(
                Mockito.anyString(),
                Mockito.any(HttpMethod.class),
                Mockito.any(HttpEntity.class),
                Mockito.eq(new ParameterizedTypeReference<ResponseWrapper<IdentityResponse>>() {
                }))).thenReturn(responseEntity3);

        ProfileResult profileResult = idrepoProfileRegistryPlugin.createProfile(requestId, profileDto);
        Assert.assertNotNull(profileResult);
        Assert.assertEquals(profileResult.getStatus(),"SUCCESS");
    }

    @Test
    public void createProfile_withInValidProfileDetails_thenFail()  {
        String requestId = "req-123";
        String individualId = "ind-456";

        JsonNode mockIdentity = createIdentity();
        ProfileDto profileDto = new ProfileDto();
        profileDto.setIndividualId(individualId);
        profileDto.setIdentity(mockIdentity);

        ResponseWrapper<UINResponse> responseWrapper = new ResponseWrapper<>();
        UINResponse uinResponse = new UINResponse();
        uinResponse.setUIN("1234567890");
        responseWrapper.setResponse(uinResponse);
        ResponseEntity<ResponseWrapper<UINResponse>> responseEntity = new ResponseEntity<>(responseWrapper, HttpStatus.OK);

        Mockito.when(profileCacheService.setHandleRequestIds(Mockito.anyString(),Mockito.anyList())).thenReturn(null);
        Mockito.when(restTemplate.exchange(
                "http://localhost:8080/identity/v1/uin",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseWrapper<UINResponse>>() {}
        )).thenReturn(responseEntity);

        ResponseWrapper<SchemaResponse> responseWrapper2 = new ResponseWrapper<>();
        SchemaResponse schemaResponse = new SchemaResponse();
        schemaResponse.setIdVersion(0.0);
        schemaResponse.setSchemaJson(schemaSchemaJson);
        responseWrapper2.setResponse(schemaResponse);
        ResponseEntity<ResponseWrapper<SchemaResponse>> responseEntity2=new ResponseEntity<>(responseWrapper2, HttpStatus.OK);
        Mockito.when(restTemplate.exchange(
                "http://localhost:8080/identity/v1/schema/"+0.0,  // Matches any URL string
                HttpMethod.GET,  // Matches any HTTP method
                null,  // Matches any HttpEntity
                new ParameterizedTypeReference<ResponseWrapper<SchemaResponse>>() {}
        )).thenReturn(responseEntity2);

        ResponseWrapper<IdentityResponse> responseWrapper3 = new ResponseWrapper<>();
        IdentityResponse identityResponse = new IdentityResponse();
        identityResponse.setStatus("SUCCESS");
        responseWrapper3.setResponse(identityResponse);
        ResponseEntity<ResponseWrapper<IdentityResponse>> responseEntity3=new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);

        Mockito.when(restTemplate.exchange(
                Mockito.anyString(),
                Mockito.any(HttpMethod.class),
                Mockito.any(HttpEntity.class),
                Mockito.eq(new ParameterizedTypeReference<ResponseWrapper<IdentityResponse>>() {
                }))).thenReturn(responseEntity3);

        try{
            idrepoProfileRegistryPlugin.createProfile(requestId, profileDto);
            Assert.fail();
        }catch (ProfileException e){
            Assert.assertEquals(e.getErrorCode(),"request_failed");
        }
    }

    @Test
    public void updateProfile_withValidProfileDetails_thenPass()  {

            String requestId = "req-123";
            String individualId = "ind-456";
            Map<String, Object> identityData = new HashMap<>();
            identityData.put("email","123@email.com");
            identityData.put("password","123456");

            JsonNode mockIdentity = objectMapper.valueToTree(identityData);
            ProfileDto profileDto = new ProfileDto();
            profileDto.setIndividualId(individualId);
            profileDto.setIdentity(mockIdentity);

            Mockito.when(profileCacheService.setHandleRequestIds(Mockito.anyString(),Mockito.anyList())).thenReturn(null);

            ResponseWrapper<SchemaResponse> responseWrapper= new ResponseWrapper<>();
            SchemaResponse schemaResponse = new SchemaResponse();
            schemaResponse.setIdVersion(0.0);
            schemaResponse.setSchemaJson(schemaSchemaJson);
            responseWrapper.setResponse(schemaResponse);
            ResponseEntity<ResponseWrapper<SchemaResponse>> responseEntity=new ResponseEntity<>(responseWrapper, HttpStatus.OK);
            Mockito.when(restTemplate.exchange(
                    "http://localhost:8080/identity/v1/schema/"+0.0,  // Matches any URL string
                    HttpMethod.GET,  // Matches any HTTP method
                    null,  // Matches any HttpEntity
                    new ParameterizedTypeReference<ResponseWrapper<SchemaResponse>>() {}
            )).thenReturn(responseEntity);

        //Mocking Password Hash
        ResponseWrapper<Password.PasswordHash> responseWrapper4 = new ResponseWrapper<>();
        Password.PasswordHash passwordHash = new Password.PasswordHash();
        passwordHash.setHashValue("123456");
        passwordHash.setSalt("123456");
        responseWrapper4.setResponse(passwordHash);
        ResponseEntity<ResponseWrapper<Password.PasswordHash>> responseEntity2=new ResponseEntity<>(responseWrapper4, HttpStatus.OK);
        Mockito.when(restTemplate.exchange(
                Mockito.anyString(),
                Mockito.any(HttpMethod.class),
                Mockito.any(HttpEntity.class),
                Mockito.eq(new ParameterizedTypeReference<ResponseWrapper<Password.PasswordHash>>() {
                }))).thenReturn(responseEntity2);

            ResponseWrapper<IdentityResponse> responseWrapper3 = new ResponseWrapper<>();
            IdentityResponse identityResponse = new IdentityResponse();
            identityResponse.setStatus("SUCCESS");
            responseWrapper3.setResponse(identityResponse);
            ResponseEntity<ResponseWrapper<IdentityResponse>> responseEntity3=new ResponseEntity<>(responseWrapper3, HttpStatus.OK);

            Mockito.when(restTemplate.exchange(
                    Mockito.anyString(),
                    Mockito.any(HttpMethod.class),
                    Mockito.any(HttpEntity.class),
                    Mockito.eq(new ParameterizedTypeReference<ResponseWrapper<IdentityResponse>>() {
                    }))).thenReturn(responseEntity3);

            ProfileResult profileResult = idrepoProfileRegistryPlugin.updateProfile(requestId, profileDto);
            Assert.assertNotNull(profileResult);
            Assert.assertEquals(profileResult.getStatus(),"SUCCESS");
    }

    @Test
    public void getProfile_withValidDetails_thenPass()  {
        String individualId = "1234567890";
        Map<String, Object> identityData = new HashMap<>();
        identityData.put("email","123@email.com");
        identityData.put("password","123456");
        identityData.put("UIN",individualId);

        JsonNode mockIdentity = objectMapper.valueToTree(identityData);

        ResponseWrapper<IdentityResponse> responseWrapper = new ResponseWrapper<>();
        IdentityResponse identityResponse = new IdentityResponse();
        identityResponse.setStatus("SUCCESS");
        identityResponse.setIdentity(mockIdentity);

        responseWrapper.setResponse(identityResponse);
        ResponseEntity<ResponseWrapper<IdentityResponse>> responseEntity=new ResponseEntity<>(responseWrapper, HttpStatus.OK);

        Mockito.when(restTemplate.exchange(
                Mockito.anyString(),
                Mockito.any(HttpMethod.class),
                Mockito.any(HttpEntity.class),
                Mockito.eq(new ParameterizedTypeReference<ResponseWrapper<IdentityResponse>>() {
                }))).thenReturn(responseEntity);
        ProfileDto profileDto= idrepoProfileRegistryPlugin.getProfile(individualId);
        Assert.assertNotNull(profileDto);
        Assert.assertEquals(profileDto.getIndividualId(),individualId);

        ReflectionTestUtils.setField(idrepoProfileRegistryPlugin, "getIdentityEndpointMethod", "GET");
        ReflectionTestUtils.setField(idrepoProfileRegistryPlugin, "getIdentityEndpointFallbackPath", "%s?type=demo");

        Mockito.when(restTemplate.exchange(
                "http://localhost:8080/identity/v1/identity/" + individualId + "@phone?type=demo&idType=HANDLE",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseWrapper<IdentityResponse>>() {})).thenReturn(responseEntity);
        profileDto= idrepoProfileRegistryPlugin.getProfile(individualId+"@phone");
        Assert.assertNotNull(profileDto);
        Assert.assertEquals(profileDto.getIndividualId(),individualId);
    }

    @Test
    public void getProfile_withErrorCodeAsIdentityFail_thenFail()  {
        String individualId = "1234567890";
        JsonNode mockIdentity = createIdentity();

        ResponseWrapper<IdentityResponse> responseWrapper = new ResponseWrapper<>();
        IdentityResponse identityResponse = new IdentityResponse();
        identityResponse.setStatus("SUCCESS");
        identityResponse.setIdentity(mockIdentity);

        responseWrapper.setResponse(null);
        Error error = new Error();
        error.setErrorCode("IDR-IDC-007");
        responseWrapper.setErrors(List.of(error));
        ResponseEntity<ResponseWrapper<IdentityResponse>> responseEntity=new ResponseEntity<>(responseWrapper, HttpStatus.OK);

        Mockito.when(restTemplate.exchange(
                Mockito.anyString(),
                Mockito.any(HttpMethod.class),
                Mockito.any(HttpEntity.class),
                Mockito.eq(new ParameterizedTypeReference<ResponseWrapper<IdentityResponse>>() {
                }))).thenReturn(responseEntity);
        ProfileDto profileDto= idrepoProfileRegistryPlugin.getProfile(individualId);
        Assert.assertNotNull(profileDto);
        Assert.assertEquals(profileDto.getIndividualId(),"1234567890");
    }

    @Test
    public void getProfileCreateUpdate_withStatusAsProcessing_thenPass(){
        ReflectionTestUtils.setField(idrepoProfileRegistryPlugin, "getStatusEndpoint","http://localhost:8080/identity/v1/identity/");

        Mockito.when(profileCacheService.getHandleRequestIds(ArgumentMatchers.anyString())).thenReturn(null);

        ResponseWrapper<IdentityStatusResponse> responseWrapper = new ResponseWrapper<>();
        IdentityStatusResponse identityStatusResponse = new IdentityStatusResponse();
        identityStatusResponse.setStatusCode("PROCESSING");

        responseWrapper.setResponse(identityStatusResponse);
        ResponseEntity<ResponseWrapper<IdentityStatusResponse>> responseEntity=new ResponseEntity<>(responseWrapper, HttpStatus.OK);

        Mockito.when(restTemplate.exchange(
                "http://localhost:8080/identity/v1/identity/requestId",  // Matches any URL string
                HttpMethod.GET,  // Matches any HTTP method
                null,  // Matches any HttpEntity
                new ParameterizedTypeReference<ResponseWrapper<IdentityStatusResponse>>() {}
        )).thenReturn(responseEntity);
        ProfileCreateUpdateStatus profileCreateUpdateStatus = idrepoProfileRegistryPlugin.getProfileCreateUpdateStatus("requestId");
        Assert.assertNotNull(profileCreateUpdateStatus);
        Assert.assertEquals(profileCreateUpdateStatus,ProfileCreateUpdateStatus.PENDING);
    }

    @Test
    public void getProfileCreateUpdateStatus_withStatusAsFailed_thenPass(){
        ReflectionTestUtils.setField(idrepoProfileRegistryPlugin, "getStatusEndpoint","http://localhost:8080/identity/v1/identity/");

        Mockito.when(profileCacheService.getHandleRequestIds(ArgumentMatchers.anyString())).thenReturn(null);

        ResponseWrapper<IdentityStatusResponse> responseWrapper = new ResponseWrapper<>();
        IdentityStatusResponse identityStatusResponse = new IdentityStatusResponse();
        identityStatusResponse.setStatusCode("FAILED");


        responseWrapper.setResponse(identityStatusResponse);
        ResponseEntity<ResponseWrapper<IdentityStatusResponse>> responseEntity=new ResponseEntity<>(responseWrapper, HttpStatus.OK);

        Mockito.when(restTemplate.exchange(
                "http://localhost:8080/identity/v1/identity/requestId",  // Matches any URL string
                HttpMethod.GET,  // Matches any HTTP method
                null,  // Matches any HttpEntity
                new ParameterizedTypeReference<ResponseWrapper<IdentityStatusResponse>>() {}
        )).thenReturn(responseEntity);
        ProfileCreateUpdateStatus profileCreateUpdateStatus = idrepoProfileRegistryPlugin.getProfileCreateUpdateStatus("requestId");
        Assert.assertNotNull(profileCreateUpdateStatus);
        Assert.assertEquals(profileCreateUpdateStatus,ProfileCreateUpdateStatus.FAILED);
    }

    @Test
    public void getProfileCreateUpdateStatus_withStatusAsStored_thenPass(){
        ReflectionTestUtils.setField(idrepoProfileRegistryPlugin, "getStatusEndpoint","http://localhost:8080/identity/v1/identity/");

        Mockito.when(profileCacheService.getHandleRequestIds(ArgumentMatchers.anyString())).thenReturn(List.of("requestId"));

        ResponseWrapper<IdentityStatusResponse> responseWrapper = new ResponseWrapper<>();
        IdentityStatusResponse identityStatusResponse = new IdentityStatusResponse();
        identityStatusResponse.setStatusCode("STORED");

        responseWrapper.setResponse(identityStatusResponse);
        ResponseEntity<ResponseWrapper<IdentityStatusResponse>> responseEntity=new ResponseEntity<>(responseWrapper, HttpStatus.OK);

        Mockito.when(restTemplate.exchange(
                "http://localhost:8080/identity/v1/identity/requestId",  // Matches any URL string
                HttpMethod.GET,  // Matches any HTTP method
                null,  // Matches any HttpEntity
                new ParameterizedTypeReference<ResponseWrapper<IdentityStatusResponse>>() {}
        )).thenReturn(responseEntity);
        ProfileCreateUpdateStatus profileCreateUpdateStatus = idrepoProfileRegistryPlugin.getProfileCreateUpdateStatus("requestId");
        Assert.assertNotNull(profileCreateUpdateStatus);
        Assert.assertEquals(profileCreateUpdateStatus,ProfileCreateUpdateStatus.COMPLETED);
    }

    @Test
    public void getProfileCreateUpdateStatus_withInvalidStatusCode_thenFail(){
        Mockito.when(profileCacheService.getHandleRequestIds(ArgumentMatchers.anyString())).thenReturn(List.of("requestId"));

        ResponseWrapper<IdentityStatusResponse> responseWrapper = new ResponseWrapper<>();
        IdentityStatusResponse identityStatusResponse = new IdentityStatusResponse();
        identityStatusResponse.setStatusCode("STORED");
        responseWrapper.setResponse(new IdentityStatusResponse());
        ResponseEntity<ResponseWrapper<IdentityStatusResponse>> responseEntity=new ResponseEntity<>(responseWrapper, HttpStatus.OK);
        Mockito.when(restTemplate.exchange(
                "http://localhost:8080/identity/v1/identity/requestId",  // Matches any URL string
                HttpMethod.GET,  // Matches any HTTP method
                null,  // Matches any HttpEntity
                new ParameterizedTypeReference<ResponseWrapper<IdentityStatusResponse>>() {}
        )).thenReturn(responseEntity);
        try{
            idrepoProfileRegistryPlugin.getProfileCreateUpdateStatus("requestId");
            Assert.fail();
        }catch (ProfileException e){
            Assert.assertEquals(e.getErrorCode(),"request_failed");
        }
    }

    @Test
    public void isMatch_withValidDetails_thenPass(){
        ReflectionTestUtils.setField(idrepoProfileRegistryPlugin, "objectMapper",objectMapper);
        Map<String, Object> identityData = new HashMap<>();
        identityData.put("email","123@email.com");
        identityData.put("password","123456");
        identityData.put("UIN","1234567890");
        JsonNode mockIdentity = objectMapper.valueToTree(identityData);
        boolean matched=idrepoProfileRegistryPlugin.isMatch(mockIdentity, mockIdentity);
        Assert.assertTrue(matched);
    }

    @Test
    public void isMatch_withInValidDetails_thenFail(){

        ReflectionTestUtils.setField(idrepoProfileRegistryPlugin, "objectMapper",objectMapper);
        Map<String, Object> identityData = new LinkedHashMap<>();
        identityData.put("email","123@email.com");
        identityData.put("UIN","1234567890");
        identityData.put("channel",List.of("email"));
        JsonNode mockIdentity = objectMapper.valueToTree(identityData);


        Map<String, Object> inputChallengeMap = new LinkedHashMap<>();
        inputChallengeMap.put("channel",List.of("email"));
        inputChallengeMap.put("email","123@email.com");
        inputChallengeMap.put("password","1234456");
        inputChallengeMap.put("UIN","1234567890");
        JsonNode inputChallenge = objectMapper.valueToTree(inputChallengeMap);

        boolean matched=idrepoProfileRegistryPlugin.isMatch(mockIdentity, inputChallenge);
        Assert.assertFalse(matched);
    }

    private JsonNode createIdentity() {
        Map<String, Object> identityData = new HashMap<>();
        identityData.put("email","123@email.com");
        identityData.put("phone","+91841987567");
        return objectMapper.valueToTree(identityData);
    }

    @Test
    public void generateAllowedValues_withValidDetails_thenPass() {
        ObjectNode dynamicItem = objectMapper.createObjectNode();
        dynamicItem.put("isActive", true);
        dynamicItem.put("name", "fieldName");
        dynamicItem.put("langCode", "eng");

        ArrayNode fieldValArray = objectMapper.createArrayNode();
        ObjectNode fieldVal = objectMapper.createObjectNode();
        fieldVal.put("code", "code1");
        fieldVal.put("value", "value1");
        fieldValArray.add(fieldVal);
        dynamicItem.set("fieldVal", fieldValArray);
        ArrayNode dynamicDataArray = objectMapper.createArrayNode();
        dynamicDataArray.add(dynamicItem);

        ObjectNode dynamicResponseNode = objectMapper.createObjectNode();
        dynamicResponseNode.put("totalPages", 1);
        dynamicResponseNode.put("totalItems", 1);
        dynamicResponseNode.set("data", dynamicDataArray);
        ObjectNode dynamicWrapper = objectMapper.createObjectNode();
        dynamicWrapper.set("response", dynamicResponseNode);

        ResponseEntity<JsonNode> dynamicEntity = new ResponseEntity<>(dynamicWrapper, HttpStatus.OK);
        Mockito.when(restTemplate.getForEntity(Mockito.contains("dynamic"), Mockito.eq(JsonNode.class))).thenReturn(dynamicEntity);

        ObjectNode categoryItem = objectMapper.createObjectNode();
        categoryItem.put("isActive", true);
        categoryItem.put("code", "cat1");
        categoryItem.put("langCode", "eng");

        ArrayNode docTypesArray = objectMapper.createArrayNode();
        ObjectNode docType = objectMapper.createObjectNode();
        docType.put("code", "doc1");
        docType.put("name", "Document Name");
        docTypesArray.add(docType);

        categoryItem.set("documentTypes", docTypesArray);

        ArrayNode docCategoriesArray = objectMapper.createArrayNode();
        docCategoriesArray.add(categoryItem);
        ObjectNode docResponseNode = objectMapper.createObjectNode();
        docResponseNode.set("documentCategories", docCategoriesArray);
        ObjectNode docWrapper = objectMapper.createObjectNode();
        docWrapper.set("response", docResponseNode);

        ResponseEntity<JsonNode> docEntity = new ResponseEntity<>(docWrapper, HttpStatus.OK);
        Mockito.when(restTemplate.getForEntity(Mockito.contains("docTypes"), Mockito.eq(JsonNode.class))).thenReturn(docEntity);

        JsonNode result = idrepoProfileRegistryPlugin.generateAllowedValues();

        Assert.assertNotNull(result);
        Assert.assertTrue(result.has("fieldName"));
        Assert.assertTrue(result.get("fieldName").has("code1"));
        Assert.assertEquals("value1", result.get("fieldName").get("code1").get("eng").asText());

        Assert.assertTrue(result.has("cat1"));
        Assert.assertTrue(result.get("cat1").has("doc1"));
        Assert.assertEquals("Document Name", result.get("cat1").get("doc1").get("eng").asText());
    }

    @Test
    public void generateAllowedValues_withInactiveDynamicField_thenFail() {
        ObjectNode dynamicItem = objectMapper.createObjectNode();
        dynamicItem.put("isActive", false);

        ArrayNode dynamicDataArray = objectMapper.createArrayNode();
        dynamicDataArray.add(dynamicItem);
        ObjectNode dynamicResponseNode = objectMapper.createObjectNode();
        dynamicResponseNode.put("totalPages", 1);
        dynamicResponseNode.put("totalItems", 1);
        dynamicResponseNode.set("data", dynamicDataArray);
        ObjectNode dynamicWrapper = objectMapper.createObjectNode();
        dynamicWrapper.set("response", dynamicResponseNode);

        ResponseEntity<JsonNode> dynamicEntity = new ResponseEntity<>(dynamicWrapper, HttpStatus.OK);
        Mockito.when(restTemplate.getForEntity(Mockito.contains("dynamic"), Mockito.eq(JsonNode.class))).thenReturn(dynamicEntity);
        ObjectNode docWrapper = objectMapper.createObjectNode();
        docWrapper.set("response", objectMapper.createObjectNode().set("documentCategories", objectMapper.createArrayNode()));
        ResponseEntity<JsonNode> docEntity = new ResponseEntity<>(docWrapper, HttpStatus.OK);
        Mockito.when(restTemplate.getForEntity(Mockito.contains("docTypes"), Mockito.eq(JsonNode.class))).thenReturn(docEntity);

        JsonNode result = idrepoProfileRegistryPlugin.generateAllowedValues();
        Assert.assertNotNull(result);
        Assert.assertEquals(0, result.size());
    }

    @Test
    public void generateAllowedValues_withInactiveDocumentTypesAndCategories_thenFail() {
        ObjectNode dynamicResponseNode = objectMapper.createObjectNode();
        dynamicResponseNode.put("totalPages", 1);
        dynamicResponseNode.put("totalItems", 0);
        dynamicResponseNode.set("data", objectMapper.createArrayNode());

        ObjectNode dynamicWrapper = objectMapper.createObjectNode();
        dynamicWrapper.set("response", dynamicResponseNode);

        ResponseEntity<JsonNode> dynamicEntity = new ResponseEntity<>(dynamicWrapper, HttpStatus.OK);
        Mockito.when(restTemplate.getForEntity(Mockito.contains("dynamic"), Mockito.eq(JsonNode.class))).thenReturn(dynamicEntity);

        ObjectNode inactiveCategory = objectMapper.createObjectNode();
        inactiveCategory.put("isActive", false); // should be skipped
        inactiveCategory.put("code", "cat1");
        inactiveCategory.put("langCode", "eng");

        ObjectNode activeCategoryWithInactiveDocType = objectMapper.createObjectNode();
        activeCategoryWithInactiveDocType.put("isActive", false);
        activeCategoryWithInactiveDocType.put("code", "cat2");
        activeCategoryWithInactiveDocType.put("langCode", "eng");

        ArrayNode docTypesArray = objectMapper.createArrayNode();
        ObjectNode inactiveDocType = objectMapper.createObjectNode();
        inactiveDocType.put("code", "doc1");
        docTypesArray.add(inactiveDocType);
        activeCategoryWithInactiveDocType.set("documentTypes", docTypesArray);

        ArrayNode docCategoriesArray = objectMapper.createArrayNode();
        docCategoriesArray.add(inactiveCategory);
        docCategoriesArray.add(activeCategoryWithInactiveDocType);

        ObjectNode docResponseNode = objectMapper.createObjectNode();
        docResponseNode.set("documentCategories", docCategoriesArray);

        ObjectNode docWrapper = objectMapper.createObjectNode();
        docWrapper.set("response", docResponseNode);

        ResponseEntity<JsonNode> docEntity = new ResponseEntity<>(docWrapper, HttpStatus.OK);
        Mockito.when(restTemplate.getForEntity(Mockito.contains("docTypes"), Mockito.eq(JsonNode.class))).thenReturn(docEntity);

        JsonNode result = idrepoProfileRegistryPlugin.generateAllowedValues();

        Assert.assertNotNull(result);
        Assert.assertEquals(0, result.size());
    }

}
