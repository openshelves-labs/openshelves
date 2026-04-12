package com.openshelves.model.entity;

import jakarta.persistence.MappedSuperclass;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.hibernate.Hibernate;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

@MappedSuperclass
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class BaseEntity<ID> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

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
