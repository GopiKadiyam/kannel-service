package com.gk.kannel.repository;

import com.gk.kannel.entities.UserMsgReqStatusEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserMessagesInfoRepository extends JpaRepository<UserMsgReqStatusEntity,Long> {
}
