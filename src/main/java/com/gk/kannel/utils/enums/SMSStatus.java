package com.gk.kannel.utils.enums;

import java.util.Arrays;

public enum SMSStatus {
    DELIVRD("DELIVRD","Message Delivered Successfully"),UNDELIV("UNDELIV","Message UnDelivered "),EXPIRED("EXPIRED","Message Expired");
    private final String value;
    private final String description;
    SMSStatus(String value,String description){
        this.value=value;
        this.description=description;
    }
    public String getValue(){
        return this.value;
    }
    public String getDescription(){
        return this.description;
    }
    public static SMSStatus fromValue(String code) {
        return Arrays.stream(SMSStatus.values())
                .filter(status -> status.getValue().equalsIgnoreCase(code))
                .findFirst()
                .orElse(UNDELIV);
    }
}
