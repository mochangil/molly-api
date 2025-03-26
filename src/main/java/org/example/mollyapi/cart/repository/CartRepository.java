package org.example.mollyapi.cart.repository;

import jakarta.persistence.LockModeType;
import org.example.mollyapi.cart.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long>, CartCustomRepository{
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Cart findByProductItemIdAndUserUserId(Long itemId, Long userId);

    Optional<Cart> findByCartIdAndUserUserId(Long cartId, Long userId);
    boolean existsByProductItemIdAndUserUserId(Long itemId, Long userId);
}
