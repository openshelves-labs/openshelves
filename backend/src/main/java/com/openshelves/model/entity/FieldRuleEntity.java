package com.openshelves.model.entity;

import com.openshelves.model.enums.MergeStrategy;
import com.openshelves.model.enums.MetadataField;
import com.openshelves.model.enums.MetadataProvider;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

/**
 * Entity representing a rule for merging and prioritizing metadata for a specific field.
 *
 * <p>Each rule defines how values for a given {@link MetadataField} should be resolved
 * when multiple providers supply conflicting information. This includes the
 * {@link MergeStrategy} to use and the priority order of {@link MetadataProvider}s.</p>
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "field_rules")
public class FieldRuleEntity extends BaseEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    /**
     * The metadata field this rule applies to.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "field_key", nullable = false)
    private MetadataField fieldKey;

    /**
     * The strategy to use when merging values for this field from different providers.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "merge_strategy", nullable = false)
    private MergeStrategy mergeStrategy;

    /**
     * An ordered list of providers, where the first provider in the list has the highest priority.
     * Only used by strategies that rely on provider ranking.
     */
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "provider_priority")
    private List<MetadataProvider> providerPriority;
}
