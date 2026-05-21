package at.helpch.chatchat.deafen;

import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class DeafenTimeFormatter {

    private static final DateTimeFormatter FORMATTER =
        DateTimeFormatter.ofPattern("MMM d, uuuu 'at' h:mm a z", Locale.US);

    private DeafenTimeFormatter() {
        throw new AssertionError("Util classes are not to be instantiated!");
    }

    public static @NotNull String format(@NotNull final Instant instant, @NotNull final ZoneId zoneId) {
        return FORMATTER.format(instant.atZone(zoneId));
    }

    public static @NotNull String format(@NotNull final Instant instant) {
        return format(instant, ZoneId.systemDefault());
    }
}
