package org.swbe.domain.search.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.swbe.domain.user.entity.AppUser;

@Entity
@Table(name = "recent_search")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecentSearch {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "recent_search_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id")
  private AppUser user;

  @Column(nullable = false, length = 100)
  private String keyword;

  @Column(name = "normalized_keyword", nullable = false, length = 100)
  private String normalizedKeyword;

  @Column(name = "searched_at", nullable = false)
  private LocalDateTime searchedAt;

  private RecentSearch(
      AppUser user,
      String keyword,
      String normalizedKeyword,
      LocalDateTime searchedAt
  ) {
    this.user = Objects.requireNonNull(user, "user must not be null");
    this.keyword = Objects.requireNonNull(
        keyword,
        "keyword must not be null"
    );
    this.normalizedKeyword = Objects.requireNonNull(
        normalizedKeyword,
        "normalizedKeyword must not be null"
    );
    this.searchedAt = Objects.requireNonNull(
        searchedAt,
        "searchedAt must not be null"
    );
  }

  public static RecentSearch create(
      AppUser user,
      String keyword,
      String normalizedKeyword,
      LocalDateTime searchedAt
  ) {
    return new RecentSearch(
        user,
        keyword,
        normalizedKeyword,
        searchedAt
    );
  }

  public void refresh(String keyword, LocalDateTime searchedAt) {
    this.keyword = Objects.requireNonNull(
        keyword,
        "keyword must not be null"
    );
    this.searchedAt = Objects.requireNonNull(
        searchedAt,
        "searchedAt must not be null"
    );
  }
}
