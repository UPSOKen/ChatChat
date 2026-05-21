package at.helpch.chatchat.command;

import dev.triumphteam.cmd.core.annotation.Default;
import dev.triumphteam.cmd.core.annotation.Command;
import dev.triumphteam.cmd.core.annotation.Optional;
import dev.triumphteam.cmd.core.annotation.SubCommand;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandRegistrationShapeTest {

    @Test
    void optionalCommandArgumentsAreOnlyFinalArguments() {
        final var commandTypes = List.of(
            ChatToggleCommand.class,
            DeafenCommand.class,
            DumpCommand.class,
            FormatTestCommand.class,
            IgnoreCommand.class,
            IgnoreListCommand.class,
            MainCommand.class,
            ManageDeafenCommand.class,
            MentionToggleCommand.class,
            RangedChatCommand.class,
            ReloadCommand.class,
            ReplyCommand.class,
            SocialSpyCommand.class,
            SwitchChannelCommand.class,
            UnignoreCommand.class,
            WhisperCommand.class,
            WhisperToggleCommand.class
        );

        for (final Class<?> commandType : commandTypes) {
            for (final var method : commandType.getDeclaredMethods()) {
                if (!method.isAnnotationPresent(SubCommand.class) && !method.isAnnotationPresent(Default.class)) {
                    continue;
                }

                final var parameters = method.getParameters();
                for (int index = 1; index < parameters.length - 1; index++) {
                    assertFalse(
                        parameters[index].isAnnotationPresent(Optional.class),
                        () -> commandType.getSimpleName() + "#" + method.getName()
                            + " has an optional argument before the final command argument"
                    );
                }
            }
        }
    }

    @Test
    void whisperDoesNotClaimShortMCommandAlias() {
        final var command = WhisperCommand.class.getAnnotation(Command.class);

        assertFalse(Arrays.asList(command.alias()).contains("m"));
    }

    @Test
    void pluginYamlDeclaresConflictingCommandLabels() throws IOException {
        final var resource = CommandRegistrationShapeTest.class.getClassLoader().getResourceAsStream("plugin.yml");
        assertNotNull(resource);
        final var pluginYaml = new String(resource.readAllBytes(), StandardCharsets.UTF_8);

        for (final var command : List.of(
            "chatchat",
            "togglechat",
            "deafen",
            "ignore",
            "unignore",
            "ignorelist",
            "togglemention",
            "rangedchat",
            "whisper",
            "reply",
            "socialspy",
            "togglemsg"
        )) {
            assertTrue(
                pluginYaml.contains("    " + command + ":"),
                () -> "plugin.yml should declare /" + command
            );
        }
    }
}
