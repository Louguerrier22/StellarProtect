package io.github.insideranh.stellarprotect.test;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.UnsafeValues;
import org.bukkit.inventory.ItemFactory;

import java.lang.reflect.Proxy;
import java.util.logging.Logger;

public final class BukkitTestSupport {

    private BukkitTestSupport() {
    }

    public static void installMinimalServer() {
        if (Bukkit.getServer() != null) {
            return;
        }

        ItemFactory itemFactory = (ItemFactory) Proxy.newProxyInstance(
            ItemFactory.class.getClassLoader(),
            new Class<?>[]{ItemFactory.class},
            (proxy, method, arguments) -> {
                if (method.getName().equals("equals")) {
                    return arguments != null && arguments.length == 2 && arguments[0] == arguments[1];
                }
                return defaultValue(method.getReturnType());
            }
        );
        UnsafeValues unsafeValues = (UnsafeValues) Proxy.newProxyInstance(
            UnsafeValues.class.getClassLoader(),
            new Class<?>[]{UnsafeValues.class},
            (proxy, method, arguments) -> {
                if (method.getName().equals("getDataVersion")) return 0;
                if (method.getName().equals("getMaterial") && arguments != null && arguments.length > 0) {
                    return Material.matchMaterial(String.valueOf(arguments[0]));
                }
                return defaultValue(method.getReturnType());
            }
        );
        Server server = (Server) Proxy.newProxyInstance(
            Server.class.getClassLoader(),
            new Class<?>[]{Server.class},
            (proxy, method, arguments) -> {
                if (method.getName().equals("getItemFactory")) return itemFactory;
                if (method.getName().equals("getUnsafe")) return unsafeValues;
                if (method.getName().equals("getLogger")) return Logger.getLogger("BukkitTestSupport");
                if (method.getName().equals("getName")) return "BukkitTestSupport";
                if (method.getName().equals("getVersion")) return "test";
                if (method.getName().equals("getBukkitVersion")) return "test";
                return defaultValue(method.getReturnType());
            }
        );
        Bukkit.setServer(server);
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0F;
        if (type == double.class) return 0D;
        if (type == char.class) return '\0';
        return null;
    }
}
