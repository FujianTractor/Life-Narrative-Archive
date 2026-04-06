package com.lifenarrative.archive.repository;

import com.lifenarrative.archive.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, String> {

    Optional<UserEntity> findByUsername(String username);

    boolean existsByUsername(String username);
}
