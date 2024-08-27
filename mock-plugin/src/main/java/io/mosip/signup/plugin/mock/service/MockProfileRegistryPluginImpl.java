/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.signup.plugin.mock.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mosip.esignet.core.util.IdentityProviderUtil;
import io.mosip.kernel.core.http.ResponseWrapper;
import io.mosip.signup.api.dto.ProfileDto;
import io.mosip.signup.api.dto.ProfileResult;
import io.mosip.signup.api.dto.VerificationDetail;
import io.mosip.signup.api.exception.InvalidProfileException;
import io.mosip.signup.api.exception.ProfileException;
import io.mosip.signup.api.spi.ProfileRegistryPlugin;
import io.mosip.signup.api.util.ErrorConstants;
import io.mosip.signup.api.util.ProfileCreateUpdateStatus;
import io.mosip.signup.plugin.mock.dto.RequestWrapper;
import io.mosip.signup.plugin.mock.dto.VerifiedClaimRequestDto;
import io.mosip.signup.plugin.mock.dto.VerifiedClaimStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@ConditionalOnProperty(value = "mosip.signup.integration.profile-registry-plugin", havingValue = "MockProfileRegistryPluginImpl")
@Slf4j
@Component
public class MockProfileRegistryPluginImpl implements ProfileRegistryPlugin {

    private static final String VERIFIED_CLAIMS_FIELD_ID = "verified_claims";

    @Value("${mosip.esignet.mock.identity.add-verified-claim-url}")
    private String verifiedClaimUrl;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    private RestTemplate restTemplate;

    @Override
    public void validate(String action, ProfileDto profileDto) throws InvalidProfileException {

    }

    @Override
    public ProfileResult createProfile(String requestId, ProfileDto profileDto) throws ProfileException {
        return null;
    }

    @Override
    public ProfileResult updateProfile(String requestId, ProfileDto profileDto) throws ProfileException {

        Map<String, VerificationDetail> verificationDetails=new HashMap<>();
        JsonNode identity = profileDto.getIdentity();
        if(identity.hasNonNull(VERIFIED_CLAIMS_FIELD_ID)) {
            try {
                verificationDetails = objectMapper.convertValue(
                        identity.get(VERIFIED_CLAIMS_FIELD_ID),objectMapper.getTypeFactory().constructParametricType(Map.class, String.class, VerificationDetail.class));

                List<VerifiedClaimRequestDto> verifiedClaimRequestDtoList = new ArrayList<>();
                for(String claim : verificationDetails.keySet()) {
                    VerificationDetail verificationDetail=verificationDetails.get(claim);
                    VerifiedClaimRequestDto verifiedClaimRequestDto = new VerifiedClaimRequestDto();
                    verifiedClaimRequestDto.setClaim(claim);
                    verifiedClaimRequestDto.setIndividualId(profileDto.getIndividualId());
                    verifiedClaimRequestDto.setTrustFramework(verificationDetail.getTrust_framework());

                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                    ZonedDateTime zonedDateTime = ZonedDateTime.parse(verificationDetail.getTime(), formatter.withZone(ZoneOffset.UTC));
                    verifiedClaimRequestDto.setVerifiedDateTime(zonedDateTime.toLocalDateTime());
                    verifiedClaimRequestDtoList.add(verifiedClaimRequestDto);
                }

                RequestWrapper<List<VerifiedClaimRequestDto>> request=new RequestWrapper(verifiedClaimRequestDtoList);
                request.setRequestTime(IdentityProviderUtil.getUTCDateTime());

                //set signature header, body and invoke add verified claim endpoint
                String requestBody = objectMapper.writeValueAsString(request);
                RequestEntity<String> requestEntity = RequestEntity
                        .post(UriComponentsBuilder.fromUriString(verifiedClaimUrl).build().toUri())
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .body(requestBody);
                ResponseEntity<ResponseWrapper<VerifiedClaimStatus>> responseEntity = restTemplate.exchange(requestEntity,
                        new ParameterizedTypeReference<>() {
                        });
                if (responseEntity.getStatusCode().is2xxSuccessful() && responseEntity.getBody() != null) {
                    if(responseEntity.getBody().getResponse()!=null){
                       log.info("Verified claim added successfully");
                       ProfileResult profileResult = new ProfileResult();
                       profileResult.setStatus("SUCCESS");
                       return profileResult;
                    }
                    log.error("Errors in response received from IDA addVerifiedClaim: {}", responseEntity.getBody().getErrors());
                    throw new ProfileException(CollectionUtils.isEmpty(responseEntity.getBody().getErrors()) ?
                            io.mosip.esignet.api.util.ErrorConstants.DATA_EXCHANGE_FAILED : responseEntity.getBody().getErrors().get(0).getErrorCode());
                }
                log.error("Errors in response received from IDA addVerifiedClaim");
                throw new ProfileException(ErrorConstants.UNKNOWN_ERROR);
            } catch (ProfileException e){
                throw e;
            } catch (Exception e) {
                log.error("Error while updating verified claim", e);
                throw new ProfileException(ErrorConstants.UNKNOWN_ERROR);
            }
        }
        ProfileResult profileResult = new ProfileResult();
        profileResult.setStatus("FAILED");
        return profileResult;
    }

    @Override
    public ProfileCreateUpdateStatus getProfileCreateUpdateStatus(String requestId) throws ProfileException {
        return null;
    }

    @Override
    public ProfileDto getProfile(String individualId) throws ProfileException {
        return null;
    }

    @Override
    public boolean isMatch(JsonNode identity, JsonNode inputChallenge) {
        return false;
    }
}
