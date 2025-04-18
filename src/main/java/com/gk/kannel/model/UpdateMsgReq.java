package com.gk.kannel.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMsgReq {
    private String statusJson;
    private String type;
    private String pId;
    private String smscId;
    private String tm;
    private String dlrUrl;
    private Instant dlrSentOn;
}
