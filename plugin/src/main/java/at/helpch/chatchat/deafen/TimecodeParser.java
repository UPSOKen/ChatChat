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
