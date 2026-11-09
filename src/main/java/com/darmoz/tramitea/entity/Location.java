package com.darmoz.tramitea.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * País o provincia (V4__locations.sql). Solo dos niveles: las provincias
 * apuntan a su país con {@code parentId}. Se carga por migraciones; la API
 * pública solo la lee.
 */
@Entity
@Table(name = "tr_locations")
public class Location {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LocationType type;

    @Column(name = "parent_id")
    private UUID parentId;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    protected Location() {
    }

    /** Id del país: el propio si es un país, el del padre si es una provincia. */
    public UUID getCountryId() {
        return parentId == null ? id : parentId;
    }

    public UUID getId() { return id; }
    public LocationType getType() { return type; }
    public UUID getParentId() { return parentId; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public boolean isActive() { return active; }
}
