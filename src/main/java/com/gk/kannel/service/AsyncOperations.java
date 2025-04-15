package com.gk.kannel.service;

import com.gk.kannel.model.MessageRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AsyncOperations {

    @Autowired
    private StoreDataService storeDataService;

    @Async
    public void createMsgReq(String tenantId, MessageRequest message) {
        storeDataService.createMsgReq(tenantId, message);
    }

    @Async
    public void updateMsgWithProviderCallStatus(String tenantId, MessageRequest message) {
        storeDataService.updateMsgWithProviderCallStatus(tenantId, message);
    }

    @Async
    public void updateMsgWithDLCBSuccess(String tenantId, MessageRequest message) {
        storeDataService.updateMsgWithDLCBSuccess(tenantId, message);
    }

//    @Async
//    public void sendWebhookCall(String tenantId, MessageRequest message){
//        storeDataService.sendWebhookCall(tenantId,message);
//    }
}
