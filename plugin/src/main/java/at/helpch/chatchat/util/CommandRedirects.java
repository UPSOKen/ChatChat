package at.helpch.chatchat.util;

import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class CommandRedirects {

    private CommandRedirects() {
    }

    public static @NotNull Optional<String> rewrite(
        @NotNull final String rawCommand,
        @NotNull final Map<String, String> redirects
    ) {
        final var command = rawCommand.startsWith("/") ? rawCommand.substring(1) : rawCommand;
        final var splitAt = command.indexOf(' ');
        final var label = splitAt >= 0 ? command.substring(0, splitAt) : command;
        if (label.contains(":")) {
            return Optional.empty();
        }

        final var target = redirects.get(label.toLowerCase(Locale.ROOT));
        if (target == null) {
            return Optional.empty();
        }

        final var args = splitAt >= 0 ? command.substring(splitAt) : "";
        return Optional.of("chatchat:" + target + args);
    }
}
