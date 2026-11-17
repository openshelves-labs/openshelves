package com.openshelves.core.persistence;

import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * Abstract base class for JPA entities.
 *
 * <p>
 * Provides a common structure including ID retrieval and robust
 * {@code equals} and {@code hashCode} implementations based on the entity's ID.
 *
 * @param <ID> the type of the entity's identifier
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class BaseEntity<ID> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Retrieves the identifier of the entity.
     */
    public abstract ID getId();

    /**
     * Compares this entity with another object for equality.
     *
     * <p>
     * Two entities are considered equal if they are of the same class and have the same non-null ID.
     *
     * @param obj the object to compare with
     * @return {@code true} if the entities are equal, {@code false} otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || Hibernate.getClass(this) != Hibernate.getClass(obj)) return false;

        BaseEntity<?> that = (BaseEntity<?>) obj;
        return this.getId() != null && Objects.equals(this.getId(), that.getId());
    }

    /**
     * Returns the hash code value for the entity.
     */
    @Override
    public int hashCode() {
        Class<?> persistentClass = Hibernate.getClass(this);
        return Objects.hash(persistentClass, this.getId());
    }

    /**
     * Returns a string representation of the entity.
     */
    @Override
    public String toString() {
        Class<?> persistentClass = Hibernate.getClass(this);
        return persistentClass.getSimpleName() + " [@id = " + getId() + "]";
    }
}
