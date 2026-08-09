package org.swbe.domain.file.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.swbe.domain.file.entity.FileResource;

public interface FileResourceRepository
    extends JpaRepository<FileResource, Long> {
}
