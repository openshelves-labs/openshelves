package com.openshelves.services.metadata;

import com.openshelves.model.entity.FieldResolutionPolicyEntity;
import com.openshelves.model.enums.MetadataField;
import com.openshelves.model.enums.MetadataProvider;
import com.openshelves.model.enums.ResolutionStrategy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MetadataFieldPolicyService {

    /**
     * Retrieves the resolution policies in bulk for the specified set of metadata fields.
     *
     * @param fields The set of metadata fields to load policies for.
     * @return A map associating each requested field with its corresponding resolution policy entity.
     */
    @Transactional(readOnly = true)
    public Map<MetadataField, FieldResolutionPolicyEntity> bulkLoadPolicies(Set<MetadataField> fields) {
        // Mock returning a static list of policies based on real-life field level decisions
        return fields.stream().collect(Collectors.toMap(
                field -> field,
                field -> {
                    FieldResolutionPolicyEntity policy = new FieldResolutionPolicyEntity();
                    policy.setFieldKey(field);

                    switch (field) {
                        case BOOK_DESCRIPTION:
                        case AUTHOR_BIO:
                            policy.setResolutionStrategy(ResolutionStrategy.HIGHEST_QUALITY_TEXT);
                            break;
                        case BOOK_TITLE:
                        case BOOK_AUTHORS:
                        case AUTHOR_NAME:
                            policy.setResolutionStrategy(ResolutionStrategy.VOTING);
                            break;
                        case BOOK_ISBN10:
                        case BOOK_ISBN13:
                        case BOOK_ASIN:
                        case BOOK_OLID:
                        case AUTHOR_ASIN:
                        case AUTHOR_OLID:
                        case BOOK_COVER_IMAGE_URL:
                        case AUTHOR_PROFILE_IMAGE_URL:
                            policy.setResolutionStrategy(ResolutionStrategy.FIRST_NON_NULL);
                            break;
                        default:
                            policy.setResolutionStrategy(ResolutionStrategy.PRIORITY);
                            policy.setRankedProviders(List.of(
                                    MetadataProvider.GOOGLE_BOOKS,
                                    MetadataProvider.OPEN_LIBRARY,
                                    MetadataProvider.GOODREADS,
                                    MetadataProvider.AMAZON
                            ));
                            break;
                    }

                    return policy;
                }
        ));
    }
}
