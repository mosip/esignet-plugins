package io.mosip.signup.plugin.mock.service;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mosip.signup.api.dto.FrameDetail;
import io.mosip.signup.api.dto.IdentityVerificationDto;
import io.mosip.signup.api.dto.VerificationDetail;
import io.mosip.signup.api.dto.VerifiedResult;
import io.mosip.signup.api.exception.IdentityVerifierException;
import io.mosip.signup.api.util.VerificationStatus;
import io.mosip.signup.plugin.mock.dto.MockScene;
import io.mosip.signup.plugin.mock.dto.MockUserStory;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class MockIdentityVerifierPluginImplTest {

    @InjectMocks
    private MockIdentityVerifierPluginImpl mockIdentityVerifierPlugin;

    @Mock
    private RestTemplate restTemplate;


    @Mock
    private KafkaTemplate kafkaTemplate;


    @Test
    public void verify_withValidIdentityVerificationDto_thenPass() throws IdentityVerifierException {

        String transactionId = "transactionId123";
        IdentityVerificationDto identityVerificationDto = new IdentityVerificationDto();
        identityVerificationDto.setStepCode("START");
        List<FrameDetail> frameDetails= new ArrayList<>();
        FrameDetail frameDetail = new FrameDetail();
        frameDetail.setFrame("frame");
        frameDetail.setOrder(0);
        frameDetails.add(frameDetail);
        identityVerificationDto.setFrames(frameDetails);
        MockUserStory mockUserStory = new MockUserStory();
        List<MockScene> mockScenes = new ArrayList<>();
        MockScene mockScene = new MockScene();
        mockScene.setFrameNumber(0);
        mockScene.setStepCode("START");
        mockScenes.add(mockScene);
        mockUserStory.setScenes(mockScenes);
        Mockito.when(restTemplate.getForObject(Mockito.anyString(), Mockito.eq(MockUserStory.class))).thenReturn(mockUserStory);
        mockIdentityVerifierPlugin.verify(transactionId, identityVerificationDto);

        Mockito.verify(restTemplate, Mockito.times(1)).getForObject(Mockito.anyString(), Mockito.eq(MockUserStory.class));
    }


    @Test
    public void getVerifiedResult_withValidTransactionId_thenPass() throws IdentityVerifierException, JsonProcessingException {

        ObjectMapper objectMapper1 = new ObjectMapper();
        ReflectionTestUtils.setField(mockIdentityVerifierPlugin, "objectMapper",objectMapper1);
        String transactionId = "transactionId123";
        MockUserStory mockUserStory = new MockUserStory();
        VerifiedResult verifiedResult = new VerifiedResult();
        Map<String, VerificationDetail> verificationDetailMap = new HashMap<>();
        VerificationDetail verificationDetail = new VerificationDetail();
        verificationDetail.setTrust_framework("trust_framework");
        verificationDetail.setVerification_process("verification_process");
        verificationDetailMap.put("claim", verificationDetail);
        verifiedResult.setVerifiedClaims(verificationDetailMap);
        verifiedResult.setStatus(VerificationStatus.COMPLETED);

        JsonNode jsonNode =objectMapper1.readTree(objectMapper1.writeValueAsString(verifiedResult));

        mockUserStory.setVerifiedResult(jsonNode);
        List<MockScene> mockScenes = new ArrayList<>();
        MockScene mockScene = new MockScene();
        mockScene.setFrameNumber(0);
        mockScene.setStepCode("START");
        mockScenes.add(mockScene);

        mockUserStory.setScenes(mockScenes);

        Mockito.when(restTemplate.getForObject(Mockito.anyString(), Mockito.eq(MockUserStory.class))).thenReturn(mockUserStory);
        VerifiedResult actualVerifiedResult = mockIdentityVerifierPlugin.getVerifiedResult(transactionId);

        Assert.assertEquals(verifiedResult.getStatus(), actualVerifiedResult.getStatus());
        Assert.assertEquals(verifiedResult.getVerifiedClaims(), actualVerifiedResult.getVerifiedClaims());
        Mockito.verify(restTemplate, Mockito.times(1)).getForObject(Mockito.anyString(), Mockito.eq(MockUserStory.class));
    }


    @Test
    public void getVerifiedResult_withValidTransactionId_thenFail() throws IdentityVerifierException {

        ObjectMapper objectMapper1 = new ObjectMapper();
        ReflectionTestUtils.setField(mockIdentityVerifierPlugin, "objectMapper",objectMapper1);
        String transactionId = "transactionId123";


        Mockito.when(restTemplate.getForObject(Mockito.anyString(), Mockito.eq(MockUserStory.class))).thenReturn(null);
        VerifiedResult actualVerifiedResult = mockIdentityVerifierPlugin.getVerifiedResult(transactionId);
        Assert.assertEquals("mock_verification_failed", actualVerifiedResult.getErrorCode());
    }
}
