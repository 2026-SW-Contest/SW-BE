package org.swbe.domain.lostitem.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.swbe.domain.file.entity.FileResource;
import org.swbe.domain.file.service.PrivateFileUrlResolver;
import org.swbe.domain.lostitem.dto.request.ItemClaimSearchCondition;
import org.swbe.domain.lostitem.entity.ClaimStatusHistory;
import org.swbe.domain.lostitem.entity.ItemClaim;
import org.swbe.domain.lostitem.entity.ItemClaimAttachment;
import org.swbe.domain.lostitem.entity.ItemClaimStatus;
import org.swbe.domain.lostitem.entity.LostItemOffice;
import org.swbe.domain.lostitem.entity.StoredItem;
import org.swbe.domain.lostitem.entity.TemporaryClaimant;
import org.swbe.domain.lostitem.exception.ItemClaimErrorCode;
import org.swbe.domain.lostitem.repository.ClaimStatusHistoryRepository;
import org.swbe.domain.lostitem.repository.ItemClaimAttachmentRepository;
import org.swbe.domain.lostitem.repository.ItemClaimRepository;
import org.swbe.domain.lostitem.repository.LostItemOfficeRepository;
import org.swbe.domain.lostitem.repository.OfficeStaffAssignmentRepository;
import org.swbe.domain.lostitem.repository.StoredItemRepository;
import org.swbe.domain.user.entity.AppUser;
import org.swbe.global.error.BusinessException;

class ItemClaimQueryServiceTest {

  private StoredItemRepository storedItemRepository;
  private ItemClaimRepository itemClaimRepository;
  private ItemClaimAttachmentRepository attachmentRepository;
  private ClaimStatusHistoryRepository historyRepository;
  private LostItemOfficeRepository officeRepository;
  private OfficeStaffAssignmentRepository assignmentRepository;
  private ItemClaimThumbnailService thumbnailService;
  private PrivateFileUrlResolver privateFileUrlResolver;
  private ItemClaimQueryService service;
  private StoredItem storedItem;

  @BeforeEach
  void setUp() {
    storedItemRepository = mock(StoredItemRepository.class);
    itemClaimRepository = mock(ItemClaimRepository.class);
    attachmentRepository = mock(ItemClaimAttachmentRepository.class);
    historyRepository = mock(ClaimStatusHistoryRepository.class);
    officeRepository = mock(LostItemOfficeRepository.class);
    assignmentRepository = mock(OfficeStaffAssignmentRepository.class);
    thumbnailService = mock(ItemClaimThumbnailService.class);
    privateFileUrlResolver = mock(PrivateFileUrlResolver.class);
    storedItem = storedItem();
    when(storedItemRepository.findDetailById(25L))
        .thenReturn(Optional.of(storedItem));
    when(officeRepository.existsById(3L)).thenReturn(true);
    when(assignmentRepository
        .existsByOffice_IdAndUser_IdAndEndedAtIsNull(3L, 7L))
        .thenReturn(true);
    service = new ItemClaimQueryService(
        storedItemRepository,
        itemClaimRepository,
        attachmentRepository,
        historyRepository,
        officeRepository,
        assignmentRepository,
        thumbnailService,
        privateFileUrlResolver
    );
  }

  @Test
  void assignedStaffGetsStoredItemClaimPage() {
    ItemClaim claim = memberClaim(
        31L,
        LocalDateTime.of(2026, 8, 12, 15, 0),
        "정석우",
        "60251423"
    );
    when(itemClaimRepository.findAllByStoredItemId(
        eq(25L),
        eq(ItemClaimStatus.WAITING),
        any(Pageable.class)
    )).thenReturn(new PageImpl<>(
        List.of(claim),
        PageRequest.of(0, 1),
        2
    ));
    when(thumbnailService.resolveAll(List.of(31L)))
        .thenReturn(Map.of(
            31L,
            new ItemClaimAttachmentSummary("https://cdn/proof.jpg", 2)
        ));

    var response = service.getItemClaims(
        25L,
        new ItemClaimSearchCondition(ItemClaimStatus.WAITING, 0, 1),
        7L,
        false
    );

    assertThat(response.data().content()).singleElement()
        .satisfies(item -> {
          assertThat(item.itemClaimId()).isEqualTo(31L);
          assertThat(item.claimantName()).isEqualTo("정석우");
          assertThat(item.studentNumber()).isEqualTo("60251423");
          assertThat(item.thumbnailUrl()).isEqualTo("https://cdn/proof.jpg");
          assertThat(item.attachmentCount()).isEqualTo(2);
        });
    assertThat(response.data().page()).isZero();
    assertThat(response.data().size()).isEqualTo(1);
    assertThat(response.data().totalElements()).isEqualTo(2);
    assertThat(response.data().totalPages()).isEqualTo(2);
    assertThat(response.data().hasNext()).isTrue();
  }

  @Test
  void assignedStaffGetsOfficeClaimPage() {
    ItemClaim claim = memberClaim(
        31L,
        LocalDateTime.of(2026, 8, 12, 15, 0),
        "정석우",
        "60251423"
    );
    when(itemClaimRepository.findAllByOfficeId(
        eq(3L),
        eq(ItemClaimStatus.WAITING),
        any(Pageable.class)
    )).thenReturn(new PageImpl<>(
        List.of(claim),
        PageRequest.of(1, 20),
        21
    ));
    when(thumbnailService.resolveAll(List.of(31L)))
        .thenReturn(Map.of());

    var response = service.getOfficeItemClaims(
        3L,
        new ItemClaimSearchCondition(ItemClaimStatus.WAITING, 1, 20),
        7L,
        false
    );

    assertThat(response.data().content()).singleElement()
        .satisfies(item -> {
          assertThat(item.itemClaimId()).isEqualTo(31L);
          assertThat(item.storedItemId()).isEqualTo(25L);
          assertThat(item.itemName()).isEqualTo("검은색 반지갑");
        });
    assertThat(response.data().page()).isEqualTo(1);
    assertThat(response.data().size()).isEqualTo(20);
    assertThat(response.data().totalElements()).isEqualTo(21);
    assertThat(response.data().totalPages()).isEqualTo(2);
    assertThat(response.data().hasNext()).isFalse();
    ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
    verify(itemClaimRepository).findAllByOfficeId(
        eq(3L),
        eq(ItemClaimStatus.WAITING),
        captor.capture()
    );
    assertThat(captor.getValue().getPageNumber()).isEqualTo(1);
    assertThat(captor.getValue().getPageSize()).isEqualTo(20);
  }

  @Test
  void pageSupportsTemporaryClaimantWithoutAttachments() {
    ItemClaim claim = temporaryClaim(31L);
    when(itemClaimRepository.findAllByStoredItemId(
        eq(25L),
        eq(null),
        any(Pageable.class)
    )).thenReturn(new PageImpl<>(List.of(claim)));
    when(thumbnailService.resolveAll(List.of(31L))).thenReturn(Map.of());

    var response = service.getItemClaims(
        25L,
        new ItemClaimSearchCondition(null, 0, 20),
        7L,
        false
    );

    var item = response.data().content().getFirst();
    assertThat(item.claimantName()).isEqualTo("임시 신청자");
    assertThat(item.studentNumber()).isEqualTo("60259999");
    assertThat(item.thumbnailUrl()).isNull();
    assertThat(item.attachmentCount()).isZero();
  }

  @Test
  void missingOfficeReturnsNotFound() {
    when(officeRepository.existsById(99L)).thenReturn(false);

    assertBusinessError(
        () -> service.getOfficeItemClaims(
            99L,
            new ItemClaimSearchCondition(null, 0, 20),
            7L,
            false
        ),
        ItemClaimErrorCode.OFFICE_NOT_FOUND
    );
    verify(assignmentRepository, never())
        .existsByOffice_IdAndUser_IdAndEndedAtIsNull(any(), any());
    verify(itemClaimRepository, never())
        .findAllByOfficeId(any(), any(), any());
  }

  @Test
  void staffWithoutOfficeAssignmentIsDenied() {
    when(assignmentRepository
        .existsByOffice_IdAndUser_IdAndEndedAtIsNull(3L, 7L))
        .thenReturn(false);

    assertBusinessError(
        () -> service.getItemClaims(
            25L,
            new ItemClaimSearchCondition(null, 0, 20),
            7L,
            false
        ),
        ItemClaimErrorCode.ACCESS_DENIED
    );
    assertBusinessError(
        () -> service.getOfficeItemClaims(
            3L,
            new ItemClaimSearchCondition(null, 0, 20),
            7L,
            false
        ),
        ItemClaimErrorCode.ACCESS_DENIED
    );
    verify(itemClaimRepository, never())
        .findAllByStoredItemId(any(), any(), any());
    verify(itemClaimRepository, never())
        .findAllByOfficeId(any(), any(), any());
  }

  @Test
  void adminReadsPagesWithoutOfficeAssignment() {
    when(itemClaimRepository.findAllByStoredItemId(
        eq(25L), eq(null), any(Pageable.class)
    )).thenReturn(new PageImpl<>(List.of()));
    when(itemClaimRepository.findAllByOfficeId(
        eq(3L), eq(null), any(Pageable.class)
    )).thenReturn(new PageImpl<>(List.of()));

    service.getItemClaims(
        25L,
        new ItemClaimSearchCondition(null, 0, 20),
        7L,
        true
    );
    service.getOfficeItemClaims(
        3L,
        new ItemClaimSearchCondition(null, 0, 20),
        7L,
        true
    );

    verify(assignmentRepository, never())
        .existsByOffice_IdAndUser_IdAndEndedAtIsNull(any(), any());
  }

  @Test
  void detailContainsAllAttachmentsAndStatusHistories() {
    ItemClaim claim = memberClaim(
        31L,
        LocalDateTime.of(2026, 8, 12, 15, 0),
        "정석우",
        "60251423"
    );
    ItemClaimAttachment attachment = mock(ItemClaimAttachment.class);
    FileResource file = mock(FileResource.class);
    ClaimStatusHistory history = mock(ClaimStatusHistory.class);
    AppUser changer = mock(AppUser.class);
    when(claim.getOwnershipDescription()).thenReturn("소유 증명 설명");
    when(claim.getUpdatedAt())
        .thenReturn(LocalDateTime.of(2026, 8, 12, 16, 0));
    when(itemClaimRepository.findDetailById(31L))
        .thenReturn(Optional.of(claim));
    when(attachment.getFile()).thenReturn(file);
    when(file.getId()).thenReturn(91L);
    when(file.getOriginalFilename()).thenReturn("proof.jpg");
    when(privateFileUrlResolver.resolve(file))
        .thenReturn("https://cdn/proof.jpg");
    when(attachmentRepository.findPublicImagesByItemClaimId(31L))
        .thenReturn(List.of(attachment));
    when(history.getId()).thenReturn(41L);
    when(history.getNewStatus()).thenReturn(ItemClaimStatus.WAITING);
    when(history.getChangedBy()).thenReturn(changer);
    when(changer.getName()).thenReturn("정석우");
    when(history.getChangedAt())
        .thenReturn(LocalDateTime.of(2026, 8, 12, 15, 0));
    when(historyRepository.findAllByItemClaimId(31L))
        .thenReturn(List.of(history));

    var response = service.getItemClaim(31L, 7L, false);

    assertThat(response.data().storedItemId()).isEqualTo(25L);
    assertThat(response.data().ownershipDescription())
        .isEqualTo("소유 증명 설명");
    assertThat(response.data().attachments()).hasSize(1);
    assertThat(response.data().statusHistories()).hasSize(1);
  }

  @Test
  void missingClaimReturnsNotFound() {
    when(itemClaimRepository.findDetailById(99L))
        .thenReturn(Optional.empty());

    assertBusinessError(
        () -> service.getItemClaim(99L, 7L, false),
        ItemClaimErrorCode.NOT_FOUND
    );
  }

  private StoredItem storedItem() {
    StoredItem item = mock(StoredItem.class);
    LostItemOffice office = mock(LostItemOffice.class);
    when(item.getId()).thenReturn(25L);
    when(item.getItemName()).thenReturn("검은색 반지갑");
    when(item.getOffice()).thenReturn(office);
    when(office.getId()).thenReturn(3L);
    return item;
  }

  private ItemClaim memberClaim(
      Long id,
      LocalDateTime createdAt,
      String name,
      String studentNumber
  ) {
    ItemClaim claim = mock(ItemClaim.class);
    AppUser claimant = mock(AppUser.class);
    when(claim.getId()).thenReturn(id);
    when(claim.getStoredItem()).thenReturn(storedItem);
    when(claim.getClaimantUser()).thenReturn(claimant);
    when(claimant.getName()).thenReturn(name);
    when(claimant.getStudentNumber()).thenReturn(studentNumber);
    when(claim.getRequestMethod()).thenReturn("ONLINE");
    when(claim.getClaimStatus()).thenReturn(ItemClaimStatus.WAITING);
    when(claim.getCreatedAt()).thenReturn(createdAt);
    return claim;
  }

  private ItemClaim temporaryClaim(Long id) {
    ItemClaim claim = mock(ItemClaim.class);
    TemporaryClaimant claimant = mock(TemporaryClaimant.class);
    when(claim.getId()).thenReturn(id);
    when(claim.getStoredItem()).thenReturn(storedItem);
    when(claim.getTemporaryClaimant()).thenReturn(claimant);
    when(claimant.getName()).thenReturn("임시 신청자");
    when(claimant.getStudentNumber()).thenReturn("60259999");
    when(claim.getRequestMethod()).thenReturn("ON_SITE_TEMPORARY");
    when(claim.getClaimStatus()).thenReturn(ItemClaimStatus.WAITING);
    when(claim.getCreatedAt())
        .thenReturn(LocalDateTime.of(2026, 8, 12, 15, 0));
    return claim;
  }

  private void assertBusinessError(
      Runnable action,
      ItemClaimErrorCode expectedError
  ) {
    assertThatThrownBy(action::run)
        .isInstanceOf(BusinessException.class)
        .extracting(exception ->
            ((BusinessException) exception).getErrorCode()
        )
        .isEqualTo(expectedError);
  }
}
