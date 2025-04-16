package io.compass.esignet.plugin.dto;

import lombok.Data;

@Data
public class ReCaptchaError {

    private String errorCode;
    private String message;
}
