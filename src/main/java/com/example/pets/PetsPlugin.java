package com.example.pets;

import org.bukkit.plugin.java.JavaPlugin;

public final class PetsPlugin extends JavaPlugin {

    private PetManager petManager;

    @Override
    public void onEnable() {
        this.petManager = new PetManager(this);
        petManager.load();

        PetCommand petCommand = new PetCommand(this, petManager);
        if (getCommand("pet") != null) {
            getCommand("pet").setExecutor(petCommand);
            getCommand("pet").setTabCompleter(petCommand);
        }

        getServer().getPluginManager().registerEvents(new PetListener(this, petManager), this);

        // Every 2 seconds: respawn missing pets, teleport pets that fell behind.
        getServer().getScheduler().runTaskTimer(this, petManager::tickPets, 40L, 40L);

        getLogger().info("Pets plugin enabled.");
    }

    @Override
    public void onDisable() {
        if (petManager != null) {
            petManager.despawnAll();
            petManager.save();
        }
    }

    public PetManager getPetManager() {
        return petManager;
    }
}
