package org.mock.esignet.plugin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class UserDetail {
    private String id;
    private String name;
    private String dob;
    private String email;
}
