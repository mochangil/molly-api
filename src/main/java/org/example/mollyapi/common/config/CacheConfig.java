package org.example.mollyapi.common.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.jcache.JCacheCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;
import javax.cache.spi.CachingProvider;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public org.springframework.cache.CacheManager cacheManager() {
        CachingProvider cachingProvider = Caching.getCachingProvider();
        javax.cache.CacheManager jCacheManager = cachingProvider.getCacheManager();

        // 첫 번째 캐시 설정 (1분 만료)
        javax.cache.configuration.Configuration<Object, Object> categoryConfig = new MutableConfiguration<>()
                .setTypes(Object.class, Object.class)
                .setExpiryPolicyFactory(CreatedExpiryPolicy.factoryOf(Duration.ETERNAL))
                .setStoreByValue(false)
                .setStatisticsEnabled(true);

        // 두 번째 캐시 설정 (1분 만료)
        javax.cache.configuration.Configuration<Object, Object> productConfig = new MutableConfiguration<>()
                .setTypes(Object.class, Object.class)
                .setExpiryPolicyFactory(CreatedExpiryPolicy.factoryOf(new Duration(TimeUnit.MILLISECONDS, 5000L)))
                .setStoreByValue(false)
                .setStatisticsEnabled(true);

        // 각각의 캐시 추가
        jCacheManager.createCache("categories", categoryConfig);
        jCacheManager.createCache("categoryPaths", categoryConfig);
        jCacheManager.createCache("productList", productConfig);

        return new JCacheCacheManager(jCacheManager);
    }
}
