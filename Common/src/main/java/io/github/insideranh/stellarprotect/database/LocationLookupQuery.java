package io.github.insideranh.stellarprotect.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public final class LocationLookupQuery {

    private final String sql;
    private final int worldId;
    private final long chunkKey;
    private final long startTime;
    private final long endTime;
    private final int blockX;
    private final int blockY;
    private final int blockZ;
    private final Integer actionTypeId;
    private final int limit;
    private final int skip;

    private LocationLookupQuery(String sql, int worldId, long chunkKey, int blockX, int blockY, int blockZ,
                                long startTime, long endTime, Integer actionTypeId, int limit, int skip) {
        this.sql = sql;
        this.worldId = worldId;
        this.chunkKey = chunkKey;
        this.blockX = blockX;
        this.blockY = blockY;
        this.blockZ = blockZ;
        this.startTime = startTime;
        this.endTime = endTime;
        this.actionTypeId = actionTypeId;
        this.limit = limit;
        this.skip = skip;
    }

    public static LocationLookupQuery create(String logEntriesTable, String playersTable,
                                             int worldId, int blockX, int blockY, int blockZ,
                                             long startTime, long endTime, Integer actionTypeId,
                                             int limit, int skip) {
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be positive");
        }
        if (skip < 0) {
            throw new IllegalArgumentException("skip cannot be negative");
        }

        String actionTypeFilter = actionTypeId != null ? "AND ple.action_type = ? " : "";
        String sql =
            "SELECT ple.*, p.name, p.uuid " +
                "FROM " + logEntriesTable + " ple " +
                "LEFT JOIN " + playersTable + " p ON ple.player_id = p.id " +
                "WHERE ple.world_id = ? " +
                "AND (ple.chunk_key = ? OR ple.chunk_key IS NULL) " +
                "AND ple.created_at BETWEEN ? AND ? " +
                "AND ple.x BETWEEN ? AND ? " +
                "AND ple.y BETWEEN ? AND ? " +
                "AND ple.z BETWEEN ? AND ? " + actionTypeFilter +
                "ORDER BY ple.created_at DESC, ple.id DESC " +
                "LIMIT ? OFFSET ?";

        return new LocationLookupQuery(sql, worldId, chunkKeyOf(worldId, blockX, blockZ), blockX, blockY, blockZ,
            startTime, endTime, actionTypeId, limit, skip);
    }

    static long chunkKeyOf(int worldId, int blockX, int blockZ) {
        int chunkX = blockX >> 4;
        int chunkZ = blockZ >> 4;
        long high = worldId & 0xFFFFFFFFL;
        long low = ((long) (chunkX & 0xFFFF) << 16) | (chunkZ & 0xFFFF);
        return (high << 32) | (low & 0xFFFFFFFFL);
    }

    public PreparedStatement prepare(Connection connection) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(sql);
        bind(statement);
        return statement;
    }

    String sql() {
        return sql;
    }

    void bind(PreparedStatement statement) throws SQLException {
        int parameter = 1;

        statement.setInt(parameter++, worldId);
        statement.setLong(parameter++, chunkKey);
        statement.setLong(parameter++, startTime);
        statement.setLong(parameter++, endTime);
        statement.setDouble(parameter++, blockX - 0.5);
        statement.setDouble(parameter++, blockX + 0.5);
        statement.setDouble(parameter++, blockY - 0.5);
        statement.setDouble(parameter++, blockY + 0.5);
        statement.setDouble(parameter++, blockZ - 0.5);
        statement.setDouble(parameter++, blockZ + 0.5);

        if (actionTypeId != null) {
            statement.setInt(parameter++, actionTypeId);
        }

        statement.setInt(parameter++, limit);
        statement.setInt(parameter, skip);
    }
}
