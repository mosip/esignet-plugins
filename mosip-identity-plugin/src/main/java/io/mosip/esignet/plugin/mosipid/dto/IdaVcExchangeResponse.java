package io.mosip.esignet.plugin.mosipid.dto;

import lombok.Data;

@Data
public class IdaVcExchangeResponse<T> {

    private T verifiableCredentials;
}
