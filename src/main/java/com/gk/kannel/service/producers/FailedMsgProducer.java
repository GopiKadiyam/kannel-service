package com.gk.kannel.service.producers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gk.kannel.entities.TenantToPartition;
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
public class FailedMsgProducer {
    @Autowired
    private KafkaTemplate<String, MessageRequest> kafkaTemplate;

    @Value("${failed.sms.requests.topic}")
    private String failedMsgRequestsTopic;

    @Autowired
    private TenantToPartitionRepository tenantToPartitionRepository;

    public void postFailedMsgToKafka(String tenantId, MessageRequest messageRequest) {
        TenantToPartition tenantToPartition = tenantToPartitionRepository.findByUser_Id(tenantId)
                .orElseThrow(() -> new RuntimeException("Partition not found for tenant: " + tenantId));

        ProducerRecord<String, MessageRequest> record = new ProducerRecord<>(
                failedMsgRequestsTopic,
                tenantToPartition.getPartitionNum(), // partition
                tenantId,                             // key
                messageRequest                        // value
        );

        kafkaTemplate.send(record);
        log.info(" msgId {} with msgStatus {} for tenantId {} pushed into kafka", messageRequest.getMsgId(), messageRequest.getMsgStatus(), tenantId);
//        ObjectMapper objectMapper = new ObjectMapper();
//        try {
//            log.info("key {} , message : {}", tenantId, objectMapper.writeValueAsString(messageRequest));
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }
}
