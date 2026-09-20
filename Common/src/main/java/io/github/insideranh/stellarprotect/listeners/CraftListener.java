package io.github.insideranh.stellarprotect.listeners;

import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.cache.LoggerCache;
import io.github.insideranh.stellarprotect.data.PlayerProtect;
import io.github.insideranh.stellarprotect.database.entries.players.PlayerItemLogEntry;
import io.github.insideranh.stellarprotect.enums.ActionType;
import io.github.insideranh.stellarprotect.items.ItemReference;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;

public class CraftListener implements Listener {

    private final StellarProtect plugin = StellarProtect.getInstance();

    @EventHandler
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        ItemStack itemStack = event.getInventory().getResult();
        if (itemStack == null || itemStack.getType().equals(Material.AIR)) return;

        Player player = (Player) event.getWhoClicked();
        if (ActionType.CRAFT.shouldSkipLog(player.getWorld().getName(), itemStack.getType().name())) return;

        PlayerProtect playerProtect = PlayerProtect.getPlayer(player);
        if (playerProtect == null) return;

        ItemReference itemReference = plugin.getItemsManager().getItemReference(itemStack);

        LoggerCache.addLog(new PlayerItemLogEntry(playerProtect.getPlayerId(), itemReference, player.getLocation(), ActionType.CRAFT));
    }

    @EventHandler
    public void onCraft(EnchantItemEvent event) {
        ItemStack itemStack = event.getItem();
        if (itemStack == null || itemStack.getType().equals(Material.AIR)) return;

        Player player = event.getEnchanter();
        if (ActionType.ENCHANT.shouldSkipLog(player.getWorld().getName(), itemStack.getType().name())) return;

        PlayerProtect playerProtect = PlayerProtect.getPlayer(player);
        if (playerProtect == null) return;

        ItemReference itemReference = plugin.getItemsManager().getItemReference(itemStack);

        LoggerCache.addLog(new PlayerItemLogEntry(playerProtect.getPlayerId(), itemReference, player.getLocation(), ActionType.ENCHANT));
    }

}
