package org.swbe.domain.file.storage;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorage {

  String provider();

  StoredFile store(MultipartFile file);

  void delete(String storageKey);
}
