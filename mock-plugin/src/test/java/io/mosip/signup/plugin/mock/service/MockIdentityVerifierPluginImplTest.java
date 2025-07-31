package io.mosip.signup.plugin.mock.service;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mosip.signup.api.dto.*;
import io.mosip.signup.api.exception.IdentityVerifierException;
import io.mosip.signup.api.util.VerificationStatus;
import io.mosip.signup.plugin.mock.verifier.MockIdentityVerifierPluginImpl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@RunWith(MockitoJUnitRunner.class)
public class MockIdentityVerifierPluginImplTest {

    @InjectMocks
    private MockIdentityVerifierPluginImpl mockIdentityVerifierPlugin;

    @Mock
    ResourceLoader resourceLoader;


    ObjectMapper objectMapper;

    @Before
    public void before(){
        objectMapper = new ObjectMapper();
        ReflectionTestUtils.setField(mockIdentityVerifierPlugin, "objectMapper",objectMapper);
    }


    @Test
    public void verify_withValidIdentityVerificationDto_thenPass() throws IdentityVerifierException, IOException {

        String transactionId = "transactionId123";
        IdentityVerificationDto identityVerificationDto = new IdentityVerificationDto();
        identityVerificationDto.setStepCode("START");
        List<FrameDetail> frameDetails= new ArrayList<>();
        FrameDetail frameDetail = new FrameDetail();
        frameDetail.setFrame("frame");
        frameDetail.setOrder(0);
        frameDetails.add(frameDetail);
        identityVerificationDto.setFrames(frameDetails);

        String jsonContent = "{\"scenes\":[{\"frameNumber\":0,\"stepCode\":\"START\",\"step\":{\"code\":\"liveness_check\",\"framesPerSecond\":3,\"durationInSeconds\":100,\"startupDelayInSeconds\":2,\"retryOnTimeout\":false,\"retryableErrorCodes\":[]},\"feedback\":null}],\"verificationResult\":{\"status\":\"COMPLETED\",\"verifiedClaims\":{\"fullName\":{\"trust_framework\":\"XYZ TF\",\"verification_process\":\"EKYC\",\"assurance_level\":\"Gold\",\"time\":\"34232432\"}},\"errorCode\":null}}";
        Resource resource = Mockito.mock(Resource.class);
        Mockito.when(resourceLoader.getResource(Mockito.anyString())).thenReturn(resource);
        Mockito.when(resource.getInputStream()).thenReturn(new ByteArrayInputStream(jsonContent.getBytes()));

        KafkaTemplate<String, IdentityVerificationResult> kafkaTemplate = Mockito.mock(KafkaTemplate.class);
        ReflectionTestUtils.setField(mockIdentityVerifierPlugin, "kafkaTemplate", kafkaTemplate);
        ReflectionTestUtils.setField(mockIdentityVerifierPlugin, "resultTopic", "ANALYZE_FRAMES_RESULT");

        mockIdentityVerifierPlugin.verify(transactionId, identityVerificationDto);

        Mockito.verify(resourceLoader).getResource(Mockito.anyString());
        ArgumentCaptor<IdentityVerificationResult> resultCaptor = ArgumentCaptor.forClass(IdentityVerificationResult.class);
        Mockito.verify(kafkaTemplate, Mockito.times(2)).send(
                Mockito.eq("ANALYZE_FRAMES_RESULT"),
                resultCaptor.capture()
        );
    }


    @Test
    public void getVerifiedResult_withValidTransactionId_thenPass() throws IdentityVerifierException, IOException {

        String transactionId = "transactionId123";
        VerificationResult verifiedResult = new VerificationResult();
        Map<String,String> fullNameMap = new HashMap<>();
        fullNameMap.put("trust_framework","XYZ TF");
        fullNameMap.put("verification_process","EKYC");
        fullNameMap.put("assurance_level","Gold");
        fullNameMap.put("time","34232432");

        Map<String,JsonNode> verifiedClaims = new HashMap<>();
        verifiedClaims.put("fullName",objectMapper.convertValue(fullNameMap,JsonNode.class));
        verifiedResult.setVerifiedClaims(verifiedClaims);
        verifiedResult.setStatus(VerificationStatus.COMPLETED);

        String jsonContent = "{\"scenes\":[{\"frameNumber\":0,\"stepCode\":\"START\",\"step\":{\"code\":\"liveness_check\",\"framesPerSecond\":3,\"durationInSeconds\":100,\"startupDelayInSeconds\":2,\"retryOnTimeout\":false,\"retryableErrorCodes\":[]},\"feedback\":null}],\"verificationResult\":{\"status\":\"COMPLETED\",\"verifiedClaims\":{\"fullName\":{\"trust_framework\":\"XYZ TF\",\"verification_process\":\"EKYC\",\"assurance_level\":\"Gold\",\"time\":\"34232432\"}},\"errorCode\":null}}";
        Resource resource = Mockito.mock(Resource.class);

        Mockito.when(resourceLoader.getResource(Mockito.anyString())).thenReturn(resource);
        Mockito.when(resource.getInputStream()).thenReturn(new ByteArrayInputStream(jsonContent.getBytes()));
        VerificationResult actualVerifiedResult = mockIdentityVerifierPlugin.getVerificationResult(transactionId);

        Assert.assertEquals(verifiedResult.getStatus(), actualVerifiedResult.getStatus());
        Assert.assertEquals(verifiedResult.getVerifiedClaims(), actualVerifiedResult.getVerifiedClaims());
    }


    @Test
    public void getVerifiedResult_withInValidTransactionId_thenFail() throws IdentityVerifierException, IOException {

        String transactionId = "transactionId123";
        String jsonContent = "{}";
        Resource resource = Mockito.mock(Resource.class);
        Mockito.when(resourceLoader.getResource(Mockito.anyString())).thenReturn(resource);
        Mockito.when(resource.getInputStream()).thenReturn(new ByteArrayInputStream(jsonContent.getBytes()));
        VerificationResult verificationResult = mockIdentityVerifierPlugin.getVerificationResult(transactionId);
        Assert.assertEquals(verificationResult.getErrorCode(),"mock_verification_failed");
        Assert.assertEquals(verificationResult.getStatus(),VerificationStatus.FAILED);
    }
    @Test
    public void initializeWithValidDetails_thenPass(){

        mockIdentityVerifierPlugin.initialize("individualId",new IdentityVerificationInitDto());
    }
}
