/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.signup.plugin.mock.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mosip.signup.api.dto.*;
import io.mosip.signup.api.exception.IdentityVerifierException;
import io.mosip.signup.api.spi.IdentityVerifierPlugin;
import io.mosip.signup.api.util.ProcessType;
import io.mosip.signup.api.util.VerificationStatus;
import io.mosip.signup.plugin.mock.dto.MockScene;
import io.mosip.signup.plugin.mock.dto.MockUserStory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

import static io.mosip.signup.api.util.ProcessType.VIDEO;

@Slf4j
@Component
public class MockIdentityVerifierPluginImpl extends IdentityVerifierPlugin {

    @Value("${mosip.signup.mock.identity-verification.story-name}")
    private String storyName;

    @Value("${mosip.signup.mock.config-server-url}")
    private String configServerUrl;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public String getVerifierId() {
        return "mock-identity-verifier";
    }

    @Override
    public List<ProcessType> getSupportedProcessTypes() {
        return List.of(VIDEO);
    }

    @Override
    public void initialize(String transactionId, IdentityVerificationInitDto identityVerificationInitDto) {
        log.info("Transaction is initialized with individualId : {} and disabilityType: {}",
                identityVerificationInitDto.getIndividualId(), identityVerificationInitDto.getDisabilityType());
        log.info("**** Nothing to initialize as its mock identity verification plugin ****");
    }

    @Override
    public void verify(String transactionId, IdentityVerificationDto identityVerificationDto) throws IdentityVerifierException {
        MockUserStory mockUserStory = restTemplate.getForObject(configServerUrl+storyName, MockUserStory.class);

        IdentityVerificationResult identityVerificationResult = new IdentityVerificationResult();
        identityVerificationResult.setId(transactionId);
        identityVerificationResult.setVerifierId(getVerifierId());

        if(isStartStep(identityVerificationDto.getStepCode())) {
            Optional<MockScene> result = Objects.requireNonNull(mockUserStory).getScenes().stream()
                    .filter(scene -> scene.getFrameNumber() == 0 && scene.getStepCode().equals(identityVerificationDto.getStepCode()))
                    .findFirst();

            if(result.isPresent()) {
                identityVerificationResult.setStep(result.get().getStep());
                identityVerificationResult.setFeedback(result.get().getFeedback());
                publishAnalysisResult(identityVerificationResult);
            }
        }

        if(identityVerificationDto.getFrames() != null) {
            for(FrameDetail frameDetail : identityVerificationDto.getFrames()) {
                Optional<MockScene> result = Objects.requireNonNull(mockUserStory).getScenes().stream()
                        .filter(scene -> scene.getFrameNumber() == frameDetail.getOrder() &&
                                scene.getStepCode().equals(identityVerificationDto.getStepCode()))
                        .findFirst();
                if(result.isPresent()) {
                    identityVerificationResult.setStep(result.get().getStep());
                    identityVerificationResult.setFeedback(result.get().getFeedback());
                    publishAnalysisResult(identityVerificationResult);
                }
            }
        }
    }

    @Override
    public VerificationResult getVerificationResult(String transactionId) throws IdentityVerifierException {
        MockUserStory mockUserStory = restTemplate.getForObject(configServerUrl+storyName, MockUserStory.class);

        if(mockUserStory != null && mockUserStory.getVerificationResult() != null) {
            try {
                return objectMapper.treeToValue(mockUserStory.getVerificationResult(), VerificationResult.class);
            } catch (JsonProcessingException e) {
               log.error("Failed to parse verified attributes in the mock user story: {}", storyName, e);
            }
        }
        VerificationResult verificationResult = new VerificationResult();
        verificationResult.setStatus(VerificationStatus.FAILED);
        verificationResult.setErrorCode("mock_verification_failed");
        return verificationResult;
    }

}