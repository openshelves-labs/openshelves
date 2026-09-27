package com.openshelves.metadata.client.impl.comicvine;

import com.openshelves.metadata.client.MetadataClient;
import com.openshelves.metadata.enums.MetadataProvider;
import com.openshelves.metadata.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComicVineClient implements MetadataClient {

    private static final String COMIC_VINE_BASE_URL = "https://comicvine.gamespot.com/api";
    private static final int DEFAULT_SEARCH_LIMIT = 5;

    private static final String VOLUME_ISSUE_FIELDS =
        "resource_type,id,name,issue_number,description,deck,image,volume," +
            "publisher,start_year,store_date,cover_date,person_credits";

    private static final String PERSON_FIELDS = "resource_type,id,name,description,deck,image";

    @Qualifier("comicVine")
    private final OkHttpClient httpClient;

    private final ComicVineMapper cvMapper;
    private final JsonMapper jsonMapper;

    /// Comic Vine API key. Free to obtain from a Comic Vine account; requests are skipped
    /// entirely when unset, since Comic Vine rejects unauthenticated calls outright.
    private String apiKey;      // TODO: Bind the API Key

    @Override
    public MetadataProvider provider() {
        return MetadataProvider.COMIC_VINE;
    }


    // -----------------------------------------------------------------------
    // Book Metadata Fetching
    // -----------------------------------------------------------------------

    @Override
    public List<BookMetadata> fetchBooks(BookRequest request, FetchOptions fetchOptions) {
        return List.of();
    }


    // -----------------------------------------------------------------------
    // Author Metadata Fetching
    // -----------------------------------------------------------------------

    @Override
    public List<AuthorMetadata> fetchAuthors(AuthorRequest request, FetchOptions fetchOptions) {
        return List.of();
    }
}
