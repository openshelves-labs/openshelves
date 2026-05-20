package com.openshelves.services.metadata;

import com.openshelves.model.entity.FieldResolutionPolicyEntity;
import com.openshelves.model.enums.MetadataField;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.Map;

@Service
public class MetadataFieldPolicyService {

    /**
     * Retrieves the resolution policies in bulk for the specified set of metadata fields.
     *
     * @param fields The set of metadata fields to load policies for.
     * @return A map associating each requested field with its corresponding resolution policy entity.
     */
    @Transactional(readOnly = true)
    public Map<MetadataField, FieldResolutionPolicyEntity> bulkLoadPolicies(EnumSet<MetadataField> fields) {
        // TODO: will implement once the repository is created
        throw new UnsupportedOperationException("Bulk load policies not yet supported");
    }
}
