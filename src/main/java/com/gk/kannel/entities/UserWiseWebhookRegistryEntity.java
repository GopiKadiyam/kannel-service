package com.gk.kannel.entities;

import com.gk.kannel.utils.enums.CRMType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_wise_webhook_registry")
public class UserWiseWebhookRegistryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotEmpty
    private String webhookId;
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccountEntity user;
    @NotEmpty
    private String webhookUrl;
    @NotNull
    private boolean activeFlag;
    @Enumerated(EnumType.STRING)
    private CRMType crmType;
//    @OneToMany(mappedBy = "callBackWebhook", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
//    private List<UserMessagesEntity> messages = new ArrayList<>();
}
