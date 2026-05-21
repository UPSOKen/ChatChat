package at.helpch.chatchat.util;

import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;

public final class CommandAliasClaimer {

    private CommandAliasClaimer() {
    }

    public static boolean claimAliases(
        @NotNull final JavaPlugin plugin,
        @NotNull final String commandName,
        @NotNull final Collection<String> aliases
    ) {
        try {
            return claimAliases(knownCommands(commandMap(plugin)), plugin.getName(), commandName, aliases);
        } catch (final ReflectiveOperationException | RuntimeException exception) {
            plugin.getLogger().warning("Unable to claim command aliases for /" + commandName + ": " + exception.getMessage());
            return false;
        }
    }

    public static void syncCommands(@NotNull final JavaPlugin plugin) {
        try {
            final Method method = plugin.getServer().getClass().getMethod("syncCommands");
            method.invoke(plugin.getServer());
        } catch (final ReflectiveOperationException | RuntimeException exception) {
            plugin.getLogger().warning("Unable to sync server command tree: " + exception.getMessage());
        }

        for (final Player player : plugin.getServer().getOnlinePlayers()) {
            player.updateCommands();
        }
    }

    static boolean claimAliases(
        @NotNull final Map<String, Command> knownCommands,
        @NotNull final String pluginName,
        @NotNull final String commandName,
        @NotNull final Collection<String> aliases
    ) {
        final var fallbackPrefix = normalize(pluginName);
        final var normalizedCommand = normalize(commandName);
        final var command = findTargetCommand(knownCommands, fallbackPrefix, normalizedCommand);
        if (command == null) {
            return false;
        }

        for (final var alias : aliases) {
            final var normalizedAlias = normalize(alias);
            knownCommands.put(normalizedAlias, command);
            knownCommands.put(fallbackPrefix + ":" + normalizedAlias, command);
        }

        return true;
    }

    private static Command findTargetCommand(
        @NotNull final Map<String, Command> knownCommands,
        @NotNull final String fallbackPrefix,
        @NotNull final String commandName
    ) {
        final var namespacedCommand = knownCommands.get(fallbackPrefix + ":" + commandName);
        if (namespacedCommand != null) {
            return namespacedCommand;
        }

        return knownCommands.get(commandName);
    }

    private static CommandMap commandMap(@NotNull final JavaPlugin plugin) throws ReflectiveOperationException {
        final Method method = plugin.getServer().getClass().getMethod("getCommandMap");
        return (CommandMap) method.invoke(plugin.getServer());
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Command> knownCommands(@NotNull final CommandMap commandMap)
        throws ReflectiveOperationException {
        Class<?> type = commandMap.getClass();
        while (type != null) {
            try {
                final Field field = type.getDeclaredField("knownCommands");
                field.setAccessible(true);
                return (Map<String, Command>) field.get(commandMap);
            } catch (final NoSuchFieldException ignored) {
                type = type.getSuperclass();
            }
        }

        throw new NoSuchFieldException("knownCommands");
    }

    private static String normalize(@NotNull final String label) {
        return label.startsWith("/")
            ? label.substring(1).toLowerCase(Locale.ROOT)
            : label.toLowerCase(Locale.ROOT);
    }
}
