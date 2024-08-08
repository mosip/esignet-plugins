package io.mosip.esignet.plugin.mock.service;


import io.mosip.esignet.api.dto.VCRequestDto;
import io.mosip.esignet.api.exception.VCIExchangeException;
import io.mosip.esignet.core.constants.ErrorConstants;
import io.mosip.esignet.core.dto.OIDCTransaction;
import io.mosip.esignet.core.dto.vci.ParsedAccessToken;
import io.mosip.esignet.core.exception.EsignetException;
import io.mosip.esignet.core.util.IdentityProviderUtil;
import io.mosip.kernel.core.keymanager.spi.KeyStore;
import io.mosip.kernel.keymanagerservice.entity.KeyAlias;
import io.mosip.kernel.keymanagerservice.helper.KeymanagerDBHelper;
import io.mosip.kernel.signature.dto.JWTSignatureResponseDto;
import io.mosip.kernel.signature.service.SignatureService;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;

@RunWith(MockitoJUnitRunner.class)
public class MockVCIssuancePluginTest {

    @Mock
    private SignatureService signatureService;

    @Mock
    private ParsedAccessToken parsedAccessToken;

    @Mock
    private Cache cache;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private KeyStore keyStore;

    @Mock
    private KeymanagerDBHelper dbHelper;

    @InjectMocks
    private MockVCIssuancePlugin mockVCIssuancePlugin;


    @Test
    public void getVerifiableCredentialWithLinkedDataProof_withValidDetails_thenPass() throws VCIExchangeException {

        ReflectionTestUtils.setField(mockVCIssuancePlugin, "verificationMethod", "http://localhost:8080/v1/esignet.mosip.net/oauth/.well-known/jwks.json");
        List<String> vcCredentialContexts = new ArrayList<>();
        vcCredentialContexts.add("https://www.w3.org/2018/credentials/v1");
        ReflectionTestUtils.setField(mockVCIssuancePlugin, "vcCredentialContexts", vcCredentialContexts);
        ReflectionTestUtils.setField(mockVCIssuancePlugin, "secureIndividualId", false);

        OIDCTransaction transaction = new OIDCTransaction();
        transaction.setAuthTransactionId("123456789");
        transaction.setLinkedTransactionId("987654321");
        transaction.setLinkedCodeHash("68392");
        transaction.setIndividualId("4258935620");

        Mockito.when(parsedAccessToken.getAccessTokenHash()).thenReturn("123456789");

        Mockito.when(cache.get("123456789", OIDCTransaction.class)).thenReturn(transaction);
        Mockito.when(cacheManager.getCache(Mockito.anyString())).thenReturn(cache);

        JWTSignatureResponseDto jwtSignatureResponseDto = new JWTSignatureResponseDto();
        jwtSignatureResponseDto.setTimestamp(LocalDateTime.now());
        jwtSignatureResponseDto.setJwtSignedData("test");
        Mockito.when(signatureService.jwtSign(Mockito.any())).thenReturn(jwtSignatureResponseDto);

        mockVCIssuancePlugin.getVerifiableCredentialWithLinkedDataProof(new VCRequestDto(), "test", new HashMap<>());
    }


    @Test
    public void getVerifiableCredentialWithLinkedDataProof_withSecureIndividualId_thenPass() throws VCIExchangeException, NoSuchAlgorithmException {

        ReflectionTestUtils.setField(mockVCIssuancePlugin, "verificationMethod", "http://localhost:8080/v1/esignet.mosip.net/oauth/.well-known/jwks.json");
        List<String> vcCredentialContexts = new ArrayList<>();
        vcCredentialContexts.add("https://www.w3.org/2018/credentials/v1");
        ReflectionTestUtils.setField(mockVCIssuancePlugin, "vcCredentialContexts", vcCredentialContexts);
        ReflectionTestUtils.setField(mockVCIssuancePlugin, "secureIndividualId", true);
        ReflectionTestUtils.setField(mockVCIssuancePlugin, "storeIndividualId", true);
        ReflectionTestUtils.setField(mockVCIssuancePlugin, "aesECBTransformation", "AES/ECB/PKCS5Padding");
        ReflectionTestUtils.setField(mockVCIssuancePlugin,"cacheSecretKeyRefId","cacheSecretKeyRefId");
        ReflectionTestUtils.setField(mockVCIssuancePlugin,"getIdentityUrl","http://localhost:8080/v1/esignet.mosip.net");

        KeyGenerator generator = KeyGenerator.getInstance("AES");
        generator.init(256);
        SecretKey key = generator.generateKey();
        String individualId = encryptIndividualId("individual-id",key);

        OIDCTransaction oidcTransaction = new OIDCTransaction();
        oidcTransaction.setIndividualId(individualId);
        oidcTransaction.setKycToken("kycToken");
        oidcTransaction.setAuthTransactionId("authTransactionId");
        oidcTransaction.setRelyingPartyId("relyingPartyId");

        Mockito.when(parsedAccessToken.getAccessTokenHash()).thenReturn("123456789");

        Mockito.when(cache.get("123456789", OIDCTransaction.class)).thenReturn(oidcTransaction);
        Mockito.when(cacheManager.getCache(Mockito.anyString())).thenReturn(cache);


        Map<String, List<KeyAlias>> keyaliasesMap = new HashMap<>();
        KeyAlias keyAlias = new KeyAlias();
        keyAlias.setAlias("test");
        keyaliasesMap.put("currentKeyAlias", Arrays.asList(keyAlias));
        Mockito.when(dbHelper.getKeyAliases(Mockito.anyString(), Mockito.anyString(), Mockito.any(LocalDateTime.class))).thenReturn(keyaliasesMap);

        JWTSignatureResponseDto jwtSignatureResponseDto = new JWTSignatureResponseDto();
        jwtSignatureResponseDto.setTimestamp(LocalDateTime.now());
        jwtSignatureResponseDto.setJwtSignedData("test");
        Mockito.when(signatureService.jwtSign(Mockito.any())).thenReturn(jwtSignatureResponseDto);


        Mockito.when(keyStore.getSymmetricKey(Mockito.any())).thenReturn(key,key);



        mockVCIssuancePlugin.getVerifiableCredentialWithLinkedDataProof(new VCRequestDto(), "test", new HashMap<>());
    }

    @Test
    public void getVerifiableCredentialWithLinkedDataProof_withInValidKeyAlias_thenFail() throws VCIExchangeException, NoSuchAlgorithmException {

        ReflectionTestUtils.setField(mockVCIssuancePlugin, "verificationMethod", "http://localhost:8080/v1/esignet.mosip.net/oauth/.well-known/jwks.json");
        List<String> vcCredentialContexts = new ArrayList<>();
        vcCredentialContexts.add("https://www.w3.org/2018/credentials/v1");
        ReflectionTestUtils.setField(mockVCIssuancePlugin, "vcCredentialContexts", vcCredentialContexts);
        ReflectionTestUtils.setField(mockVCIssuancePlugin, "secureIndividualId", true);
        ReflectionTestUtils.setField(mockVCIssuancePlugin, "storeIndividualId", true);
        ReflectionTestUtils.setField(mockVCIssuancePlugin, "aesECBTransformation", "AES/ECB/PKCS5Padding");
        ReflectionTestUtils.setField(mockVCIssuancePlugin,"cacheSecretKeyRefId","cacheSecretKeyRefId");
        ReflectionTestUtils.setField(mockVCIssuancePlugin,"getIdentityUrl","http://localhost:8080/v1/esignet.mosip.net");

        KeyGenerator generator = KeyGenerator.getInstance("AES");
        generator.init(256);
        SecretKey key = generator.generateKey();
        String individualId = encryptIndividualId("individual-id",key);

        OIDCTransaction oidcTransaction = new OIDCTransaction();
        oidcTransaction.setIndividualId(individualId);
        oidcTransaction.setKycToken("kycToken");
        oidcTransaction.setAuthTransactionId("authTransactionId");
        oidcTransaction.setRelyingPartyId("relyingPartyId");

        Mockito.when(parsedAccessToken.getAccessTokenHash()).thenReturn("123456789");

        Mockito.when(cache.get("123456789", OIDCTransaction.class)).thenReturn(oidcTransaction);
        Mockito.when(cacheManager.getCache(Mockito.anyString())).thenReturn(cache);


        Map<String, List<KeyAlias>> keyaliasesMap = new HashMap<>();
        keyaliasesMap.put("currentKeyAlias", Arrays.asList());
        Mockito.when(dbHelper.getKeyAliases(Mockito.anyString(), Mockito.anyString(), Mockito.any(LocalDateTime.class))).thenReturn(keyaliasesMap);

        JWTSignatureResponseDto jwtSignatureResponseDto = new JWTSignatureResponseDto();
        jwtSignatureResponseDto.setTimestamp(LocalDateTime.now());
        jwtSignatureResponseDto.setJwtSignedData("test");
        Mockito.when(signatureService.jwtSign(Mockito.any())).thenReturn(jwtSignatureResponseDto);
        mockVCIssuancePlugin.getVerifiableCredentialWithLinkedDataProof(new VCRequestDto(), "test", new HashMap<>());
    }

    @Test
    public void getVerifiableCredential_InValidDetails_thenFail(){
        try{
            mockVCIssuancePlugin.getVerifiableCredential(new VCRequestDto(), "test", new HashMap<>());
        } catch (VCIExchangeException e) {
            Assert.assertEquals("not_implemented", e.getErrorCode());
        }
    }


    private String encryptIndividualId(String individualId, Key key) {
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            byte[] secretDataBytes = individualId.getBytes(StandardCharsets.UTF_8);
            cipher.init(Cipher.ENCRYPT_MODE,key);
            return IdentityProviderUtil.b64Encode(cipher.doFinal(secretDataBytes, 0, secretDataBytes.length));
        } catch(Exception e) {
            throw new EsignetException(ErrorConstants.AES_CIPHER_FAILED);
        }
    }


}
