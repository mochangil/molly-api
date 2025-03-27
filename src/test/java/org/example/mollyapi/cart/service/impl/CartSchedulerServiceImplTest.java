package org.example.mollyapi.cart.service.impl;

import org.example.mollyapi.cart.entity.Cart;
import org.example.mollyapi.cart.repository.CartRepository;
import org.example.mollyapi.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
public class CartSchedulerServiceImplTest {
    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() throws IOException {
        String sql = new String(Files.readAllBytes(Paths.get("src/test/resources/cartSetup.sql")));
        jdbcTemplate.execute(sql);
    }

    @DisplayName("만료된 장바구니 내역을 삭제한다.")
    @Test
    void deleteExpiredCarts() {
        //given, when
        Cart testCart1 = cartRepository.findByProductItemIdAndUserUserId(1L, 1L);
        Cart testCart2 = cartRepository.findByProductItemIdAndUserUserId(2L, 1L);
        Cart testCart3 = cartRepository.findByProductItemIdAndUserUserId(3L, 1L);
        List<Long> cartIdList = Arrays.asList(testCart1.getCartId(), testCart2.getCartId(), testCart3.getCartId());

        // when
        List<Long> expiredCartIdList = cartRepository.getExpiredCartId();

        // 삭제 로직: 만료된 장바구니 ID에 대해 삭제 처리
        for (Long cartId : expiredCartIdList) {
            cartRepository.deleteById(cartId);
        }

        // then
        // 만료된 장바구니가 삭제되었는지 확인
        Optional<Cart> optionalCart1 = cartRepository.findById(cartIdList.get(0));
        Optional<Cart> optionalCart2 = cartRepository.findById(cartIdList.get(1));
        Optional<Cart> optionalCart3 = cartRepository.findById(cartIdList.get(2));

        // 삭제된 장바구니는 Optional.empty()여야 함
        assertThat(optionalCart1).isEmpty();
        assertThat(optionalCart2).isEmpty();

        // 만료되지 않은 장바구니는 여전히 존재해야 함
        assertThat(optionalCart3).isPresent();
    }
}
