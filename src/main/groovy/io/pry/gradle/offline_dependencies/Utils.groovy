package io.pry.gradle.offline_dependencies

public final class Utils {
    public static <K, V> void addToMultimap(Map<K, Set<V>> map, K key, V value) {
        if (!map.containsKey(key)) {
            map.put(key, [] as Set<V>)
        }
        map.get(key).add(value)
    }

    public static <K, V> void addToMultimap(Map<K, Set<V>> targetMap, Map<K, Set<V>> sourceMap) {
        sourceMap.each { entry ->
            entry.value.each { targetMapInnerValue ->
                addToMultimap(targetMap, entry.key, targetMapInnerValue)
            }
        }
    }
}
