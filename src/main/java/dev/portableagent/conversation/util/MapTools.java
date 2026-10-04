package dev.portableagent.conversation.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class MapTools {

    private MapTools() {}

    public static <K, V> Map<K, V> byKey(List<V> values, Function<V, K> key) {
        return values.stream().collect(Collectors.toUnmodifiableMap(key, Function.identity()));
    }

    public static <K, V> Map<K, V> byKeys(List<V> values, Function<V, Set<K>> keys) {
        var result = new HashMap<K, V>();
        for (var value : values) {
            for (var key : keys.apply(value)) {
                if (result.putIfAbsent(key, value) != null) {
                    throw new IllegalArgumentException("Handler is duplicated: " + key);
                }
            }
        }
        return Map.copyOf(result);
    }
}
