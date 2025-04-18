package com.gk.kannel.service.producers;

import com.gk.kannel.entities.UserWiseKafkaPartition;
import com.gk.kannel.model.MessageRequest;
import com.gk.kannel.repository.TenantToPartitionRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class UpdateMsgStatusProducer {

    @Autowired
    private KafkaTemplate<String, MessageRequest> kafkaTemplate;

    @Value("${sms.requests.topic}")
    private String smsRequestsTopic;

    @Autowired
    private TenantToPartitionRepository tenantToPartitionRepository;

    public void postUpdateMessageToKafka(String tenantId, MessageRequest messageRequest) {

        UserWiseKafkaPartition userWiseKafkaPartition = tenantToPartitionRepository.findByUser_Id(tenantId)
                .orElseThrow(() -> new RuntimeException("Partition not found for tenant: " + tenantId));
        ProducerRecord<String, MessageRequest> record = new ProducerRecord<>(
                smsRequestsTopic,
                userWiseKafkaPartition.getPartitionNum(), // partition
                tenantId,                             // key
                messageRequest                        // value
        );
        kafkaTemplate.send(record);
//        log.info(" msgId {} with msgStatus {} for tenantId {} pushed into kafka", messageRequest.getMsgId(), messageRequest.getMsgStatus(), tenantId);
//        ObjectMapper objectMapper = new ObjectMapper();
//        try {
//            log.info("key {} , message : {}", tenantId, objectMapper.writeValueAsString(messageRequest));
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }
}
