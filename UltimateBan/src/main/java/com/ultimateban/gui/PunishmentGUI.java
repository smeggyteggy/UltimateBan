package com.ultimateban.gui;

import com.ultimateban.UltimateBan;
import com.ultimateban.models.PunishmentType;
import com.ultimateban.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class PunishmentGUI implements Listener {
    private final UltimateBan plugin;
    private final Player staff;
    private final Player target;
    private final Inventory inventory;

    public PunishmentGUI(UltimateBan plugin, Player staff, Player target) {
        this.plugin = plugin;
        this.staff = staff;
        this.target = target;
        this.inventory = Bukkit.createInventory(null, 27, MessageUtil.color("&cPunish " + target.getName()));
        
        // Register this class as a listener
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        
        // Add punishment options
        addPunishmentOption(PunishmentType.BAN, Material.BARRIER, "&cBan", Arrays.asList(
            "&7Click to ban the player",
            "&7This will permanently ban the player"
        ));
        addPunishmentOption(PunishmentType.TEMP_BAN, Material.CLOCK, "&cTempban", Arrays.asList(
            "&7Click to temporarily ban the player",
            "&7You will be prompted for duration"
        ));
        addPunishmentOption(PunishmentType.MUTE, Material.PAPER, "&cMute", Arrays.asList(
            "&7Click to mute the player",
            "&7This will permanently mute the player"
        ));
        addPunishmentOption(PunishmentType.TEMP_MUTE, Material.CLOCK, "&cTempmute", Arrays.asList(
            "&7Click to temporarily mute the player",
            "&7You will be prompted for duration"
        ));
        addPunishmentOption(PunishmentType.WARN, Material.BOOK, "&cWarn", Arrays.asList(
            "&7Click to warn the player",
            "&7This will add a warning to their record"
        ));
        addPunishmentOption(PunishmentType.KICK, Material.IRON_DOOR, "&cKick", Arrays.asList(
            "&7Click to kick the player",
            "&7This will kick the player from the server"
        ));
    }

    private void addPunishmentOption(PunishmentType type, Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(MessageUtil.color(name));
        meta.setLore(lore.stream().map(MessageUtil::color).collect(Collectors.toList()));
        item.setItemMeta(meta);
        inventory.addItem(item);
    }

    public void open() {
        staff.openInventory(inventory);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getInventory().equals(inventory)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) return;
        Player clicker = (Player) event.getWhoClicked();
        if (!clicker.equals(staff)) return;

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        String itemName = clickedItem.getItemMeta().getDisplayName();
        PunishmentType type = null;

        if (itemName.contains("Ban")) {
            type = PunishmentType.BAN;
        } else if (itemName.contains("Tempban")) {
            type = PunishmentType.TEMP_BAN;
        } else if (itemName.contains("Mute")) {
            type = PunishmentType.MUTE;
        } else if (itemName.contains("Tempmute")) {
            type = PunishmentType.TEMP_MUTE;
        } else if (itemName.contains("Warn")) {
            type = PunishmentType.WARN;
        } else if (itemName.contains("Kick")) {
            type = PunishmentType.KICK;
        }

        if (type != null) {
            // Close the inventory
            staff.closeInventory();
            
            // Handle the punishment
            handlePunishment(type);
        }
    }

    private void handlePunishment(PunishmentType type) {
        // TODO: Implement punishment handling with reason input and duration for temporary punishments
        // This will require creating a new GUI or using chat input
        staff.sendMessage(MessageUtil.color("&cPunishment GUI is not fully implemented yet!"));
    }
} 