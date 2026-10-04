package dev.portableagent.conversation.util;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MapToolsTest {

    @Test
    void byKeys_whenHandlerIsDuplicated_shouldFailAtStartup() {
        var first = new Item(Set.of("same"));
        var second = new Item(Set.of("same"));

        assertThatThrownBy(() -> MapTools.byKeys(List.of(first, second), Item::keys))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Handler is duplicated: same");
    }

    private record Item(Set<String> keys) {}
}
