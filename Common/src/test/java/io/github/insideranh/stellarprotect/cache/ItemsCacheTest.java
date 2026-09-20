package io.github.insideranh.stellarprotect.cache;

import io.github.insideranh.stellarprotect.arguments.ArgumentsParser;
import io.github.insideranh.stellarprotect.items.ItemTemplate;
import io.github.insideranh.stellarprotect.test.BukkitTestSupport;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemsCacheTest {

    @BeforeAll
    static void installBukkit() {
        BukkitTestSupport.installMinimalServer();
    }

    @Test
    void retainsAndSearchesTemplatesBeyondTheOriginalFixedCapacity() {
        ItemsCache cache = new ItemsCache();
        int templateCount = 6_000;

        for (int id = 0; id < templateCount; id++) {
            assertTrue(cache.put(template(id)), "template " + id + " was dropped");
        }

        assertEquals(templateCount, cache.size());
        assertNotNull(cache.getById(templateCount - 1L));
        assertEquals(templateCount, cache.findIdsByTypeNameContains(
            Collections.singletonList("stone"),
            ItemsCache.FieldType.LOWER_TYPE_NAME
        ).size());
    }

    @Test
    void routesDisplayAndLoreFiltersToTheirOwnIndexes() {
        ItemsCache cache = new ItemsCache();
        cache.put(template(1L, "Celestial Pickaxe", Collections.singletonList("Forged in starlight")));

        Map<String, List<String>> displayFilter = new HashMap<>();
        displayFilter.put(ArgumentsParser.DISPLAY, Collections.singletonList("celestial"));
        Map<String, List<String>> loreFilter = new HashMap<>();
        loreFilter.put(ArgumentsParser.LORE, Collections.singletonList("starlight"));

        assertEquals(Collections.singletonList(1L), cache.findIdsContains(displayFilter));
        assertEquals(Collections.singletonList(1L), cache.findIdsContains(loreFilter));
    }

    private ItemTemplate template(long id) {
        return new ItemTemplate(id, new ItemStack(Material.STONE), "base64-" + id);
    }

    private ItemTemplate template(long id, String displayName, List<String> lore) {
        ItemMeta meta = (ItemMeta) Proxy.newProxyInstance(
            ItemMeta.class.getClassLoader(),
            new Class<?>[]{ItemMeta.class},
            (proxy, method, arguments) -> {
                if (method.getName().equals("hasDisplayName")) return true;
                if (method.getName().equals("getDisplayName")) return displayName;
                if (method.getName().equals("hasLore")) return true;
                if (method.getName().equals("getLore")) return lore;
                if (method.getName().equals("clone")) return proxy;
                if (method.getReturnType() == boolean.class) return false;
                if (method.getReturnType() == int.class) return 0;
                return null;
            }
        );
        ItemStack stack = new ItemStack(Material.STONE) {
            @Override
            public boolean hasItemMeta() {
                return true;
            }

            @Override
            public ItemMeta getItemMeta() {
                return meta;
            }
        };
        return new ItemTemplate(id, stack, "base64-" + id);
    }
}
