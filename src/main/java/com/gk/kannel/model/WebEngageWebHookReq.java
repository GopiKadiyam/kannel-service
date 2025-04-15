package com.gk.kannel.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebEngageWebHookReq {
    private String version;
    private String messageId;
    private String toNumber;
    private String status;
    private int statusCode;
    private String contentTemplateId;
    private String principalEntityId;
    private String message;
}
