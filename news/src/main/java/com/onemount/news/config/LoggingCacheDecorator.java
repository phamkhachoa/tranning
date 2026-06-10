package com.onemount.news.config;

import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;

class LoggingCacheDecorator implements Cache {

    private static final Logger LOG = LoggerFactory.getLogger(LoggingCacheDecorator.class);

    private final Cache delegate;

    LoggingCacheDecorator(Cache delegate) {
        this.delegate = delegate;
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public Object getNativeCache() {
        return delegate.getNativeCache();
    }

    @Override
    public ValueWrapper get(Object key) {
        ValueWrapper value = delegate.get(key);
        if (value != null) {
            LOG.debug("[CACHE HIT]   cache='{}' key='{}'", delegate.getName(), key);
        } else {
            LOG.debug("[CACHE MISS]  cache='{}' key='{}'", delegate.getName(), key);
        }
        return value;
    }

    @Override
    public <T> T get(Object key, Class<T> type) {
        T value = delegate.get(key, type);
        if (value != null) {
            LOG.debug("[CACHE HIT]   cache='{}' key='{}'", delegate.getName(), key);
        } else {
            LOG.debug("[CACHE MISS]  cache='{}' key='{}'", delegate.getName(), key);
        }
        return value;
    }

    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        return delegate.get(key, valueLoader);
    }

    @Override
    public void put(Object key, Object value) {
        LOG.debug("[CACHE PUT]   cache='{}' key='{}'", delegate.getName(), key);
        delegate.put(key, value);
    }

    @Override
    public void evict(Object key) {
        LOG.debug("[CACHE EVICT] cache='{}' key='{}'", delegate.getName(), key);
        delegate.evict(key);
    }

    @Override
    public void clear() {
        LOG.debug("[CACHE CLEAR] cache='{}'", delegate.getName());
        delegate.clear();
    }
}
