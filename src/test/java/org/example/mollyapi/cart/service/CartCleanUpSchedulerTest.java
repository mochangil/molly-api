package org.example.mollyapi.cart.service;

import org.example.mollyapi.cart.entity.Cart;
import org.example.mollyapi.cart.repository.CartRepository;
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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
public class CartCleanUpSchedulerTest {
    @Autowired
    private CartCleanUpScheduler cartCleanUpScheduler;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    void setUp() throws IOException {
        String sql = new String(Files.readAllBytes(Paths.get("src/test/resources/cartSetup.sql")));
        jdbcTemplate.execute(sql);
    }

    @DisplayName("만료된 장바구니 내역을 삭제한다.")
    @Test
    void deleteExpiredCarts_WithExpiredCarts() throws IOException {
        setUp();
        // given
        Cart testCart1 = cartRepository.findByProductItemIdAndUserUserId(1L, 1L);
        Cart testCart2 = cartRepository.findByProductItemIdAndUserUserId(2L, 1L);
        Cart testCart3 = cartRepository.findByProductItemIdAndUserUserId(3L, 1L);
        List<Long> cartIdList = Arrays.asList(testCart1.getCartId(), testCart2.getCartId(), testCart3.getCartId());

        // when
        cartCleanUpScheduler.cleanUpExpiredCarts();

        // then
        assertThat(cartRepository.findById(cartIdList.get(0))).isEmpty();
        assertThat(cartRepository.findById(cartIdList.get(1))).isEmpty();
    }
}
