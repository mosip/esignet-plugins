package io.mosip.signup.plugin.mock.dto;

import io.mosip.esignet.core.validator.RequestTime;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

import javax.validation.Valid;

@Data
public class RequestWrapper<T> {

    @RequestTime
    private String requestTime;

    @NotNull()
    @Valid
    private T request;
}
