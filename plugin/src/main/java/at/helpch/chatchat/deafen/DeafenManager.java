package at.helpch.chatchat.deafen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class DeafenManager {

    private static final long PERMANENT_EXPIRATION = -1L;
    private static final Type STORAGE_TYPE = new TypeToken<Map<String, DeafenEntry>>() {}.getType();

    private final Path dataFile;
    private final Clock clock;
    private final Gson gson = new GsonBuilder()
        .setPrettyPrinting()
        .create();
    private final Map<UUID, DeafenEntry> entries = new HashMap<>();

    public DeafenManager(@NotNull final Path dataFile, @NotNull final Clock clock) {
        this.dataFile = dataFile;
        this.clock = clock;
        load();
    }

    public synchronized boolean isDeafened(@NotNull final UUID uuid) {
        return state(uuid) != DeafenState.NOT_DEAFENED;
    }

    public synchronized @NotNull DeafenState state(@NotNull final UUID uuid) {
        expire(uuid);

        final var entry = entries.get(uuid);
        if (entry == null) {
            return DeafenState.NOT_DEAFENED;
        }

        return entry.expiresAt() == PERMANENT_EXPIRATION ? DeafenState.PERMANENT : DeafenState.TEMPORARY;
    }

    public synchronized @NotNull Optional<Instant> expiresAt(@NotNull final UUID uuid) {
        expire(uuid);

        final var entry = entries.get(uuid);
        if (entry == null || entry.expiresAt() == PERMANENT_EXPIRATION) {
            return Optional.empty();
        }

        return Optional.of(Instant.ofEpochMilli(entry.expiresAt()));
    }

    public synchronized void setPermanent(@NotNull final UUID uuid) {
        entries.put(uuid, new DeafenEntry(PERMANENT_EXPIRATION));
        save();
    }

    public synchronized void setTemporary(@NotNull final UUID uuid, @NotNull final Duration duration) {
        entries.put(uuid, new DeafenEntry(clock.instant().plus(duration).toEpochMilli()));
        save();
    }

    public synchronized void clear(@NotNull final UUID uuid) {
        if (entries.remove(uuid) != null) {
            save();
        }
    }

    public synchronized @NotNull Map<UUID, DeafenEntry> activeEntries() {
        expireAll();
        return Collections.unmodifiableMap(new HashMap<>(entries));
    }

    private void expire(@NotNull final UUID uuid) {
        final var entry = entries.get(uuid);
        if (entry == null || entry.expiresAt() == PERMANENT_EXPIRATION) {
            return;
        }

        if (Instant.ofEpochMilli(entry.expiresAt()).isAfter(clock.instant())) {
            return;
        }

        entries.remove(uuid);
        save();
    }

    private void expireAll() {
        if (entries.entrySet().removeIf(entry ->
            entry.getValue().expiresAt() != PERMANENT_EXPIRATION &&
                !Instant.ofEpochMilli(entry.getValue().expiresAt()).isAfter(clock.instant())
        )) {
            save();
        }
    }

    private void load() {
        if (!Files.exists(dataFile)) {
            return;
        }

        try (final Reader reader = Files.newBufferedReader(dataFile)) {
            final Map<String, DeafenEntry> stored = gson.fromJson(reader, STORAGE_TYPE);
            if (stored == null) {
                return;
            }

            stored.forEach((uuid, entry) -> {
                try {
                    entries.put(UUID.fromString(uuid), entry);
                } catch (IllegalArgumentException ignored) {
                    // Ignore invalid keys so one bad entry does not disable deafen loading.
                }
            });
            expireAll();
        } catch (IOException | JsonParseException exception) {
            entries.clear();
        }
    }

    private void save() {
        try {
            final var parent = dataFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            final var stored = new HashMap<String, DeafenEntry>();
            entries.forEach((uuid, entry) -> stored.put(uuid.toString(), entry));

            try (final Writer writer = Files.newBufferedWriter(dataFile)) {
                gson.toJson(stored, STORAGE_TYPE, writer);
            }
        } catch (IOException ignored) {
            // Commands and chat delivery should continue even if persistence fails.
        }
    }

    public static final class DeafenEntry {
        private long expiresAt;

        private DeafenEntry() {
        }

        private DeafenEntry(final long expiresAt) {
            this.expiresAt = expiresAt;
        }

        public long expiresAt() {
            return expiresAt;
        }
    }
}
