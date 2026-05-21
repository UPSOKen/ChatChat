package at.helpch.chatchat.util;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandRedirectsTest {

    @Test
    void redirectsBareCommandsToChatChatNamespace() {
        final var redirects = Map.of(
            "msg", "whisper",
            "reply", "reply"
        );

        assertEquals(
            "chatchat:whisper Lecraeman hello there",
            CommandRedirects.rewrite("/msg Lecraeman hello there", redirects).orElseThrow()
        );
        assertEquals(
            "chatchat:reply hello",
            CommandRedirects.rewrite("/reply hello", redirects).orElseThrow()
        );
    }

    @Test
    void doesNotRedirectNamespacedOrUnmappedCommands() {
        final var redirects = Map.of("msg", "whisper");

        assertTrue(CommandRedirects.rewrite("/minecraft:msg Lecraeman hello", redirects).isEmpty());
        assertTrue(CommandRedirects.rewrite("/chatchat:whisper Lecraeman hello", redirects).isEmpty());
        assertTrue(CommandRedirects.rewrite("/m Lecraeman hello", redirects).isEmpty());
    }
}
