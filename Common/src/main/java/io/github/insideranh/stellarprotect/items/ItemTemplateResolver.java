package io.github.insideranh.stellarprotect.items;

import org.bukkit.inventory.ItemStack;

public final class ItemTemplateResolver {

    private ItemTemplateResolver() {
    }

    public static String materialNameOrFallback(ItemTemplate template, String fallback) {
        if (template == null) return fallback;
        ItemStack item = template.getBukkitItem();
        if (item == null || item.getType() == null) return fallback;
        return item.getType().name();
    }
}
