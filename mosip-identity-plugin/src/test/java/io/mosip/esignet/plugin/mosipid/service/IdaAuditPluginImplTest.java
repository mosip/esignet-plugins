package io.mosip.esignet.plugin.mosipid.service;

import io.mosip.esignet.api.dto.AuditDTO;
import io.mosip.esignet.api.util.Action;
import io.mosip.esignet.api.util.ActionStatus;
import io.mosip.esignet.plugin.mosipid.dto.AuditResponse;
import io.mosip.esignet.plugin.mosipid.helper.AuthTransactionHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mosip.kernel.core.http.ResponseWrapper;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
public class IdaAuditPluginImplTest {
    @InjectMocks
    private IdaAuditPluginImpl idaAuditPlugin;
    @Mock
    private AuthTransactionHelper authTransactionHelper;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private RestTemplate restTemplate;
    @Test
    public void logAudit_WithValidDetails_ThenPass() {
        Action action = Action.AUTHENTICATE;
        ActionStatus status = ActionStatus.SUCCESS;
        AuditDTO auditDTO = new AuditDTO();
        try {
            idaAuditPlugin.logAudit(action, status, auditDTO, null);
            Assertions.assertTrue(true);
        } catch (Exception e) {
            Assertions.fail();
        }
    }
    @Test
    public void logAudit_WithThrowable_ThenPass() {
        Action action = Action.GENERATE_TOKEN;
        ActionStatus status = ActionStatus.SUCCESS;
        AuditDTO auditDTO = new AuditDTO();
        Throwable throwable = new RuntimeException("Test Exception");
        try {
            idaAuditPlugin.logAudit(action, status, auditDTO, throwable);
            Assertions.assertTrue(true);
        } catch (Exception e) {
            Assertions.fail();
        }
    }
    @Test
    public void logAudit_WithUsername_WithValidDetails_ThenPass() {
        String username = "username";
        Action action = Action.OIDC_CLIENT_UPDATE;
        ActionStatus status = ActionStatus.SUCCESS;
        AuditDTO auditDTO = new AuditDTO();
        try {
            idaAuditPlugin.logAudit(username, action, status, auditDTO, null);
            Assertions.assertTrue(true);
        } catch (Exception e) {
            Assertions.fail();
        }
    }

    @Test
    public void logAudit_WithUsername_WithThrowable() throws Exception {
        String username = "username";
        Action action = Action.GENERATE_TOKEN;
        ActionStatus status = ActionStatus.SUCCESS;
        AuditDTO auditDTO = new AuditDTO();
        Throwable throwable = new RuntimeException("Test Exception");
        try {
            idaAuditPlugin.logAudit(username,action, status, auditDTO, throwable);
            Assertions.assertTrue(true);
        } catch (Exception e) {
            Assertions.fail();
        }
    }
    @Test
    public void logAudit_WithValidStatus_ThenPass() throws Exception {
        ReflectionTestUtils.setField(idaAuditPlugin, "auditManagerUrl", "auditManagerUrl");
        String username = "username";
        Action action = Action.SAVE_CONSENT;
        ActionStatus status = ActionStatus.SUCCESS;
        AuditDTO auditDTO = new AuditDTO();
        ResponseWrapper<AuditResponse> mockresponseWrapper = new ResponseWrapper<>();
        ResponseEntity<ResponseWrapper> responseEntity = ResponseEntity.ok(mockresponseWrapper);
        ParameterizedTypeReference<ResponseWrapper> responseType =
                new ParameterizedTypeReference<ResponseWrapper>() {
                };
        Mockito.when(authTransactionHelper.getAuthToken()).thenReturn("authToken");
        Mockito.when(objectMapper.writeValueAsString(any())).thenReturn("requestBody");
        Mockito.when(restTemplate.exchange(
                Mockito.any(RequestEntity.class),
                Mockito.eq(responseType)
        )).thenReturn(responseEntity);
        try {
            idaAuditPlugin.logAudit(username,action, status, auditDTO, null);
            Assertions.assertTrue(true);
        } catch (Exception e) {
            Assertions.fail();
        }
    }
    @Test
    public void logAudit_WithUnauthorizedStatus_ThenPass() throws Exception {
        ReflectionTestUtils.setField(idaAuditPlugin, "auditManagerUrl", "auditManagerUrl");
        String username = "username";
        Action action = Action.SAVE_CONSENT;
        ActionStatus status = ActionStatus.SUCCESS;
        AuditDTO auditDTO = new AuditDTO();
        ResponseWrapper<AuditResponse> mockresponseWrapper = new ResponseWrapper<>();
        ResponseEntity<ResponseWrapper> responseEntity = ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(mockresponseWrapper);
        ParameterizedTypeReference<ResponseWrapper> responseType =
                new ParameterizedTypeReference<ResponseWrapper>() {
                };
        Mockito.when(authTransactionHelper.getAuthToken()).thenReturn("authToken");
        Mockito.when(objectMapper.writeValueAsString(any())).thenReturn("requestBody");
        Mockito.when(restTemplate.exchange(
                Mockito.any(RequestEntity.class),
                Mockito.eq(responseType)
        )).thenReturn(responseEntity);
        try {
            idaAuditPlugin.logAudit(username,action, status, auditDTO, null);
            Assertions.assertTrue(true);
        } catch (Exception e) {
            Assertions.fail();
        }
    }
    @Test
    public void logAudit_WithForbiddenStatus_ThenPass() throws Exception {
        ReflectionTestUtils.setField(idaAuditPlugin, "auditManagerUrl", "auditManagerUrl");
        String username = "username";
        Action action = Action.SAVE_CONSENT;
        ActionStatus status = ActionStatus.SUCCESS;
        AuditDTO auditDTO = new AuditDTO();
        ResponseWrapper<AuditResponse> mockresponseWrapper = new ResponseWrapper<>();
        ResponseEntity<ResponseWrapper> responseEntity = ResponseEntity.status(HttpStatus.FORBIDDEN).body(mockresponseWrapper);
        ParameterizedTypeReference<ResponseWrapper> responseType =
                new ParameterizedTypeReference<ResponseWrapper>() {
                };
        Mockito.when(authTransactionHelper.getAuthToken()).thenReturn("authToken");
        Mockito.when(objectMapper.writeValueAsString(any())).thenReturn("requestBody");
        Mockito.when(restTemplate.exchange(
                Mockito.any(RequestEntity.class),
                Mockito.eq(responseType)
        )).thenReturn(responseEntity);
        try {
            idaAuditPlugin.logAudit(username,action, status, auditDTO, null);
            Assertions.assertTrue(true);
        } catch (Exception e) {
            Assertions.fail();
        }
    }
}