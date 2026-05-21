package at.helpch.chatchat.listener;

import at.helpch.chatchat.ChatChatPlugin;
import at.helpch.chatchat.api.user.ChatUser;
import at.helpch.chatchat.deafen.DeafenTimeFormatter;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

public final class PlayerListener implements Listener {

    private final ChatChatPlugin plugin;

    public PlayerListener(@NotNull final ChatChatPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    private void onJoin(final PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            final var user = plugin.usersHolder().getUser(event.getPlayer());
            final var uuid = event.getPlayer().getUniqueId();

            if (!plugin.deafenManager().isDeafened(uuid)) {
                return;
            }

            Bukkit.getScheduler().runTask(plugin, () -> user.sendMessage(plugin.configManager().messages().deafenStillActive()
                .replaceText(builder -> builder.matchLiteral("<expires>").replacement(expiresAt(uuid)))));
        });
    }

    @EventHandler
    private void onLeave(final PlayerQuitEvent event) {

        // find everyone who last messaged the person leaving, and remove their reference
        plugin.usersHolder().users().stream()
                .filter(user -> user instanceof ChatUser)
                .map(user -> (ChatUser) user)
                .filter(user -> user.lastMessagedUser().isPresent())
                .filter(user -> user.lastMessagedUser().get().player().equals(event.getPlayer()))
                .forEach(user -> user.lastMessagedUser(null));

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> plugin.usersHolder().removeUser(event.getPlayer().getUniqueId()));
    }

    private @NotNull String expiresAt(@NotNull final java.util.UUID uuid) {
        return plugin.deafenManager().expiresAt(uuid)
            .map(DeafenTimeFormatter::format)
            .orElse("never");
    }
}
