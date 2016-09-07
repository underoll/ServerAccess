package ru.naumen.servacc;

/**
 * @author aarkaev
 * @since 08.09.2016
 */
public interface IConnectionsManager<C> {
    void put(String key, C client);

    void remove(String key);

    C get(String key);

    boolean containsKey(String key);

    void clearCache();

    void cleanup();
}
