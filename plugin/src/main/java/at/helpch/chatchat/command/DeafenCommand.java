package at.helpch.chatchat.command;

import at.helpch.chatchat.ChatChatPlugin;
import at.helpch.chatchat.api.user.ChatUser;
import at.helpch.chatchat.deafen.DeafenTimeFormatter;
import at.helpch.chatchat.deafen.TimecodeParser;
import dev.triumphteam.cmd.bukkit.annotation.Permission;
import dev.triumphteam.cmd.core.BaseCommand;
import dev.triumphteam.cmd.core.annotation.Command;
import dev.triumphteam.cmd.core.annotation.Default;
import dev.triumphteam.cmd.core.annotation.SubCommand;
import org.jetbrains.annotations.NotNull;

@Command("deafen")
public final class DeafenCommand extends BaseCommand {

    private static final String DEAFEN_PERMISSION = "chatchat.deafen";
    private final ChatChatPlugin plugin;

    public DeafenCommand(@NotNull final ChatChatPlugin plugin) {
        this.plugin = plugin;
    }

    @Default
    @Permission(DEAFEN_PERMISSION)
    public void status(final ChatUser sender) {
        sender.sendMessage(switch (plugin.deafenManager().state(sender.uuid())) {
            case NOT_DEAFENED -> plugin.configManager().messages().deafenSelfStatusNone();
            case PERMANENT -> plugin.configManager().messages().deafenSelfStatusPermanent();
            case TEMPORARY -> plugin.configManager().messages().deafenSelfStatusTemporary()
                .replaceText(builder -> builder.matchLiteral("<expires>").replacement(expiresAt(sender)));
        });
    }

    @SubCommand("toggle")
    @Permission(DEAFEN_PERMISSION)
    public void toggle(final ChatUser sender) {
        if (plugin.deafenManager().isDeafened(sender.uuid())) {
            plugin.deafenManager().clear(sender.uuid());
            sender.sendMessage(plugin.configManager().messages().deafenDisabled());
            return;
        }

        plugin.deafenManager().setPermanent(sender.uuid());
        sender.sendMessage(plugin.configManager().messages().deafenEnabled());
    }

    @SubCommand("time")
    @Permission(DEAFEN_PERMISSION)
    public void time(final ChatUser sender, final String timecode) {
        final var duration = TimecodeParser.parse(timecode);
        if (duration.isEmpty()) {
            sender.sendMessage(plugin.configManager().messages().deafenInvalidTime());
            return;
        }

        plugin.deafenManager().setTemporary(sender.uuid(), duration.get());
        sender.sendMessage(plugin.configManager().messages().deafenTimed()
            .replaceText(builder -> builder.matchLiteral("<expires>").replacement(expiresAt(sender))));
    }

    @SubCommand("clear")
    @Permission(DEAFEN_PERMISSION)
    public void clear(final ChatUser sender) {
        if (!plugin.deafenManager().isDeafened(sender.uuid())) {
            sender.sendMessage(plugin.configManager().messages().deafenClearNotActive());
            return;
        }

        plugin.deafenManager().clear(sender.uuid());
        sender.sendMessage(plugin.configManager().messages().deafenDisabled());
    }

    private @NotNull String expiresAt(@NotNull final ChatUser user) {
        return plugin.deafenManager().expiresAt(user.uuid())
            .map(DeafenTimeFormatter::format)
            .orElse("never");
    }
}
