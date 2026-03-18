/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.signup.plugin.mosipid.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Password {

    private String hash;
    private String salt;
    private String value; // Added for compatible with 1.3.0 IDRepo

    @Data
    @AllArgsConstructor
    public static class PasswordPlaintext{
        private String inputData;
    }

    @Data
    public static class PasswordHash {
        private String hashValue;
        private String salt;
    }
}
