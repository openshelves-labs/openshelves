package com.openshelves.model.entity;

import jakarta.persistence.MappedSuperclass;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.hibernate.Hibernate;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * Abstract base class for JPA entities.
 * Provides a common structure including ID retrieval and robust
 * {@code equals} and {@code hashCode} implementations based on the entity's ID.
 *
 * @param <ID> the type of the entity's identifier
 */
@MappedSuperclass
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class BaseEntity<ID> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Retrieves the identifier of the entity.
     *
     * @return the identifier
     */
    public abstract ID getId();

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || Hibernate.getClass(this) != Hibernate.getClass(obj)) return false;

        BaseEntity<?> that = (BaseEntity<?>) obj;
        return this.getId() != null && Objects.equals(this.getId(), that.getId());
    }

    @Override
    public int hashCode() {
        Class<?> persistentClass = Hibernate.getClass(this);
        return Objects.hash(persistentClass, this.getId());
    }

    @Override
    public String toString() {
        Class<?> persistentClass = Hibernate.getClass(this);
        return persistentClass.getSimpleName() + " [@id = " + getId() + "]";
    }
}
