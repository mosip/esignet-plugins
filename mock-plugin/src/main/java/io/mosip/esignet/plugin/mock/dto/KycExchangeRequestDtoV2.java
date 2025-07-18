/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.esignet.plugin.mock.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.util.Map;

@Data
public class KycExchangeRequestDtoV2 extends KycExchangeRequestDto{

    private Map<String, JsonNode> acceptedClaimDetail;
}
