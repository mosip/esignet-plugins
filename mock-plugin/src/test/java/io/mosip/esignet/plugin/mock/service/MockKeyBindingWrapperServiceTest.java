package io.mosip.esignet.plugin.mock.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import io.mosip.esignet.api.dto.*;
import io.mosip.esignet.api.exception.KeyBindingException;
import io.mosip.esignet.api.exception.SendOtpException;
import io.mosip.esignet.api.util.ErrorConstants;
import io.mosip.kernel.core.http.ResponseWrapper;
import io.mosip.kernel.core.keymanager.model.CertificateEntry;
import io.mosip.kernel.keymanagerservice.dto.SignatureCertificate;

import io.mosip.kernel.keymanagerservice.service.KeymanagerService;
import org.bouncycastle.x509.X509V3CertificateGenerator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import javax.security.auth.x500.X500Principal;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.*;

@ExtendWith(MockitoExtension.class)
public class MockKeyBindingWrapperServiceTest {


    @Mock
    private MockHelperService mockHelperService;

    @Mock
    private RestTemplate restTemplate;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private KeymanagerService keymanagerService;


    @InjectMocks
    private MockKeyBindingWrapperService mockKeyBindingWrapperService;

    @BeforeEach
    public void setup() {
        ReflectionTestUtils.setField(mockKeyBindingWrapperService, "objectMapper", objectMapper);
    }

    @Test
    public void sendBindingOtp_withValidDetails_thenPass() throws SendOtpException {
       SendOtpResult sendOtpResult = new SendOtpResult();
       sendOtpResult.setMaskedEmail("testEmail");
       sendOtpResult.setTransactionId("testTransactionId");
       sendOtpResult.setMaskedMobile("testMobile");

       Mockito.when(mockHelperService.sendOtpMock(Mockito.anyString(),Mockito.anyString(),Mockito.anyList(),Mockito.anyString(),Mockito.anyString())).thenReturn(sendOtpResult);

       SendOtpResult result = mockKeyBindingWrapperService.sendBindingOtp("testIndividualId", List.of("mobile"), null);

       Assertions.assertNotNull(result);
       Assertions.assertEquals(result,sendOtpResult);
    }


    @Test
    public void sendBindingOtp_withInValidDetails_thenFail() throws SendOtpException {
        SendOtpResult sendOtpResult = new SendOtpResult();
        sendOtpResult.setMaskedEmail("testEmail");
        sendOtpResult.setTransactionId("testTransactionId");
        sendOtpResult.setMaskedMobile("testMobile");

        Mockito.when(mockHelperService.sendOtpMock(Mockito.anyString(),Mockito.anyString(),Mockito.anyList(),Mockito.anyString(),Mockito.anyString()))
                .thenThrow(new SendOtpException("Error while sending otp"));
        try{
            mockKeyBindingWrapperService.sendBindingOtp("testIndividualId", List.of("mobile"), null);
            Assertions.fail();
        }catch (SendOtpException e){
            Assertions.assertEquals("Error while sending otp",e.getMessage());
        }
    }


    @Test
    public void doKeyBinding_withValidDetails_thenPass() throws Exception {
        ReflectionTestUtils.setField(mockKeyBindingWrapperService, "supportedBindAuthFactorTypes", List.of("WLA")) ;
        ReflectionTestUtils.setField(mockKeyBindingWrapperService, "expireInDays", 10) ;
        ReflectionTestUtils.setField(mockKeyBindingWrapperService,"getIdentityEndpoint","http://localhost:8080/"); ;

        KycAuthResult kycAuthResult = new KycAuthResult();
        kycAuthResult.setKycToken("testKycToken");
        kycAuthResult.setPartnerSpecificUserToken("testPartnerSpecificUserToken");
        Mockito.when(mockHelperService.doKycAuthMock(Mockito.anyString(),Mockito.anyString(),Mockito.any(),Mockito.anyBoolean()))
                .thenReturn(kycAuthResult);

        ObjectNode identityData = objectMapper.createObjectNode();
        identityData.put("email", "testEmail");
        ResponseWrapper<JsonNode> responseWrapper = new ResponseWrapper<>();
        responseWrapper.setResponse(identityData);
        var responseEntity = new ResponseEntity<>(new ResponseWrapper(), HttpStatus.OK);
        Mockito.when(restTemplate.exchange(
                Mockito.any(RequestEntity.class),
                Mockito.eq(ResponseWrapper.class)
        )).thenReturn(responseEntity);

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        JWK jwk=generateJWK_RSA();
        Map<String, Object> publicKeyMap = jwk.toJSONObject();
        X509Certificate certificate=getCertificate();
        PrivateKey privateKey=jwk.toRSAKey().toPrivateKey();

        SignatureCertificate signatureCertificate = new SignatureCertificate();
        CertificateEntry<X509Certificate, PrivateKey> certificateEntry = new CertificateEntry();
        certificateEntry.setPrivateKey(privateKey);
        certificateEntry.setChain(new X509Certificate[]{certificate});
        signatureCertificate.setCertificateEntry(certificateEntry);

        Mockito.when(keymanagerService.getSignatureCertificate(Mockito.anyString(),Mockito.any(),Mockito.anyString()))
                .thenReturn(signatureCertificate);

        List<AuthChallenge> challengeList = new ArrayList<>();
        AuthChallenge authChallenge = new AuthChallenge();
        authChallenge.setChallenge("testChallenge");
        authChallenge.setAuthFactorType("OTP");
        authChallenge.setFormat("alpha-numeric");

        KeyBindingResult keyBindingResult = mockKeyBindingWrapperService.doKeyBinding("testIndividualId", challengeList, publicKeyMap, "WLA", null);
        Assertions.assertNotNull(keyBindingResult);
    }


    @Test
    public void doKeyBinding_withUnSupportedBindAuthFactor_thenFail() throws Exception {
        ReflectionTestUtils.setField(mockKeyBindingWrapperService, "supportedBindAuthFactorTypes", List.of("WLA")) ;

        try{
            mockKeyBindingWrapperService.doKeyBinding("testIndividualId", new ArrayList<>(), new HashMap<>(), "OTP", null);
            Assertions.fail();
        }catch (Exception e){
            Assertions.assertEquals("invalid_bind_auth_factor_type",e.getMessage());
        }
    }


    @Test
    public void doKeyBinding_withInValidKycAuthResult_thenFail() throws Exception {
        ReflectionTestUtils.setField(mockKeyBindingWrapperService, "supportedBindAuthFactorTypes", List.of("WLA")) ;
        ReflectionTestUtils.setField(mockKeyBindingWrapperService, "expireInDays", 10) ;
        ReflectionTestUtils.setField(mockKeyBindingWrapperService,"getIdentityEndpoint","http://localhost:8080/"); ;

        Mockito.when(mockHelperService.doKycAuthMock(Mockito.anyString(),Mockito.anyString(),Mockito.any(),Mockito.anyBoolean()))
                .thenReturn(null);
        try{
            mockKeyBindingWrapperService.doKeyBinding("testIndividualId", new ArrayList<>(), new HashMap<>(), "WLA", null);
            Assertions.fail();
        }catch (KeyBindingException e){
            Assertions.assertEquals(ErrorConstants.KEY_BINDING_FAILED,e.getMessage());
        }
    }

    @Test
    public void doKeyBinding_withInValidDetails_thenFail() throws Exception {
        ReflectionTestUtils.setField(mockKeyBindingWrapperService, "supportedBindAuthFactorTypes", List.of("WLA")) ;
        ReflectionTestUtils.setField(mockKeyBindingWrapperService, "expireInDays", 10) ;
        ReflectionTestUtils.setField(mockKeyBindingWrapperService,"getIdentityEndpoint","http://localhost:8080/"); ;

        KycAuthResult kycAuthResult = new KycAuthResult();
        kycAuthResult.setKycToken("testKycToken");
        kycAuthResult.setPartnerSpecificUserToken("testPartnerSpecificUserToken");
        Mockito.when(mockHelperService.doKycAuthMock(Mockito.anyString(),Mockito.anyString(),Mockito.any(),Mockito.anyBoolean()))
                .thenReturn(kycAuthResult);

        Mockito.when(restTemplate.exchange(
                Mockito.any(RequestEntity.class),
                Mockito.eq(ResponseWrapper.class)
        )).thenThrow(new RuntimeException("Error while fetching identity data"));

        try{
            mockKeyBindingWrapperService.doKeyBinding("testIndividualId", new ArrayList<>(), new HashMap<>(), "WLA", null);
            Assertions.fail();
        }catch (Exception e){
            Assertions.assertEquals("auth_failed -> Error while fetching identity data",e.getMessage());
        }
    }


    private JWK generateJWK_RSA() {
        // Generate the RSA key pair
        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(2048);
            KeyPair keyPair = gen.generateKeyPair();
            // Convert public key to JWK format
            return new RSAKey.Builder((RSAPublicKey)keyPair.getPublic())
                    .privateKey((RSAPrivateKey)keyPair.getPrivate())
                    .keyUse(KeyUse.SIGNATURE)
                    .keyID(UUID.randomUUID().toString())
                    .build();
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }


    private X509Certificate getCertificate() throws Exception {
        X509V3CertificateGenerator generator = new X509V3CertificateGenerator();
        X500Principal dnName = new X500Principal("CN=Test");
        generator.setSubjectDN(dnName);
        generator.setIssuerDN(dnName); // use the same
        generator.setNotBefore(new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000));
        generator.setNotAfter(new Date(System.currentTimeMillis() + 24 * 365 * 24 * 60 * 60 * 1000));
        generator.setPublicKey(generateJWK_RSA().toRSAKey().toPublicKey());
        generator.setSignatureAlgorithm("SHA256WITHRSA");
        generator.setSerialNumber(new BigInteger(String.valueOf(System.currentTimeMillis())));
        return generator.generate(generateJWK_RSA().toRSAKey().toPrivateKey());
    }

}
