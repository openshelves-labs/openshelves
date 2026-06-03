package com.openshelves.model.dto.metadata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/// Represents normalized author metadata retrieved from external providers
/// such as Google Books or Open Library.
///
/// This DTO is intentionally tolerant and minimal:
///
///   - All fields are optional and may be `null`
///   - Unknown properties from upstream APIs are ignored
///   - `null` fields are excluded from JSON serialization
///
/// The structure is designed for ingestion and mapping, not as a
/// fully authoritative domain model.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExternalAuthor {

    // -------------------------------------------------------------------------
    // Core metadata
    // -------------------------------------------------------------------------

    /// Display name of the author.
    private String name;

    /// Short biography or description of the author.
    private String bio;

    /// Nationality of the author.
    private String nationality;


    // -------------------------------------------------------------------------
    // Dates
    // -------------------------------------------------------------------------

    /// Year the author was born.
    private Short birthYear;

    /// Year the author died, if applicable.
    private Short deathYear;


    // -------------------------------------------------------------------------
    // Identifiers
    // -------------------------------------------------------------------------

    /// Amazon identifier for the author, if available.
    private String asin;

    /// Open Library identifier for the author.
    private String olid;


    // -------------------------------------------------------------------------
    // Media
    // -------------------------------------------------------------------------

    /// URL pointing to the author's profile image.
    private String profileImageUrl;
}
