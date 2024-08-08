package io.mosip.signup.plugin.mock.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class VerifiedClaimRequestDto {


    private String individualId;

    private String claim;

    private String trustFramework;

    private LocalDateTime verifiedDateTime;

}
