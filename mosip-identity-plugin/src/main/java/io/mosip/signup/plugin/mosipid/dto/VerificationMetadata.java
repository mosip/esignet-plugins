/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.signup.plugin.mosipid.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Class to hold verification metadata details to be updated to ID Repo from signup.
 */
@Data
public class VerificationMetadata {
    /**
    trust framework verifying the claims
     */
    private String trustFramework;

    /**
    verification process used for the verification
     */
    private String verificationProcess;

    /**
     * list of verified claims
     */
    private List<String> claims;

    /**
     * verified claims metadata which hold information
     * like trust_framework, verification_process, time etc
     */
    private Map<String, Object> metadata;

}
