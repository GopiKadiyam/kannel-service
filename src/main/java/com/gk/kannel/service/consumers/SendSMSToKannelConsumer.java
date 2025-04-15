package com.gk.kannel.service.consumers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gk.kannel.entities.SMSCEntity;
import com.gk.kannel.entities.UserEntity;
import com.gk.kannel.entities.UserWiseServiceTypeEntity;
import com.gk.kannel.exception.EntityNotFoundException;
import com.gk.kannel.exception.InvalidRequestException;
import com.gk.kannel.model.MessageRequest;
import com.gk.kannel.repository.UserRepository;
import com.gk.kannel.service.AsyncOperations;
import com.gk.kannel.service.producers.FailedMsgProducer;
import com.gk.kannel.service.producers.UpdateMsgStatusProducer;
import com.gk.kannel.utils.enums.KafkaMsgType;
import com.gk.kannel.utils.enums.MsgStatus;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.stream.Collectors;

import static com.gk.kannel.utils.common.CommonUtils.encodeToSHA256;
import static com.gk.kannel.utils.common.CommonUtils.encodeURL;

@Component
@Slf4j
public class SendSMSToKannelConsumer {

    @Value("${kannel.username}")
    private String kannelUsername;
    @Value("${kannel.password}")
    private String kannelPassword;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UpdateMsgStatusProducer updateMsgStatusProducer;
    @Autowired
    private AsyncOperations asyncOperations;
    @Autowired
    private FailedMsgProducer failedMsgProducer;
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private ObjectMapper objectMapper;
    @Value("${kannel.base.url}")
    private String kannelBaseUrl;
    @Value("${is.test}")
    private boolean isTest;

    @KafkaListener(topics = "${sms.requests.topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeMessage(ConsumerRecord<String, Object> record) {
        processMessageToKannel(record.key(), record.value());
    }

    //    @Transactional
    public void processMessageToKannel(String tenantId, Object msg) {
        MessageRequest request = (MessageRequest) msg;
        if (KafkaMsgType.INSERT_MSG == request.getKafkaMsgType()) {
            asyncOperations.createMsgReq(tenantId, request);
            UserEntity userEntity = userRepository.findById(request.getTenantId())
                    .orElseThrow(() -> new EntityNotFoundException("tenantId", "tenantId " + tenantId + " not found"));
            UserWiseServiceTypeEntity uwste = userEntity.getUserServices().stream()
                    .filter(us -> us.getServiceType().getName().equalsIgnoreCase(request.getServiceType().getValue()))
                    .findFirst()
                    .orElseThrow(() -> new InvalidRequestException("serviceType", "TenantId (" + tenantId + ") is not supported serviceType - " + request.getServiceType()));
            SMSCEntity smscEntity = uwste.getSmsc();
//            String kannelUrl = "http://localhost:13013/cgi-bin/sendsms" +
//                    "?username=" + kannelUsername + "&password=" + kannelPassword +
//                    "&smsc=" + smscEntity.getName() + "&text=" + request.getBody() + "&from=" + request.getFrom() +
//                    "&charset=utf-8&dlr-mask=19&dlr-url=" + encodeURL(getDlrUrl(request.getMsgId(), tenantId)) +
//                    "&coding=" + request.getMessageType().getKennelValue() +
//                    "&meta-data=" + encodeURL("?smpp?&PE_ID=" + request.getEntityId() + "&Template_ID=" + request.getTemplateId() + "&TM_ID=" + getTmId(smscEntity.isActiveFlag(), smscEntity.isEncryptionFlag(), request.getEntityId(), smscEntity.getTelemarketerId())) +
//                    "&to=" + request.getTo();
            UriComponentsBuilder builder;
            ResponseEntity<String> response = null;
            try {
                builder = UriComponentsBuilder.fromHttpUrl(kannelBaseUrl)
                        .queryParam("username", kannelUsername)
                        .queryParam("password", kannelPassword)
                        .queryParam("text", encodeURL(request.getBody()))
                        .queryParam("from", request.getFrom())
                        .queryParam("charset", "utf-8")
                        .queryParam("dlr-mask", "19")
                        .queryParam("dlr-url", encodeURL(getDlrUrl(request.getMsgId(), tenantId)))  // raw URL
                        .queryParam("coding", request.getMessageType().getKennelValue())
                        .queryParam("to", request.getTo());
                if (isTest) {
                    builder.queryParam("meta-data", encodeURL("?smpp?"));  // raw metadata
                    builder.queryParam("smsc", "UATB");
                } else {
                    builder.queryParam("smsc", smscEntity.getName());
                    builder.queryParam("meta-data", encodeURL("?smpp?&PE_ID=" + request.getEntityId() + "&Template_ID=" + request.getTemplateId() + "&TM_ID=" + getTmId(smscEntity.isActiveFlag(), smscEntity.isEncryptionFlag(), request.getEntityId(), smscEntity.getTelemarketerId())));  // raw metadata
                }
                if (request.isFlash()) {
                    builder.queryParam("mclass", -1);
                }
                URI uri = builder.build(true).toUri();  // `true` = don’t re-encode
                log.debug("kannelUrl : {}", uri);
                response = restTemplate.getForEntity(uri, String.class);
                log.debug("kannel response {}", objectMapper.writeValueAsString(response));
            } catch (Exception e) {
                request.setMsgStatus(MsgStatus.UNABLE_TO_SENT_TO_PROVIDER);
                failedMsgProducer.postFailedMsgToKafka(tenantId, request);
                e.printStackTrace();
            }
            try {
                MessageRequest updateMsgRequest = new MessageRequest();
                updateMsgRequest.setMsgId(request.getMsgId());
                if (response.getStatusCode().is2xxSuccessful()) {
                    updateMsgRequest.setMsgStatus(MsgStatus.SENT_TO_PROVIDER);
                    log.info("SENT_MSG_TO_KANNEL for msgId {}", request.getMsgId());
                } else {
                    updateMsgRequest.setMsgStatus(MsgStatus.FAILED_TO_SEND_PROVIDER);
                    log.info("FAILED_TO_SEND_MSG_TO_KANNEL for msgId {}", request.getMsgId());
                    //TODO add failed msg column to table
                }
                updateMsgRequest.setKafkaMsgType(KafkaMsgType.UPDATE_MSG);
                updateMsgRequest.setTenantId(request.getTenantId());
                updateMsgStatusProducer.postUpdateMessageToKafka(request.getTenantId(), updateMsgRequest);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (KafkaMsgType.UPDATE_MSG == request.getKafkaMsgType()) {
            if (MsgStatus.SENT_TO_PROVIDER == request.getMsgStatus() || MsgStatus.FAILED_TO_SEND_PROVIDER == request.getMsgStatus()) {
                asyncOperations.updateMsgWithProviderCallStatus(tenantId, request);
            } else if (MsgStatus.DLR_CB_SUCCESS == request.getMsgStatus()) {
                asyncOperations.updateMsgWithDLCBSuccess(tenantId, request);
            }
        }
        log.info("processed kafka message");
    }

    private String getDlrUrl(String msgId, String tenantId) {
        return "http://localhost:8077/sms/status/update?status=%A&type=%d&pid=%F&smscid=%i&tm=%T&mid=" + msgId + "&tenid=" + tenantId;
    }

    private String getTmId(boolean activeFlag, boolean encryptionFlag, String entityId, String teleMarketerId) {
        return (activeFlag && encryptionFlag) ? encodeToSHA256(entityId + "," + teleMarketerId) : (entityId + "," + teleMarketerId);
    }

}
