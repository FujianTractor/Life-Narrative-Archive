package com.lifenarrative.archive.repository;

import com.lifenarrative.archive.entity.ArchiveEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ArchiveRepository extends JpaRepository<ArchiveEntity, String> {

    List<ArchiveEntity> findAllByOrderByUpdatedAtDesc();

    List<ArchiveEntity> findByUserIdOrderByUpdatedAtDesc(String userId);

    Optional<ArchiveEntity> findByIdAndUserId(String id, String userId);
}