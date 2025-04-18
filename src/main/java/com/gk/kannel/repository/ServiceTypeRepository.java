package com.gk.kannel.repository;

import com.gk.kannel.entities.MsgServiceTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceTypeRepository extends JpaRepository<MsgServiceTypeEntity,Long> {

    Optional<MsgServiceTypeEntity> findByName(String serviceType);
    List<MsgServiceTypeEntity> findAllByNameIn(List<String> serviceNames);
}
