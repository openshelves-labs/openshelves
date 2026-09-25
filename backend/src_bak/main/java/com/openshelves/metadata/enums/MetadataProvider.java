package com.openshelves.metadata.enums;

import com.openshelves.metadata.client.MetadataClient;

/// Represents the external systems that provide book and author metadata.
///
/// Used to identify the source of metadata in a provider-agnostic way across the system.
/// Each value corresponds to a third-party service from which metadata can be fetched.
///
/// Each provider is expected to have a corresponding [MetadataClient]
/// implementation responsible for interacting with the external system and mapping responses
/// into normalized DTOs.
public enum MetadataProvider {
    GOOGLE_BOOKS,   // Google Books
    OPEN_LIBRARY,   // Open Library
    HARDCOVER,      // Hardcover
    COMIC_VINE,     // Comic Vine (comics/graphic novels)
    RANOBEDB,       // RanobeDB (light novels)
}
