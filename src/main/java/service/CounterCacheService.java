package service;

import app.config.CacheConfig;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

public class CounterCacheService {

    private static final String CHANGE_LINKS_KEY = "change-links";
    private static final String CHANGE_IGNORE_KEY = "change-ignored";
    private final CacheManager cacheManager;

    public CounterCacheService(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    void updateCache(String cacheKey, Object value) {
        if (value != null) {
            Cache cache = cacheManager.getCache(CacheConfig.MEDIA_LINKS_SERVICE);
            if (cache != null) {
                cache.put(cacheKey, value);
            }
        }
    }

    <T> T getFromCache(String cacheKey, Class<T> type) {
        Cache cache = cacheManager.getCache(CacheConfig.MEDIA_LINKS_SERVICE);
        return (cache != null) ? cache.get(cacheKey, type) : null;
    }

    void clearCache(String cacheKey) {
        Cache cache = cacheManager.getCache(CacheConfig.MEDIA_LINKS_SERVICE);
        if (cache != null) cache.evict(cacheKey);
    }

    int getChangedElementsCount(String cacheKey) {
        Integer fromCache = getFromCache(
                cacheKey,
                Integer.class);
        return (fromCache == null) ? 0 : fromCache;
    }

    void setChangedLinksCount(Integer changedLinksCount) {
        int changedElementsCount = getChangedElementsCount(CHANGE_LINKS_KEY);
        changedElementsCount += changedLinksCount;
        updateCache(CHANGE_LINKS_KEY, changedElementsCount);
    }

    String getSignedValue(Integer value) {
        if (value == null) return "0";
        if (value > 0) {
            return "+" + value;
        }
        if (value < 0) {
            return String.valueOf(value);
        }
        return "0";
    }

    // TODO is the user suppose to see last event change or session change?
    String getChangedLinksCount() {
        int changedElementsCount = getChangedElementsCount(CHANGE_LINKS_KEY);
        return getSignedValue(changedElementsCount);
    }

    void setChangedIgnoreCount(Integer changedIgnoreCount) {
        int changedElementsCount = getChangedElementsCount(CHANGE_IGNORE_KEY);
        changedElementsCount += changedIgnoreCount;
        updateCache(CHANGE_IGNORE_KEY, changedIgnoreCount);
    }

    String getChangedIgnoreCount() {
        int changedElementsCount = getChangedElementsCount(CHANGE_IGNORE_KEY);
        return getSignedValue(changedElementsCount);
    }
}
