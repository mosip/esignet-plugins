/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.signup.plugin.mock.service;

import io.mosip.esignet.core.util.IdentityProviderUtil;
import io.mosip.kernel.core.util.StringUtils;
import io.mosip.signup.api.dto.*;
import io.mosip.signup.api.exception.IdentityVerifierException;
import io.mosip.signup.api.spi.IdentityVerifierPlugin;
import io.mosip.signup.api.util.ProcessType;
import io.mosip.signup.api.util.VerificationStatus;
import io.mosip.signup.plugin.mock.dto.UseCaseFlow;
import io.mosip.signup.plugin.mock.dto.UseCaseScene;
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

    Map<String, String> localStore = new HashMap<>();

    private List<UseCaseFlow> useCase;

    private UseCaseScene useCaseScene;

    @Value("${mosip.signup.identity-verification.mock.usecase}")
    private String useCaseName;

    @Value("${mosip.signup.config-server-url}")
    private String configServerUrl;

    @Autowired
    private RestTemplate restTemplate;


    @Override
    public String getVerifierId() {
        return "mock-identity-verifier";
    }

    @Override
    public List<ProcessType> getSupportedProcessTypes() {
        return List.of(VIDEO);
    }

    @Override
    public void verify(String transactionId, IdentityVerificationDto identityVerificationDto) throws IdentityVerifierException {
        log.info("Started the verification process");

        if(useCaseScene == null) {
            useCaseScene = restTemplate.getForObject(configServerUrl+useCaseName, UseCaseScene.class);
        }
        if(useCaseScene.getFlow()==null){
            throw new IdentityVerifierException("Use case flow is not available");
        }
        useCase=useCaseScene.getFlow();
        IdentityVerificationResult identityVerificationResult = new IdentityVerificationResult();
        identityVerificationResult.setId(transactionId);
        identityVerificationResult.setVerifierId(getVerifierId());

        if(isStartStep(identityVerificationDto.getStepCode())) {
            Optional<UseCaseFlow> result = useCase.stream().filter(scene -> scene.getFrameNumber() == 0 &&
                    scene.getStepCode().equals(identityVerificationDto.getStepCode())).findFirst();
            if(result.isPresent()) {
                identityVerificationResult.setStep(result.get().getStep());
                identityVerificationResult.setFeedback(result.get().getFeedback());
                publishAnalysisResult(identityVerificationResult);
            }
        }

        if(identityVerificationDto.getFrames() != null) {
            for(FrameDetail frameDetail : identityVerificationDto.getFrames()) {
                Optional<UseCaseFlow> result = useCase.stream()
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
    public VerifiedResult getVerifiedResult(String transactionId) throws IdentityVerifierException {
        log.info("TODO - we should save the verification details in mock-identity-system");
        log.info("Retrieving verified result for transactionId");

        if(useCaseScene == null) {
            useCaseScene = restTemplate.getForObject(configServerUrl+useCaseName, UseCaseScene.class);
        }
        if(useCaseScene.getVerified_claims()==null || StringUtils.isEmpty(useCaseScene.getTrust_framework()) || StringUtils.isEmpty(useCaseScene.getProcess_name())){
            throw new IdentityVerifierException("Verified details are not available");
        }
        List<String> verifiedClaims = useCaseScene.getVerified_claims();
        VerifiedResult verifiedResult = new VerifiedResult();
        Map<String, VerificationDetail> verifiedClaimsMap = new HashMap<>();
        for(String claim : verifiedClaims) {
            VerificationDetail verificationDetail = new VerificationDetail();
            verificationDetail.setVerification_process(useCaseScene.getProcess_name());
            verificationDetail.setTrust_framework(useCaseScene.getTrust_framework());
            verificationDetail.setTime(IdentityProviderUtil.getUTCDateTime());
            verifiedClaimsMap.put(claim, verificationDetail);
        }
        verifiedResult.setVerifiedClaims(verifiedClaimsMap);
        verifiedResult.setStatus(VerificationStatus.COMPLETED);
        log.info("Successfully retrieved verified result for transactionId");
        return verifiedResult;
    }

}