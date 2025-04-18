package com.gk.kannel.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.gk.kannel.utils.enums.Country;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;

@Data
public class CustomerWebHookReq {

    private String id;
    private String from;
    private Country country;
    private String to;
    private String status;
    private String statusCode;
    private boolean isSuccess;
    private String deliveredTime;
    private String sentTime;
    private String submitTime;
    private String customId;
    private Map<String,String> metadata;
    private String tenantId;
    private boolean flash;
    private String serviceType;
    private String messageType;
    private String templateId;
    private String entityId;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "Asia/Kolkata")
    private Instant dlrSentOn;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "Asia/Kolkata")
    private Instant dlrDeliveredOn;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "Asia/Kolkata")
    private Instant msgSubmittedOn;
}
