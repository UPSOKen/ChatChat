package at.helpch.chatchat.listener;

import at.helpch.chatchat.ChatChatPlugin;
import at.helpch.chatchat.util.CommandRedirects;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.jetbrains.annotations.NotNull;

public final class CommandRedirectListener implements Listener {

    private final ChatChatPlugin plugin;

    public CommandRedirectListener(@NotNull final ChatChatPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private void onPlayerCommand(@NotNull final PlayerCommandPreprocessEvent event) {
        final var rewritten = CommandRedirects.rewrite(event.getMessage(), plugin.commandRedirects());
        if (rewritten.isEmpty()) {
            return;
        }

        event.setCancelled(true);
        Bukkit.getScheduler().runTask(plugin, () -> Bukkit.dispatchCommand(event.getPlayer(), rewritten.get()));
    }
}
