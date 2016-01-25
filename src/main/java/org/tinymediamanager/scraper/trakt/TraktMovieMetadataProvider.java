/*
 * Copyright 2012 - 2015 Manuel Laggner
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.tinymediamanager.scraper.trakt;

import com.uwetrottmann.trakt.v2.TraktV2;
import com.uwetrottmann.trakt.v2.entities.SearchResult;
import com.uwetrottmann.trakt.v2.enums.Type;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.scraper.MediaSearchOptions;
import org.tinymediamanager.scraper.MediaSearchResult;
import org.tinymediamanager.scraper.MediaType;
import org.tinymediamanager.scraper.UnsupportedMediaTypeException;
import org.tinymediamanager.scraper.util.MetadataUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * The class TraktMovieMetadataProvider is used to provide metadata for movies from trakt.tv
 */
class TraktMovieMetadataProvider {
  private static final Logger LOGGER = LoggerFactory.getLogger(TraktMovieMetadataProvider.class);

  final TraktV2 api;

  public TraktMovieMetadataProvider(TraktV2 api) {
    this.api = api;
  }

  List<MediaSearchResult> search(MediaSearchOptions options) throws Exception {
    LOGGER.debug("search() " + options.toString());

    if (options.getMediaType() != MediaType.MOVIE) {
      throw new UnsupportedMediaTypeException(options.getMediaType());
    }

    String searchString = "";
    int year = 0;

    if (StringUtils.isEmpty(searchString) && StringUtils.isNotEmpty(options.get(MediaSearchOptions.SearchParam.QUERY))) {
      searchString = options.get(MediaSearchOptions.SearchParam.QUERY);
    }

    if (StringUtils.isNotEmpty(options.get(MediaSearchOptions.SearchParam.YEAR))) {
      try {
        year = Integer.parseInt(options.get(MediaSearchOptions.SearchParam.YEAR));
      }
      catch (Exception e) {
        year = 0;
      }
    }

    List<MediaSearchResult> results = new ArrayList<>();
    List<SearchResult> searchResults = null;

    try {
      if (year != 0) {
        searchResults = api.search().textQuery(searchString, Type.MOVIE, year, 1, 25);
      }
      else {
        searchResults = api.search().textQuery(searchString, Type.MOVIE, null, 1, 25);
      }
    }
    catch (Exception e) {
      LOGGER.error("Problem scraping for " + searchString + "; " + e.getMessage());
    }

    if (searchResults == null || searchResults.isEmpty()) {
      LOGGER.info("nothing found");
      return results;
    }

    for (SearchResult result : searchResults) {
      MediaSearchResult mediaSearchResult = new MediaSearchResult(TraktMetadataProvider.providerInfo.getId());

      mediaSearchResult.setTitle(result.movie.title);

      mediaSearchResult.setScore(MetadataUtil.calculateScore(searchString, mediaSearchResult.getTitle()));

      results.add(mediaSearchResult);
    }

    return results;
  }
}
