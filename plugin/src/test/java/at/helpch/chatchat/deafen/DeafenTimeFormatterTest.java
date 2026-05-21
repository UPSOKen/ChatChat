package at.helpch.chatchat.deafen;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

final class DeafenTimeFormatterTest {

    @Test
    void formatsInstantInServerTimeZone() {
        final var formatted = DeafenTimeFormatter.format(
            Instant.parse("2026-05-21T03:54:31.388Z"),
            ZoneId.of("America/Chicago")
        );

        assertEquals("May 20, 2026 at 10:54 PM CDT", formatted);
    }

    @Test
    void formattedInstantDoesNotExposeIsoTimestamp() {
        final var formatted = DeafenTimeFormatter.format(
            Instant.parse("2026-05-21T03:54:31.388Z"),
            ZoneId.of("America/Chicago")
        );

        assertFalse(formatted.contains("T03:54:31.388Z"));
    }
}
