package com.openshelves.model.dto.metadata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.openshelves.model.enums.MetadataProvider;
import lombok.*;
import lombok.experimental.Delegate;


/// Represents an author reference enriched with its source provider and a deterministic sort order.
///
/// This DTO acts as a wrapper around an [ExternalBook.AuthorRef], tying it to the specific
/// [MetadataProvider] it was fetched from, and retaining the author's sequence in the provider's data.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProviderAuthorRef {

    /// The original author reference as provided by the external metadata provider.
    @Delegate
    @JsonUnwrapped
    private ExternalBook.AuthorRef authorRef;


    // -------------------------------------------------------------------------
    // Provider-specific metadata
    // -------------------------------------------------------------------------

    /// The provider from which this author reference was sourced (e.g., Google Books, Open Library).
    private MetadataProvider provider;


    // -------------------------------------------------------------------------
    // Sort-order in role
    // -------------------------------------------------------------------------

    /// The order of this author within their role (e.g., first author, second author).
    private Integer sortOrder;
}
