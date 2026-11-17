package com.openshelves.metadata.model;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder(setterPrefix = "with")
@Jacksonized
public class AuthorMetadata {

    // -------------------------------------------------------------------------
    // Core metadata
    // -------------------------------------------------------------------------

    /// Display name of the author.
    String name;

    /// Short biography or description of the author.
    String bio;

    /// Nationality of the author.
    String nationality;


    // -------------------------------------------------------------------------
    // Dates
    // -------------------------------------------------------------------------

    /// Year the author was born.
    Short birthYear;

    /// Year the author died, if applicable.
    Short deathYear;


    // -------------------------------------------------------------------------
    // Identifiers
    // -------------------------------------------------------------------------

    /// Amazon identifier for the author, if available.
    String asin;

    /// Open Library identifier for the author.
    String olid;


    // -------------------------------------------------------------------------
    // Media
    // -------------------------------------------------------------------------

    /// URL pointing to the author's profile image.
    String profileImageUrl;
}
