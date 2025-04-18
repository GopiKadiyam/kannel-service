package com.gk.kannel.service.mappers;

import com.gk.kannel.entities.UserAccountEntity;
import com.gk.kannel.entities.UserMsgReqEntity;
import com.gk.kannel.entities.UserMsgReqStatusEntity;
import com.gk.kannel.model.CustomerWebHookReq;
import com.gk.kannel.model.MessageRequest;
import com.gk.kannel.utils.enums.MessageType;
import com.gk.kannel.utils.enums.SMSStatus;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class CommonMapper {
    public static UserMsgReqEntity messageRequestToMessageEntity(MessageRequest request) {
        if (request == null) {
            return null;
        }

        UserMsgReqEntity userMsgReqEntity = new UserMsgReqEntity();
        userMsgReqEntity.setId(request.getMsgId());
        userMsgReqEntity.setMsgGroupId(request.getMsgGroupId());
        userMsgReqEntity.setCountry(request.getCountry());
        userMsgReqEntity.setServiceType(request.getServiceType());
        userMsgReqEntity.setFrom(request.getFrom());
        userMsgReqEntity.setTo(request.getTo());
        userMsgReqEntity.setBody(request.getBody());
        userMsgReqEntity.setTemplateId(request.getTemplateId());
        userMsgReqEntity.setEntityId(request.getEntityId());
        userMsgReqEntity.setMessageType(request.getMessageType());
        userMsgReqEntity.setCustomId(request.getCustomId());
        userMsgReqEntity.setFlash(request.isFlash());
        userMsgReqEntity.setCrmMsgId(request.getCrmMsgId());
        userMsgReqEntity.setWebEngageVersion(request.getWebEngageVersion());
        userMsgReqEntity.setCrmMsgType(request.getCrmMsgType());
        userMsgReqEntity.setSmsLength(request.getSmsLength());
        userMsgReqEntity.setCredits(request.getCredits());
        userMsgReqEntity.setUser(new UserAccountEntity(request.getTenantId()));
        Map<String, String> map = request.getMetadata();
        if (map != null) {
            userMsgReqEntity.setMetadata(new HashMap<String, String>(map));
        }
        return userMsgReqEntity;
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

    public static CustomerWebHookReq userMessagesEntityToCustomerWebHookReq(UserMsgReqEntity userMsgReqEntity) {
        if (userMsgReqEntity == null) {
            return null;
        }

        CustomerWebHookReq customerWebHookReq = new CustomerWebHookReq();
        customerWebHookReq.setId(userMsgReqEntity.getId());

        UserMsgReqStatusEntity userMessagesInfo = userMsgReqEntity.getUserMessagesInfo();
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
        customerWebHookReq.setTenantId(userMsgReqEntity.getUser().getId());
        customerWebHookReq.setFrom(userMsgReqEntity.getFrom());
        customerWebHookReq.setCountry(userMsgReqEntity.getCountry());
        customerWebHookReq.setTo(userMsgReqEntity.getTo());
        customerWebHookReq.setCustomId(userMsgReqEntity.getCustomId());
        Map<String, String> map = userMsgReqEntity.getMetadata();
        if (map != null) {
            customerWebHookReq.setMetadata(new HashMap<String, String>(map));
        }
        customerWebHookReq.setFlash(userMsgReqEntity.isFlash());
        if (userMsgReqEntity.getServiceType() != null) {
            customerWebHookReq.setServiceType(userMsgReqEntity.getServiceType().name());
        }
        if (userMsgReqEntity.getMessageType() != null) {
            customerWebHookReq.setMessageType(userMsgReqEntity.getMessageType().name());
        }
        customerWebHookReq.setTemplateId(userMsgReqEntity.getTemplateId());
        customerWebHookReq.setEntityId(userMsgReqEntity.getEntityId());

        return customerWebHookReq;
    }

}
