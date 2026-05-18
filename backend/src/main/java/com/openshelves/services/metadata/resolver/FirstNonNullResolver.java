package com.openshelves.services.metadata.resolver;

import com.openshelves.model.enums.ResolutionStrategy;
import com.openshelves.services.metadata.MetadataFieldResolver;
import org.springframework.stereotype.Component;

@Component
public class FirstNonNullResolver implements MetadataFieldResolver {

    @Override
    public ResolutionStrategy getStrategy() {
        return ResolutionStrategy.FIRST_NON_NULL;
    }
}
