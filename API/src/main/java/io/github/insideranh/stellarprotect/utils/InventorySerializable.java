package io.github.insideranh.stellarprotect.utils;

import lombok.SneakyThrows;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectStreamClass;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class InventorySerializable {

    private static final int MAX_ITEM_ENCODED_BYTES = 16 * 1024 * 1024;
    private static final int MAX_INVENTORY_ENCODED_BYTES = 64 * 1024 * 1024;
    private static final int MAX_INVENTORY_ITEMS = 216;

    @SneakyThrows
    public static String itemStackToBase64(ItemStack item) {
        if (item == null) return null;
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream)) {
            dataOutput.writeObject(item);
            return Base64Coder.encodeLines(outputStream.toByteArray());
        } catch (Exception e) {
            return itemStackToBase64Fallback(item);
        }
    }

    @SneakyThrows
    public static ItemStack itemStackFromBase64(String data) {
        if (data == null || data.trim().isEmpty() || data.equals("null")) return null;
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(decodeBounded(data, MAX_ITEM_ENCODED_BYTES));
             BukkitObjectInputStream dataInput = new SafeBukkitObjectInputStream(inputStream)) {
            return (ItemStack) dataInput.readObject();
        } catch (Exception e) {
            return itemStackFromBase64Fallback(data);
        }
    }

    @SneakyThrows
    public static String itemStackToBase64Fallback(ItemStack item) {
        if (item == null) return null;

        try {
            YamlConfiguration config = new YamlConfiguration();
            config.set("item", item);
            String yaml = config.saveToString();
            return Base64.getEncoder().encodeToString(yaml.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("Unable to save item stacks.", e);
        }
    }

    public static ItemStack itemStackFromBase64Fallback(String data) {
        if (data == null || data.trim().isEmpty() || data.equals("null")) {
            return null;
        }

        try {
            if (data.length() > MAX_ITEM_ENCODED_BYTES) return null;
            byte[] decodedBytes = Base64.getDecoder().decode(data);
            if (decodedBytes.length > MAX_ITEM_ENCODED_BYTES) return null;
            String yaml = new String(decodedBytes, StandardCharsets.UTF_8);

            YamlConfiguration config = new YamlConfiguration();
            config.loadFromString(yaml);

            Object item = config.get("item");
            if (item instanceof ItemStack) {
                return (ItemStack) item;
            } else {
                return null;
            }

        } catch (Exception e) {
            return null;
        }
    }

    @SneakyThrows
    public static String itemStackArrayToBase64(ItemStack[] items) {
        if (items == null || items.length == 0) return "";
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream)) {
            dataOutput.writeInt(items.length);
            for (ItemStack item : items) {
                dataOutput.writeObject(item);
            }
            return Base64Coder.encodeLines(outputStream.toByteArray());
        } catch (Exception e) {
            return "";
        }
    }

    @SneakyThrows
    public static ItemStack[] itemStackArrayFromBase64(String data) {
        if (data == null || data.trim().isEmpty()) return new ItemStack[0];
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(decodeBounded(data, MAX_INVENTORY_ENCODED_BYTES));
             BukkitObjectInputStream dataInput = new SafeBukkitObjectInputStream(inputStream)) {
            int len = dataInput.readInt();
            if (len < 0 || len > MAX_INVENTORY_ITEMS) return new ItemStack[0];
            ItemStack[] result = new ItemStack[len];
            for (int i = 0; i < len; i++) {
                try {
                    result[i] = (ItemStack) dataInput.readObject();
                } catch (Exception ignored) {
                    result[i] = null;
                }
            }
            return result;
        } catch (Exception e) {
            return new ItemStack[0];
        }
    }

    private static byte[] decodeBounded(String data, int maximumBytes) throws IOException {
        if (data.length() > maximumBytes) {
            throw new IOException("Serialized item payload exceeds the encoded size limit");
        }
        byte[] decoded = Base64Coder.decodeLines(data);
        if (decoded.length > maximumBytes) {
            throw new IOException("Serialized item payload exceeds the decoded size limit");
        }
        return decoded;
    }

    private static final class SafeBukkitObjectInputStream extends BukkitObjectInputStream {

        private SafeBukkitObjectInputStream(ByteArrayInputStream inputStream) throws IOException {
            super(inputStream);
        }

        @Override
        protected Class<?> resolveClass(ObjectStreamClass descriptor) throws IOException, ClassNotFoundException {
            Class<?> resolved = super.resolveClass(descriptor);
            if (!isAllowedClass(resolved)) {
                throw new InvalidClassException("Rejected serialized class", resolved.getName());
            }
            return resolved;
        }

        @Override
        protected Class<?> resolveProxyClass(String[] interfaces) throws IOException {
            throw new InvalidClassException("Serialized proxy classes are not allowed");
        }

        private static boolean isAllowedClass(Class<?> type) {
            while (type.isArray()) type = type.getComponentType();
            if (type.isPrimitive() || type.isEnum()) return true;

            String name = type.getName();
            return name.startsWith("org.bukkit.")
                || name.startsWith("net.minecraft.")
                || name.startsWith("net.kyori.adventure.")
                || name.startsWith("io.papermc.paper.")
                || name.startsWith("com.mojang.authlib.")
                || name.startsWith("com.google.common.collect.")
                || name.startsWith("it.unimi.dsi.fastutil.")
                || name.startsWith("org.joml.")
                || name.equals("java.lang.Object")
                || name.equals("java.lang.String")
                || name.equals("java.lang.Boolean")
                || name.equals("java.lang.Byte")
                || name.equals("java.lang.Character")
                || name.equals("java.lang.Double")
                || name.equals("java.lang.Float")
                || name.equals("java.lang.Integer")
                || name.equals("java.lang.Long")
                || name.equals("java.lang.Number")
                || name.equals("java.lang.Short")
                || name.equals("java.util.ArrayList")
                || name.equals("java.util.HashMap")
                || name.equals("java.util.HashSet")
                || name.equals("java.util.LinkedHashMap")
                || name.equals("java.util.LinkedHashSet")
                || name.equals("java.util.LinkedList")
                || name.equals("java.util.TreeMap")
                || name.equals("java.util.TreeSet")
                || name.equals("java.util.UUID");
        }
    }

}
