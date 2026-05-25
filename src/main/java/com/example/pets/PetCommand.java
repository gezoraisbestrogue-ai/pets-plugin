package com.example.pets;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PetCommand implements CommandExecutor, TabCompleter {

    private final PetsPlugin plugin;
    private final PetManager manager;

    public PetCommand(PetsPlugin plugin, PetManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command is for players only.");
            return true;
        }
        if (args.length == 0) {
            sendUsage(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "summon"  -> handleSummon(player, args);
            case "dismiss" -> handleDismiss(player);
            case "name"    -> handleName(player, args);
            case "tp"      -> handleTeleport(player);
            case "list"    -> handleList(player);
            default        -> sendUsage(player);
        }
        return true;
    }

    // ---------- handlers ----------

    private void handleSummon(Player player, String[] args) {
        if (manager.hasPet(player.getUniqueId())) {
            player.sendMessage(Component.text(
                    "You already have a pet. Use /pet dismiss first.", NamedTextColor.RED));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(Component.text(
                    "Usage: /pet summon <wolf|cat|parrot> [name]", NamedTextColor.YELLOW));
            return;
        }

        EntityType type;
        try {
            type = EntityType.valueOf(args[1].toUpperCase());
        } catch (IllegalArgumentException ex) {
            player.sendMessage(Component.text(
                    "Unknown type. Try wolf, cat, or parrot.", NamedTextColor.RED));
            return;
        }
        if (!PetManager.ALLOWED_TYPES.contains(type)) {
            player.sendMessage(Component.text(
                    "That mob isn't an allowed pet type.", NamedTextColor.RED));
            return;
        }

        String name = (args.length >= 3)
                ? String.join(" ", Arrays.copyOfRange(args, 2, args.length))
                : player.getName() + "'s " + capitalize(type.name());

        manager.createPet(player, type, name);
        player.sendMessage(Component.text("Pet summoned: " + name, NamedTextColor.GREEN));
    }

    private void handleDismiss(Player player) {
        if (!manager.hasPet(player.getUniqueId())) {
            player.sendMessage(Component.text("You don't have a pet.", NamedTextColor.RED));
            return;
        }
        manager.removePet(player.getUniqueId());
        player.sendMessage(Component.text("Your pet has been dismissed.", NamedTextColor.GREEN));
    }

    private void handleName(Player player, String[] args) {
        Pet pet = manager.getPet(player.getUniqueId());
        if (pet == null) {
            player.sendMessage(Component.text("You don't have a pet.", NamedTextColor.RED));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(Component.text(
                    "Usage: /pet name <new name>", NamedTextColor.YELLOW));
            return;
        }
        String newName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        pet.setName(newName);

        if (pet.getEntityId() != null) {
            Entity entity = Bukkit.getEntity(pet.getEntityId());
            if (entity != null) {
                entity.customName(Component.text(newName));
                entity.setCustomNameVisible(true);
            }
        }
        player.sendMessage(Component.text("Pet renamed to: " + newName, NamedTextColor.GREEN));
    }

    private void handleTeleport(Player player) {
        Pet pet = manager.getPet(player.getUniqueId());
        if (pet == null || pet.getEntityId() == null) {
            player.sendMessage(Component.text("No active pet to teleport.", NamedTextColor.RED));
            return;
        }
        Entity entity = Bukkit.getEntity(pet.getEntityId());
        if (entity == null) {
            player.sendMessage(Component.text("Couldn't find your pet entity.", NamedTextColor.RED));
            return;
        }
        entity.teleport(player.getLocation());
        player.sendMessage(Component.text("Pet teleported to you.", NamedTextColor.GREEN));
    }

    private void handleList(Player player) {
        player.sendMessage(Component.text(
                "Available pet types: wolf, cat, parrot", NamedTextColor.AQUA));
    }

    private void sendUsage(Player player) {
        player.sendMessage(Component.text("Pet commands:", NamedTextColor.AQUA));
        player.sendMessage(Component.text("  /pet summon <type> [name]", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("  /pet dismiss", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("  /pet name <new name>", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("  /pet tp", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("  /pet list", NamedTextColor.YELLOW));
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.charAt(0) + s.substring(1).toLowerCase();
    }

    // ---------- tab completion ----------

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, String[] args) {
        if (args.length == 1) {
            return filterPrefix(Arrays.asList("summon", "dismiss", "name", "tp", "list"), args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("summon")) {
            return filterPrefix(Arrays.asList("wolf", "cat", "parrot"), args[1]);
        }
        return new ArrayList<>();
    }

    private List<String> filterPrefix(List<String> options, String prefix) {
        List<String> out = new ArrayList<>();
        String lower = prefix.toLowerCase();
        for (String s : options) if (s.toLowerCase().startsWith(lower)) out.add(s);
        return out;
    }
}
