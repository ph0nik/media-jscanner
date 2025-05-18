package app.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    @Autowired
    private Environment env;

    @Value("${cache.settings.expire}")
    private int cacheExpirationTimeMinutes;

    public static final String MEDIA_LINKS_SERVICE = "mediaLinksService";
    public static final String LAST_REQUEST = "lastRequest";
    public static final String MEDIA_QUERIES = "mediaQueries";

    @Bean
    public Caffeine<Object, Object> caffeineConfig() {
        return Caffeine
                .newBuilder()
                .expireAfterAccess(cacheExpirationTimeMinutes, TimeUnit.MINUTES);
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
