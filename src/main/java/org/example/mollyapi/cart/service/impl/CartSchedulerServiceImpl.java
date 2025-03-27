package org.example.mollyapi.cart.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.mollyapi.cart.repository.CartRepository;
import org.example.mollyapi.cart.service.CartSchedulerService;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CartSchedulerServiceImpl implements CartSchedulerService {
    private final CartRepository cartRep;

    /**
     * 장바구니 보관기간이 365일은 넘긴 상품 삭제
     * */
    public void deleteExpiredCarts() {
        List<Long> expiredCartIdList = cartRep.getExpiredCartId();
        if(!expiredCartIdList.isEmpty()) {
            for(Long cartId : expiredCartIdList) {
                log.info("삭제되는 장바구니 ID: " + cartId);
                cartRep.deleteById(cartId);
            }
        } else {
            log.info("만료된 장바구니 내역이 존재하지 않습니다.");
        }
    }
}
