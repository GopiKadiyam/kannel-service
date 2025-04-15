package com.gk.kannel.utils.enums;

public enum ServiceType {
    TRANS("TRANS"), PROMO("PROMO"),OTP("OTP"),GLOBAL("GLOBAL");
    private String value;
    ServiceType(String value){
        this.value=value;
    }
    public String getValue(){
        return this.value;
    }
}
