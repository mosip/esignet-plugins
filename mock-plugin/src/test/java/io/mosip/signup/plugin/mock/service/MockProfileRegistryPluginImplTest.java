package io.mosip.signup.plugin.mock.service;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.mosip.esignet.core.util.IdentityProviderUtil;
import io.mosip.kernel.core.http.ResponseWrapper;
import io.mosip.signup.api.dto.*;
import io.mosip.signup.api.exception.ProfileException;
import io.mosip.signup.api.util.ErrorConstants;
import io.mosip.signup.plugin.mock.dto.*;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;



@RunWith(MockitoJUnitRunner.class)
public class MockProfileRegistryPluginImplTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private MockProfileRegistryPluginImpl mockProfileRegistryPlugin;

    @Test
    public void updateProfile_withValidVerifiedClaims_thenPass()  {
        ReflectionTestUtils.setField(mockProfileRegistryPlugin, "verifiedClaimUrl", "http://localhost:8080/verified-claim");

        String requestId = "req-123";
        String individualId = "ind-456";

        Map<String, Map<String, VerificationDetail>> verifiedData = new HashMap<>();
        Map<String,VerificationDetail> map=new HashMap<>();
        VerificationDetail verificationDetail = new VerificationDetail();
        verificationDetail.setVerification_process("F1");
        verificationDetail.setTrust_framework("trust-123");
        verificationDetail.setTime(IdentityProviderUtil.getUTCDateTime());
        map.put("name", verificationDetail);
        verifiedData.put("verified_claims", map);

        ObjectMapper objectMapper1=new ObjectMapper();
        JsonNode mockIdentity = objectMapper1.valueToTree(verifiedData);

        Mockito.when(objectMapper.convertValue(Mockito.any(ObjectNode.class), Mockito.eq(Map.class))).thenReturn(map);
        ProfileDto profileDto = new ProfileDto();
        profileDto.setIndividualId(individualId);
        profileDto.setIdentity(mockIdentity);


        ResponseWrapper<VerifiedClaimStatus> responseWrapper = new ResponseWrapper<>();
        VerifiedClaimStatus verifiedClaimStatus = new VerifiedClaimStatus();
        verifiedClaimStatus.setStatus("success");
        responseWrapper.setResponse(verifiedClaimStatus);
        ResponseEntity<ResponseWrapper<VerifiedClaimStatus>> responseEntity = new ResponseEntity(responseWrapper,HttpStatus.OK);
        
        Mockito.when(restTemplate.exchange(
                Mockito.any(RequestEntity.class),
                Mockito.eq(new ParameterizedTypeReference<ResponseWrapper<VerifiedClaimStatus>>() {
                })
        )).thenReturn(responseEntity);

        ProfileResult profileResult = mockProfileRegistryPlugin.updateProfile(requestId, profileDto);
        Assert.assertNotNull(profileResult);
        Assert.assertEquals(profileResult.getStatus(),"SUCCESS");
    }

    @Test
    public void updateProfile_withInValidVerifiedClaimsJson_thenFail() {
        ReflectionTestUtils.setField(mockProfileRegistryPlugin, "verifiedClaimUrl", "http://localhost:8080/verified-claim");

        String requestId = "req-123";
        String individualId = "ind-456";

        Map<String, Map<String, VerificationDetail>> verifiedData = new HashMap<>();
        Map<String,VerificationDetail> map=new HashMap<>();
        VerificationDetail verificationDetail = new VerificationDetail();
        verificationDetail.setVerification_process("F1");
        verificationDetail.setTrust_framework("trust-123");
        verificationDetail.setTime(IdentityProviderUtil.getUTCDateTime());
        map.put("name", verificationDetail);
        verifiedData.put("verified_claims", map);

        ObjectMapper objectMapper1=new ObjectMapper();
        JsonNode mockIdentity = objectMapper1.valueToTree(verifiedData);

        Mockito.when(objectMapper.convertValue(Mockito.any(ObjectNode.class), Mockito.eq(Map.class))).thenThrow(new ProfileException(ErrorConstants.UNKNOWN_ERROR) {
        });
        ProfileDto profileDto = new ProfileDto();
        profileDto.setIndividualId(individualId);
        profileDto.setIdentity(mockIdentity);

        try{
            mockProfileRegistryPlugin.updateProfile(requestId, profileDto);
        }catch (ProfileException e){
            Assert.assertEquals(e.getErrorCode(),ErrorConstants.UNKNOWN_ERROR);
        }
    }

    @Test
    public void updateProfile_withInValidRequest_thenFail() throws Exception {
        ReflectionTestUtils.setField(mockProfileRegistryPlugin, "verifiedClaimUrl", "http://localhost:8080/verified-claim");
        String requestId = "req-123";
        String individualId = "ind-456";

        Map<String, Map<String, VerificationDetail>> verifiedData = new HashMap<>();
        Map<String,VerificationDetail> map=new HashMap<>();
        VerificationDetail verificationDetail = new VerificationDetail();
        verificationDetail.setVerification_process("F1");
        verificationDetail.setTrust_framework("trust-123");
        verificationDetail.setTime(IdentityProviderUtil.getUTCDateTime());
        map.put("name", verificationDetail);
        verifiedData.put("verified_claims", map);

        ObjectMapper objectMapper1=new ObjectMapper();
        JsonNode mockIdentity = objectMapper1.valueToTree(verifiedData);

        Mockito.when(objectMapper.convertValue(Mockito.any(ObjectNode.class), Mockito.eq(Map.class))).thenReturn(map);

        ProfileDto profileDto = new ProfileDto();
        profileDto.setIndividualId(individualId);
        profileDto.setIdentity(mockIdentity);


        ResponseWrapper<VerifiedClaimStatus> responseWrapper = new ResponseWrapper<>();
        VerifiedClaimStatus verifiedClaimStatus = new VerifiedClaimStatus();
        verifiedClaimStatus.setStatus("success");
        responseWrapper.setResponse(verifiedClaimStatus);
        ResponseEntity<ResponseWrapper<VerifiedClaimStatus>> responseEntity = new ResponseEntity(responseWrapper,HttpStatus.BAD_REQUEST);

        Mockito.when(restTemplate.exchange(
                Mockito.any(RequestEntity.class),
                Mockito.eq(new ParameterizedTypeReference<ResponseWrapper<VerifiedClaimStatus>>() {
                })
        )).thenReturn(responseEntity); {
        }
        try{
            mockProfileRegistryPlugin.updateProfile(requestId, profileDto);
        }catch (ProfileException e){
            Assert.assertEquals(e.getErrorCode(),io.mosip.esignet.api.util.ErrorConstants.DATA_EXCHANGE_FAILED);
        }
    }

}



