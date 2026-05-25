package com.example.pets;

import org.bukkit.entity.EntityType;

import java.util.UUID;

/**
 * In-memory record for one player's pet. Persisted to pets.yml.
 */
public class Pet {

    private final UUID ownerId;
    private final EntityType type;
    private String name;
    private UUID entityId; // null when not currently spawned

    public Pet(UUID ownerId, EntityType type, String name) {
        this.ownerId = ownerId;
        this.type = type;
        this.name = name;
    }

    public UUID getOwnerId() { return ownerId; }
    public EntityType getType() { return type; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public UUID getEntityId() { return entityId; }
    public void setEntityId(UUID entityId) { this.entityId = entityId; }
}
