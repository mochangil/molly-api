package org.example.mollyapi.product.repository;
import org.example.mollyapi.product.dto.request.ProductBulkItemReqDto;
import org.example.mollyapi.product.dto.request.ProductBulkReqDto;
import org.example.mollyapi.product.entity.Product;
import org.example.mollyapi.product.mapper.ProductItemMapper;
import org.example.mollyapi.product.mapper.ProductMapper;
import org.example.mollyapi.user.entity.User;
import org.example.mollyapi.user.repository.UserRepository;
import org.example.mollyapi.user.type.Sex;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

@SpringBootTest
@ActiveProfiles("test")
class ProductBulkJdbcRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("배치쿼리")
    void saveProductByBatchQuery() {

    }

    @Test
    void saveProductItemByBatchQuery() {

    }

    private User createUser() {
        return User.builder()
            .sex(Sex.FEMALE)
            .nickname("꽃달린감나무")
            .cellPhone("01011112222")
            .birth(LocalDate.now())
            .profileImage("default.jpg")
            .name("꽃감이")
            .build();
    }


}

