package io.mosip.signup.plugin.mock.dto;

import javax.validation.constraints.NotBlank;

import io.mosip.signup.plugin.mock.util.ErrorConstants;

public class BiometricData {
	@NotBlank(message = ErrorConstants.INVALID_FORMAT)
	private String format;

	@NotBlank(message = ErrorConstants.INVALID_VERSION)
	private double version;

	@NotBlank(message = ErrorConstants.INVALID_VALUE)
	private String value;
}
