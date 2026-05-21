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
