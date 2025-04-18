package com.gk.kannel.repository;

import com.gk.kannel.entities.UserAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserAccountEntity,String> {
    Optional<UserAccountEntity> findByUsername(String userName);
}
