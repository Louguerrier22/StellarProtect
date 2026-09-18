package io.github.insideranh.stellarprotect.database;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LocationLookupQueryTest {

    @Test
    void limitsBlockHistoryToTheRequestedWorldAndIncludesALookaheadRow() throws Exception {
        try (Connection connection = openDatabase()) {
            insertLog(connection, 1, 1, 10.0, 64.0, 10.0, 1, 100L);
            insertLog(connection, 2, 1, 10.4, 64.0, 10.0, 1, 200L);
            insertLog(connection, 3, 1, 10.0, 64.0, 10.0, 1, 300L);
            insertLog(connection, 4, 1, 10.0, 64.0, 10.0, 1, 400L);
            insertLog(connection, 5, 2, 10.0, 64.0, 10.0, 1, 500L);
            insertLog(connection, 6, 1, 10.6, 64.0, 10.0, 1, 600L);

            LocationLookupQuery query = LocationLookupQuery.create(
                "logs", "players", 1, 10, 64, 10,
                0L, 1_000L, null, 4, 0
            );

            assertEquals(Arrays.asList(4L, 3L, 2L, 1L), executeIds(connection, query));
        }
    }

    @Test
    void appliesTheOptionalActionFilter() throws Exception {
        try (Connection connection = openDatabase()) {
            insertLog(connection, 1, 1, 10.0, 64.0, 10.0, 1, 100L);
            insertLog(connection, 2, 1, 10.0, 64.0, 10.0, 2, 200L);

            LocationLookupQuery query = LocationLookupQuery.create(
                "logs", "players", 1, 10, 64, 10,
                0L, 1_000L, 2, 10, 0
            );

            assertEquals(Arrays.asList(2L), executeIds(connection, query));
        }
    }

    private Connection openDatabase() throws Exception {
        Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:");
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE players (id INTEGER PRIMARY KEY, name TEXT, uuid TEXT)");
            statement.execute("INSERT INTO players (id, name, uuid) VALUES (1, 'tester', 'uuid')");
            statement.execute("CREATE TABLE logs (" +
                "id INTEGER PRIMARY KEY, player_id INTEGER, world_id INTEGER, " +
                "x REAL, y REAL, z REAL, action_type INTEGER, created_at INTEGER)");
        }
        return connection;
    }

    private void insertLog(Connection connection, long id, int worldId, double x, double y, double z,
                           int actionType, long createdAt) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
            "INSERT INTO logs (id, player_id, world_id, x, y, z, action_type, created_at) " +
                "VALUES (?, 1, ?, ?, ?, ?, ?, ?)")) {
            statement.setLong(1, id);
            statement.setInt(2, worldId);
            statement.setDouble(3, x);
            statement.setDouble(4, y);
            statement.setDouble(5, z);
            statement.setInt(6, actionType);
            statement.setLong(7, createdAt);
            statement.executeUpdate();
        }
    }

    private List<Long> executeIds(Connection connection, LocationLookupQuery query) throws Exception {
        List<Long> ids = new ArrayList<>();
        try (PreparedStatement statement = query.prepare(connection);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                ids.add(resultSet.getLong("id"));
            }
        }
        return ids;
    }
}
