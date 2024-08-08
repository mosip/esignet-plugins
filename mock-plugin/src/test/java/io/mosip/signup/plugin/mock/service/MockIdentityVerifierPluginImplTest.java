package io.mosip.signup.plugin.mock.service;


import io.mosip.signup.api.dto.*;
import io.mosip.signup.api.exception.IdentityVerifierException;
import io.mosip.signup.api.util.ProcessFeedbackType;
import io.mosip.signup.api.util.VerificationStatus;
import io.mosip.signup.plugin.mock.dto.UseCaseFlow;
import io.mosip.signup.plugin.mock.dto.UseCaseScene;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class MockIdentityVerifierPluginImplTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private KafkaTemplate kafkaTemplate;

    @InjectMocks
    MockIdentityVerifierPluginImpl mockIdentityVerifierPluginImpl;


    @Test
    public void verify_withStartFrameStep_thenPass() throws IdentityVerifierException {

        String transactionId = "txn-123";
        int frameOrder = 0;
        IdentityVerificationDto dto = new IdentityVerificationDto();
        dto.setStepCode("START");
        FrameDetail frameDetail = new FrameDetail();
        frameDetail.setOrder(frameOrder);
        frameDetail.setFrame("frame-1");
        dto.setFrames(List.of(frameDetail));


        UseCaseScene useCaseScene = new UseCaseScene();
        List<UseCaseFlow> useCaseFlowList = new ArrayList<>();
        UseCaseFlow useCaseFlow = new UseCaseFlow();
        useCaseFlow.setFrameNumber(0);
        useCaseFlow.setStepCode("START");

        IDVProcessStepDetail idvProcessStepDetail = new IDVProcessStepDetail();
        idvProcessStepDetail.setCode("liveness_check");
        idvProcessStepDetail.setFramesPerSecond(5);
        idvProcessStepDetail.setStartupDelayInSeconds(2);
        idvProcessStepDetail.setRetryOnTimeout(false);
        idvProcessStepDetail.setRetryableErrorCodes(new ArrayList<>());

        useCaseFlow.setStep(idvProcessStepDetail);
        useCaseFlow.setFeedback(null);

        useCaseFlowList.add(useCaseFlow);

        UseCaseFlow useCaseFlow1 = new UseCaseFlow();
        useCaseFlow1.setFrameNumber(1);
        useCaseFlow1.setStepCode("liveness_check");
        useCaseFlow1.setStep(null);

        IDVProcessFeedback idvProcessFeedback1 = new IDVProcessFeedback();
        idvProcessFeedback1.setType(ProcessFeedbackType.MESSAGE);
        idvProcessFeedback1.setCode("turn_left");
        useCaseFlow1.setFeedback(idvProcessFeedback1);

        useCaseFlowList.add(useCaseFlow1);

        useCaseScene.setFlow(useCaseFlowList);
        useCaseScene.setVerified_claims(List.of("email","gender"));
        useCaseScene.setProcess_name("dummy_process");
        useCaseScene.setTrust_framework("dummy_trust_framework");

        Mockito.when(restTemplate.getForObject(Mockito.anyString(), Mockito.eq(UseCaseScene.class))).thenReturn(useCaseScene);
        Mockito.when(kafkaTemplate.send(Mockito.anyString(), Mockito.any(IdentityVerificationResult.class))).thenReturn(null);

        mockIdentityVerifierPluginImpl.verify(transactionId, dto);

    }


    @Test
    public void verify_withLivenessFrameStep_thenPass() throws IdentityVerifierException {

        String transactionId = "txn-123";
        int frameOrder = 1;
        IdentityVerificationDto dto = new IdentityVerificationDto();
        dto.setStepCode("liveness_check");
        FrameDetail frameDetail = new FrameDetail();
        frameDetail.setOrder(frameOrder);
        frameDetail.setFrame("frame-1");
        dto.setFrames(List.of(frameDetail));


        UseCaseScene useCaseScene = new UseCaseScene();
        List<UseCaseFlow> useCaseFlowList = new ArrayList<>();
        UseCaseFlow useCaseFlow = new UseCaseFlow();
        useCaseFlow.setFrameNumber(0);
        useCaseFlow.setStepCode("START");

        IDVProcessStepDetail idvProcessStepDetail = new IDVProcessStepDetail();
        idvProcessStepDetail.setCode("liveness_check");
        idvProcessStepDetail.setFramesPerSecond(5);
        idvProcessStepDetail.setStartupDelayInSeconds(2);
        idvProcessStepDetail.setRetryOnTimeout(false);
        idvProcessStepDetail.setRetryableErrorCodes(new ArrayList<>());

        useCaseFlow.setStep(idvProcessStepDetail);
        useCaseFlow.setFeedback(null);

        useCaseFlowList.add(useCaseFlow);

        UseCaseFlow useCaseFlow1 = new UseCaseFlow();
        useCaseFlow1.setFrameNumber(1);
        useCaseFlow1.setStepCode("liveness_check");
        useCaseFlow1.setStep(null);

        IDVProcessFeedback idvProcessFeedback1 = new IDVProcessFeedback();
        idvProcessFeedback1.setType(ProcessFeedbackType.MESSAGE);
        idvProcessFeedback1.setCode("turn_left");
        useCaseFlow1.setFeedback(idvProcessFeedback1);

        useCaseFlowList.add(useCaseFlow1);

        useCaseScene.setFlow(useCaseFlowList);
        useCaseScene.setVerified_claims(List.of("email","gender"));
        useCaseScene.setProcess_name("dummy_process");
        useCaseScene.setTrust_framework("dummy_trust_framework");

        Mockito.when(restTemplate.getForObject(Mockito.anyString(), Mockito.eq(UseCaseScene.class))).thenReturn(useCaseScene);
        Mockito.when(kafkaTemplate.send(Mockito.anyString(), Mockito.any(IdentityVerificationResult.class))).thenReturn(null);

        mockIdentityVerifierPluginImpl.verify(transactionId, dto);

    }

    @Test
    public void verify_withOutFrame_thenFail() throws IdentityVerifierException {

        String transactionId = "txn-123";
        int frameOrder = 1;
        IdentityVerificationDto dto = new IdentityVerificationDto();
        dto.setStepCode("liveness_check");
        FrameDetail frameDetail = new FrameDetail();
        frameDetail.setOrder(frameOrder);
        frameDetail.setFrame("frame-1");
        dto.setFrames(List.of(frameDetail));


        UseCaseScene useCaseScene = new UseCaseScene();
        useCaseScene.setVerified_claims(List.of("email","gender"));
        useCaseScene.setProcess_name("dummy_process");
        useCaseScene.setTrust_framework("dummy_trust_framework");

        Mockito.when(restTemplate.getForObject(Mockito.anyString(), Mockito.eq(UseCaseScene.class))).thenReturn(useCaseScene);
        try{
            mockIdentityVerifierPluginImpl.verify(transactionId, dto);
            Assert.fail();
        }catch (IdentityVerifierException e){
            Assert.assertEquals(e.getErrorCode(),"Use case flow is not available");
        }
    }

    @Test
    public void getVerifiedResult_withValidUseCase_thenPass() throws IdentityVerifierException {

        String transactionId = "txn-123";

        UseCaseScene useCaseScene = new UseCaseScene();
        List<UseCaseFlow> useCaseFlowList = new ArrayList<>();
        UseCaseFlow useCaseFlow = new UseCaseFlow();
        useCaseFlow.setFrameNumber(0);
        useCaseFlow.setStepCode("START");

        IDVProcessStepDetail idvProcessStepDetail = new IDVProcessStepDetail();
        idvProcessStepDetail.setCode("liveness_check");
        idvProcessStepDetail.setFramesPerSecond(5);
        idvProcessStepDetail.setStartupDelayInSeconds(2);
        idvProcessStepDetail.setRetryOnTimeout(false);
        idvProcessStepDetail.setRetryableErrorCodes(new ArrayList<>());

        useCaseFlow.setStep(idvProcessStepDetail);
        useCaseFlow.setFeedback(null);

        useCaseFlowList.add(useCaseFlow);

        UseCaseFlow useCaseFlow1 = new UseCaseFlow();
        useCaseFlow1.setFrameNumber(1);
        useCaseFlow1.setStepCode("liveness_check");
        useCaseFlow1.setStep(null);

        IDVProcessFeedback idvProcessFeedback1 = new IDVProcessFeedback();
        idvProcessFeedback1.setType(ProcessFeedbackType.MESSAGE);
        idvProcessFeedback1.setCode("turn_left");
        useCaseFlow1.setFeedback(idvProcessFeedback1);

        useCaseFlowList.add(useCaseFlow1);

        useCaseScene.setFlow(useCaseFlowList);
        useCaseScene.setVerified_claims(List.of("email","gender"));
        useCaseScene.setProcess_name("dummy_process");
        useCaseScene.setTrust_framework("dummy_trust_framework");

        Mockito.when(restTemplate.getForObject(Mockito.anyString(), Mockito.eq(UseCaseScene.class))).thenReturn(useCaseScene);

        VerifiedResult verifiedResult = mockIdentityVerifierPluginImpl.getVerifiedResult(transactionId);

        // Verify results
        Assert.assertNotNull(verifiedResult);
        Assert.assertEquals(VerificationStatus.COMPLETED, verifiedResult.getStatus());
        Assert.assertNotNull(verifiedResult.getVerifiedClaims());
        Assert.assertEquals(2, verifiedResult.getVerifiedClaims().size());
        Assert.assertTrue(verifiedResult.getVerifiedClaims().containsKey("email"));
        Assert.assertTrue(verifiedResult.getVerifiedClaims().containsKey("gender"));

    }

    @Test
    public void getVerifiedResult_withMissingDetails_thenFail() throws IdentityVerifierException {
        String transactionId = "txn-123";
        UseCaseScene useCaseScene = new UseCaseScene();
        Mockito.when(restTemplate.getForObject(Mockito.anyString(), Mockito.eq(UseCaseScene.class))).thenReturn(useCaseScene);

        try{
            mockIdentityVerifierPluginImpl.getVerifiedResult(transactionId);
            Assert.fail();
        }catch (IdentityVerifierException e){
            Assert.assertEquals(e.getMessage(),"Verified details are not available");
        }
    }

}
