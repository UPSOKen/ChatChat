package at.helpch.chatchat.deafen;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DeafenManagerTest {

    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final Instant NOW = Instant.parse("2026-05-21T10:00:00Z");

    @TempDir
    Path tempDir;

    @Test
    void permanentDeafenPersistsUntilCleared() {
        final var manager = managerAt(NOW);

        manager.setPermanent(PLAYER);

        assertTrue(manager.isDeafened(PLAYER));

        manager.clear(PLAYER);

        assertFalse(manager.isDeafened(PLAYER));
    }

    @Test
    void temporaryDeafenUsesWallClockTime() {
        final var manager = managerAt(NOW);

        manager.setTemporary(PLAYER, Duration.ofHours(3));

        assertTrue(manager.isDeafened(PLAYER));
        assertTrue(managerAt(NOW.plus(Duration.ofHours(2))).isDeafened(PLAYER));
        assertFalse(managerAt(NOW.plus(Duration.ofHours(3))).isDeafened(PLAYER));
    }

    @Test
    void dataSurvivesNewManagerInstance() {
        managerAt(NOW).setPermanent(PLAYER);

        assertTrue(managerAt(NOW).isDeafened(PLAYER));
    }

    private DeafenManager managerAt(final Instant instant) {
        return new DeafenManager(tempDir.resolve("deafens.json"), Clock.fixed(instant, ZoneOffset.UTC));
    }
}
