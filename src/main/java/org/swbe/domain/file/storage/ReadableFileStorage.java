package org.swbe.domain.file.storage;

import org.springframework.core.io.Resource;

public interface ReadableFileStorage extends FileStorage {

  Resource load(String storageKey);
}
