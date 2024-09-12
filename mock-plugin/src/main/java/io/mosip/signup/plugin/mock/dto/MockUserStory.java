package io.mosip.signup.plugin.mock.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.util.List;

@Data
public class MockUserStory {

    private List<MockScene> scenes;
    private JsonNode verificationResult;
}
