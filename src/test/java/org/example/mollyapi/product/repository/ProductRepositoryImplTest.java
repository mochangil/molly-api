package org.example.mollyapi.product.repository;

import com.github.f4b6a3.tsid.TsidCreator;
import jakarta.transaction.Transactional;
import lombok.Getter;
import org.example.mollyapi.product.dto.*;
import org.example.mollyapi.product.entity.*;
import org.example.mollyapi.product.enums.OrderBy;
import org.example.mollyapi.product.service.impl.ProductServiceImpl;
import org.example.mollyapi.user.entity.User;
import org.example.mollyapi.user.repository.UserRepository;
import org.example.mollyapi.user.type.Sex;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.example.mollyapi.product.enums.ProductImageType.*;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Transactional
@ActiveProfiles("test")
class ProductRepositoryImplTest {

    @Autowired
    private ProductRepository productRepository;

    static private CategoryRepository categoryRepository;
    static private UserRepository userRepository;

    static private User testUser;

    @Autowired
    public void setCategoryRepository(CategoryRepository categoryRepository) {
        ProductRepositoryImplTest.categoryRepository = categoryRepository;
    }

    @Autowired
    public void setUserRepository(UserRepository userRepository) {
        ProductRepositoryImplTest.userRepository = userRepository;
    }

    @BeforeAll
    static void setUpBeforeClass() throws Exception {

        categoryRepository.save(new Category("의류" + 1, null));
        categoryRepository.save(new Category("의류" + 2, null));
        categoryRepository.save(new Category("의류" + 3, null));


        testUser = userRepository.save(
                User.builder()
                        .nickname("testUser")
                        .cellPhone("01012345678")
                        .sex(Sex.MALE) // 성별 Enum 값 (예시: MALE, FEMALE)
                        .profileImage("default.jpg")
                        .birth(LocalDate.of(1995, 5, 15)) // 생년월일 (예시)
                        .name("김정환") // 사용자 이름
                        .build()
        );
    }

    @DisplayName("각 브랜드에 포함된 상품의 조회수 총합으로 브랜드를 조회순으로 정렬할 수 있다")
    @Test
    void getTotalViewGroupByBrandName() {
        //given
        // 브랜드 Nike 테스트 total: 600
        createTestProduct("RED", "Red Air Max", "M", 1L, "Nike", 60000L, 200L, 100L, 50L);  // viewCount: 200
        createTestProduct("BLUE", "Blue Jordan", "L", 1L, "Nike", 80000L, 300L, 200L, 150L);  // viewCount: 300
        createTestProduct("BLACK", "Black Dunk", "XL", 1L, "Nike", 50000L, 100L, 150L, 75L);  // viewCount: 100

        // 브랜드 Adidas 테스트 total: 900
        createTestProduct("WHITE", "White Ultraboost", "M", 2L, "Adidas", 90000L, 500L, 250L, 200L);  // viewCount: 500
        createTestProduct("GRAY", "Gray Yeezy", "S", 2L, "Adidas", 75000L, 400L, 200L, 130L);  // viewCount: 400

        // 브랜드 Puma 테스트 total: 750
        createTestProduct("GREEN", "Green Suede", "L", 3L, "Puma", 70000L, 400L, 300L, 200L);  // viewCount: 400
        createTestProduct("YELLOW", "Yellow RS-X", "M", 3L, "Puma", 55000L, 350L, 180L, 120L);  // viewCount: 350

        //when
        PageRequest pageable = PageRequest.of(0, 10);
        List<BrandSummaryDto> content = productRepository.getTotalViewGroupByBrandName(pageable).getContent();

        //then
        assertThat(content).hasSize(3);
        assertThat(content).extracting("brandName").containsExactly("Adidas", "Puma", "Nike");
        assertThat(content).extracting("viewCount").containsExactly(900L, 750L, 600L);
    }

    static Stream<ProductFilterCondition> provideSearchConditionsForSingleFilter() {
        return Stream.of(
                // 필터 하나만 설정
                ProductFilterCondition.of(List.of("RED"), null, null, null, null, null, null, null, null), // 색상 필터
                ProductFilterCondition.of(null, List.of("M"), null, null, null, null, null, null, null), // 사이즈 필터
                ProductFilterCondition.of(null, null, List.of(1L), null, null, null, null, null, null), // 카테고리 필터
                ProductFilterCondition.of(null, null, null, "Nike", null, null, null, null, null), // 브랜드 필터
                ProductFilterCondition.of(null, null, null, null, 50000L, null, null, null, null), // 가격 최소값 필터
                ProductFilterCondition.of(null, null, null, null, null, 100000L, null, null, null), // 가격 최대값 필터
                ProductFilterCondition.of(null, null, null, null, null, null, testUser.getUserId(), null, true), // 판매자 필터
                ProductFilterCondition.of(null, null, null, null, null, null, null, null, true) // 품절 제외 필터
        );
    }

    @DisplayName("필터조건 1개를 적용하여 조회할 수 있다")
    @ParameterizedTest
    @MethodSource("provideSearchConditionsForSingleFilter")
    void findByCondition_singleFilter(ProductFilterCondition condition) {
        //given
        createTestProduct(1L, "Nike", 60000L, 100L, 50L, List.of(
                new ProductItemOption("RED", "Red", "M", 200L),
                new ProductItemOption("BLUE", "Blue", "L", 150L)
        ));

        createTestProduct(2L, "Adidas", 80000L, 120L, 70L, List.of(
                new ProductItemOption("RED", "Red", "S", 180L),
                new ProductItemOption("GREEN", "Green", "M", 140L)
        ));

        createTestProduct(1L, "Nike", 40000L, 90L, 30L, List.of(
                new ProductItemOption("RED", "Red", "M", 100L),
                new ProductItemOption("BLUE", "Blue", "L", 120L)
        ));

        createTestProduct(3L, "Puma", 75000L, 200L, 100L, List.of(
                new ProductItemOption("CYAN", "Cyan", "L", 50L),
                new ProductItemOption("MAGENTA", "Magenta", "XL", 80L)
        ));

        createTestProduct(2L, "Nike", 90000L, 10L, 5L, List.of(
                new ProductItemOption("RED", "Red", "M", 0L),
                new ProductItemOption("BLACK", "Black", "S", 100L)
        ));        // when
        PageRequest pageable = PageRequest.of(0, 10);
        List<ProductAndThumbnailDto> content = productRepository.findByCondition(condition, pageable).getContent();

        // then
        assertThat(content).isNotEmpty();
        assertCondition(content, condition);
    }

    static Stream<ProductFilterCondition> provideSearchConditionsForTwoFilters() {
        return Stream.of(
                // 필터 두 개를 조합
                ProductFilterCondition.of(List.of("RED"), List.of("M"), null, null, null, null, null, null, null), // 색상 + 사이즈 필터
                ProductFilterCondition.of(null, List.of("M"), List.of(1L), null, null, null, null, null, null), // 사이즈 + 카테고리 필터
                ProductFilterCondition.of(List.of("RED"), null, List.of(1L), null, null, null, null, null, null), // 색상 + 카테고리 필터
                ProductFilterCondition.of(null, null, List.of(1L), "Nike", null, null, null, null, null), // 카테고리 + 브랜드 필터
                ProductFilterCondition.of(List.of("BLUE"), null, null, "Nike", 50000L, null, null, null, null), // 색상 + 브랜드 + 가격 최소
                ProductFilterCondition.of(null, null, null, null, 50000L, 100000L, null, null, null), // 가격 범위
                ProductFilterCondition.of(null, null, null, null, null, null, null, null, true) // 품절 제외 + 필터
        );
    }

    @DisplayName("필터 조건 2개를 적용해서 조회할 수 있다")
    @ParameterizedTest
    @MethodSource("provideSearchConditionsForTwoFilters")
    void findByCondition_twoFilters(ProductFilterCondition condition) {
        // given
        // 색상 필터 (RED) 테스트
        createTestProduct("RED", "Red", "M", 1L, "Nike", 60000L, 100L, 50L, 200L);
        // 사이즈 필터 (M) 테스트
        createTestProduct("BLUE", "Blue", "M", 1L, "Nike", 80000L, 200L, 150L, 300L);
        // 카테고리 필터 (1번 카테고리) 테스트
        createTestProduct("GREEN", "Green", "L", 1L, "Puma", 70000L, 300L, 200L, 400L);
        // 브랜드 필터 (Nike) 테스트
        createTestProduct("BLACK", "Black", "XL", 1L, "Nike", 50000L, 150L, 75L, 100L);
        // 가격 최소값 필터 (50000L) 테스트
        createTestProduct("YELLOW", "Yellow", "S", 1L, "Reebok", 55000L, 180L, 120L, 350L);
        // 가격 최대값 필터 (100000L) 테스트
        createTestProduct("WHITE", "White", "M", 2L, "Adidas", 90000L, 250L, 200L, 500L);
        // 품절 제외 필터 테스트 (품절이 아닌 상품 추가)
        createTestProduct("GRAY", "Gray", "M", 3L, "Nike", 75000L, 200L, 130L, 100L);


        // when
        PageRequest pageable = PageRequest.of(0, 10);
        List<ProductAndThumbnailDto> content = productRepository.findByCondition(condition, pageable).getContent();

        //then
        assertThat(content).isNotEmpty();
        assertCondition(content, condition);
    }

    @DisplayName("최소 가격 변수(priceGoe)가 최대 가격 변수(priceLt)보다 크게 조회하면 빈 배열이 반환된다")
    @Test
    void findByCondition_priceGoeBiggerThanPriceLt() {
        // given
        ProductFilterCondition condition = new ProductFilterCondition(null, null, null, null, 100000L, 50000L, null, null, null);
        // 최소값보다 비싼 상품
        createTestProduct("WHITE", "White", "M", 2L, "Adidas", 110000L, 250L, 200L, 500L);
        // 최소값과 최대값 사이
        createTestProduct("BLUE", "Blue", "M", 1L, "Nike", 80000L, 200L, 150L, 300L);
        // 최대값보다 저렴한 상품
        createTestProduct("GRAY", "Gray", "M", 3L, "Nike", 30000L, 200L, 130L, 100L);


        // when
        PageRequest pageable = PageRequest.of(0, 10);
        List<ProductAndThumbnailDto> content = productRepository.findByCondition(condition, pageable).getContent();

        //then
        assertThat(content).isEmpty();
    }

    @DisplayName("condition이 null이면 아무 조건도 적용되지 않는다")
    @Test
    void findByCondition_ConditionNull() {
        // given
        createTestProduct("WHITE", "White", "M", 2L, "Adidas", 110000L, 250L, 200L, 500L);
        createTestProduct("BLUE", "Blue", "M", 1L, "Nike", 80000L, 200L, 150L, 300L);
        createTestProduct("GRAY", "Gray", "M", 3L, "Nike", 30000L, 200L, 130L, 100L);

        // when
        PageRequest pageable = PageRequest.of(0, 10);
        List<ProductAndThumbnailDto> content = productRepository.findByCondition(null, pageable).getContent();

        //then
        assertThat(content).hasSize(3)
                .extracting("brandName").contains("Adidas", "Nike", "Nike");
    }

    @DisplayName("pageable이 null이면 페이징이 적용되지 않는다")
    @Test
    void findByCondition_PageableNull() {
        // given
        ProductFilterCondition condition = new ProductFilterCondition(List.of("WHITE"), null, null, null, null, null, null, null, null);
        // 최소값보다 비싼 상품
        createTestProduct("WHITE", "White", "M", 2L, "Adidas", 110000L, 250L, 200L, 500L);
        createTestProduct("WHITE", "Blue", "M", 1L, "Nike", 80000L, 200L, 150L, 300L);
        createTestProduct("WHITE", "Gray", "M", 3L, "Nike", 30000L, 200L, 130L, 100L);
        createTestProduct("BLUE", "Gray", "M", 3L, "Nike", 30000L, 200L, 130L, 100L);

        // when
        List<ProductAndThumbnailDto> content = productRepository.findByCondition(condition, null).getContent();

        //then
        assertThat(content).hasSize(3)
                .extracting("brandName").contains("Adidas", "Nike", "Nike");
    }


    static Stream<ProductFilterCondition> provideSearchConditionsForFiltersAndSorting() {
        return Stream.of(
                // 필터 없이 정렬 조건만 테스트 (정렬 조건이 하나만 있는 경우)
                ProductFilterCondition.of(null, null, null, null, null, null, null, OrderBy.CREATED_AT, null), // 생성일 정렬
                ProductFilterCondition.of(null, null, null, null, null, null, null, OrderBy.VIEW_COUNT, null), // 조회수 정렬
                ProductFilterCondition.of(null, null, null, null, null, null, null, OrderBy.PURCHASE_COUNT, null), // 구매수 정렬
                ProductFilterCondition.of(null, null, null, null, null, null, null, OrderBy.PRICE_DESC, null), // 가격 내림차순 정렬
                ProductFilterCondition.of(null, null, null, null, null, null, null, OrderBy.PRICE_ASC, null), // 가격 오름차순 정렬

                // 필터 하나만 설정 + 정렬
                ProductFilterCondition.of(List.of("RED"), null, null, null, null, null, null, OrderBy.CREATED_AT, null), // 색상 필터 + 생성일 정렬
                ProductFilterCondition.of(null, List.of("M"), null, null, null, null, null, OrderBy.VIEW_COUNT, null), // 사이즈 필터 + 조회수 정렬
                ProductFilterCondition.of(null, null, List.of(1L), null, null, null, null, OrderBy.PURCHASE_COUNT, null), // 카테고리 필터 + 구매수 정렬
                ProductFilterCondition.of(null, null, null, "Nike", null, null, null, OrderBy.PRICE_DESC, null), // 브랜드 필터 + 가격 내림차순 정렬
                ProductFilterCondition.of(null, null, null, null, 50000L, null, null, OrderBy.PRICE_ASC, null), // 가격 최소값 필터 + 가격 오름차순 정렬
                ProductFilterCondition.of(List.of("RED"), null, null, null, null, null, null, OrderBy.VIEW_COUNT, true) // 색상 필터 + 생성일 정렬 + 품절 제외
        );
    }

    @DisplayName("필터와 정렬 조건을 동시헤 적용할 수 있다")
    @ParameterizedTest
    @MethodSource("provideSearchConditionsForFiltersAndSorting")
    void findByCondition_filtersAndSorting(ProductFilterCondition condition) {
        // given
        // 1. 색상 필터 (RED) + CREATED_AT 정렬
        createTestProduct("RED", "Red", "M", 1L, "Nike", 60000L, 100L, 50L, 200L);
        // 2. 사이즈 필터 (M) + VIEW_COUNT 정렬
        createTestProduct("BLUE", "Blue", "M", 1L, "Adidas", 80000L, 300L, 150L, 100L);
        // 3. 카테고리 필터 (1L) + PURCHASE_COUNT 정렬
        createTestProduct("GREEN", "Green", "L", 1L, "Puma", 70000L, 200L, 400L, 300L);
        // 4. 브랜드 필터 (Nike) + PRICE_DESC 정렬
        createTestProduct("BLACK", "Black", "XL", 2L, "Nike", 90000L, 150L, 75L, 50L);
        // 5. 가격 최소값 필터 (50000L) + PRICE_ASC 정렬
        createTestProduct("YELLOW", "Yellow", "S", 3L, "Reebok", 55000L, 180L, 120L, 350L);
        // 6. 품절 제외 필터 + CREATED_AT 정렬
        createTestProduct("GRAY", "Gray", "M", 2L, "UnderArmour", 75000L, 250L, 130L, 0L);  // 품절
        createTestProduct("WHITE", "White", "L", 2L, "Nike", 65000L, 220L, 110L, 150L);   // 품절 아님
        // 7. 색상 (BLUE) + PRICE_DESC 정렬
        createTestProduct("BLUE", "Blue", "XL", 3L, "Puma", 85000L, 140L, 90L, 200L);
        // 8. 사이즈 (L) + PURCHASE_COUNT 정렬
        createTestProduct("ORANGE", "Orange", "L", 1L, "Adidas", 72000L, 160L, 300L, 250L);
        // 9. 카테고리 (2L) + VIEW_COUNT 정렬
        createTestProduct("PURPLE", "Purple", "S", 2L, "Reebok", 58000L, 400L, 80L, 120L);
        // 10. 브랜드 (Adidas) + PRICE_ASC 정렬
        createTestProduct("PINK", "Pink", "M", 3L, "Adidas", 50000L, 130L, 60L, 180L);
        // 11. 가격 최소값 (70000L) + CREATED_AT 정렬
        createTestProduct("BROWN", "Brown", "XL", 1L, "Nike", 78000L, 170L, 140L, 90L);
        // 12. 품절 제외 + PRICE_DESC 정렬
        createTestProduct("CYAN", "Cyan", "S", 2L, "Puma", 95000L, 210L, 200L, 10L);  // 품절 아님

        PageRequest pageable = PageRequest.of(0, 10);
        List<ProductAndThumbnailDto> content = productRepository.findByCondition(condition, pageable).getContent();

        //then
        assertThat(content).isNotEmpty();
        assertCondition(content, condition);
    }


    private void assertCondition(List<ProductAndThumbnailDto> content, ProductFilterCondition condition) {
        // 카테고리 필터
        assertFilterCondition(content, condition.categoryId(),
                (BiPredicate<ProductAndThumbnailDto, List<Long>>) (product, value) -> value.contains(product.getCategoryId()), "CategoryId");

        // 브랜드 필터
        assertFilterCondition(content, condition.brandName(),
                (product, value) -> product.getBrandName().equals(value), "BrandName");

        // 가격 이상 필터
        assertFilterCondition(content, condition.priceGoe(),
                (product, value) -> product.getPrice() >= value, "PriceGoe");

        // 가격 이하 필터
        assertFilterCondition(content, condition.priceLt(),
                (product, value) -> product.getPrice() <= value, "PriceLt");

        // 판매자 검증
        assertFilterCondition(content, condition.sellerId(),
                (prodcut, value) -> prodcut.getSellerId().equals(value), "UserId");

        // 중복된 productId가 없는지 검증
        assertNoDuplicateProductId(content);

        // 정렬 조건 검증
        if (condition.orderBy() != null) {
            assertSortCondition(content, condition.orderBy());
        }
    }

    private void assertSortCondition(List<ProductAndThumbnailDto> content, OrderBy orderBy) {
        if (content.size() <= 1) {
            return; // 정렬 검증이 필요 없는 경우 (0개 또는 1개 항목)
        }

        for (int i = 0; i < content.size() - 1; i++) {
            ProductAndThumbnailDto current = content.get(i);
            ProductAndThumbnailDto next = content.get(i + 1);

            switch (orderBy) {
                case CREATED_AT:
                    assertThat(current.getCreatedAt()).isAfterOrEqualTo(next.getCreatedAt())
                            .as("Sorted by CREATED_AT descending");
                    break;
                case VIEW_COUNT:
                    assertThat(current.getViewCount()).isGreaterThanOrEqualTo(next.getViewCount())
                            .as("Sorted by VIEW_COUNT descending");
                    break;
                case PURCHASE_COUNT:
                    assertThat(current.getPurchaseCount()).isGreaterThanOrEqualTo(next.getPurchaseCount())
                            .as("Sorted by PURCHASE_COUNT descending");
                    break;
                case PRICE_ASC:
                    assertThat(current.getPrice()).isLessThanOrEqualTo(next.getPrice())
                            .as("Sorted by PRICE ascending");
                    break;
                case PRICE_DESC:
                    assertThat(current.getPrice()).isGreaterThanOrEqualTo(next.getPrice())
                            .as("Sorted by PRICE descending");
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported OrderBy: " + orderBy);
            }
        }
    }

    private <T> void assertFilterCondition(List<ProductAndThumbnailDto> content, List<T> conditionValue,
                                           BiPredicate<ProductAndThumbnailDto, T> predicate, String fieldName) {
        if (conditionValue != null && !conditionValue.isEmpty()) {
            content.forEach(product ->
                    conditionValue.forEach(value ->
                            assertThat(predicate.test(product, value)).as(fieldName + " is valid").isTrue()
                    )
            );
        }
    }

    private <T> void assertFilterCondition(List<ProductAndThumbnailDto> content, T conditionValue,
                                           BiPredicate<ProductAndThumbnailDto, T> predicate, String fieldName) {
        if (conditionValue != null) {
            content.forEach(product ->
                    assertThat(predicate.test(product, conditionValue)).as(fieldName + " is valid").isTrue()
            );
        }
    }

    private void assertNoDuplicateProductId(List<ProductAndThumbnailDto> content) {
        Set<Long> uniqueIds = new HashSet<>();
        for (ProductAndThumbnailDto product : content) {
            assertThat(uniqueIds.add(product.getId()))
                    .as("중복된 productId가 존재합니다: " + product.getId())
                    .isTrue();
        }
    }


    private Product createTestProduct(
            String colorCode,  String color, String size, Long categoryId, String brandName,
            Long price, Long viewCount, Long purchaseCount, Long stock
    ) {
        // 카테고리 및 사용자 데이터 준비 (기존 저장된 데이터가 있다고 가정)
        Category category = categoryRepository.findById(categoryId).orElseThrow();

        // 상품 생성
        Product product = Product.builder()
                .category(category)
                .brandName(brandName)
                .productName("TestProduct")
                .price(price)
                .description("Test description")
                .user(testUser)
                .build();

        // 상품 이미지 추가 (대표 이미지 1개)
        ProductImage image = createTestProductImage(product);
        product.addImage(image);

        // 조회수 및 구매수 설정
        product.setPurchaseCount(purchaseCount);
        for (long i = 0; i < viewCount; i++) {
            product.increaseViewCount();
        }

        // 상품 아이템 추가 (색상, 사이즈 정보 포함)
        ProductItem item = createTestProductItem(colorCode, color, size, stock, product);
        product.addItem(item);

        return productRepository.save(product);
    }

    private ProductImage createTestProductImage(Product product) {
        String url = "/thumbnail.jpg";
        String filename = "thumbnail.jpg";

        return ProductImage.builder()
                .uploadFile(UploadFile.builder().uploadFileName(filename).storedFileName(url).build())
                .type(THUMBNAIL)
                .imageIndex(0L)
                .product(product)
                .build();
    }

    private ProductItem createTestProductItem(String colorCode, String color, String size, Long quantity, Product product) {
        return ProductItem.builder()
                .colorCode(colorCode)
                .color(color)
                .size(size)
                .quantity(quantity)
                .product(product)
                .build();
    }

    private Product createTestProduct(
            Long categoryId, String brandName, Long price, Long viewCount, Long purchaseCount,
            List<ProductItemOption> itemOptions // 변경: Tuple → ProductItemOption 리스트
    ) {
        // 카테고리 조회
        Category category = categoryRepository.findById(categoryId).orElseThrow();

        // 상품 생성
        Product product = Product.builder()
                .category(category)
                .brandName(brandName)
                .productName("TestProduct")
                .price(price)
                .description("Test description")
                .user(testUser)
                .build();

        // 상품 대표 이미지 추가
        ProductImage image = createTestProductImage(product);
        product.addImage(image);

        // 조회수 및 구매수 설정
        product.setPurchaseCount(purchaseCount);
        for (long i = 0; i < viewCount; i++) {
            product.increaseViewCount();
        }

        // 여러 개의 상품 아이템 추가
        for (ProductItemOption option : itemOptions) {
            ProductItem item = createTestProductItem(
                    option.getColorCode(),
                    option.getColor(),
                    option.getSize(),
                    option.getStock(),
                    product
            );
            product.addItem(item);
        }


        return productRepository.save(product);
    }

    @Getter
    public class ProductItemOption {
        private final String colorCode;
        private final String color;
        private final String size;
        private final Long stock;

        public ProductItemOption(String colorCode, String color, String size, Long stock) {
            this.colorCode = colorCode;
            this.color = color;
            this.size = size;
            this.stock = stock;
        }
    }
}