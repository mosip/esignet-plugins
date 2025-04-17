package io.compass.esignet.plugin.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class UserInfo  implements Serializable {

    private static final long serialVersionUID = 1L;
    private UUID userInfoId;
    private String birthCountry;
    private Long cardAccessNumber;
    private LocalDate dateOfBirth;
    private String email;
    private String faceImageColor;
    private String firstNamePrimary;
    private String gender;
    private String lastNameSecondary;
    private String nationalUid;
    private String nationality;
    private String compassId;
    private LocalDate issuanceDate;
}
