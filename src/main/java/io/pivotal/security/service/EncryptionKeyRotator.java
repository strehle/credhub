package io.pivotal.security.service;

import io.pivotal.security.data.EncryptedValueDataService;
import io.pivotal.security.data.EncryptionKeyCanaryDataService;
import io.pivotal.security.entity.EncryptedValue;
import io.pivotal.security.exceptions.KeyNotFoundException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class EncryptionKeyRotator {

  private final EncryptedValueDataService encryptedValueDataService;
  private final Logger logger;
  private final EncryptionKeyCanaryMapper encryptionKeyCanaryMapper;
  private final EncryptionKeyCanaryDataService encryptionKeyCanaryDataService;

  @Autowired
  EncryptionKeyRotator(
      EncryptedValueDataService encryptedValueDataService,
      EncryptionKeyCanaryMapper encryptionKeyCanaryMapper,
      EncryptionKeyCanaryDataService encryptionKeyCanaryDataService
  ) {
    this.encryptedValueDataService = encryptedValueDataService;
    this.logger = LogManager.getLogger(this.getClass());
    this.encryptionKeyCanaryMapper = encryptionKeyCanaryMapper;
    this.encryptionKeyCanaryDataService = encryptionKeyCanaryDataService;
  }

  public void rotate() {
    final long start = System.currentTimeMillis();
    logger.info("Starting encryption key rotation.");
    int rotatedRecordCount = 0;

    final long startingNotRotatedRecordCount = encryptedValueDataService.countAllNotEncryptedByActiveKey();

    Slice<EncryptedValue> valuesEncryptedByOldKey = encryptedValueDataService
        .findEncryptedWithAvailableInactiveKey();
    while (valuesEncryptedByOldKey.hasContent()) {
      for (EncryptedValue value : valuesEncryptedByOldKey.getContent()) {
        try {
          encryptedValueDataService.rotate(value);
          rotatedRecordCount++;
        } catch (KeyNotFoundException e) {
          logger.error("key not found for value, unable to rotate");
        }
      }
      valuesEncryptedByOldKey = encryptedValueDataService.findEncryptedWithAvailableInactiveKey();
    }

    final long finish = System.currentTimeMillis();
    final long duration = finish - start;
    final long endingNotRotatedRecordCount = startingNotRotatedRecordCount - rotatedRecordCount;

    if (rotatedRecordCount == 0 && endingNotRotatedRecordCount == 0) {
      logger.info("Found no records in need of encryption key rotation.");
    } else {
      logger.info("Finished encryption key rotation in " + duration + " milliseconds. Details:");
      logger.info("  Successfully rotated " + rotatedRecordCount + " item(s)");
      logger.info("  Skipped " + endingNotRotatedRecordCount
          + " item(s) due to missing master encryption key(s).");
    }

    deleteKnownAndInactiveCanaries();
  }

  private void deleteKnownAndInactiveCanaries() {
    List<UUID> knownAndInactiveKeys = encryptionKeyCanaryMapper.getCanaryUuidsWithKnownAndInactiveKeys();
    encryptionKeyCanaryDataService.delete(knownAndInactiveKeys);
  }
}
