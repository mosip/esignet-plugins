package io.mosip.esignet.plugin.mosipid.service;

import io.mosip.signup.plugin.mosipid.util.ProfileCacheService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProfileCacheServiceTest {

    private static final String REQUEST_IDS = "request_ids";

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache cache;

    @InjectMocks
    private ProfileCacheService profileCacheService;

    @Test
    public void testSetHandleRequestIds() {
        String requestId = "123";
        List<String> handleRequestIds = Arrays.asList("RID1", "RID2");
        List<String> result = profileCacheService.setHandleRequestIds(requestId, handleRequestIds);
        Assertions.assertEquals(handleRequestIds, result);
    }

    @Test
    public void testGetHandleRequestIdsFromCache() {
        when(cacheManager.getCache(REQUEST_IDS)).thenReturn(cache);
        String requestId = "123";
        List<String> handleRequestIds = Arrays.asList("RID1", "RID2");
        when(cache.get(requestId, List.class)).thenReturn(handleRequestIds);
        List<String> cachedResult = profileCacheService.getHandleRequestIds(requestId);
        Assertions.assertEquals(handleRequestIds, cachedResult);
        verify(cacheManager, times(1)).getCache(REQUEST_IDS);
        verify(cache, times(1)).get(requestId, List.class);
    }

    @Test
    public void testCachingBehaviorForSameRequestId() {
        when(cacheManager.getCache(REQUEST_IDS)).thenReturn(cache);
        String requestId = "123";
        List<String> handleRequestIds = Arrays.asList("requestId1", "requestId2");
        List<String> result1 = profileCacheService.setHandleRequestIds(requestId, handleRequestIds);

        when(cache.get(requestId, List.class)).thenReturn(handleRequestIds);
        List<String> cachedResult = profileCacheService.getHandleRequestIds(requestId);

        Assertions.assertEquals(handleRequestIds, result1);

        Assertions.assertEquals(handleRequestIds, cachedResult);
        verify(cacheManager, times(1)).getCache(REQUEST_IDS);
        verify(cache, times(1)).get(requestId, List.class);
    }

}
