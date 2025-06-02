package org.mock.esignet.plugin.service;

import io.mosip.esignet.api.dto.*;
import io.mosip.esignet.api.exception.KycAuthException;
import io.mosip.esignet.api.exception.KycExchangeException;
import io.mosip.esignet.api.exception.KycSigningCertificateException;
import io.mosip.esignet.api.exception.SendOtpException;
import io.mosip.esignet.api.spi.Authenticator;

import java.util.List;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.bouncycastle.x509.X509V3CertificateGenerator;
import org.mock.esignet.plugin.dto.UserDetail;
import org.mock.esignet.plugin.repositories.UserDetailRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.security.auth.x500.X500Principal;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.time.ZoneOffset;
import java.util.*;

@Component
public class MockDBAuthenticatorImpl implements Authenticator {

    private Map<String, String> localMap = new HashMap<>();

    @Autowired
    private UserDetailRepository userDetailRepository;
    private X509Certificate keyCertificate;
    private KeyPair localKey;

    @Override
    public KycAuthResult doKycAuth(String relyingPartyId, String clientId, KycAuthDto kycAuthDto) throws KycAuthException {
        UserDetail userDetail = userDetailRepository.findUserById(kycAuthDto.getIndividualId());

        if (userDetail == null)
            throw new KycAuthException("user_not_found");

        boolean authStatus = false;
        AuthChallenge authChallenge = kycAuthDto.getChallengeList().get(0);
        switch (authChallenge.getAuthFactorType()) {
            case "OTP":
                authStatus = authChallenge.getChallenge().equals("111111");
                break;
            default:
                throw new KycAuthException("invalid_auth_factor");
        }

        if(!authStatus)
            throw new KycAuthException("auth_failed");

        String token = UUID.randomUUID().toString();
        localMap.put(token, kycAuthDto.getIndividualId());
        KycAuthResult kycAuthResult = new KycAuthResult();
        kycAuthResult.setKycToken(token);
        kycAuthResult.setPartnerSpecificUserToken(token);
        return kycAuthResult;
    }

    @Override
    public KycExchangeResult doKycExchange(String relyingPartyId, String clientId, KycExchangeDto kycExchangeDto) throws KycExchangeException {
        String storedKycToken = localMap.get(kycExchangeDto.getKycToken());
        if (storedKycToken == null)
            throw new KycExchangeException("invalid_kyc_token");

        UserDetail userDetail = userDetailRepository.findUserById(storedKycToken);

        JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
                .subject(storedKycToken)
                .issuer("eSignet")
                .expirationTime(new Date(System.currentTimeMillis() + 3600 * 1000));

        for (String claim : kycExchangeDto.getAcceptedClaims()) {
            switch (claim) {
                case "name":
                    builder.claim("name", userDetail.getName());
                    break;
                case "birthdate":
                    builder.claim("birthDate", userDetail.getDob());
                    break;
                case "email":
                    builder.claim("email", userDetail.getEmail());
                    break;
            }
        }

        JWTClaimsSet claimsSet = builder.build();
        String signedJWT = signJWT(claimsSet); //additionally can be encrypted with public key of relying party

        KycExchangeResult kycExchangeResult = new KycExchangeResult();
        kycExchangeResult.setEncryptedKyc(signedJWT);
        return kycExchangeResult;
    }



    @Override
    public SendOtpResult sendOtp(String relyingPartyId, String clientId, SendOtpDto sendOtpDto) throws SendOtpException {
        SendOtpResult sendOtpResult = new SendOtpResult();
        sendOtpResult.setTransactionId(sendOtpDto.getTransactionId());
        sendOtpResult.setMaskedMobile("");
        return sendOtpResult;
    }

    @Override
    public boolean isSupportedOtpChannel(String channel) {
        return true;
    }

    @Override
    public List<KycSigningCertificateData> getAllKycSigningCertificates() throws KycSigningCertificateException {
        KycSigningCertificateData kycSigningCertificateData = new KycSigningCertificateData();
        try {

            Base64.Encoder encoder = Base64.getMimeEncoder(64, "\n".getBytes());
            String encodedCert = encoder.encodeToString(keyCertificate.getEncoded());
            kycSigningCertificateData.setCertificateData("-----BEGIN CERTIFICATE-----\n" + encodedCert + "\n-----END CERTIFICATE-----");
            kycSigningCertificateData.setExpiryAt(keyCertificate.getNotAfter().toInstant().atZone(ZoneOffset.UTC).toLocalDateTime());
            kycSigningCertificateData.setIssuedAt(keyCertificate.getNotBefore().toInstant().atZone(ZoneOffset.UTC).toLocalDateTime());

        } catch (CertificateEncodingException e) {
            throw new RuntimeException(e);
        }
        //return public key from this method
        return List.of(kycSigningCertificateData);
    }

    public void generateKeyCertificate() {
        if (localKey == null || keyCertificate == null) {
            try {
                KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
                keyPairGenerator.initialize(2048);
                localKey = keyPairGenerator.generateKeyPair();

                X509V3CertificateGenerator certGen = new X509V3CertificateGenerator();
                X500Principal dnName = new X500Principal("CN=Self-Signed, O=Example Org, C=US");
                certGen.setSerialNumber(BigInteger.valueOf(System.currentTimeMillis()));
                certGen.setSubjectDN(dnName);
                certGen.setIssuerDN(dnName); // Self-signed
                certGen.setNotBefore(new Date(System.currentTimeMillis()));
                certGen.setNotAfter(new Date(System.currentTimeMillis() + (365 * 24 * 60 * 60 * 1000L))); // 1 year validity
                certGen.setPublicKey(localKey.getPublic());
                certGen.setSignatureAlgorithm("SHA256WithRSAEncryption");
                keyCertificate = certGen.generate(localKey.getPrivate(), "BC");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private String signJWT(JWTClaimsSet claimsSet) throws KycExchangeException {
        if(localKey == null || keyCertificate == null) {
            generateKeyCertificate();
        }

        JWSHeader header = new JWSHeader(JWSAlgorithm.RS256);
        SignedJWT signedJWT = new SignedJWT(header, claimsSet);
        JWSSigner signer = new RSASSASigner((RSAPrivateKey)localKey.getPrivate());
        try {
            signedJWT.sign(signer);
        } catch (JOSEException e) {
            throw new KycExchangeException("signing_failed");
        }
        return signedJWT.serialize();
    }
}
