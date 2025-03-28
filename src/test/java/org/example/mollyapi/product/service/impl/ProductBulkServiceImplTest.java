package org.example.mollyapi.product.service.impl;

import org.example.mollyapi.common.config.ApiQueryCounter;
import org.example.mollyapi.common.config.ApiQueryInspector;
import org.example.mollyapi.product.repository.ProductItemRepository;
import org.example.mollyapi.product.repository.ProductRepository;
import org.example.mollyapi.product.service.ProductBulkService;
import org.example.mollyapi.user.entity.User;
import org.example.mollyapi.user.repository.UserRepository;
import org.example.mollyapi.user.type.Sex;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;


import java.io.IOException;
import java.time.LocalDate;



@SpringBootTest
@Transactional
@ActiveProfiles("test")
class ProductBulkServiceImplTest {

    @Autowired
    private ApiQueryInspector queryInspector;

    @Autowired
    private ApiQueryCounter apiQueryCounter;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductItemRepository productItemRepository;

    @Autowired
    private ProductBulkService productBulkService;



    @Test
    @DisplayName("Excel 파일로 상품등록이 가능하다.")
    void saveBulkProduct_By_Using_Excel_Success() throws IOException {

    }

    @Test
    @DisplayName("유효하지 않은 상품은 반환되고, 유효한 상품만 저장된다.")
    void saveBulkProduct_By_Using_Wrong_Excel() throws IOException {

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