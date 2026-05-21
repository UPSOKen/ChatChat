package at.helpch.chatchat.command;

import at.helpch.chatchat.ChatChatPlugin;
import at.helpch.chatchat.api.user.User;
import at.helpch.chatchat.deafen.TimecodeParser;
import dev.triumphteam.cmd.bukkit.annotation.Permission;
import dev.triumphteam.cmd.core.annotation.Optional;
import dev.triumphteam.cmd.core.annotation.SubCommand;
import dev.triumphteam.cmd.core.annotation.Suggestion;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.stream.Collectors;

public final class ManageDeafenCommand extends ChatChatCommand {

    private static final String ADMIN_DEAFEN_PERMISSION = "chatchat.admin.deafen";
    private final ChatChatPlugin plugin;

    public ManageDeafenCommand(@NotNull final ChatChatPlugin plugin) {
        this.plugin = plugin;
    }

    @SubCommand("manage-deafen")
    @Permission(ADMIN_DEAFEN_PERMISSION)
    public void manage(
        final User sender,
        final @Suggestion("deafen-actions") String action,
        final @Suggestion("online-players") @Optional String target,
        final @Optional String timecode
    ) {
        switch (action.toLowerCase()) {
            case "check" -> check(sender, target);
            case "list" -> list(sender);
            case "toggle" -> toggle(sender, target);
            case "time" -> time(sender, target, timecode);
            case "clear" -> clear(sender, target);
            default -> sender.sendMessage(plugin.configManager().messages().invalidUsage());
        }

    }

    private void check(final User sender, final String target) {
        if (target == null) {
            sender.sendMessage(plugin.configManager().messages().invalidUsage());
            return;
        }

        final var player = targetPlayer(target);
        sender.sendMessage(statusMessage(player));
    }

    private void list(final User sender) {
        final var players = Bukkit.getOnlinePlayers()
            .stream()
            .filter(player -> plugin.deafenManager().isDeafened(player.getUniqueId()))
            .map(OfflinePlayer::getName)
            .collect(Collectors.joining(", "));

        if (players.isBlank()) {
            sender.sendMessage(plugin.configManager().messages().deafenAdminListNone());
            return;
        }

        sender.sendMessage(plugin.configManager().messages().deafenAdminList()
            .replaceText(builder -> builder.matchLiteral("<players>").replacement(players)));
    }

    private void toggle(final User sender, final String target) {
        if (target == null) {
            sender.sendMessage(plugin.configManager().messages().invalidUsage());
            return;
        }

        final var player = targetPlayer(target);

        if (plugin.deafenManager().isDeafened(player.uuid())) {
            plugin.deafenManager().clear(player.uuid());
            sender.sendMessage(plugin.configManager().messages().deafenAdminCleared()
                .replaceText(builder -> builder.matchLiteral("<player>").replacement(player.name())));
            return;
        }

        plugin.deafenManager().setPermanent(player.uuid());
        sender.sendMessage(plugin.configManager().messages().deafenAdminEnabled()
            .replaceText(builder -> builder.matchLiteral("<player>").replacement(player.name())));
    }

    private void time(final User sender, final String target, final String timecode) {
        if (target == null || timecode == null) {
            sender.sendMessage(plugin.configManager().messages().invalidUsage());
            return;
        }

        final var duration = TimecodeParser.parse(timecode);
        if (duration.isEmpty()) {
            sender.sendMessage(plugin.configManager().messages().deafenInvalidTime());
            return;
        }

        final var player = targetPlayer(target);
        plugin.deafenManager().setTemporary(player.uuid(), duration.get());
        sender.sendMessage(plugin.configManager().messages().deafenAdminTimed()
            .replaceText(builder -> builder.matchLiteral("<player>").replacement(player.name()))
            .replaceText(builder -> builder.matchLiteral("<expires>").replacement(expiresAt(player.uuid()))));
    }

    private void clear(final User sender, final String target) {
        if (target == null) {
            sender.sendMessage(plugin.configManager().messages().invalidUsage());
            return;
        }

        final var player = targetPlayer(target);
        plugin.deafenManager().clear(player.uuid());
        sender.sendMessage(plugin.configManager().messages().deafenAdminCleared()
            .replaceText(builder -> builder.matchLiteral("<player>").replacement(player.name())));
    }

    private @NotNull Component statusMessage(@NotNull final TargetPlayer player) {
        return switch (plugin.deafenManager().state(player.uuid())) {
            case NOT_DEAFENED -> plugin.configManager().messages().deafenStatusNone()
                .replaceText(builder -> builder.matchLiteral("<player>").replacement(player.name()));
            case PERMANENT -> plugin.configManager().messages().deafenStatusPermanent()
                .replaceText(builder -> builder.matchLiteral("<player>").replacement(player.name()));
            case TEMPORARY -> plugin.configManager().messages().deafenStatusTemporary()
                .replaceText(builder -> builder.matchLiteral("<player>").replacement(player.name()))
                .replaceText(builder -> builder.matchLiteral("<expires>").replacement(expiresAt(player.uuid())));
        };
    }

    private @NotNull String expiresAt(@NotNull final UUID uuid) {
        return plugin.deafenManager().expiresAt(uuid)
            .map(DateTimeFormatter.ISO_INSTANT::format)
            .orElse("never");
    }

    private static @NotNull TargetPlayer targetPlayer(@NotNull final String input) {
        final var online = Bukkit.getPlayerExact(input);
        if (online != null) {
            return new TargetPlayer(online.getUniqueId(), online.getName());
        }

        try {
            final var uuid = UUID.fromString(input);
            final var offlineByUuid = Bukkit.getOfflinePlayer(uuid);
            return new TargetPlayer(uuid, offlineByUuid.getName() == null ? uuid.toString() : offlineByUuid.getName());
        } catch (IllegalArgumentException ignored) {
        }

        final OfflinePlayer offline = Bukkit.getOfflinePlayer(input);
        return new TargetPlayer(offline.getUniqueId(), offline.getName() == null ? input : offline.getName());
    }

    private record TargetPlayer(@NotNull UUID uuid, @NotNull String name) {
    }
}
