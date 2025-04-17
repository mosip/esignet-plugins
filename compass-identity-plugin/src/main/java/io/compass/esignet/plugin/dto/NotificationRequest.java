package io.compass.esignet.plugin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
@AllArgsConstructor
public class NotificationRequest {
    private String[] mailTo;
    private String[] mailCc;
    private String[] mailSubject;
    private String[] mailContent;
    private MultipartFile[] attachments;
}

