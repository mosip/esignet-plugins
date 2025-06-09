/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.esignet.plugin.mosipid.dto;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IdaKycExchangeRequest {

    private String id;
    private String version;
    private String requestTime;
    private String transactionID;
    private String kycToken;
    private List<String> consentObtained;
    private List<String> locales;
    private String respType;
    private String individualId;

    private Map<String, Object> metadata;
    /**
     * verified claims
     */
    List<Map<String, Object>> verifiedConsentedClaims;
    /**
     * User consented unverified claims list.
     */
    Map<String, Object> unVerifiedConsentedClaims;

}
