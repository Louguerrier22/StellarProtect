package io.github.insideranh.stellarprotect.database.entries.world;

import com.google.gson.JsonObject;
import io.github.insideranh.stellarprotect.database.entries.LogEntry;
import io.github.insideranh.stellarprotect.enums.ActionType;
import lombok.Getter;
import org.bukkit.Location;

import java.sql.ResultSet;

@Getter
public class RaidLogEntry extends LogEntry {

    private final int phase;

    public RaidLogEntry(ResultSet resultSet, JsonObject jsonObject) {
        super(resultSet);
        this.phase = jsonObject.has("p") ? jsonObject.get("p").getAsInt() : 0;
    }

    public RaidLogEntry(long playerId, Location location, RaidPhase phase) {
        super(playerId, ActionType.RAID.getId(), location, System.currentTimeMillis());
        this.phase = phase.ordinal();
    }

    @Override
    public String toSaveJson() {
        return "{\"p\":" + phase + "}";
    }

    @Getter
    public enum RaidPhase {
        TRIGGER(0),
        SPAWN(1),
        WAVE(2),
        FINISH(3),
        STOP(4);

        private final int id;

        RaidPhase(int id) {
            this.id = id;
        }

        public static RaidPhase fromId(int id) {
            for (RaidPhase phase : values()) {
                if (phase.id == id) return phase;
            }
            return TRIGGER;
        }
    }

}