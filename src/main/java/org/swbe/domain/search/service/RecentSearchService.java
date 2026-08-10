package org.swbe.domain.search.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.swbe.domain.search.dto.request.RecentSearchCreateRequest;
import org.swbe.domain.search.dto.response.RecentSearchListResponse;
import org.swbe.domain.search.dto.response.RecentSearchResponse;
import org.swbe.domain.search.entity.RecentSearch;
import org.swbe.domain.search.repository.RecentSearchRepository;
import org.swbe.domain.user.entity.AppUser;
import org.swbe.domain.user.exception.AuthErrorCode;
import org.swbe.domain.user.repository.AppUserRepository;
import org.swbe.global.error.BusinessException;
import org.swbe.global.error.CommonErrorCode;

@Service
@RequiredArgsConstructor
public class RecentSearchService {

  private static final int MAX_RECENT_SEARCHES = 6;
  private static final int MAX_KEYWORD_LENGTH = 100;

  private final RecentSearchRepository recentSearchRepository;
  private final AppUserRepository appUserRepository;
  private final Clock clock;

  @Transactional
  public RecentSearchListResponse record(
      Long userId,
      RecentSearchCreateRequest request
  ) {
    String keyword = validateAndStrip(request.keyword());
    String normalizedKeyword = keyword.toLowerCase(Locale.ROOT);
    AppUser user = appUserRepository.findByIdForUpdate(userId)
        .orElseThrow(() -> new BusinessException(
            AuthErrorCode.ACCOUNT_NOT_FOUND
        ));
    LocalDateTime now = LocalDateTime.now(clock);

    recentSearchRepository
        .findByUserIdAndNormalizedKeyword(
            userId,
            normalizedKeyword
        )
        .ifPresentOrElse(
            recentSearch -> recentSearch.refresh(keyword, now),
            () -> recentSearchRepository.save(
                RecentSearch.create(
                    user,
                    keyword,
                    normalizedKeyword,
                    now
                )
            )
        );

    List<RecentSearch> ordered = recentSearchRepository
        .findAllByUserIdOrderBySearchedAtDescIdDesc(userId);

    if (ordered.size() <= MAX_RECENT_SEARCHES) {
      return toResponse(ordered);
    }

    List<RecentSearch> retained = List.copyOf(
        ordered.subList(0, MAX_RECENT_SEARCHES)
    );
    List<RecentSearch> overflow = List.copyOf(
        ordered.subList(MAX_RECENT_SEARCHES, ordered.size())
    );
    recentSearchRepository.deleteAll(overflow);

    return toResponse(retained);
  }

  @Transactional(readOnly = true)
  public RecentSearchListResponse getRecentSearches(Long userId) {
    return toResponse(
        recentSearchRepository
            .findTop6ByUserIdOrderBySearchedAtDescIdDesc(userId)
    );
  }

  @Transactional
  public void delete(Long userId, Long recentSearchId) {
    recentSearchRepository.deleteByIdAndUserId(
        recentSearchId,
        userId
    );
  }

  @Transactional
  public void deleteAll(Long userId) {
    recentSearchRepository.deleteAllByUserId(userId);
  }

  private String validateAndStrip(String keyword) {
    if (keyword == null) {
      throw new BusinessException(CommonErrorCode.VALIDATION_FAILED);
    }

    String stripped = keyword.strip();
    if (stripped.isEmpty()
        || stripped.length() > MAX_KEYWORD_LENGTH) {
      throw new BusinessException(CommonErrorCode.VALIDATION_FAILED);
    }
    return stripped;
  }

  private RecentSearchListResponse toResponse(
      List<RecentSearch> recentSearches
  ) {
    List<RecentSearchResponse> data = recentSearches.stream()
        .map(recentSearch -> new RecentSearchResponse(
            recentSearch.getId(),
            recentSearch.getKeyword(),
            recentSearch.getSearchedAt()
        ))
        .toList();

    return new RecentSearchListResponse(data);
  }
}
