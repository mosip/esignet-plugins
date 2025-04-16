package io.compass.esignet.plugin.service;

import io.compass.esignet.plugin.dto.KycAuth;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

@Service
public class CacheService {

    public static final String KYC_AUTH_CACHE="kycauth";

    public static final String CHALLENGE_HASH="challengehash";

    @Autowired
    CacheManager cacheManager;

    public void setKycAuth(String kycToken, KycAuth kycAuth) {
        cacheManager.getCache(KYC_AUTH_CACHE).put(kycToken,kycAuth);
    }

    public KycAuth getKycAuth(String kycToken) {
        return cacheManager.getCache(KYC_AUTH_CACHE).get(kycToken, KycAuth.class);	//NOSONAR getCache() will not be returning null here.
    }

    public void setChallengeHash(String challengeHash, String transactionId) {
        cacheManager.getCache(CHALLENGE_HASH).put(transactionId, challengeHash);
    }

    public String getChallengeHash(String transactionId) {
        Cache.ValueWrapper valueWrapper = cacheManager.getCache(CHALLENGE_HASH).get(transactionId);
        return valueWrapper != null ? (String) valueWrapper.get() : null;
    }

}
