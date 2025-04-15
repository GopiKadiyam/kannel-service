package com.gk.kannel.repository;

import com.gk.kannel.entities.ServiceTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceTypeRepository extends JpaRepository<ServiceTypeEntity,Long> {

    Optional<ServiceTypeEntity> findByName(String serviceType);
    List<ServiceTypeEntity> findAllByNameIn(List<String> serviceNames);
}
