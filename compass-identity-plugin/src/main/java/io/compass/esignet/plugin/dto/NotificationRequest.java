package io.compass.esignet.plugin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class NotificationRequest {
    private String number;
    private String message;
}

