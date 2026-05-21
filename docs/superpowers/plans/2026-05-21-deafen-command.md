# Deafen Command Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add permissioned `/deafen` player commands and `/chatchat manage-deafen` admin commands that filter public ChatChat delivery per recipient.

**Architecture:** Store deafen state in a fork-owned `deafens.json` file keyed by UUID, with permanent and wall-clock temporary entries. A focused deafen manager owns parsing, expiry, persistence, and status descriptions; commands, join handling, and message delivery all call that manager.

**Tech Stack:** Java 21, Gradle Kotlin DSL, Bukkit/Spigot API, Triumph commands, Adventure components, Gson, JUnit Jupiter for pure unit tests.

---

### Task 1: Add Test Support and Time Parser

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `plugin/build.gradle.kts`
- Create: `plugin/src/main/java/at/helpch/chatchat/deafen/TimecodeParser.java`
- Test: `plugin/src/test/java/at/helpch/chatchat/deafen/TimecodeParserTest.java`

- [ ] **Step 1: Add JUnit version and library aliases**

In `gradle/libs.versions.toml`, add:

```toml
junit = "5.11.4"
```

Under `[libraries]`, add:

```toml
junit-bom = { module = "org.junit:junit-bom", version.ref = "junit" }
junit-jupiter = { module = "org.junit.jupiter:junit-jupiter" }
```

- [ ] **Step 2: Enable JUnit in the plugin module**

In `plugin/build.gradle.kts`, add these dependencies:

```kotlin
testImplementation(platform(libs.junit.bom))
testImplementation(libs.junit.jupiter)
```

Add this inside the existing `tasks` block:

```kotlin
test {
    useJUnitPlatform()
}
```

- [ ] **Step 3: Write failing parser tests**

Create `plugin/src/test/java/at/helpch/chatchat/deafen/TimecodeParserTest.java`:

```java
package at.helpch.chatchat.deafen;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TimecodeParserTest {

    @Test
    void parsesMinutesHoursAndDays() {
        assertEquals(Optional.of(Duration.ofMinutes(5)), TimecodeParser.parse("5m"));
        assertEquals(Optional.of(Duration.ofHours(1)), TimecodeParser.parse("1h"));
        assertEquals(Optional.of(Duration.ofDays(3)), TimecodeParser.parse("3d"));
    }

    @Test
    void rejectsInvalidTimecodes() {
        assertTrue(TimecodeParser.parse("0m").isEmpty());
        assertTrue(TimecodeParser.parse("-1h").isEmpty());
        assertTrue(TimecodeParser.parse("1.5h").isEmpty());
        assertTrue(TimecodeParser.parse("1h30m").isEmpty());
        assertTrue(TimecodeParser.parse("10s").isEmpty());
        assertTrue(TimecodeParser.parse("").isEmpty());
    }
}
```

- [ ] **Step 4: Run parser tests and verify RED**

Run: `.\gradlew.bat :chat-chat-plugin:test --tests at.helpch.chatchat.deafen.TimecodeParserTest`

Expected: compilation fails because `TimecodeParser` does not exist.

- [ ] **Step 5: Implement the parser**

Create `plugin/src/main/java/at/helpch/chatchat/deafen/TimecodeParser.java`:

```java
package at.helpch.chatchat.deafen;

import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Optional;
import java.util.regex.Pattern;

public final class TimecodeParser {

    private static final Pattern TIME_PATTERN = Pattern.compile("^[1-9]\\d*[mhd]$");

    private TimecodeParser() {
        throw new AssertionError("Util classes are not to be instantiated!");
    }

    public static @NotNull Optional<Duration> parse(@NotNull final String timecode) {
        if (!TIME_PATTERN.matcher(timecode).matches()) {
            return Optional.empty();
        }

        final var amount = Long.parseLong(timecode.substring(0, timecode.length() - 1));
        final var unit = timecode.charAt(timecode.length() - 1);

        return Optional.of(switch (unit) {
            case 'm' -> Duration.ofMinutes(amount);
            case 'h' -> Duration.ofHours(amount);
            case 'd' -> Duration.ofDays(amount);
            default -> throw new IllegalStateException("Unexpected time unit: " + unit);
        });
    }
}
```

- [ ] **Step 6: Run parser tests and verify GREEN**

Run: `.\gradlew.bat :chat-chat-plugin:test --tests at.helpch.chatchat.deafen.TimecodeParserTest`

Expected: tests pass.

### Task 2: Add Deafen Data Model and Manager

**Files:**
- Create: `plugin/src/main/java/at/helpch/chatchat/deafen/DeafenState.java`
- Create: `plugin/src/main/java/at/helpch/chatchat/deafen/DeafenManager.java`
- Test: `plugin/src/test/java/at/helpch/chatchat/deafen/DeafenManagerTest.java`

- [ ] **Step 1: Write failing manager tests**

Create `plugin/src/test/java/at/helpch/chatchat/deafen/DeafenManagerTest.java`:

```java
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
```

- [ ] **Step 2: Run manager tests and verify RED**

Run: `.\gradlew.bat :chat-chat-plugin:test --tests at.helpch.chatchat.deafen.DeafenManagerTest`

Expected: compilation fails because `DeafenManager` does not exist.

- [ ] **Step 3: Add deafen state enum**

Create `plugin/src/main/java/at/helpch/chatchat/deafen/DeafenState.java`:

```java
package at.helpch.chatchat.deafen;

public enum DeafenState {
    NOT_DEAFENED,
    PERMANENT,
    TEMPORARY
}
```

- [ ] **Step 4: Implement manager persistence and expiry**

Create `plugin/src/main/java/at/helpch/chatchat/deafen/DeafenManager.java` with methods:

```java
public DeafenManager(@NotNull Path dataFile, @NotNull Clock clock)
public boolean isDeafened(@NotNull UUID uuid)
public @NotNull DeafenState state(@NotNull UUID uuid)
public @NotNull Optional<Instant> expiresAt(@NotNull UUID uuid)
public void setPermanent(@NotNull UUID uuid)
public void setTemporary(@NotNull UUID uuid, @NotNull Duration duration)
public void clear(@NotNull UUID uuid)
public @NotNull Map<UUID, DeafenEntry> activeEntries()
```

The JSON format is:

```json
{
  "00000000-0000-0000-0000-000000000001": {
    "expiresAt": -1
  }
}
```

Permanent entries use `expiresAt = -1`. Temporary entries use epoch milliseconds.

- [ ] **Step 5: Run manager tests and verify GREEN**

Run: `.\gradlew.bat :chat-chat-plugin:test --tests at.helpch.chatchat.deafen.DeafenManagerTest`

Expected: tests pass.

### Task 3: Wire Commands and Messages

**Files:**
- Modify: `plugin/src/main/java/at/helpch/chatchat/ChatChatPlugin.java`
- Create: `plugin/src/main/java/at/helpch/chatchat/command/DeafenCommand.java`
- Create: `plugin/src/main/java/at/helpch/chatchat/command/ManageDeafenCommand.java`
- Modify: `plugin/src/main/java/at/helpch/chatchat/config/holder/MessagesHolder.java`
- Modify: `plugin/src/main/resources/messages.yml`
- Modify: `plugin/build.gradle.kts`

- [ ] **Step 1: Add manager field and command registration**

Add a `DeafenManager` field to `ChatChatPlugin`, initialize it with `getDataFolder().toPath().resolve("deafens.json")` and `Clock.systemUTC()`, add a `deafenManager()` getter, register online-player suggestions, and register `DeafenCommand` plus `ManageDeafenCommand`.

- [ ] **Step 2: Add messages**

Add customizable message fields and getters for self deafen enabled, disabled, timed, invalid timecode, join reminder, admin check/list/toggle/time/clear responses, and player-not-known.

- [ ] **Step 3: Implement `/deafen`**

Create `DeafenCommand` with:

```java
@Command("deafen")
public final class DeafenCommand extends BaseCommand {
    @SubCommand("toggle")
    @Permission("chatchat.deafen")
    public void toggle(ChatUser sender) {
        if (plugin.deafenManager().isDeafened(sender.uuid())) {
            plugin.deafenManager().clear(sender.uuid());
            sender.sendMessage(plugin.configManager().messages().deafenDisabled());
            return;
        }

        plugin.deafenManager().setPermanent(sender.uuid());
        sender.sendMessage(plugin.configManager().messages().deafenEnabled());
    }

    @SubCommand("time")
    @Permission("chatchat.deafen")
    public void time(ChatUser sender, String timecode) {
        final var duration = TimecodeParser.parse(timecode);
        if (duration.isEmpty()) {
            sender.sendMessage(plugin.configManager().messages().deafenInvalidTime());
            return;
        }

        plugin.deafenManager().setTemporary(sender.uuid(), duration.get());
        sender.sendMessage(plugin.configManager().messages().deafenTimed());
    }
}
```

- [ ] **Step 4: Implement `/chatchat manage-deafen`**

Create `ManageDeafenCommand extends ChatChatCommand` with `check`, `list`, `toggle`, `time`, and `clear` subcommands. Each target argument is a `String` so offline names and UUIDs can be resolved manually.

- [ ] **Step 5: Register plugin permissions**

In `plugin/build.gradle.kts`, add generated Bukkit permissions for `chatchat.deafen` and `chatchat.admin.deafen`.

### Task 4: Integrate Public Chat Filtering and Join Reminder

**Files:**
- Modify: `plugin/src/main/java/at/helpch/chatchat/util/MessageProcessor.java`
- Modify: `plugin/src/main/java/at/helpch/chatchat/listener/PlayerListener.java`

- [ ] **Step 1: Filter deafened recipients**

In `MessageProcessor.process`, before sending to a non-console target, skip recipients where `plugin.deafenManager().isDeafened(target.uuid())` is true.

- [ ] **Step 2: Send join reminder**

In `PlayerListener.onJoin`, after the async user load, check `plugin.deafenManager().isDeafened(event.getPlayer().getUniqueId())`. If active, send the configurable reminder to the loaded user.

### Task 5: Verify and Commit

**Files:**
- Verify all touched files.

- [ ] **Step 1: Run unit tests**

Run: `.\gradlew.bat :chat-chat-plugin:test`

Expected: all JUnit tests pass.

- [ ] **Step 2: Run full build**

Run: `.\gradlew.bat build --no-daemon --stacktrace`

Expected: build succeeds. Existing Javadoc warnings may remain.

- [ ] **Step 3: Review git diff**

Run: `git diff --stat`

Expected: only deafen feature files, messages, build files, and docs plan changes are present.

- [ ] **Step 4: Commit implementation**

Run:

```powershell
git add -- .
git commit -m "feat: add deafen command"
```
