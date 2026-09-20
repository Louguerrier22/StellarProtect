package io.github.insideranh.stellarprotect.database.types.mysql;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.zaxxer.hikari.HikariDataSource;
import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.database.repositories.ItemsRepository;
import io.github.insideranh.stellarprotect.items.ItemTemplate;
import io.github.insideranh.stellarprotect.utils.Debugger;
import io.github.insideranh.stellarprotect.utils.InventorySerializable;
import org.bukkit.inventory.ItemStack;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ItemsRepositoryMySQL implements ItemsRepository {

    private final StellarProtect stellarProtect = StellarProtect.getInstance();
    private final HikariDataSource dataSource;

    public ItemsRepositoryMySQL(HikariDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void saveItems(List<ItemTemplate> itemTemplates) {
        stellarProtect.getExecutor().execute(() -> {
            String sql = "INSERT INTO " + stellarProtect.getConfigManager().getTablesItemTemplates() + " (id, base64, s) VALUES (?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE base64 = VALUES(base64)";

            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {

                connection.setAutoCommit(false);

                int batchSize = 0;
                int maxBatchSize = 1000;

                statement.setByte(3, (byte) 0);

                for (ItemTemplate template : itemTemplates) {
                    if (template == null) continue;
                    statement.setLong(1, template.getId());
                    statement.setString(2, template.getBase64());
                    statement.addBatch();

                    batchSize++;

                    if (batchSize >= maxBatchSize) {
                        statement.executeBatch();
                        connection.commit();
                        statement.clearBatch();
                        batchSize = 0;
                    }
                }

                if (batchSize > 0) {
                    statement.executeBatch();
                }

                connection.commit();
                Debugger.debugSave("Saved " + itemTemplates.size() + " item templates in MySQL");
            } catch (SQLException e) {
                Debugger.debugSave("Error on save items in MySQL: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    @Override
    public void loadMostUsedItems() {
        ListeningExecutorService executor = MoreExecutors.listeningDecorator(
            new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(1024))
        );

        executor.execute(() -> {
            String sql = "SELECT id, base64 FROM " +
                stellarProtect.getConfigManager().getTablesItemTemplates() + " ORDER BY id";

            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql);
                 ResultSet resultSet = statement.executeQuery()) {

                int skipped = 0;
                while (resultSet.next()) {
                    try {
                        long id = resultSet.getLong("id");
                        String base64 = resultSet.getString("base64");
                        ItemStack bukkitItem = InventorySerializable.itemStackFromBase64(base64);
                        if (bukkitItem == null) {
                            skipped++;
                            continue;
                        }

                        ItemTemplate template = new ItemTemplate(id, bukkitItem, base64);
                        stellarProtect.getItemsManager().loadItemReference(template, base64);
                    } catch (Exception exception) {
                        skipped++;
                    }
                }
                long count = stellarProtect.getItemsManager().getItemReferenceCount();
                Debugger.debugLog("Loaded " + count + " item references.");
                if (skipped > 0) {
                    stellarProtect.getLogger().warning("Skipped " + skipped + " unreadable item templates while loading StellarProtect history.");
                }
            } catch (SQLException e) {
                stellarProtect.getLogger().info("Error en loadMostUsedItems: " + e.getMessage());
            }
        });

        executor.shutdown();
    }

    @Override
    public long getNextItemTemplateId() {
        String sql = "SELECT COALESCE(MAX(id), -1) + 1 FROM " +
            stellarProtect.getConfigManager().getTablesItemTemplates();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            return resultSet.next() ? resultSet.getLong(1) : 0L;
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not initialize the next item template id", exception);
        }
    }

}
