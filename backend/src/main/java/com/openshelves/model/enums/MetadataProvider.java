package com.openshelves.model.enums;

/// Represents the external systems that provide book and author metadata.
///
/// Used to identify the source of metadata in a provider-agnostic way across the system.
/// Each value corresponds to a third-party service from which metadata can be fetched.
///
/// Each provider is expected to have a corresponding [com.openshelves.services.metadata.MetadataClient]
/// implementation responsible for interacting with the external system and mapping responses
/// into normalized DTOs.
public enum MetadataProvider {
    GOOGLE_BOOKS,   // Google Books
    OPEN_LIBRARY,   // Open Library
    AMAZON,         // Amazon Books
    GOODREADS       // Goodreads
}
