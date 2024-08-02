/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.signup.plugin.mock.service;

import static io.mosip.signup.api.util.ErrorConstants.SERVER_UNREACHABLE;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.beanutils.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;

import io.mosip.esignet.core.dto.RequestWrapper;
import io.mosip.esignet.core.dto.ResponseWrapper;
import io.mosip.signup.api.dto.ProfileDto;
import io.mosip.signup.api.dto.ProfileResult;
import io.mosip.signup.api.exception.InvalidProfileException;
import io.mosip.signup.api.exception.ProfileException;
import io.mosip.signup.api.spi.ProfileRegistryPlugin;
import io.mosip.signup.api.util.ProfileCreateUpdateStatus;
import io.mosip.signup.plugin.mock.dto.LanguageValue;
import io.mosip.signup.plugin.mock.dto.MockIdentityRequest;
import io.mosip.signup.plugin.mock.dto.MockIdentityResponse;
import io.mosip.signup.plugin.mock.dto.Password;
import io.mosip.signup.plugin.mock.util.ErrorConstants;
import io.mosip.signup.plugin.mock.util.ProfileCacheService;
import lombok.extern.slf4j.Slf4j;

@ConditionalOnProperty(value = "mosip.signup.integration.profile-registry-plugin", havingValue = "MockProfileRegistryPluginImpl")
@Slf4j
@Component
public class MockProfileRegistryPluginImpl implements ProfileRegistryPlugin {

    private static final String UTC_DATETIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    private static final String PASSWORD = "password";
    private static final List<String> ACTIONS = Arrays.asList("CREATE", "UPDATE");
    

    @Value("#{'${mosip.signup.mandatory-attributes.mock.new-registration:}'.split(',')}")
    private List<String> requiredFields;

    @Value("#{${mosip.signup.mock.register.field-details}}")
    private List<Map<String, String>> fieldDetailList;

    @Value("#{'${mosip.signup.mock.lang-based-attributes:}'.split(',')}")
    private List<String> langBasedFields;
    
    @Value("${mosip.signup.mock.identity.endpoint}")
    private String identityEndpoint;

    @Value("${mosip.signup.mock.get-identity.endpoint}")
    private String getIdentityEndpoint;

    @Value("${mosip.signup.idrepo.generate-hash.endpoint}")
    private String generateHashEndpoint;

    @Value("${mosip.signup.mock.get-status.endpoint}")
    private String getStatusEndpoint;

    @Autowired
    @Qualifier("selfTokenRestTemplate")
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;


    @Override
    public void validate(String action, ProfileDto profileDto) throws InvalidProfileException {
    	if (!ACTIONS.contains(action)) {
            throw new InvalidProfileException(ErrorConstants.INVALID_ACTION);
        }

        JsonNode inputJson = profileDto.getIdentity();
        if (action.equals("CREATE")) {
            for (String fieldName:requiredFields) {
                if (inputJson.get(fieldName) == null) {
                    log.error("Null value found in the required field of {}, required: {}", fieldName, requiredFields);
                    throw new InvalidProfileException(fieldName.toLowerCase().concat("_required")); //TODO we should add exception message
                }
            }
        }
    }

    @Override
    public ProfileResult createProfile(String requestId, ProfileDto profileDto) throws ProfileException {
    	JsonNode inputJson = profileDto.getIdentity();

        MockIdentityRequest identityRequest = buildIdentityRequest(inputJson);
        identityRequest.setIndividualId(getUniqueIdentifier().toString());
        MockIdentityResponse identityResponse = addIdentity(identityRequest);
        ProfileResult profileResult = new ProfileResult();
        profileResult.setStatus(identityResponse.getStatus());
        return profileResult;
    }

    @Override
    public ProfileResult updateProfile(String requestId, ProfileDto profileDto) throws ProfileException {
        JsonNode inputJson = profileDto.getIdentity();
        //Build identity request
        MockIdentityRequest identityRequest = buildIdentityRequest(inputJson);
        MockIdentityResponse identityResponse = updateIdentity(identityRequest);
        ProfileResult profileResult = new ProfileResult();
        profileResult.setStatus(identityResponse.getStatus());
        return profileResult;
    }

    @Override
    public ProfileCreateUpdateStatus getProfileCreateUpdateStatus(String requestId) throws ProfileException {
    	ResponseWrapper<MockIdentityRequest> responseWrapper = request(getStatusEndpoint+requestId,
                HttpMethod.GET, null, new ParameterizedTypeReference<ResponseWrapper<MockIdentityRequest>>() {});
        if (responseWrapper != null && responseWrapper.getResponse() != null ) {
               return ProfileCreateUpdateStatus.COMPLETED;
        }
        log.error("Get registration status failed with response {}", requestId, responseWrapper);
        return ProfileCreateUpdateStatus.PENDING;
    }

    @Override
    public ProfileDto getProfile(String individualId) throws ProfileException {
    	try {
            ResponseWrapper<MockIdentityRequest> responseWrapper = request(getStatusEndpoint+individualId, HttpMethod.GET, null,
                    new ParameterizedTypeReference<ResponseWrapper<MockIdentityRequest>>() {});
            ProfileDto profileDto = new ProfileDto();
            profileDto.setIndividualId(responseWrapper.getResponse().getIndividualId());
            profileDto.setIdentity(objectMapper.convertValue(responseWrapper.getResponse(), JsonNode.class));
            profileDto.setActive(true);
            return profileDto;
        } catch (ProfileException e) {
        	e.printStackTrace();
            throw e;
        }
    }

    @Override
    public boolean isMatch(JsonNode identity, JsonNode inputChallenge) {
    	int matchCount = 0;
        Iterator itr = inputChallenge.fieldNames();
        while(itr.hasNext()) {
            String fieldName = (String) itr.next();
            if(!identity.has(fieldName))
                break;

            if(identity.get(fieldName).isArray()) {
                for (JsonNode jsonNode : identity.get(fieldName)) {
                    //As of now assumption is we take user input only in single language
                    matchCount = matchCount + ((jsonNode.equals(inputChallenge.get(fieldName).get(0))) ? 1 : 0);
                }
            }
            else {
                matchCount = matchCount + ((identity.get(fieldName).equals(inputChallenge.get(fieldName))) ? 1 : 0);
            }
        }
        return !inputChallenge.isEmpty() && matchCount >= inputChallenge.size();
    }
    
    private <T> ResponseWrapper<T> request(String url, HttpMethod method, Object request,
            ParameterizedTypeReference<ResponseWrapper<T>> responseType) {
		try {
				HttpEntity<?> httpEntity = null;
				if(request != null) {
					httpEntity = new HttpEntity<>(request);
				}
				ResponseWrapper<T> responseWrapper = restTemplate.exchange(url, method, httpEntity, responseType).getBody();
				if (responseWrapper != null && responseWrapper.getResponse() != null) {
					return responseWrapper;
				}
				log.error("{} endpoint returned error response {} ", url, responseWrapper);
				throw new ProfileException(responseWrapper != null && !CollectionUtils.isEmpty(responseWrapper.getErrors()) ?
				responseWrapper.getErrors().get(0).getErrorCode() : ErrorConstants.REQUEST_FAILED);
			} catch (RestClientException e) {
				log.error("{} endpoint is unreachable.", url, e);
				throw new ProfileException(SERVER_UNREACHABLE);
		}
	}
    
    private MockIdentityRequest buildIdentityRequest(JsonNode inputJson){
    	MockIdentityRequest mockIdentityRequest = new MockIdentityRequest();
    	Iterator<Map.Entry<String, JsonNode>> itr = inputJson.fields();
    	List<String> fieldNames = new ArrayList<>();
    	Field[] fields = mockIdentityRequest.getClass().getDeclaredFields();
    	for(Field field:fields) {
    		fieldNames.add(field.getName());
    	}
    	while(itr.hasNext()) {
    		String field = ((TextNode)itr.next()).textValue();
    		if(fieldNames.contains(field)) {
    			try {
	    			if(field.equalsIgnoreCase(PASSWORD)) {
	    				Password password = generateSaltedHash(inputJson.get("password").asText());
						BeanUtils.setProperty(mockIdentityRequest, field, password);
	
	    			}
	    			else if(langBasedFields.contains(field)) {
	    				List<LanguageValue> fieldValue = objectMapper.readValue(inputJson.get(field).toString(), 
									new TypeReference<List<LanguageValue>>() {});
	    				BeanUtils.setProperty(mockIdentityRequest, field, fieldValue);
	    			}
	    			else {
	    				BeanUtils.setProperty(mockIdentityRequest, field, inputJson.get(field).toString());
	    			}
	    		}catch(IllegalAccessException|InvocationTargetException|JsonProcessingException ex) {
	    			ex.printStackTrace();
	    		}
    		}
    	}
    	
    	return mockIdentityRequest;
    }
    
    private UUID getUniqueIdentifier() throws ProfileException {
    	return UUID.randomUUID();

    }
    
    private MockIdentityResponse addIdentity(MockIdentityRequest identityRequest) throws ProfileException{
        RequestWrapper<MockIdentityRequest> restRequest = new RequestWrapper<>();
        restRequest.setRequestTime(getUTCDateTime());
        restRequest.setRequest(identityRequest);
        ResponseWrapper<MockIdentityResponse> responseWrapper = request(identityEndpoint, HttpMethod.POST, restRequest,
                new ParameterizedTypeReference<ResponseWrapper<MockIdentityResponse>>() {});
        return responseWrapper.getResponse();
    }
    
    private MockIdentityResponse updateIdentity(MockIdentityRequest identityRequest) throws ProfileException{
        RequestWrapper<MockIdentityRequest> restRequest = new RequestWrapper<>();
        restRequest.setRequestTime(getUTCDateTime());
        restRequest.setRequest(identityRequest);
        ResponseWrapper<MockIdentityResponse> responseWrapper = request(identityEndpoint, HttpMethod.PUT, restRequest,
                new ParameterizedTypeReference<ResponseWrapper<MockIdentityResponse>>() {});
        return responseWrapper.getResponse();
    }
    
    private String getUTCDateTime() {
        return ZonedDateTime
                .now(ZoneOffset.UTC)
                .format(DateTimeFormatter.ofPattern(UTC_DATETIME_PATTERN));
    }
    
    private Password generateSaltedHash(String password) throws ProfileException {
        RequestWrapper<Password.PasswordPlaintext> requestWrapper = new RequestWrapper<>();
        requestWrapper.setRequestTime(getUTCDateTime());
        requestWrapper.setRequest(new Password.PasswordPlaintext(password));
        ResponseWrapper<Password.PasswordHash> responseWrapper = request(generateHashEndpoint, HttpMethod.POST, requestWrapper,
                new ParameterizedTypeReference<ResponseWrapper<Password.PasswordHash>>() {});
        if (!StringUtils.isEmpty(responseWrapper.getResponse().getHashValue()) &&
                !StringUtils.isEmpty(responseWrapper.getResponse().getSalt())) {
            return new Password(responseWrapper.getResponse().getHashValue(),
                    responseWrapper.getResponse().getSalt());
        }
        log.error("Failed to generate salted hash {}", responseWrapper.getResponse());
        throw new ProfileException(ErrorConstants.REQUEST_FAILED);
    }
}
