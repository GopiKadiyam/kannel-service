package com.gk.kannel.repository;

import com.gk.kannel.entities.UserWiseServicePermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserWiseServiceTypeRepository extends JpaRepository<UserWiseServicePermissionEntity,Long> {
}
