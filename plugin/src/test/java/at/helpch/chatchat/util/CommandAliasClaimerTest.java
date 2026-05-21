package at.helpch.chatchat.util;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandAliasClaimerTest {

    @Test
    void claimsBareAliasesForNamespacedFallbackCommand() {
        final var knownCommands = new LinkedHashMap<String, Command>();
        final var vanillaMessage = new DummyCommand("msg");
        final var chatChatWhisper = new DummyCommand("whisper");

        final var customM = new DummyCommand("m");

        knownCommands.put("minecraft:msg", vanillaMessage);
        knownCommands.put("msg", vanillaMessage);
        knownCommands.put("m", customM);
        knownCommands.put("chatchat:whisper", chatChatWhisper);

        final var claimed = CommandAliasClaimer.claimAliases(
            knownCommands,
            "ChatChat",
            "whisper",
            List.of("msg", "tell", "w")
        );

        assertTrue(claimed);
        assertSame(chatChatWhisper, knownCommands.get("msg"));
        assertSame(chatChatWhisper, knownCommands.get("tell"));
        assertSame(chatChatWhisper, knownCommands.get("w"));
        assertSame(chatChatWhisper, knownCommands.get("chatchat:msg"));
        assertSame(vanillaMessage, knownCommands.get("minecraft:msg"));
        assertSame(customM, knownCommands.get("m"));
    }

    @Test
    void doesNotClaimAliasesWhenTargetCommandIsMissing() {
        final var knownCommands = new LinkedHashMap<String, Command>();
        final var vanillaMessage = new DummyCommand("msg");
        knownCommands.put("msg", vanillaMessage);

        final var claimed = CommandAliasClaimer.claimAliases(
            knownCommands,
            "ChatChat",
            "whisper",
            List.of("msg")
        );

        assertFalse(claimed);
        assertSame(vanillaMessage, knownCommands.get("msg"));
    }

    private static final class DummyCommand extends Command {

        private DummyCommand(@NotNull final String name) {
            super(name);
        }

        @Override
        public boolean execute(
            @NotNull final CommandSender sender,
            @NotNull final String commandLabel,
            @NotNull final String[] args
        ) {
            return true;
        }
    }
}
