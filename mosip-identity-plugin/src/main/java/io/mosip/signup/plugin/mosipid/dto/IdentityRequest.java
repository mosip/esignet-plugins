/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.signup.plugin.mosipid.dto;

import com.fasterxml.jackson.databind.JsonNode;
import io.mosip.esignet.plugin.mosipid.dto.VerificationMetadata;
import lombok.Data;

import java.util.List;

@Data
public class IdentityRequest {

    private String registrationId;
    private JsonNode identity;
    private List<VerificationMetadata> verifiedAttributes;
}
