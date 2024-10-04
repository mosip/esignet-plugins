/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.signup.plugin.mock.service;

import static io.mosip.signup.api.util.ErrorConstants.SERVER_UNREACHABLE;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.esignet.core.dto.RequestWrapper;
import io.mosip.esignet.core.dto.ResponseWrapper;
import io.mosip.signup.api.dto.ProfileDto;
import io.mosip.signup.api.dto.ProfileResult;
import io.mosip.signup.api.exception.InvalidProfileException;
import io.mosip.signup.api.exception.ProfileException;
import io.mosip.signup.api.spi.ProfileRegistryPlugin;
import io.mosip.signup.api.util.ProfileCreateUpdateStatus;
import io.mosip.signup.plugin.mock.dto.MockIdentityResponse;
import io.mosip.signup.plugin.mock.util.ErrorConstants;
import lombok.extern.slf4j.Slf4j;

@ConditionalOnProperty(value = "mosip.signup.integration.profile-registry-plugin", havingValue = "MockProfileRegistryPluginImpl")
@Slf4j
@Component
public class MockProfileRegistryPluginImpl implements ProfileRegistryPlugin {

    private static final String UTC_DATETIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    private static final List<String> ACTIONS = Arrays.asList("CREATE", "UPDATE");
    
    @Value("${mosip.signup.mock.username.field:phone}")
    private String usernameField;

    @Value("#{'${mosip.signup.mock.mandatory-attributes.CREATE:}'.split(',')}")
    private List<String> requiredFieldsOnCreate;

    @Value("#{'${mosip.signup.mock.mandatory-attributes.UPDATE:}'.split(',')}")
    private List<String> requiredFieldsOnUpdate;

    @Value("#{'${mosip.signup.mock.lang-based-attributes:}'.split(',')}")
    private List<String> langBasedFields;

    @Value("${mosip.signup.mock.identity.endpoint}")
    private String identityEndpoint;

    @Value("${mosip.signup.mock.get-identity.endpoint}")
    private String getIdentityEndpoint;

    @Value("${mosip.signup.mock.add-verified-claims.endpoint}")
    private String addVerifiedClaimsEndpoint;

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
        List<String> requiredFields = action.equals("CREATE") ? requiredFieldsOnCreate : requiredFieldsOnUpdate;
        for (String fieldName : requiredFields) {
            if (!fieldName.isEmpty() && (!inputJson.hasNonNull(fieldName) || (inputJson.get(fieldName).isArray() && inputJson.get(fieldName).isEmpty()))) {
                log.error("Null value found in the required field of {}, required: {}", fieldName, requiredFields);
                throw new InvalidProfileException("invalid_".concat(fieldName.toLowerCase()));
            }
        }
    }

    @Override
    public ProfileResult createProfile(String requestId, ProfileDto profileDto) throws ProfileException {
    	if(usernameField != null && !profileDto.getIndividualId().equalsIgnoreCase(profileDto.getIdentity().get(usernameField).asText())) {
            log.error("{} and userName mismatch", usernameField);
            throw new InvalidProfileException(ErrorConstants.IDENTIFIER_MISMATCH);
        }
    	JsonNode inputJson = profileDto.getIdentity();
        ((ObjectNode)inputJson).put("individualId", profileDto.getIndividualId());

        MockIdentityResponse identityResponse = addIdentity(inputJson);
        ProfileResult profileResult = new ProfileResult();
        profileResult.setStatus(identityResponse.getStatus());
        return profileResult;
    }

    @Override
    public ProfileResult updateProfile(String requestId, ProfileDto profileDto) throws ProfileException {
        JsonNode inputJson = profileDto.getIdentity();

        MockIdentityResponse identityResponse = null;
        if(profileDto.getIdentity().hasNonNull("verified_claims")) {
            identityResponse = addVerifiedClaims(profileDto.getIndividualId(), inputJson) ;
        }
        else {
            ((ObjectNode)inputJson).put("individualId", profileDto.getIndividualId());
            identityResponse = updateIdentity(inputJson);
        }

        ProfileResult profileResult = new ProfileResult();
        profileResult.setStatus(identityResponse.getStatus());
        return profileResult;
    }

    @Override
    public ProfileCreateUpdateStatus getProfileCreateUpdateStatus(String requestId) throws ProfileException {
        return ProfileCreateUpdateStatus.COMPLETED;
    }

    @Override
    public ProfileDto getProfile(String individualId) throws ProfileException {
    	try {
            ResponseWrapper<JsonNode> responseWrapper = request(getIdentityEndpoint+individualId, HttpMethod.GET, null,
                    new ParameterizedTypeReference<ResponseWrapper<JsonNode>>() {});
            ProfileDto profileDto = new ProfileDto();
            profileDto.setIndividualId(responseWrapper.getResponse().get("individualId").asText());
            profileDto.setIdentity(responseWrapper.getResponse());
            profileDto.setActive(true);
            return profileDto;
        } catch (ProfileException e) {
            if (e.getErrorCode().equals("invalid_individual_id")) {
                ProfileDto profileDto = new ProfileDto();
                profileDto.setIndividualId(individualId);
                profileDto.setActive(false);
                return profileDto;
            }
            throw e;
        }
    }

    @Override
    public boolean isMatch(JsonNode identity, JsonNode inputChallenge) {
    	int matchCount = 0;
        Iterator<String> itr = inputChallenge.deepCopy().fieldNames();
        while(itr.hasNext()) {
            String fieldName = itr.next();
            if(!identity.hasNonNull(fieldName))
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
				if (responseWrapper != null && responseWrapper.getResponse() != null && CollectionUtils.isEmpty(responseWrapper.getErrors())) {
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

    private MockIdentityResponse addIdentity(JsonNode identityRequest) throws ProfileException{
        RequestWrapper<JsonNode> restRequest = new RequestWrapper<>();
        restRequest.setRequestTime(getUTCDateTime());
        restRequest.setRequest(identityRequest);
        ResponseWrapper<MockIdentityResponse> responseWrapper = request(identityEndpoint, HttpMethod.POST, restRequest,
                new ParameterizedTypeReference<ResponseWrapper<MockIdentityResponse>>() {});
        return responseWrapper.getResponse();
    }
    
    private MockIdentityResponse updateIdentity(JsonNode identityRequest) throws ProfileException{
        RequestWrapper<JsonNode> restRequest = new RequestWrapper<>();
        restRequest.setRequestTime(getUTCDateTime());
        restRequest.setRequest(identityRequest);
        ResponseWrapper<MockIdentityResponse> responseWrapper = request(identityEndpoint, HttpMethod.PUT, restRequest,
                new ParameterizedTypeReference<ResponseWrapper<MockIdentityResponse>>() {});
        return responseWrapper.getResponse();
    }

    private MockIdentityResponse addVerifiedClaims(String individualId, JsonNode identityRequest) throws ProfileException {
        ObjectNode objectNode = objectMapper.createObjectNode();
        objectNode.put("individualId", individualId);
        objectNode.put("verificationDetail", identityRequest.get("verified_claims"));
        objectNode.put("active", true);

        RequestWrapper<JsonNode> restRequest = new RequestWrapper<>();
        restRequest.setRequestTime(getUTCDateTime());
        restRequest.setRequest(objectNode);
        ResponseWrapper<MockIdentityResponse> responseWrapper = request(addVerifiedClaimsEndpoint, HttpMethod.POST, restRequest,
                new ParameterizedTypeReference<ResponseWrapper<MockIdentityResponse>>() {});
        return responseWrapper.getResponse();
    }
    
    private String getUTCDateTime() {
        return ZonedDateTime
                .now(ZoneOffset.UTC)
                .format(DateTimeFormatter.ofPattern(UTC_DATETIME_PATTERN));
    }
    
}
