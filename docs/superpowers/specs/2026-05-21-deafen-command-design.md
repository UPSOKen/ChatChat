# Deafen Command Design

## Goal

Add a player-controlled deafen feature to this ChatChat fork. Deafened players do not receive public ChatChat messages, but they can still send public chat, receive and send private messages, see messages from other plugins, and remain visible to chat log systems.

## Compatibility

Deafen data must not interfere with switching between this fork and upstream ChatChat.

The feature will use a separate data file, `deafens.json`, in the ChatChat data folder. It will not add fields to the existing per-user JSON files. Upstream ChatChat will ignore this file. If a server switches back to this fork later, the fork can read the file again.

## Deafen State

Each deafen entry is stored by player UUID.

- Missing UUID: the player is not deafened.
- Permanent deafen: the player is deafened until the deafen is cleared.
- Temporary deafen: the player is deafened until an absolute wall-clock expiration time.

Temporary deafens are based on real time, not server uptime or player play time. If a player starts a 3-hour deafen at 10:00 AM, it expires at 1:00 PM even if the server is offline from 11:00 AM to 12:00 PM.

Expired temporary deafens are cleared when checked by command, player join, public chat delivery, or admin listing.

## Player Commands

The player-facing command is `/deafen`.

`/deafen toggle`

Toggles the sender's own deafen. If the sender is not deafened, this creates a permanent deafen. If the sender is deafened, temporary or permanent, this clears it.

`/deafen time <timecode>`

Sets the sender's own temporary deafen. Supported timecodes are compact duration strings such as `5m`, `1h`, and `3d`.

Timecodes are one positive whole number followed by one unit: `m` for minutes, `h` for hours, or `d` for days. Decimal values, zero durations, negative durations, and combined strings such as `1h30m` are invalid.

Player commands require `chatchat.deafen`.

## Admin Commands

Admin commands live under `/chatchat manage-deafen`.

`/chatchat manage-deafen check <player>`

Shows whether the target player is deafened. If deafened, the output identifies whether the deafen is permanent or temporary and, for temporary deafens, when it expires.

`/chatchat manage-deafen list`

Lists currently online players who are deafened.

`/chatchat manage-deafen toggle <player>`

Toggles a permanent deafen for the target player. If the target is not deafened, this creates a permanent deafen. If the target is deafened, temporary or permanent, this clears it.

`/chatchat manage-deafen time <player> <timecode>`

Sets a temporary deafen for the target player.

`/chatchat manage-deafen clear <player>`

Explicitly removes any deafen from the target player.

Admin commands require `chatchat.admin.deafen`.

Admin commands must work for online and offline players. Tab completion only needs to suggest online player names, but typed offline player names are accepted using Bukkit offline-player lookup. UUID input is also accepted when it is a valid UUID string.

## Chat Behavior

ChatChat public messages skip deafened recipients during ChatChat's own recipient delivery step.

This must not cancel or modify the original Bukkit chat event for other systems. Console logging, chat log plugins, private messages, social spy, and messages sent directly by other plugins remain unaffected.

The sender can still send public chat while deafened. Deafening is only a receive filter for public ChatChat messages.

## Join Reminder

When a player joins and still has an active deafen, ChatChat sends them a configurable message telling them they are still deafened from public chat.

If the player has a temporary deafen that expired while they were offline or while the server was stopped, the deafen is cleared and no still-deafened reminder is sent.

## Messages and Tab Completion

New user-facing messages will be added to `messages.yml` and `MessagesHolder` so server owners can customize command responses and join reminders.

All new commands will support tab completion. Player target suggestions only need to include online players.

## Implementation Shape

Add a small deafen service responsible for loading, saving, querying, setting, clearing, and expiring deafen entries. Register it on the plugin main class so commands, join handling, and public chat delivery can share one source of truth.

Add player and admin command classes following the existing Triumph command style.

Update public chat delivery in `MessageProcessor` to skip recipients whose deafen is active.

Update player join handling to send the still-deafened reminder after the user is loaded.

## Verification

At minimum, verify:

- The project builds successfully.
- `/deafen toggle` creates and clears a permanent self-deafen.
- `/deafen time <timecode>` creates a temporary self-deafen with a wall-clock expiration.
- A deafened player does not receive public ChatChat messages.
- A deafened player can still send public chat.
- Private messages are unaffected.
- Admin commands can check, list, toggle, time, and clear deafens.
- Offline player admin changes persist and apply when that player joins later.
- Switching back to upstream ChatChat does not require changes to existing user JSON files.
