package org.swbe.domain.lostitem.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.swbe.domain.file.entity.FileResource;
import org.swbe.domain.file.service.FilePublicUrlResolver;
import org.swbe.domain.lostitem.entity.ItemCategory;
import org.swbe.domain.lostitem.entity.ItemClaim;
import org.swbe.domain.lostitem.entity.ItemClaimStatus;
import org.swbe.domain.lostitem.entity.LostItemOffice;
import org.swbe.domain.lostitem.entity.StoredItem;
import org.swbe.domain.lostitem.entity.StoredItemAttachment;
import org.swbe.domain.lostitem.entity.StoredItemStatus;
import org.swbe.domain.lostitem.repository.StoredItemAttachmentRepository;
import org.swbe.domain.lostitem.repository.ItemClaimRepository;
import org.swbe.domain.lostitem.repository.StoredItemRepository;
import org.swbe.global.error.BusinessException;

class StoredItemDetailServiceTest {

  private StoredItemRepository storedItemRepository;
  private StoredItemAttachmentRepository attachmentRepository;
  private ItemClaimRepository itemClaimRepository;
  private FilePublicUrlResolver filePublicUrlResolver;
  private StoredItemDetailService service;

  @BeforeEach
  void setUp() {
    storedItemRepository = mock(StoredItemRepository.class);
    attachmentRepository = mock(StoredItemAttachmentRepository.class);
    itemClaimRepository = mock(ItemClaimRepository.class);
    filePublicUrlResolver = mock(FilePublicUrlResolver.class);
    service = new StoredItemDetailService(
        storedItemRepository,
        attachmentRepository,
        itemClaimRepository,
        filePublicUrlResolver
    );
  }

  @Test
  void returnsPublicDetailAndAllAttachments() {
    StoredItem item = item();
    FileResource file = mock(FileResource.class);
    when(file.getId()).thenReturn(31L);
    when(file.getOriginalFilename()).thenReturn("wallet.jpg");
    StoredItemAttachment attachment = mock(StoredItemAttachment.class);
    when(attachment.getFile()).thenReturn(file);
    when(storedItemRepository.findDetailById(25L))
        .thenReturn(Optional.of(item));
    when(attachmentRepository.findPublicImagesByStoredItemId(25L))
        .thenReturn(List.of(attachment));
    when(filePublicUrlResolver.resolve(file))
        .thenReturn("https://cdn.example.com/public/image.jpg");

    var response = service.getStoredItem(25L, null);

    assertThat(response.data().storedItemId()).isEqualTo(25L);
    assertThat(response.data().description()).isEqualTo("공개 설명");
    assertThat(response.data().category().name())
        .isEqualTo("지갑/카드/현금");
    assertThat(response.data().foundLocation().locationId()).isNull();
    assertThat(response.data().foundLocation().name())
        .isEqualTo("명진관 앞 벤치");
    assertThat(response.data().office().name()).isEqualTo("본관 경비실");
    assertThat(response.data().publicStatusName()).isEqualTo("보관중");
    assertThat(response.data().attachments()).singleElement()
        .satisfies(result -> {
          assertThat(result.fileId()).isEqualTo(31L);
          assertThat(result.originalFilename()).isEqualTo("wallet.jpg");
          assertThat(result.fileUrl())
              .isEqualTo("https://cdn.example.com/public/image.jpg");
        });
    assertThat(response.data().myClaimResult()).isNull();
    verifyNoInteractions(itemClaimRepository);
  }

  @Test
  void returnsLatestApprovedClaimOnlyToItsClaimant() {
    StoredItem item = item();
    ItemClaim claim = decisionClaim(ItemClaimStatus.APPROVED, "승인 메시지");
    when(storedItemRepository.findDetailById(25L))
        .thenReturn(Optional.of(item));
    when(attachmentRepository.findPublicImagesByStoredItemId(25L))
        .thenReturn(List.of());
    when(itemClaimRepository
        .findFirstByStoredItem_IdAndClaimantUser_IdOrderByCreatedAtDescIdDesc(
            25L,
            7L
        )).thenReturn(Optional.of(claim));

    var response = service.getStoredItem(25L, 7L);

    assertThat(response.data().myClaimResult()).satisfies(result -> {
      assertThat(result.itemClaimId()).isEqualTo(31L);
      assertThat(result.decision()).isEqualTo("APPROVED");
      assertThat(result.decisionName()).isEqualTo("승인");
      assertThat(result.message()).isEqualTo("승인 메시지");
      assertThat(result.decidedAt()).isEqualTo(
          LocalDateTime.of(2026, 8, 12, 14, 30)
      );
    });
  }

  @Test
  void returnsRejectedClaimWithReasonToItsClaimant() {
    StoredItem item = item();
    ItemClaim claim = decisionClaim(ItemClaimStatus.REJECTED, "정보 부족");
    when(storedItemRepository.findDetailById(25L))
        .thenReturn(Optional.of(item));
    when(attachmentRepository.findPublicImagesByStoredItemId(25L))
        .thenReturn(List.of());
    when(itemClaimRepository
        .findFirstByStoredItem_IdAndClaimantUser_IdOrderByCreatedAtDescIdDesc(
            25L,
            7L
        )).thenReturn(Optional.of(claim));

    var response = service.getStoredItem(25L, 7L);

    assertThat(response.data().myClaimResult()).satisfies(result -> {
      assertThat(result.decision()).isEqualTo("REJECTED");
      assertThat(result.decisionName()).isEqualTo("거부");
      assertThat(result.message()).isEqualTo("정보 부족");
    });
  }

  @Test
  void latestWaitingClaimHidesPreviousDecisionResult() {
    StoredItem item = item();
    ItemClaim waitingClaim = mock(ItemClaim.class);
    when(waitingClaim.getClaimStatus()).thenReturn(ItemClaimStatus.WAITING);
    when(storedItemRepository.findDetailById(25L))
        .thenReturn(Optional.of(item));
    when(attachmentRepository.findPublicImagesByStoredItemId(25L))
        .thenReturn(List.of());
    when(itemClaimRepository
        .findFirstByStoredItem_IdAndClaimantUser_IdOrderByCreatedAtDescIdDesc(
            25L,
            7L
        )).thenReturn(Optional.of(waitingClaim));

    var response = service.getStoredItem(25L, 7L);

    assertThat(response.data().myClaimResult()).isNull();
  }

  @Test
  void userWithoutClaimReceivesNoDecisionResult() {
    StoredItem item = item();
    when(storedItemRepository.findDetailById(25L))
        .thenReturn(Optional.of(item));
    when(attachmentRepository.findPublicImagesByStoredItemId(25L))
        .thenReturn(List.of());
    when(itemClaimRepository
        .findFirstByStoredItem_IdAndClaimantUser_IdOrderByCreatedAtDescIdDesc(
            25L,
            99L
        )).thenReturn(Optional.empty());

    var response = service.getStoredItem(25L, 99L);

    assertThat(response.data().myClaimResult()).isNull();
  }

  @Test
  void missingStoredItemRaisesDomainNotFoundError() {
    when(storedItemRepository.findDetailById(99L))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.getStoredItem(99L, null))
        .isInstanceOf(BusinessException.class)
        .satisfies(exception -> assertThat(
            ((BusinessException) exception).getErrorCode().code()
        ).isEqualTo("STORED_ITEM_NOT_FOUND"));
    verify(storedItemRepository).findDetailById(99L);
  }

  private StoredItem item() {
    ItemCategory category = mock(ItemCategory.class);
    when(category.getId()).thenReturn(2L);
    when(category.getName()).thenReturn("지갑/카드/현금");
    LostItemOffice office = mock(LostItemOffice.class);
    when(office.getId()).thenReturn(3L);
    when(office.getName()).thenReturn("본관 경비실");
    StoredItem item = mock(StoredItem.class);
    when(item.getId()).thenReturn(25L);
    when(item.getItemName()).thenReturn("검은색 지갑");
    when(item.getPublicDescription()).thenReturn("공개 설명");
    when(item.getItemCategory()).thenReturn(category);
    when(item.getFoundDate()).thenReturn(LocalDate.of(2026, 8, 10));
    when(item.getFoundLocationName()).thenReturn("명진관 앞 벤치");
    when(item.getPublicStatus()).thenReturn(StoredItemStatus.STORED);
    when(item.getOffice()).thenReturn(office);
    when(item.getCreatedAt()).thenReturn(
        LocalDateTime.of(2026, 8, 10, 14, 30)
    );
    when(item.getUpdatedAt()).thenReturn(
        LocalDateTime.of(2026, 8, 10, 14, 30)
    );
    return item;
  }

  private ItemClaim decisionClaim(
      ItemClaimStatus status,
      String message
  ) {
    ItemClaim claim = mock(ItemClaim.class);
    when(claim.getId()).thenReturn(31L);
    when(claim.getClaimStatus()).thenReturn(status);
    when(claim.getDecisionMessage()).thenReturn(message);
    when(claim.getDecidedAt()).thenReturn(
        LocalDateTime.of(2026, 8, 12, 14, 30)
    );
    return claim;
  }
}
