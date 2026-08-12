package org.swbe.global.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.swbe.domain.facilityrequest.entity.QFacilityRequest;
import org.swbe.domain.lostitem.entity.QStoredItem;

class QuerydslSetupTest {

  @Test
  void generatesQueryTypesForSearchEntities() {
    assertNotNull(QStoredItem.storedItem);
    assertNotNull(QFacilityRequest.facilityRequest);
  }
}
