package io.github.insideranh.stellarprotect.items;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ItemTemplateResolverTest {

    @Test
    void usesPersistedBlockDataWhenTheHistoricalItemTemplateIsUnavailable() {
        assertEquals(
            "minecraft:shulker_box[facing=up]",
            ItemTemplateResolver.materialNameOrFallback(null, "minecraft:shulker_box[facing=up]")
        );
    }
}
