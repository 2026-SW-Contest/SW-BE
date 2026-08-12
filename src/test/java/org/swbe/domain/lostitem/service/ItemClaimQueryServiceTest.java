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
import org.springframework.data.domain.Pageable;
import org.swbe.domain.file.entity.FileResource;
import org.swbe.domain.file.service.PrivateFileUrlResolver;
import org.swbe.domain.lostitem.cursor.ItemClaimCursorCodec;
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
import org.swbe.domain.lostitem.repository.OfficeStaffAssignmentRepository;
import org.swbe.domain.lostitem.repository.StoredItemRepository;
import org.swbe.domain.user.entity.AppUser;
import org.swbe.global.error.BusinessException;

class ItemClaimQueryServiceTest {

  private StoredItemRepository storedItemRepository;
  private ItemClaimRepository itemClaimRepository;
  private ItemClaimAttachmentRepository attachmentRepository;
  private ClaimStatusHistoryRepository historyRepository;
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
    assignmentRepository = mock(OfficeStaffAssignmentRepository.class);
    thumbnailService = mock(ItemClaimThumbnailService.class);
    privateFileUrlResolver = mock(PrivateFileUrlResolver.class);
    storedItem = storedItem();
    when(storedItemRepository.findDetailById(25L))
        .thenReturn(Optional.of(storedItem));
    when(assignmentRepository
        .existsByOffice_IdAndUser_IdAndEndedAtIsNull(3L, 7L))
        .thenReturn(true);
    service = new ItemClaimQueryService(
        storedItemRepository,
        itemClaimRepository,
        attachmentRepository,
        historyRepository,
        assignmentRepository,
        thumbnailService,
        privateFileUrlResolver,
        new ItemClaimCursorCodec()
    );
  }

  @Test
  void assignedStaffGetsFilteredClaimsWithThumbnailAndCursor() {
    ItemClaim first = memberClaim(
        31L,
        LocalDateTime.of(2026, 8, 12, 15, 0),
        "정석우",
        "60251423"
    );
    ItemClaim second = memberClaim(
        30L,
        LocalDateTime.of(2026, 8, 12, 14, 0),
        "홍길동",
        "60250001"
    );
    when(itemClaimRepository.findAllByCursor(
        eq(25L),
        eq(ItemClaimStatus.WAITING),
        eq(null),
        eq(null),
        any(Pageable.class)
    )).thenReturn(List.of(first, second));
    when(thumbnailService.resolveAll(List.of(31L)))
        .thenReturn(Map.of(
            31L,
            new ItemClaimAttachmentSummary("https://cdn/proof.jpg", 2)
        ));

    var response = service.getItemClaims(
        25L,
        new ItemClaimSearchCondition(
            ItemClaimStatus.WAITING,
            null,
            1
        ),
        7L,
        false
    );

    assertThat(response.data().content()).hasSize(1);
    var item = response.data().content().getFirst();
    assertThat(item.itemClaimId()).isEqualTo(31L);
    assertThat(item.claimantName()).isEqualTo("정석우");
    assertThat(item.studentNumber()).isEqualTo("60251423");
    assertThat(item.claimStatus()).isEqualTo("WAITING");
    assertThat(item.claimStatusName()).isEqualTo("대기");
    assertThat(item.thumbnailUrl()).isEqualTo("https://cdn/proof.jpg");
    assertThat(item.attachmentCount()).isEqualTo(2);
    assertThat(response.data().hasNext()).isTrue();
    assertThat(response.data().nextCursor()).isNotBlank();
  }

  @Test
  void listSupportsTemporaryClaimantWithoutAttachments() {
    ItemClaim claim = temporaryClaim(31L);
    when(itemClaimRepository.findAllByCursor(
        eq(25L),
        eq(null),
        eq(null),
        eq(null),
        any(Pageable.class)
    )).thenReturn(List.of(claim));
    when(thumbnailService.resolveAll(List.of(31L))).thenReturn(Map.of());

    var response = service.getItemClaims(
        25L,
        new ItemClaimSearchCondition(null, null, 20),
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
    assertThat(response.data().attachments().getFirst().fileUrl())
        .isEqualTo("https://cdn/proof.jpg");
    assertThat(response.data().statusHistories()).hasSize(1);
    assertThat(response.data().statusHistories().getFirst().newStatusName())
        .isEqualTo("대기");
  }

  @Test
  void staffWithoutOfficeAssignmentIsDenied() {
    when(assignmentRepository
        .existsByOffice_IdAndUser_IdAndEndedAtIsNull(3L, 7L))
        .thenReturn(false);

    assertBusinessError(
        () -> service.getItemClaims(
            25L,
            new ItemClaimSearchCondition(null, null, 20),
            7L,
            false
        ),
        ItemClaimErrorCode.ACCESS_DENIED
    );
    verify(itemClaimRepository, never()).findAllByCursor(
        any(), any(), any(), any(), any()
    );
  }

  @Test
  void adminCanViewClaimsWithoutOfficeAssignment() {
    when(itemClaimRepository.findAllByCursor(
        eq(25L), eq(null), eq(null), eq(null), any(Pageable.class)
    )).thenReturn(List.of());

    service.getItemClaims(
        25L,
        new ItemClaimSearchCondition(null, null, 20),
        7L,
        true
    );

    verify(assignmentRepository, never())
        .existsByOffice_IdAndUser_IdAndEndedAtIsNull(any(), any());
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

  @Test
  void malformedCursorIsRejected() {
    assertBusinessError(
        () -> service.getItemClaims(
            25L,
            new ItemClaimSearchCondition(null, "invalid", 20),
            7L,
            false
        ),
        ItemClaimErrorCode.INVALID_CURSOR
    );
  }

  private StoredItem storedItem() {
    StoredItem item = mock(StoredItem.class);
    LostItemOffice office = mock(LostItemOffice.class);
    when(item.getId()).thenReturn(25L);
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
