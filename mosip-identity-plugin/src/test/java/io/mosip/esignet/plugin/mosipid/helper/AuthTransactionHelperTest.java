package io.mosip.esignet.plugin.mosipid.helper;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mosip.esignet.core.dto.ResponseWrapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AuthTransactionHelperTest {

    @Mock
    ObjectMapper objectMapper;

    @Mock
    RestTemplate restTemplate;

    @InjectMocks
    AuthTransactionHelper authTransactionHelper;

    @Test
    public void GetAuthTokenWithValidDetails_thenPass() throws Exception {
        ReflectionTestUtils.setField(authTransactionHelper, "authTokenUrl", "test");
        ReflectionTestUtils.setField(authTransactionHelper, "clientId", "test");
        ReflectionTestUtils.setField(authTransactionHelper,"secretKey","test");
        ReflectionTestUtils.setField(authTransactionHelper,"appId","test");        String expectedAuthToken = "testAuthToken";

        ResponseEntity<ResponseWrapper> responseEntity = ResponseEntity.ok()
                .header("authorization", expectedAuthToken)
                .build();
        when(restTemplate.exchange(Mockito.any(RequestEntity.class), Mockito.any(ParameterizedTypeReference.class)))
                .thenReturn(responseEntity);
        String authToken = authTransactionHelper.getAuthToken();
        Assertions.assertEquals(expectedAuthToken, authToken);
    }
}
