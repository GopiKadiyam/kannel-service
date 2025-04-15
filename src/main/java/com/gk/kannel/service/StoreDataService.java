package com.gk.kannel.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gk.kannel.entities.MessageWebhookStatus;
import com.gk.kannel.entities.UserEntity;
import com.gk.kannel.entities.UserMessagesEntity;
import com.gk.kannel.entities.UserMessagesInfoEntity;
import com.gk.kannel.entities.UserWiseWebhookEntity;
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
import com.gk.kannel.utils.enums.MessageType;
import com.gk.kannel.utils.enums.MsgStatus;
import com.gk.kannel.utils.enums.MsgWebhookStatus;
import com.gk.kannel.utils.enums.SMSStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

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
            UserMessagesEntity userMessagesEntity = commonMapper.messageRequestToMessageEntity(request);
            userMessagesEntity.setCallBackWebhook(request.getWebhookId() != null ?
                    userWiseWebhookRepository.findByWebhookId(request.getWebhookId()).orElse(null) : null);

            UserMessagesInfoEntity userMessagesInfoEntity = new UserMessagesInfoEntity();
            userMessagesInfoEntity.setUserId(tenantId);
            userMessagesInfoEntity.setMsgStatus(MsgStatus.CREATED);
            userMessagesInfoEntity.setSmsSentOn(request.getSmsSentOn());
            userMessagesInfoEntity.setMsgGroupId(request.getMsgGroupId());

            userMessagesInfoEntity.setUserMessage(userMessagesEntity);
            userMessagesEntity.setUserMessagesInfo(userMessagesInfoEntity);
            //userMessagesInfoRepository.save(userMessagesInfoEntity);
            userMessagesEntity = userMessagesRepository.save(userMessagesEntity);
            log.info("MsgReq created with id {} and status {} into db", request.getMsgId(), userMessagesEntity.getUserMessagesInfo().getMsgStatus());
        } catch (Exception e) {
            log.info("Unable create MsgReq with id {} and msg {} into db", request.getMsgId(), request);
            failedMsgProducer.postFailedMsgToKafka(tenantId, request);
            e.printStackTrace();
        }
    }

    public void updateMsgWithProviderCallStatus(String tenantId, MessageRequest request) {
        try {
            MessageRequest finalMessage = request;
            UserMessagesEntity userMessagesEntity = userMessagesRepository.findById(request.getMsgId())
                    .orElseThrow(() -> {
                        failedMsgProducer.postFailedMsgToKafka(tenantId, finalMessage);
                        return new EntityNotFoundException("msgId", request.getMsgId() + " not present");
                    });
            UserMessagesInfoEntity userMessagesInfoEntity = userMessagesEntity.getUserMessagesInfo();
            userMessagesInfoEntity.setMsgStatus(request.getMsgStatus());
            userMessagesInfoEntity.setDlrSentOn(LocalDateTime.now(ZoneOffset.UTC));
            userMessagesEntity.setUserMessagesInfo(userMessagesInfoEntity);
            //userMessagesInfoEntity.setUpdatedOn(LocalDateTime.now(ZoneOffset.UTC));
            userMessagesRepository.save(userMessagesEntity);
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
            UserMessagesEntity userMessagesEntity = userMessagesRepository.findById(request.getMsgId())
                    .orElseThrow(() -> {
                        failedMsgProducer.postFailedMsgToKafka(tenantId, finalMessage);
                        return new EntityNotFoundException("msgId", "msgId :" + request.getMsgId() + " is not present");
                    });
            UserMessagesInfoEntity userMessagesInfoEntity = userMessagesEntity.getUserMessagesInfo();
            userMessagesInfoEntity.setMsgStatus(MsgStatus.DLR_CB_SUCCESS);

            UpdateMsgReq updateMsgReq = request.getUpdateMsgReq();
            String dlrStatusString = updateMsgReq.getStatusJson();
            userMessagesInfoEntity.setDlrDeliveredOn(convertToGMTLocalDateTime(extractFieldFromSource(dlrStatusString, "done date:([^\\s]+)").trim()));
            SMSStatus smsStatus = SMSStatus.fromValue(extractFieldFromSource(dlrStatusString, "stat:([^\\s]+)").trim());
            userMessagesInfoEntity.setDlrStatus(smsStatus);
            userMessagesInfoEntity.setDlrStatusCode(extractFieldFromSource(dlrStatusString, "err:([^\\s]+)").trim());
            userMessagesInfoEntity.setDlrStatusDescription(smsStatus.getDescription());

            userMessagesEntity.setUserMessagesInfo(userMessagesInfoEntity);
            userMessagesEntity = userMessagesRepository.save(userMessagesEntity);
            sendWebhookCall(tenantId, userMessagesEntity);
            log.info("msgId {} updated with status {} into db", request.getMsgId(), request.getMsgStatus());
        } catch (Exception e) {
            log.info("unable to update msgId {} with status {} into db", request.getMsgId(), request.getMsgStatus());
            failedMsgProducer.postFailedMsgToKafka(tenantId, request);
            e.printStackTrace();
        }
    }

    public void sendWebhookCall(String tenantId, UserMessagesEntity userMessagesEntity) {
        UserWiseWebhookEntity webhookEntity = userMessagesEntity.getCallBackWebhook();
        try {
            ResponseEntity<Object> response;
            if (webhookEntity.getCrmType() == CRMType.WEB_ENGAGE) {
                WebEngageWebHookReq req = new WebEngageWebHookReq();
                req.setVersion(userMessagesEntity.getWebEngageVersion());
                req.setMessageId(userMessagesEntity.getCrmMsgId());
                req.setToNumber(userMessagesEntity.getTo());
                SMSStatus smsStatus = userMessagesEntity.getUserMessagesInfo().getDlrStatus();
                if (SMSStatus.DELIVRD == smsStatus) {
                    req.setStatus("sms_sent");
                    req.setStatusCode(0);
                } else {
                    req.setStatus("sms_failed");
                    req.setStatusCode(9988);
                    req.setMessage(smsStatus.getDescription());
                }
                req.setContentTemplateId(userMessagesEntity.getTemplateId());
                req.setPrincipalEntityId(userMessagesEntity.getEntityId());
                response = restTemplate.postForEntity(webhookEntity.getWebhookUrl(), req, Object.class);
            } else {
                CustomerWebHookReq customerWebHookReq = commonMapper.userMessagesEntityToCustomerWebHookReq(userMessagesEntity);
                customerWebHookReq.setSuccess(customerWebHookReq.getStatus().equalsIgnoreCase("DELIVRD"));
                response = restTemplate.postForEntity(webhookEntity.getWebhookUrl(), customerWebHookReq, Object.class);
            }

            MessageWebhookStatus messageWebhookStatus = new MessageWebhookStatus();
            messageWebhookStatus.setUserMessagesEntity(userMessagesEntity);
            messageWebhookStatus.setWebhookId(webhookEntity.getWebhookId());
            messageWebhookStatus.setUserId(userMessagesEntity.getUser().getId());
            if (response.getStatusCode().is2xxSuccessful()) {
                messageWebhookStatus.setStatus(MsgWebhookStatus.SUCCESS);
            } else {
                messageWebhookStatus.setStatus(MsgWebhookStatus.FAILED);
            }
            messageWebhookStatus.setResponse(response.getStatusCode().toString());
            messageWebhookStatus.setRetryCount(1);
            messageWebhookStatusRepository.save(messageWebhookStatus);
            log.info("webhook url sent for msgId {} , with status {} ", userMessagesEntity.getId(), messageWebhookStatus.getStatus());

        } catch (Exception e) {
            //failedMsgProducer.postFailedMsgToKafka(tenantId, request);
            log.info("failure while sending customer webhook call for tenantId {}, message {}", tenantId, userMessagesEntity.toString());
            e.printStackTrace();
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

    private LocalDateTime convertToGMTLocalDateTime(String doneDate) {
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
            ZonedDateTime utcZonedDateTime = istZonedDateTime.withZoneSameInstant(ZoneId.of("UTC"));
            return utcZonedDateTime.toLocalDateTime(); // Return as LocalDateTime in UTC
        } catch (Exception e) {
            log.info("Error while converting doneDate {}", doneDate);
            return null;
        }
    }
}
