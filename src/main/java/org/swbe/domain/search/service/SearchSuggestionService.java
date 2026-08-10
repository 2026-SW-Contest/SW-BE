package org.swbe.domain.search.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.swbe.domain.search.dto.response.SearchSuggestionDataResponse;
import org.swbe.domain.search.dto.response.SearchSuggestionListResponse;
import org.swbe.domain.search.repository.SearchSuggestionQueryRepository;
import org.swbe.domain.search.support.SearchKeyword;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchSuggestionService {

  private final SearchSuggestionQueryRepository queryRepository;

  public SearchSuggestionListResponse getSuggestions(
      String rawQuery,
      int size
  ) {
    SearchKeyword keyword = SearchKeyword.from(rawQuery);

    return new SearchSuggestionListResponse(
        new SearchSuggestionDataResponse(
            queryRepository.findLostItemSuggestions(
                keyword.normalized(),
                keyword.containsPattern(),
                keyword.prefixPattern(),
                size
            ),
            queryRepository.findFacilityRequestSuggestions(
                keyword.normalized(),
                keyword.containsPattern(),
                keyword.prefixPattern(),
                size
            )
        )
    );
  }
}
