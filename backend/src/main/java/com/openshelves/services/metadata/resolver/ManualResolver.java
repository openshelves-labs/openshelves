package com.openshelves.services.metadata.resolver;

import com.openshelves.model.enums.ResolutionStrategy;
import com.openshelves.services.metadata.MetadataFieldResolver;
import org.springframework.stereotype.Component;

@Component
public class ManualResolver implements MetadataFieldResolver {

    @Override
    public ResolutionStrategy getStrategy() {
        return ResolutionStrategy.MANUAL;
    }
}
