package com.openshelves.metadata.resolver.impl.roleassignment;

import com.openshelves.metadata.models.ExternalBook;
import com.openshelves.metadata.models.ResolvedAuthorRef;
import com.openshelves.metadata.enums.MetadataProvider;

import java.util.List;
import java.util.Map;

public class AuthorRoleResolver {

    public List<ResolvedAuthorRef> resolve(Map<MetadataProvider, List<ExternalBook.AuthorRef>> candidates) {
        return null;
    }
}
