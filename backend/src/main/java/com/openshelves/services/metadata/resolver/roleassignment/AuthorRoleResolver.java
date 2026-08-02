package com.openshelves.services.metadata.resolver.roleassignment;

import com.openshelves.model.dto.metadata.ExternalBook;
import com.openshelves.model.dto.metadata.ResolvedAuthorRef;
import com.openshelves.model.enums.MetadataProvider;

import java.util.List;
import java.util.Map;

public class AuthorRoleResolver {

    public List<ResolvedAuthorRef> resolve(Map<MetadataProvider, List<ExternalBook.AuthorRef>> candidates) {
        return null;
    }
}
