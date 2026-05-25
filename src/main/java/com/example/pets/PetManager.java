package com.example.pets;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PetManager {

    /** Pet types we let players summon. */
    public static final Set<EntityType> ALLOWED_TYPES = EnumSet.of(
            EntityType.WOLF, EntityType.CAT, EntityType.PARROT);

    private final PetsPlugin plugin;
    private final Map<UUID, Pet> pets = new HashMap<>();
    private final NamespacedKey ownerKey;
    private final File petsFile;

    public PetManager(PetsPlugin plugin) {
        this.plugin = plugin;
        this.ownerKey = new NamespacedKey(plugin, "pet_owner");
        this.petsFile = new File(plugin.getDataFolder(), "pets.yml");
    }

    public NamespacedKey getOwnerKey() { return ownerKey; }

    public Pet getPet(UUID ownerId) { return pets.get(ownerId); }

    public boolean hasPet(UUID ownerId) { return pets.containsKey(ownerId); }

    // ---------- spawning ----------

    public void createPet(Player owner, EntityType type, String name) {
        Pet pet = new Pet(owner.getUniqueId(), type, name);
        pets.put(owner.getUniqueId(), pet);
        spawnPet(owner, pet);
    }

    public LivingEntity spawnPet(Player owner, Pet pet) {
        Location loc = owner.getLocation();
        LivingEntity entity = (LivingEntity) owner.getWorld().spawnEntity(loc, pet.getType());

        entity.customName(Component.text(pet.getName()));
        entity.setCustomNameVisible(true);
        entity.setRemoveWhenFarAway(false);
        entity.setPersistent(true);

        // Stamp the entity so we can recognize it across restarts and in event handlers.
        entity.getPersistentDataContainer().set(
                ownerKey, PersistentDataType.STRING, owner.getUniqueId().toString());

        if (entity instanceof Tameable tameable) {
            tameable.setTamed(true);
            tameable.setOwner(owner);
        }

        pet.setEntityId(entity.getUniqueId());
        return entity;
    }

    public void despawnPet(UUID ownerId) {
        Pet pet = pets.get(ownerId);
        if (pet == null || pet.getEntityId() == null) return;
        Entity e = Bukkit.getEntity(pet.getEntityId());
        if (e != null) e.remove();
        pet.setEntityId(null);
    }

    public void removePet(UUID ownerId) {
        despawnPet(ownerId);
        pets.remove(ownerId);
    }

    public void despawnAll() {
        for (UUID id : pets.keySet()) despawnPet(id);
    }

    // ---------- ticking ----------

    /**
     * Runs on a timer. Re-spawns missing pets and pulls wandering pets
     * back to their owner if they're more than 20 blocks away.
     */
    public void tickPets() {
        for (Pet pet : pets.values()) {
            Player owner = Bukkit.getPlayer(pet.getOwnerId());
            if (owner == null || !owner.isOnline()) continue;

            if (pet.getEntityId() == null) {
                spawnPet(owner, pet);
                continue;
            }

            Entity entity = Bukkit.getEntity(pet.getEntityId());
            if (entity == null) {
                // Entity got unloaded or removed; respawn one near the owner.
                spawnPet(owner, pet);
                continue;
            }

            if (!entity.getWorld().equals(owner.getWorld())
                    || entity.getLocation().distanceSquared(owner.getLocation()) > 400) {
                entity.teleport(owner.getLocation());
            }
        }
    }

    // ---------- session hooks ----------

    public void onPlayerJoin(Player player) {
        Pet pet = pets.get(player.getUniqueId());
        if (pet != null && pet.getEntityId() == null) {
            spawnPet(player, pet);
        }
    }

    public void onPlayerQuit(Player player) {
        despawnPet(player.getUniqueId());
    }

    // ---------- persistence ----------

    public void load() {
        if (!petsFile.exists()) {
            plugin.getDataFolder().mkdirs();
            return;
        }
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(petsFile);
        for (String key : cfg.getKeys(false)) {
            try {
                UUID ownerId = UUID.fromString(key);
                String typeStr = cfg.getString(key + ".type");
                String name = cfg.getString(key + ".name", "Pet");
                if (typeStr == null) continue;
                EntityType type = EntityType.valueOf(typeStr);
                pets.put(ownerId, new Pet(ownerId, type, name));
            } catch (Exception ex) {
                plugin.getLogger().warning("Skipping invalid pet entry: " + key);
            }
        }
        plugin.getLogger().info("Loaded " + pets.size() + " pet(s).");
    }

    public void save() {
        YamlConfiguration cfg = new YamlConfiguration();
        for (Pet pet : pets.values()) {
            String base = pet.getOwnerId().toString();
            cfg.set(base + ".type", pet.getType().name());
            cfg.set(base + ".name", pet.getName());
        }
        try {
            plugin.getDataFolder().mkdirs();
            cfg.save(petsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save pets: " + e.getMessage());
        }
    }
}
