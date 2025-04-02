//package org.example.mollyapi.order.event.V2.handler.product;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.example.mollyapi.common.exception.CustomException;
//import org.example.mollyapi.common.exception.error.impl.ProductItemError;
//import org.example.mollyapi.product.repository.ProductItemRepository;
//import org.example.mollyapi.product.repository.RedisProductItemRepository;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.data.redis.core.script.DefaultRedisScript;
//import org.springframework.scheduling.annotation.Async;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.Collections;
//import java.util.Objects;
//import java.util.Set;
//import java.util.concurrent.TimeUnit;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class StockService {
//
//    private final RedisTemplate<String, Integer> redisTemplate;
//
//    private final ProductItemRepository productItemRepository;
//    private final RedisProductItemRepository redisProductItemRepository;
//
//    public boolean processOrder(Long productItemId, Long quantity) {
//        String key = "product_stock:" + productItemId;
//
//        Integer stock = redisTemplate.opsForValue().get(key);
//        if (stock == null) {
//            System.out.println("cache miss");
//            stock = loadStockFromDB(productItemId);
//        }
//
//        String script =
//                "local current = tonumber(redis.call('GET', KEYS[1])) " +
//                        "if current and current >= tonumber(ARGV[1]) then " +
//                        "  return redis.call('DECRBY', KEYS[1], ARGV[1]) " +
//                        "else " +
//                        "  return -1 " +
//                        "end";
//
//        Long result = redisTemplate.execute(
//                new DefaultRedisScript<>(script, Long.class),
//                Collections.singletonList(key),
//                String.valueOf(quantity)
//        );
//
//        if (result != null && result >= 0) {
//            return true;
//        } else {
//            throw new CustomException(ProductItemError.SOLD_OUT);
//        }
//    }
//
//    private Integer loadStockFromDB(Long productId) {
//        int stock = productItemRepository.findById(productId)
//                .orElseThrow(() -> new CustomException(ProductItemError.SOLD_OUT))
//                .getQuantity()
//                .intValue();
//
//        // DB에서 가져온 재고를 Redis에 캐시 (TTL 1분)
//        redisTemplate.opsForValue().set("product_stock:" + productId, stock, 1, TimeUnit.MINUTES);
//        return stock;
//    }
//
//    private boolean decrementStock(String key, Integer stock, Long quantity) {
//        // Lua 스크립트로 재고 차감
//        String script = "local current = redis.call('GET', KEYS[1]) " +
//                "if current and tonumber(current) >= tonumber(ARGV[1]) then " +
//                "  return redis.call('DECRBY', KEYS[1], ARGV[1]) " +
//                "else " +
//                "  return -1 " +
//                "end";
//
//        Long result = redisTemplate.execute(new DefaultRedisScript<>(script, Long.class),
//                Collections.singletonList(key),
//                String.valueOf(quantity));
//
//        if (result >= 0) {
//            return true;
//        } else {
//            throw new CustomException(ProductItemError.SOLD_OUT);
//        }
//    }
//
//    @Async
//    @Scheduled(fixedRate = 60000)
//    @Transactional
//    public void writeBackStock() {
//        Set<String> keys = redisTemplate.keys("product_stock:*");
//        for (String key : keys) {
//            String productItemIdStr = key.replace("product_stock:", "");
//            Long productItemId = Long.parseLong(productItemIdStr);
//
//            Integer stock = redisTemplate.opsForValue().get(key);
//            if (stock != null) {
//                productItemRepository.findByIdWithLock(productItemId)
//                        .ifPresent(productItem -> {
//                            productItem.updateQuantity(Long.valueOf(stock));
//                            productItemRepository.save(productItem);
//                        });
//                // Write-Back 완료 후 캐시 삭제
////                redisTemplate.delete(key);
//            }
//        }
//        log.info("Write-Back to DB complete.");
//
//    }
//}