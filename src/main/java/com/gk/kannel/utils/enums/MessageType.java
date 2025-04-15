package com.gk.kannel.utils.enums;

public enum MessageType {
    U("U",2),N("N",0),A("A",0);
    private final String value;
    private final int kennelValue;
    MessageType(String value,int kennelValue){
        this.value=value;
        this.kennelValue=kennelValue;
    }
    public String getValue() {
        return this.value;
    }
    public int getKennelValue() {
        return this.kennelValue;
    }

}
