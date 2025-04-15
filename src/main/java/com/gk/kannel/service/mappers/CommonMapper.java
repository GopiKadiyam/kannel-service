package com.gk.kannel.service.mappers;

import com.gk.kannel.entities.UserEntity;
import com.gk.kannel.entities.UserMessagesEntity;
import com.gk.kannel.entities.UserMessagesInfoEntity;
import com.gk.kannel.model.CustomerWebHookReq;
import com.gk.kannel.model.MessageRequest;
import com.gk.kannel.repository.UserWiseWebhookRepository;
import com.gk.kannel.utils.enums.MessageType;
import com.gk.kannel.utils.enums.SMSStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

@Component
public class CommonMapper {
    public static UserMessagesEntity messageRequestToMessageEntity(MessageRequest request) {
        if (request == null) {
            return null;
        }

        UserMessagesEntity userMessagesEntity = new UserMessagesEntity();
        userMessagesEntity.setId(request.getMsgId());
        userMessagesEntity.setCountry(request.getCountry());
        userMessagesEntity.setServiceType(request.getServiceType());
        userMessagesEntity.setFrom(request.getFrom());
        userMessagesEntity.setTo(request.getTo());
        userMessagesEntity.setBody(request.getBody());
        userMessagesEntity.setTemplateId(request.getTemplateId());
        userMessagesEntity.setEntityId(request.getEntityId());
        userMessagesEntity.setMessageType(request.getMessageType());
        userMessagesEntity.setCustomId(request.getCustomId());
        userMessagesEntity.setFlash(request.isFlash());
        userMessagesEntity.setCrmMsgId(request.getCrmMsgId());
        userMessagesEntity.setWebEngageVersion(request.getWebEngageVersion());
        userMessagesEntity.setCrmMsgType(request.getCrmMsgType());
        userMessagesEntity.setSmsLength(request.getBody().length());
        userMessagesEntity.setCredits(getUnits(request.getBody(), request.getMessageType()));
        userMessagesEntity.setUser(new UserEntity(request.getTenantId()));
        Map<String, String> map = request.getMetadata();
        if (map != null) {
            userMessagesEntity.setMetadata(new HashMap<String, String>(map));
        }
        return userMessagesEntity;
    }

    private static int getUnits(String msgBody, MessageType messageType) {
        if (msgBody == null || msgBody.equalsIgnoreCase(""))
            return 1;
        int unit;
        switch (messageType) {
            case N -> unit = (msgBody.length() <= 160) ? 1 : (int) Math.ceil((double) msgBody.length() / 153);
            case U -> unit = (msgBody.length() <= 70) ? 1 : (int) Math.ceil((double) msgBody.length() / 67);
            case A -> {
                boolean isNonAscii = msgBody.chars().anyMatch(c -> c >= 128);
                if (!isNonAscii) {
                    // ASCII logic
                    unit = (msgBody.length() <= 160) ? 1 : (int) Math.ceil((double) msgBody.length() / 153);
                } else {
                    // Non-ASCII logic
                    unit = (msgBody.length() <= 70) ? 1 : (int) Math.ceil((double) msgBody.length() / 67);
                }
            }
            default -> unit = 0;
        }
        return unit;
    }

    public static CustomerWebHookReq userMessagesEntityToCustomerWebHookReq(UserMessagesEntity userMessagesEntity) {
        if (userMessagesEntity == null) {
            return null;
        }

        CustomerWebHookReq customerWebHookReq = new CustomerWebHookReq();
        customerWebHookReq.setId(userMessagesEntity.getId());

        UserMessagesInfoEntity userMessagesInfo = userMessagesEntity.getUserMessagesInfo();
        if (userMessagesInfo != null) {
            SMSStatus dlrStatus = userMessagesInfo.getDlrStatus();
            if (dlrStatus != null) {
                customerWebHookReq.setStatus(dlrStatus.name());
            }
            customerWebHookReq.setStatusCode(userMessagesInfo.getDlrStatusCode());
            customerWebHookReq.setSuccess(SMSStatus.DELIVRD == dlrStatus);
            customerWebHookReq.setDlrSentOn(userMessagesInfo.getDlrSentOn());
            customerWebHookReq.setDlrDeliveredOn(userMessagesInfo.getDlrDeliveredOn());
            customerWebHookReq.setMsgSubmittedOn(userMessagesInfo.getSmsSentOn());
        }
        customerWebHookReq.setTenantId(userMessagesEntity.getUser().getId());
        customerWebHookReq.setFrom(userMessagesEntity.getFrom());
        customerWebHookReq.setCountry(userMessagesEntity.getCountry());
        customerWebHookReq.setTo(userMessagesEntity.getTo());
        customerWebHookReq.setCustomId(userMessagesEntity.getCustomId());
        Map<String, String> map = userMessagesEntity.getMetadata();
        if (map != null) {
            customerWebHookReq.setMetadata(new HashMap<String, String>(map));
        }
        customerWebHookReq.setFlash(userMessagesEntity.isFlash());
        if (userMessagesEntity.getServiceType() != null) {
            customerWebHookReq.setServiceType(userMessagesEntity.getServiceType().name());
        }
        if (userMessagesEntity.getMessageType() != null) {
            customerWebHookReq.setMessageType(userMessagesEntity.getMessageType().name());
        }
        customerWebHookReq.setTemplateId(userMessagesEntity.getTemplateId());
        customerWebHookReq.setEntityId(userMessagesEntity.getEntityId());

        return customerWebHookReq;
    }

}
