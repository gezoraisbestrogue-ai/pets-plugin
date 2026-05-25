package com.example.pets;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataType;

public class PetListener implements Listener {

    private final PetsPlugin plugin;
    private final PetManager manager;

    public PetListener(PetsPlugin plugin, PetManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // Delay one tick so the player is fully loaded into the world before spawning the pet.
        plugin.getServer().getScheduler().runTask(plugin,
                () -> manager.onPlayerJoin(event.getPlayer()));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        manager.onPlayerQuit(event.getPlayer());
    }

    /** Pets cannot be hurt by anything. */
    @EventHandler
    public void onPetDamage(EntityDamageEvent event) {
        if (isPet(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    /** Pets cannot hurt players. */
    @EventHandler
    public void onPetAttackPlayer(EntityDamageByEntityEvent event) {
        if (isPet(event.getDamager()) && event.getEntity() instanceof Player) {
            event.setCancelled(true);
        }
    }

    private boolean isPet(Entity entity) {
        return entity.getPersistentDataContainer()
                .has(manager.getOwnerKey(), PersistentDataType.STRING);
    }
}
