package com.gk.kannel.repository;

import com.gk.kannel.entities.TenantToPartition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TenantToPartitionRepository extends JpaRepository<TenantToPartition,Long> {
    Optional<TenantToPartition> findByUser_Id(String userId);
}
