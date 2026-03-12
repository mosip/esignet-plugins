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
import java.util.*;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ResourceLoader;
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

    @Value("${mosip.signup.identifier.name:phone}")
    private String identifierField;

    //Endpoint to add/update identity data
    @Value("${mosip.signup.mock.identity.endpoint}")
    private String identityEndpoint;

    //Endpoint to fetch identity data
    @Value("${mosip.signup.mock.get-identity.endpoint}")
    private String getIdentityEndpoint;

    @Value("${mosip.signup.mock.add-verified-claims.endpoint}")
    private String addVerifiedClaimsEndpoint;

    @Value("${mosip.signup.mock.identity-schema.endpoint}")
    private String identitySchemaEndpoint;

    @Value("${mosip.signup.mock.ui-schema.endpoint}")
    private String uiSchemaEndpoint;

    @Value("${mosip.signup.mock.face.biometric.field-name:encodedPhoto}")
    private String faceBiometricFieldName;

    @Value("${mosip.signup.mock.face.biometric.value.prefix:data:image/jpeg;base64,}")
    private String faceBiometricValuePrefix;

    @Autowired
    @Qualifier("selfTokenRestTemplate")
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ResourceLoader resourceLoader;

    private volatile JsonSchema schema;

    @Override
    public void validate(String action, ProfileDto profileDto) throws InvalidProfileException {

        if(schema == null) {
            synchronized (this) {
                ResponseWrapper<JsonNode> responseWrapper = request(identitySchemaEndpoint, HttpMethod.GET, null,
                        new ParameterizedTypeReference<ResponseWrapper<JsonNode>>() {
                        });
                JsonSchemaFactory jsonSchemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
                schema = jsonSchemaFactory.getSchema(responseWrapper.getResponse());
            }
        }

        if(!ACTIONS.contains(action)) {
            log.error("Invalid action value : {}. Allowed values are CREATE and UPDATE", action);
            throw new InvalidProfileException(ErrorConstants.INVALID_ACTION);
        }

        Set<ValidationMessage> errors = schema.validate(profileDto.getIdentity());

        for(ValidationMessage error : errors) {
            log.error("Validation error for field {} with message {}", error.getInstanceLocation(), error.getMessage());
            String fieldName = error.getInstanceLocation().getNameCount() > 0 ? error.getInstanceLocation().getName(0) :
                    error.getProperty();
            if(action.equals("UPDATE") && error.getCode().equals("1028")) {
                //Ignore required field validation errors for update action as in an update scenario, not all fields are mandatory
                continue;
            }
            throw new InvalidProfileException(fieldName != null ? "invalid_".concat(fieldName.toLowerCase()) :
                    "unknown_field");
        }
    }

    @Override
    public ProfileResult createProfile(String requestId, ProfileDto profileDto) throws ProfileException {
    	if(identifierField != null && !profileDto.getIdentity().hasNonNull(identifierField)
                && !profileDto.getIndividualId().equalsIgnoreCase(profileDto.getIdentity().get(identifierField).asText())) {
            log.error("{} and userName mismatch", identifierField);
            throw new InvalidProfileException(ErrorConstants.IDENTIFIER_MISMATCH);
        }
        ObjectNode inputJson = (ObjectNode) profileDto.getIdentity();
        inputJson.put("individualId", profileDto.getIndividualId());

        if(inputJson.hasNonNull(faceBiometricFieldName) && inputJson.get(faceBiometricFieldName).hasNonNull("value")) {
            ObjectNode facePhotoNode = (ObjectNode) inputJson.get(faceBiometricFieldName);
            inputJson.put(faceBiometricFieldName, faceBiometricValuePrefix+facePhotoNode.get("value").asText());
        }

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

    @Override
    public JsonNode getUISpecification() {
        ResponseWrapper<JsonNode> responseWrapper = request(uiSchemaEndpoint, HttpMethod.GET ,null,
                new ParameterizedTypeReference<ResponseWrapper<JsonNode>>() {});
        return responseWrapper.getResponse();
    }
}
