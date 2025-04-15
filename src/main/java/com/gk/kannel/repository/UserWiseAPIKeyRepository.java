package com.gk.kannel.repository;

import com.gk.kannel.entities.UserWiseAPIKeyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserWiseAPIKeyRepository extends JpaRepository<UserWiseAPIKeyEntity,Long> {

    boolean existsByApiKey(String apiKey);

}
