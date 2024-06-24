package io.mosip.esignet.plugin.mock.dto;


import lombok.Data;

@Data
public class KycAuthResponseDto {

    private boolean authStatus;
    private String kycToken;
    private String partnerSpecificUserToken;
}