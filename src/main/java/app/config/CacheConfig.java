package app.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String MEDIA_LINKS_SERVICE = "mediaLinksService";
    public static final String LAST_REQUEST = "lastRequest";
    public static final String MEDIA_QUERIES = "mediaQueries";

    @Bean
    public Caffeine<Object, Object> caffeineConfig() {
        return Caffeine
                .newBuilder()
                .expireAfterAccess(60, TimeUnit.MINUTES);
    }

    @Bean
    public CacheManager cacheManager(Caffeine<Object, Object> caffeine) {
        CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager(
                MEDIA_QUERIES, MEDIA_LINKS_SERVICE, LAST_REQUEST
        );
        caffeineCacheManager.setCaffeine(caffeine);
        return caffeineCacheManager;
    }
}
