/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.signup.plugin.mock.dto;

import io.mosip.signup.api.dto.IDVProcessFeedback;
import io.mosip.signup.api.dto.IDVProcessStepDetail;
import lombok.Data;

@Data
public class UseCaseFlow {
    private int frameNumber;
    private String stepCode;
    private IDVProcessStepDetail step;
    private IDVProcessFeedback feedback;
}