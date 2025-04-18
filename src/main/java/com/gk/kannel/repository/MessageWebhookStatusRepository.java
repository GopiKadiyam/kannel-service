package com.gk.kannel.repository;

import com.gk.kannel.entities.WebhookCallbackStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageWebhookStatusRepository  extends JpaRepository<WebhookCallbackStatus,Long> {
}
