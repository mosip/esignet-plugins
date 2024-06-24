package io.mosip.esignet.plugin.ida.dto;

import lombok.Data;

@Data
public class IdaVcExchangeResponse<T> {

    private T verifiableCredentials;
}
