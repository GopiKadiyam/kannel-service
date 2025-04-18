package com.gk.kannel.repository;

import com.gk.kannel.entities.UserMsgReqEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserMessagesRepository extends JpaRepository<UserMsgReqEntity, String> {
}
