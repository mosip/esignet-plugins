package io.mosip.esignet.plugin.mosipid.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.util.List;

@Data
public class MockUserStory {

    private List<MockScene> scenes;
    private JsonNode verificationResult;
}
