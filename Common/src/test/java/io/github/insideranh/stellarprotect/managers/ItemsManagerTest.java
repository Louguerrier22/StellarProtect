package io.github.insideranh.stellarprotect.managers;

import io.github.insideranh.stellarprotect.items.ItemTemplate;
import io.github.insideranh.stellarprotect.test.BukkitTestSupport;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ItemsManagerTest {

    @BeforeAll
    static void installBukkit() {
        BukkitTestSupport.installMinimalServer();
    }

    @Test
    void advancesTheNextIdPastTheHighestPersistedTemplateId() {
        ItemsManager manager = new ItemsManager();

        manager.loadItemReference(template(25L), "base64-25");
        manager.loadItemReference(template(9_000L), "base64-9000");
        manager.loadItemReference(template(4L), "base64-4");

        assertEquals(9_001L, manager.getCurrentId().get());
    }

    @Test
    void deduplicatesConcurrentCreationOfTheSameTemplate() throws Exception {
        ItemsManager manager = new ItemsManager();
        ExecutorService executor = Executors.newFixedThreadPool(8);
        try {
            List<Callable<Long>> tasks = new ArrayList<>();
            for (int i = 0; i < 64; i++) {
                tasks.add(() -> manager.createItemTemplate(new ItemStack(Material.STONE), "same-base64"));
            }

            List<Future<Long>> ids = executor.invokeAll(tasks);
            for (Future<Long> id : ids) assertEquals(0L, id.get());
            assertEquals(1L, manager.getCurrentId().get());
            assertEquals(1L, manager.getItemReferenceCount());
        } finally {
            executor.shutdownNow();
        }
    }

    private ItemTemplate template(long id) {
        return new ItemTemplate(id, new ItemStack(Material.STONE), "base64-" + id);
    }
}
