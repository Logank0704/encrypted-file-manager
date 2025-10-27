package com.kc.encrypted.efm.repository;

import com.kc.encrypted.efm.domain.StoredFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface StoredFileRepository extends JpaRepository<StoredFile, UUID> { }