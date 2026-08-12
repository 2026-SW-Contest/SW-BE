package org.swbe.domain.lostitem.service;

import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.swbe.domain.campus.entity.Building;
import org.swbe.domain.campus.entity.Location;
import org.swbe.domain.lostitem.dto.response.LostItemOfficeListResponse;
import org.swbe.domain.lostitem.dto.response.LostItemOfficeResponse;
import org.swbe.domain.lostitem.entity.LostItemOffice;
import org.swbe.domain.lostitem.repository.LostItemOfficeRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LostItemOfficeQueryService {

  private final LostItemOfficeRepository officeRepository;

  public LostItemOfficeListResponse getOffices() {
    List<LostItemOfficeResponse> offices = officeRepository
        .findAllByActiveTrueAndBuilding_ActiveTrueAndLocation_ActiveTrue()
        .stream()
        .sorted(Comparator
            .comparingInt((LostItemOffice office) ->
                office.getBuilding().displayOrder())
            .thenComparing(Comparator.comparing(
                LostItemOffice::isPrimary
            ).reversed())
            .thenComparing(LostItemOffice::getName))
        .map(this::toResponse)
        .toList();

    return new LostItemOfficeListResponse(offices);
  }

  private LostItemOfficeResponse toResponse(LostItemOffice office) {
    Building building = office.getBuilding();
    Location location = office.getLocation();
    return new LostItemOfficeResponse(
        office.getId(),
        office.getName(),
        building.getId(),
        building.getCode(),
        building.getName(),
        location.getId(),
        location.getName(),
        location.getFloor(),
        office.getDepartment().getName(),
        office.getOperatingHours(),
        office.getGuidance(),
        office.isPrimary()
    );
  }
}
