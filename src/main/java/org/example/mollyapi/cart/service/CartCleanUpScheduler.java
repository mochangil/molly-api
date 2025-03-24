package org.example.mollyapi.cart.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CartCleanUpScheduler {
    private final CartService cartService;

    //매일 정각에 장바구니 만료된 데이터 삭제
    @Scheduled(cron = "0 0 0 * * *")
    public void cleanUpExpiredCarts() {
        log.info("[Scheduler] 시작: 만료된 장바구니 내역 삭제 시작");
        cartService.deleteExpiredCarts();
        log.info("[Scheduler] 완료: 만료된장바구니 내역 삭제 완료");
    }
}
