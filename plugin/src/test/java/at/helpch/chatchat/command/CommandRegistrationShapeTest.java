package at.helpch.chatchat.command;

import dev.triumphteam.cmd.core.annotation.Default;
import dev.triumphteam.cmd.core.annotation.Optional;
import dev.triumphteam.cmd.core.annotation.SubCommand;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;

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
}
