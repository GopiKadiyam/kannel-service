package com.gk.kannel.entities;

import com.gk.kannel.utils.enums.MsgWebhookStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "webhook_callback_status")
public class WebhookCallbackStatus {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name = "message_id")
    private UserMsgReqEntity userMsgReqEntity;
    @Column(name = "user_id", nullable = false)
    private String userId;
    private String webhookId;
    @Enumerated(EnumType.STRING)
    private MsgWebhookStatus status;
    private String response;
    private int retryCount;
}
