package com.gk.kannel.entities;

import com.gk.kannel.config.converters.MetadataConverter;
import com.gk.kannel.utils.enums.CRMType;
import com.gk.kannel.utils.enums.Country;
import com.gk.kannel.utils.enums.MessageType;
import com.gk.kannel.utils.enums.ServiceType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_msg_requests")
public class UserMsgReqEntity {
    @Id
    @Column(name = "id", updatable = false, nullable = false, length = 36)
    private String id;
    @Enumerated(EnumType.STRING)
    private Country country;
    @Enumerated(EnumType.STRING)
    private ServiceType serviceType;
    @Column(name = "sms_from")
    private String from;
    @Column(name = "sms_to")
    private String to;
    private String body;
    @Column(name = "template_id", length = 19)
    private String templateId;
    @Column(name = "entity_id", length = 19)
    private String entityId;
    @Enumerated(EnumType.STRING)
    private MessageType messageType;
    private String customId;
    @Convert(converter = MetadataConverter.class)
    @Column(columnDefinition = "json") // use "json" or "text" depending on DB
    private Map<String, String> metadata;
    private boolean flash;
    @ManyToOne
    @JoinColumn(name = "webhook_id")
    private UserWiseWebhookRegistryEntity callBackWebhook;
    private Integer smsLength;
    private Integer credits;
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccountEntity user;
    private String crmMsgId;
    private String webEngageVersion;
    @Enumerated(EnumType.STRING)
    private CRMType crmMsgType;
    private String msgGroupId;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, mappedBy = "userMessage")
    private UserMsgReqStatusEntity userMessagesInfo;
}
