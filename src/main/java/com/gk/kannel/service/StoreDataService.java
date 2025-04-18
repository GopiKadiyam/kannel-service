package com.gk.kannel.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gk.kannel.entities.WebhookCallbackStatus;
import com.gk.kannel.entities.UserMsgReqEntity;
import com.gk.kannel.entities.UserMsgReqStatusEntity;
import com.gk.kannel.entities.UserWiseWebhookRegistryEntity;
import com.gk.kannel.exception.EntityNotFoundException;
import com.gk.kannel.model.CustomerWebHookReq;
import com.gk.kannel.model.MessageRequest;
import com.gk.kannel.model.UpdateMsgReq;
import com.gk.kannel.model.WebEngageWebHookReq;
import com.gk.kannel.repository.MessageWebhookStatusRepository;
import com.gk.kannel.repository.UserMessagesInfoRepository;
import com.gk.kannel.repository.UserMessagesRepository;
import com.gk.kannel.repository.UserWiseWebhookRepository;
import com.gk.kannel.service.mappers.CommonMapper;
import com.gk.kannel.service.producers.FailedMsgProducer;
import com.gk.kannel.service.producers.UpdateMsgStatusProducer;
import com.gk.kannel.utils.enums.CRMType;
import com.gk.kannel.utils.enums.MsgStatus;
import com.gk.kannel.utils.enums.MsgWebhookStatus;
import com.gk.kannel.utils.enums.SMSStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j

public class StoreDataService {
    @Autowired
    private CommonMapper commonMapper;
    @Autowired
    private UserWiseWebhookRepository userWiseWebhookRepository;
    @Autowired
    private UserMessagesRepository userMessagesRepository;
    @Autowired
    private FailedMsgProducer failedMsgProducer;
    @Autowired
    private UpdateMsgStatusProducer updateMsgStatusProducer;
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private MessageWebhookStatusRepository messageWebhookStatusRepository;
    @Autowired
    private UserMessagesInfoRepository userMessagesInfoRepository;

    public void createMsgReq(String tenantId, MessageRequest request) {
        try {
            UserMsgReqEntity userMsgReqEntity = commonMapper.messageRequestToMessageEntity(request);
            userMsgReqEntity.setCallBackWebhook(request.getWebhookId() != null ?
                    userWiseWebhookRepository.findByWebhookId(request.getWebhookId()).orElse(null) : null);

            UserMsgReqStatusEntity userMsgReqStatusEntity = new UserMsgReqStatusEntity();
            userMsgReqStatusEntity.setUserId(tenantId);
            userMsgReqStatusEntity.setMsgStatus(MsgStatus.CREATED);
            userMsgReqStatusEntity.setSmsSentOn(request.getSmsSentOn());
            userMsgReqStatusEntity.setMsgGroupId(request.getMsgGroupId());

            userMsgReqStatusEntity.setUserMessage(userMsgReqEntity);
            userMsgReqEntity.setUserMessagesInfo(userMsgReqStatusEntity);
            //userMessagesInfoRepository.save(userMessagesInfoEntity);
            userMsgReqEntity = userMessagesRepository.save(userMsgReqEntity);
            log.info("MsgReq created with id {} and status {} into db", request.getMsgId(), userMsgReqEntity.getUserMessagesInfo().getMsgStatus());
        } catch (Exception e) {
            log.info("Unable create MsgReq with id {} and msg {} into db", request.getMsgId(), request);
            failedMsgProducer.postFailedMsgToKafka(tenantId, request);
            e.printStackTrace();
        }
    }

    public void updateMsgWithProviderCallStatus(String tenantId, MessageRequest request) {
        try {
            MessageRequest finalMessage = request;
            UserMsgReqEntity userMsgReqEntity = userMessagesRepository.findById(request.getMsgId())
                    .orElseThrow(() -> {
                        failedMsgProducer.postFailedMsgToKafka(tenantId, finalMessage);
                        return new EntityNotFoundException("msgId", request.getMsgId() + " not present");
                    });
            UserMsgReqStatusEntity userMsgReqStatusEntity = userMsgReqEntity.getUserMessagesInfo();
            userMsgReqStatusEntity.setMsgStatus(request.getMsgStatus());
            userMsgReqStatusEntity.setDlrSentOn(request.getUpdateMsgReq() != null ? request.getUpdateMsgReq().getDlrSentOn() : null);
            userMsgReqEntity.setUserMessagesInfo(userMsgReqStatusEntity);
            //userMessagesInfoEntity.setUpdatedOn(LocalDateTime.now(ZoneOffset.UTC));
            userMessagesRepository.save(userMsgReqEntity);
            log.info("msgId {} updated with status {} into db", request.getMsgId(), request.getMsgStatus());
        } catch (Exception e) {
            log.info("unable to update msgId {} with status {} into db", request.getMsgId(), request.getMsgStatus());
            failedMsgProducer.postFailedMsgToKafka(tenantId, request);
            e.printStackTrace();
        }
    }

    public void updateMsgWithDLCBSuccess(String tenantId, MessageRequest request) {
        try {
            MessageRequest finalMessage = request;
            UserMsgReqEntity userMsgReqEntity = userMessagesRepository.findById(request.getMsgId())
                    .orElseThrow(() -> {
                        failedMsgProducer.postFailedMsgToKafka(tenantId, finalMessage);
                        return new EntityNotFoundException("msgId", "msgId :" + request.getMsgId() + " is not present");
                    });
            UserMsgReqStatusEntity userMsgReqStatusEntity = userMsgReqEntity.getUserMessagesInfo();
            userMsgReqStatusEntity.setMsgStatus(MsgStatus.DLR_CB_SUCCESS);

            UpdateMsgReq updateMsgReq = request.getUpdateMsgReq();
            String dlrStatusString = updateMsgReq.getStatusJson();
            userMsgReqStatusEntity.setDlrDeliveredOn(convertToGMTLocalDateTime(extractFieldFromSource(dlrStatusString, "done date:([^\\s]+)").trim()));
            SMSStatus smsStatus = SMSStatus.fromValue(extractFieldFromSource(dlrStatusString, "stat:([^\\s]+)").trim());
            userMsgReqStatusEntity.setDlrStatus(smsStatus);
            userMsgReqStatusEntity.setDlrStatusCode(extractFieldFromSource(dlrStatusString, "err:([^\\s]+)").trim());
            userMsgReqStatusEntity.setDlrStatusDescription(smsStatus.getDescription());

            userMsgReqEntity.setUserMessagesInfo(userMsgReqStatusEntity);
            userMsgReqEntity = userMessagesRepository.save(userMsgReqEntity);
            sendWebhookCall(tenantId, userMsgReqEntity);
            log.info("msgId {} updated with status {} into db", request.getMsgId(), request.getMsgStatus());
        } catch (Exception e) {
            log.info("unable to update msgId {} with status {} into db", request.getMsgId(), request.getMsgStatus());
            failedMsgProducer.postFailedMsgToKafka(tenantId, request);
            e.printStackTrace();
        }
    }

    public void sendWebhookCall(String tenantId, UserMsgReqEntity userMsgReqEntity) {
        UserWiseWebhookRegistryEntity webhookEntity = userMsgReqEntity.getCallBackWebhook();
        if (webhookEntity != null) {
            try {
                ResponseEntity<Object> response = null;
                if (userMsgReqEntity.getCrmMsgType() == null || userMsgReqEntity.getCrmMsgType() == CRMType.MT_ADAPTER) {
                    CustomerWebHookReq customerWebHookReq = commonMapper.userMessagesEntityToCustomerWebHookReq(userMsgReqEntity);
                    customerWebHookReq.setSuccess(customerWebHookReq.getStatus().equalsIgnoreCase("DELIVRD"));
                    response = restTemplate.postForEntity(webhookEntity.getWebhookUrl(), customerWebHookReq, Object.class);
                }
                else if (userMsgReqEntity.getCrmMsgType() == CRMType.WEB_ENGAGE) {
                    WebEngageWebHookReq req = new WebEngageWebHookReq();
                    req.setVersion(userMsgReqEntity.getWebEngageVersion());
                    req.setMessageId(userMsgReqEntity.getCrmMsgId());
                    req.setToNumber(userMsgReqEntity.getTo());
                    SMSStatus smsStatus = userMsgReqEntity.getUserMessagesInfo().getDlrStatus();
                    if (SMSStatus.DELIVRD == smsStatus) {
                        req.setStatus("sms_sent");
                        req.setStatusCode(0);
                    } else {
                        req.setStatus("sms_failed");
                        req.setStatusCode(9988);
                        req.setMessage(smsStatus.getDescription());
                    }
                    req.setContentTemplateId(userMsgReqEntity.getTemplateId());
                    req.setPrincipalEntityId(userMsgReqEntity.getEntityId());
                    response = restTemplate.postForEntity(webhookEntity.getWebhookUrl(), req, Object.class);
                } else {
                    response = null;
                }
                if (response != null) {
                    WebhookCallbackStatus webhookCallbackStatus = new WebhookCallbackStatus();
                    webhookCallbackStatus.setUserMsgReqEntity(userMsgReqEntity);
                    webhookCallbackStatus.setWebhookId(webhookEntity.getWebhookId());
                    webhookCallbackStatus.setUserId(userMsgReqEntity.getUser().getId());
                    if (response.getStatusCode().is2xxSuccessful()) {
                        webhookCallbackStatus.setStatus(MsgWebhookStatus.SUCCESS);
                    } else {
                        webhookCallbackStatus.setStatus(MsgWebhookStatus.FAILED);
                    }
                    webhookCallbackStatus.setResponse(response.getStatusCode().toString());
                    webhookCallbackStatus.setRetryCount(1);
                    messageWebhookStatusRepository.save(webhookCallbackStatus);
                    log.info("webhook url sent for msgId {} , with status {} ", userMsgReqEntity.getId(), webhookCallbackStatus.getStatus());
                }
            } catch (Exception e) {
                //failedMsgProducer.postFailedMsgToKafka(tenantId, request);
                log.info("failure while sending customer webhook call for tenantId {}, message {}", tenantId, userMsgReqEntity.toString());
                e.printStackTrace();
            }
        }


    }

    private String extractFieldFromSource(String source, String regexPattern) {
        Pattern pattern = Pattern.compile(regexPattern);
        Matcher matcher = pattern.matcher(source);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private Instant convertToGMTLocalDateTime(String doneDate) {
        try {
            DateTimeFormatter inputFormatter;
            switch (doneDate.length()) {
                case 12 -> inputFormatter = DateTimeFormatter.ofPattern("yyMMddHHmmss");
                case 10 -> inputFormatter = DateTimeFormatter.ofPattern("yyMMddHHmm");
                default -> {
                    log.info("Error while converting doneDate {}", doneDate);
                    return null;
                }
            }
            LocalDateTime localDateTime = LocalDateTime.parse(doneDate, inputFormatter);
            // Convert from IST (Asia/Kolkata) to UTC (GMT)
            ZonedDateTime istZonedDateTime = localDateTime.atZone(ZoneId.of("Asia/Kolkata"));
            return istZonedDateTime.toInstant();
        } catch (Exception e) {
            log.info("Error while converting doneDate {}", doneDate);
            return null;
        }
    }
}
