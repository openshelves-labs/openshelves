package com.openshelves.services.metadata;

import com.openshelves.model.dto.metadata.*;
import com.openshelves.model.enums.MetadataProvider;

import java.util.List;

/// Contract for interacting with an external metadata provider.
///
/// Implementations of this interface are responsible for communicating with
/// a specific third-party service and mapping its responses into normalized,
/// provider-agnostic DTOs used by the system.
public interface MetadataClient {

    /// Returns the metadata provider handled by this client.
    ///
    /// @return the associated [MetadataProvider]
    MetadataProvider provider();


    /// Fetches a list of books matching the given search criteria.
    ///
    /// The provider implementation determines which fields of `request` are
    /// used and how results are ranked or filtered. At least one non-null field in
    /// `request` should be provided for meaningful results.
    ///
    /// @param request      the search criteria (title, identifiers, authors, etc.)
    /// @param fetchOptions controls result limits and language filtering
    /// @return a list of matching [ExternalBook] results; never `null`, may be empty
    List<ExternalBook> fetchBooks(BookRequest request, FetchOptions fetchOptions);

    /// Fetches a list of authors matching the given search criteria.
    ///
    /// The provider implementation determines which fields of `request` are
    /// used and how results are ranked or filtered. At least one non-null field in
    /// `request` should be provided for meaningful results.
    ///
    /// @param request      the search criteria (name, identifiers, etc.)
    /// @param fetchOptions controls result limits and language filtering
    /// @return a list of matching [ExternalAuthor] results; never `null`, may be empty
    List<ExternalAuthor> fetchAuthors(AuthorRequest request, FetchOptions fetchOptions);

}
