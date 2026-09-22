package com.openshelves.metadata.services;

import org.springframework.stereotype.Service;

/// AI-assisted fallback service for resolving metadata conflicts that conventional
/// [com.openshelves.metadata.resolver.MetadataFieldResolver] implementations could not settle.
///
/// Intended to be invoked after all standard resolution strategies (e.g. ranked provider,
/// popular voting) have been exhausted or have returned an inconclusive result. This service
/// delegates to an AI model to make a best-effort judgement on the correct field value.
@Service
public class MetadataResolutionService {

}
