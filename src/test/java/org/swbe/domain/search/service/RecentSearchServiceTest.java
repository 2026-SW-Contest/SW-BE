package org.swbe.domain.search.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.swbe.domain.search.dto.request.RecentSearchCreateRequest;
import org.swbe.domain.search.entity.RecentSearch;
import org.swbe.domain.search.repository.RecentSearchRepository;
import org.swbe.domain.user.entity.AppUser;
import org.swbe.domain.user.repository.AppUserRepository;

class RecentSearchServiceTest {

  private static final Long USER_ID = 10L;
  private static final LocalDateTime NOW =
      LocalDateTime.of(2026, 8, 8, 12, 0);

  private RecentSearchRepository recentSearchRepository;
  private AppUserRepository appUserRepository;
  private RecentSearchService service;
  private AppUser user;

  @BeforeEach
  void setUp() {
    recentSearchRepository = mock(RecentSearchRepository.class);
    appUserRepository = mock(AppUserRepository.class);
    Clock clock = Clock.fixed(
        Instant.parse("2026-08-08T12:00:00Z"),
        ZoneOffset.UTC
    );
    service = new RecentSearchService(
        recentSearchRepository,
        appUserRepository,
        clock
    );
    user = AppUser.registerStudent(
        "student@mju.ac.kr",
        "{bcrypt}encoded-password",
        "홍길동",
        "60241234",
        NOW.minusDays(1)
    );
    ReflectionTestUtils.setField(user, "id", USER_ID);
  }

  @Test
  void duplicateKeywordIsMovedToMostRecentPosition() {
    RecentSearch existing = recentSearch(
        1L,
        "redis",
        NOW.minusHours(1)
    );
    when(appUserRepository.findByIdForUpdate(USER_ID))
        .thenReturn(Optional.of(user));
    when(recentSearchRepository
        .findByUserIdAndNormalizedKeyword(USER_ID, "redis"))
        .thenReturn(Optional.of(existing));
    when(recentSearchRepository
        .findAllByUserIdOrderBySearchedAtDescIdDesc(USER_ID))
        .thenReturn(List.of(existing));

    var response = service.record(
        USER_ID,
        new RecentSearchCreateRequest(" Redis ")
    );

    assertThat(existing.getKeyword()).isEqualTo("Redis");
    assertThat(existing.getSearchedAt()).isEqualTo(NOW);
    assertThat(response.data()).singleElement()
        .satisfies(item -> {
          assertThat(item.recentSearchId()).isEqualTo(1L);
          assertThat(item.keyword()).isEqualTo("Redis");
          assertThat(item.searchedAt()).isEqualTo(NOW);
        });
    verify(recentSearchRepository, never()).save(any());
  }

  @Test
  void onlySixMostRecentKeywordsAreRetained() {
    RecentSearch refreshed = recentSearch(
        1L,
        "에어",
        NOW.minusDays(1)
    );
    List<RecentSearch> ordered = List.of(
        refreshed,
        recentSearch(2L, "검색어2", NOW.minusMinutes(1)),
        recentSearch(3L, "검색어3", NOW.minusMinutes(2)),
        recentSearch(4L, "검색어4", NOW.minusMinutes(3)),
        recentSearch(5L, "검색어5", NOW.minusMinutes(4)),
        recentSearch(6L, "검색어6", NOW.minusMinutes(5)),
        recentSearch(7L, "검색어7", NOW.minusMinutes(6))
    );
    when(appUserRepository.findByIdForUpdate(USER_ID))
        .thenReturn(Optional.of(user));
    when(recentSearchRepository
        .findByUserIdAndNormalizedKeyword(USER_ID, "에어"))
        .thenReturn(Optional.of(refreshed));
    when(recentSearchRepository
        .findAllByUserIdOrderBySearchedAtDescIdDesc(USER_ID))
        .thenReturn(ordered);

    var response = service.record(
        USER_ID,
        new RecentSearchCreateRequest("에어")
    );

    assertThat(response.data()).hasSize(6);
    verify(recentSearchRepository).deleteAll(
        List.of(ordered.getLast())
    );
  }

  @Test
  void deletionUsesBothSearchIdAndCurrentUserId() {
    service.delete(USER_ID, 25L);

    verify(recentSearchRepository).deleteByIdAndUserId(
        25L,
        USER_ID
    );
  }

  private RecentSearch recentSearch(
      Long id,
      String keyword,
      LocalDateTime searchedAt
  ) {
    RecentSearch recentSearch = RecentSearch.create(
        user,
        keyword,
        keyword.toLowerCase(),
        searchedAt
    );
    ReflectionTestUtils.setField(recentSearch, "id", id);
    return recentSearch;
  }
}
