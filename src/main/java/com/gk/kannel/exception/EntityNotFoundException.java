package com.gk.kannel.exception;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class EntityNotFoundException extends RuntimeException{
    private String message;
    private String key;
    public EntityNotFoundException(String key,String message){
        super(message);
        this.message=message;
        this.key=key;
    }
}
