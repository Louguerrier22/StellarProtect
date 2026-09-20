package io.github.insideranh.stellarprotect.utils;

import io.github.insideranh.stellarprotect.test.BukkitTestSupport;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class InventorySerializableSecurityTest {

    @BeforeAll
    static void installBukkit() {
        BukkitTestSupport.installMinimalServer();
    }

    @Test
    void roundTripsAnAllowedBukkitItem() {
        ItemStack item = new ItemStack(Material.STONE, 3);

        ItemStack restored = InventorySerializable.itemStackFromBase64(
            InventorySerializable.itemStackToBase64(item));

        assertEquals(item, restored);
    }

    @Test
    void rejectsAnUnexpectedSerializedClassBeforeItsReadHookRuns() throws Exception {
        UnexpectedPayload.readHookRan = false;

        assertNull(InventorySerializable.itemStackFromBase64(serialize(new UnexpectedPayload())));
        assertFalse(UnexpectedPayload.readHookRan);
    }

    @Test
    void rejectsOversizedEncodedItems() {
        char[] oversized = new char[16 * 1024 * 1024 + 1];
        Arrays.fill(oversized, 'A');

        assertNull(InventorySerializable.itemStackFromBase64(new String(oversized)));
    }

    @Test
    void rejectsUnboundedInventoryLengthsBeforeAllocation() throws Exception {
        assertEquals(0, InventorySerializable.itemStackArrayFromBase64(serializeLength(217)).length);
    }

    private String serialize(Object value) throws Exception {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
             BukkitObjectOutputStream objectOutput = new BukkitObjectOutputStream(output)) {
            objectOutput.writeObject(value);
            return Base64Coder.encodeLines(output.toByteArray());
        }
    }

    private String serializeLength(int length) throws Exception {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
             BukkitObjectOutputStream objectOutput = new BukkitObjectOutputStream(output)) {
            objectOutput.writeInt(length);
            return Base64Coder.encodeLines(output.toByteArray());
        }
    }

    private static final class UnexpectedPayload implements Serializable {
        private static final long serialVersionUID = 1L;
        private static boolean readHookRan;

        private void readObject(ObjectInputStream input) throws IOException, ClassNotFoundException {
            readHookRan = true;
            input.defaultReadObject();
        }
    }
}
