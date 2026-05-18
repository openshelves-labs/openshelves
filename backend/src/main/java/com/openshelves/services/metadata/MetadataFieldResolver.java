package com.openshelves.services.metadata;

import com.openshelves.model.enums.ResolutionStrategy;

public interface MetadataFieldResolver {

    ResolutionStrategy getStrategy();
}
