package org.example.mollyapi.product.repository;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RedisProductItemRepository {
    private static final String STOCK_KEY_PREFIX = "stock:";

    private final RedisTemplate<String, Integer> redisTemplate;

    public RedisProductItemRepository(RedisTemplate<String, Integer> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // 재고 조회
    public Integer getStock(Long productId) {
        return redisTemplate.opsForValue().get(STOCK_KEY_PREFIX + productId);
    }

    // 재고 감소
    public boolean decreaseStock(Long productId, int quantity) {
        String key = STOCK_KEY_PREFIX + productId;
        Long stock = redisTemplate.opsForValue().decrement(key, quantity);
        return stock != null && stock >= 0; // 재고가 0 이상이면 성공
    }

    // 재고 초기화 (DB -> Redis)
    public void setStock(Long productItemId, int stock) {
        redisTemplate.opsForValue().set(STOCK_KEY_PREFIX + productItemId, stock);
    }
}