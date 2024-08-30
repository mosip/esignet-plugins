package io.mosip.signup.plugin.mock.service;


import com.fasterxml.jackson.databind.JsonNode;
import io.mosip.signup.api.dto.ProfileDto;
import io.mosip.signup.api.dto.ProfileResult;
import io.mosip.signup.api.exception.ProfileException;
import io.mosip.signup.api.util.ProfileCreateUpdateStatus;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class MockProfileRegistryPluginImplTest {


    @InjectMocks
    private MockProfileRegistryPluginImpl mockProfileRegistryPlugin;


    @Test
    public void validate_withValidActionAndProfileDto_thenPass() {
        // Arrange
        String action = "create";
        ProfileDto profileDto = new ProfileDto();
        mockProfileRegistryPlugin.validate(action, profileDto);
    }

    @Test
    public void createProfile_withValidRequestAndProfileDto_thenPass() throws ProfileException {
        // Arrange
        String requestId = "requestId123";
        ProfileDto profileDto = new ProfileDto();
        ProfileResult result = mockProfileRegistryPlugin.createProfile(requestId, profileDto);

        Assert.assertNull(result);
    }


    @Test
    public void getProfileCreateUpdateStatus_withValidRequestId_thenPass() throws ProfileException {
        // Arrange
        String requestId = "requestId123";

        // Act
        ProfileCreateUpdateStatus status = mockProfileRegistryPlugin.getProfileCreateUpdateStatus(requestId);

        // Assert
        Assert.assertNull(status);
    }

    @Test
    public void getProfile_withValidIndividualId_thenPass() throws ProfileException {
        // Arrange
        String individualId = "individualId123";

        // Act
        ProfileDto profileDto = mockProfileRegistryPlugin.getProfile(individualId);

        // Assert
        Assert.assertNull(profileDto);
    }

    @Test
    public void isMatch_withMatchingIdentityAndInputChallenge_thenPass() {
        // Arrange
        JsonNode identity = Mockito.mock(JsonNode.class); // Simulate matching identity
        JsonNode inputChallenge = Mockito.mock(JsonNode.class); // Simulate matching challenge
        // Act
        boolean isMatch = mockProfileRegistryPlugin.isMatch(identity, inputChallenge);
        // Assert
        Assert.assertFalse(isMatch);
    }
}
