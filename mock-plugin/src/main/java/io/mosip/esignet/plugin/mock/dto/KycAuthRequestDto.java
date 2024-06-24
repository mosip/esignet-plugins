package io.mosip.esignet.plugin.mock.dto;


import lombok.Data;

import java.util.List;

@Data
public class KycAuthRequestDto {

    private String transactionId;
    private String individualId;
    private String otp;
    private String pin;
    private String biometrics;
    private String kba;
    private List<String> tokens;
}
