package com.lifenarrative.archive.repository;

import com.lifenarrative.archive.entity.ArchiveEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArchiveRepository extends JpaRepository<ArchiveEntity, String> {

    List<ArchiveEntity> findAllByOrderByUpdatedAtDesc();
}