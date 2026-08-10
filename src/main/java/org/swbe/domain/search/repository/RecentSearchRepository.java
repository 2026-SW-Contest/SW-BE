package org.swbe.domain.search.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.swbe.domain.search.entity.RecentSearch;

public interface RecentSearchRepository
    extends JpaRepository<RecentSearch, Long> {

  Optional<RecentSearch> findByUserIdAndNormalizedKeyword(
      Long userId,
      String normalizedKeyword
  );

  List<RecentSearch> findAllByUserIdOrderBySearchedAtDescIdDesc(
      Long userId
  );

  List<RecentSearch> findTop6ByUserIdOrderBySearchedAtDescIdDesc(
      Long userId
  );

  long deleteByIdAndUserId(Long id, Long userId);

  void deleteAllByUserId(Long userId);
}
