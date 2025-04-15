package com.gk.kannel.repository;

import com.gk.kannel.entities.SMSCEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SMSCRepository extends JpaRepository<SMSCEntity,Long> {
    List<SMSCEntity> findAllByNameIn(List<String> smscList);
}
