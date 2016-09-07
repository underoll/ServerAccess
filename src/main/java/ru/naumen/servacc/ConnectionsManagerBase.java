package ru.naumen.servacc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores list of connections.
 *
 * @author arkaev
 * @since  08.09.2016
 */
public abstract class ConnectionsManagerBase<C> implements IConnectionsManager<C> {
    private List<C> connections;
    private Map<String, C> cache;

    protected abstract void closeConnections(List<C> connections);

    public ConnectionsManagerBase()
    {
        cache = new ConcurrentHashMap<>();
        connections = new ArrayList<>();
    }

    @Override
    public void put(String key, C client)
    {
        cache.put(key, client);
    }

    @Override
    public void remove(String key)
    {
        // TODO: We do not put connection into connections list, so it will not be closed at exit. Bug or feature?
        cache.remove(key);
    }

    @Override
    public C get(String key)
    {
        return cache.get(key);
    }

    @Override
    public boolean containsKey(String key)
    {
        return cache.containsKey(key);
    }

    @Override
    public void clearCache()
    {
        // keep track of all open connections so we can close them on exit
        connections.addAll(cache.values());
        cache.clear();
    }

    @Override
    public void cleanup()
    {
        clearCache();
        closeConnections(connections);
    }
}
