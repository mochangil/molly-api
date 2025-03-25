package org.example.mollyapi.product.service.impl;

import jakarta.transaction.Transactional;
import org.example.mollyapi.product.dto.ProductAndThumbnailDto;
import org.example.mollyapi.product.dto.ProductFilterCondition;
import org.example.mollyapi.product.dto.ProductItemDto;
import org.example.mollyapi.product.dto.UploadFile;
import org.example.mollyapi.product.dto.response.ColorDetailDto;
import org.example.mollyapi.product.dto.response.FileInfoDto;
import org.example.mollyapi.product.dto.response.ProductResDto;
import org.example.mollyapi.product.dto.response.SizeDetailDto;
import org.example.mollyapi.product.entity.Category;
import org.example.mollyapi.product.entity.Product;
import org.example.mollyapi.product.entity.ProductImage;
import org.example.mollyapi.product.entity.ProductItem;
import org.example.mollyapi.product.enums.ProductImageType;
import org.example.mollyapi.product.repository.ProductRepository;
import org.example.mollyapi.product.service.CategoryService;
import org.example.mollyapi.product.service.ProductReadService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.example.mollyapi.product.enums.ProductImageType.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@Transactional
class ProductReadServiceImplTest {

    @Autowired
    private ProductReadService productReadService;

    @MockBean
    private ProductRepository productRepository;

    @MockBean
    private CategoryService categoryService;

//    @BeforeEach
//    void setUp() {
//        when(categoryService.getCategoryPath(any(Category.class))).thenReturn(List.of());
//    }

    @DisplayName("condition이 null이면 필터,정렬이 적용되지 않은 데이터가 반환된다")
    @Test
    void getAllProducts_ConditionNull() {
        // given
        when(categoryService.getCategoryPath(any(Category.class))).thenReturn(List.of());
        List<ProductAndThumbnailDto> wrongContent = List.of(createProductAndThumbnailDto("wrongProductName1"));
        List<ProductAndThumbnailDto> rightContent = List.of(createProductAndThumbnailDto("rightProductName1"));
        PageRequest pageRequest = PageRequest.of(0, 10);

        // 잘못된 케이스
        SliceImpl<ProductAndThumbnailDto> wrongResult = new SliceImpl<>(wrongContent, pageRequest, false);
        when(productRepository.findByCondition(null, pageRequest, 0L)).thenReturn(wrongResult);
        // 기대되는 케이스
        SliceImpl<ProductAndThumbnailDto> rightResult = new SliceImpl<>(rightContent, pageRequest, false);
        when(productRepository.findByCondition(ProductFilterCondition.builder().build(), pageRequest, 0L)).thenReturn(rightResult);

        // when
        List<ProductResDto> products = productReadService.getAllProducts(null, pageRequest, 0L).getContent();

        // then
        assertThat(products).isNotEmpty()
                .extracting("productName").contains("rightProductName1");
    }


    @DisplayName("pageable이 null이면 10개의 데이터만 가져온다.")
    @Test
    void getAllProducts_PageableNull() {
        // given
        when(categoryService.getCategoryPath(any(Category.class))).thenReturn(List.of());
        List<ProductAndThumbnailDto> wrongContent = List.of(createProductAndThumbnailDto("wrongProductName1"));
        List<ProductAndThumbnailDto> rightContent = List.of(createProductAndThumbnailDto("rightProductName1"));

        ProductFilterCondition condition = ProductFilterCondition.builder().build();

        // 잘못된 케이스
        SliceImpl<ProductAndThumbnailDto> wrongResult = new SliceImpl<>(wrongContent, Pageable.unpaged(), false);
        when(productRepository.findByCondition(condition, null, 0L)).thenReturn(wrongResult);
        // 기대되는 케이스
        SliceImpl<ProductAndThumbnailDto> rightResult = new SliceImpl<>(rightContent,  PageRequest.of(0,10), false);
        when(productRepository.findByCondition(condition, PageRequest.of(0,10), 0L)).thenReturn(rightResult);

        // when
        List<ProductResDto> products = productReadService.getAllProducts(condition, null, 0L).getContent();

        // then
        assertThat(products).isNotEmpty()
                .extracting("productName").contains("rightProductName1");
    }

    @DisplayName("offset이 null이면 가장 상위의 10개의 데이터만 가져온다.")
    @Test
    void getAllProducts_OffsetNull() {
        // given
        when(categoryService.getCategoryPath(any(Category.class))).thenReturn(List.of());
        List<ProductAndThumbnailDto> wrongContent = List.of(createProductAndThumbnailDto("wrongProductName1"));
        List<ProductAndThumbnailDto> rightContent = List.of(createProductAndThumbnailDto("rightProductName1"));
        PageRequest pageRequest = PageRequest.of(0, 10);

        ProductFilterCondition condition = ProductFilterCondition.builder().build();

        // 잘못된 케이스
        SliceImpl<ProductAndThumbnailDto> wrongResult = new SliceImpl<>(wrongContent, Pageable.unpaged(), false);
        when(productRepository.findByCondition(condition, null, 0L)).thenReturn(wrongResult);
        // 기대되는 케이스
        SliceImpl<ProductAndThumbnailDto> rightResult = new SliceImpl<>(rightContent,  PageRequest.of(0,10), false);
        when(productRepository.findByCondition(condition, PageRequest.of(0,10), 0L)).thenReturn(rightResult);

        // when
        List<ProductResDto> products = productReadService.getAllProducts(condition, pageRequest, null).getContent();

        // then
        assertThat(products).isNotEmpty()
                .extracting("productName").contains("rightProductName1");
    }

    @DisplayName("상품 목록을 조회하면 상품별 상품 정보와 썸네일이 반환된다")
    @Test
    public void getAllProduct() {
        //given
        when(categoryService.getCategoryPath(101L)).thenReturn(List.of("category", "1"));
        when(categoryService.getCategoryPath(102L)).thenReturn(List.of("category", "2"));
        when(categoryService.getCategoryPath(103L)).thenReturn(List.of("category", "3"));
        when(categoryService.getCategoryPath(104L)).thenReturn(List.of("category", "4"));
        when(categoryService.getCategoryPath(105L)).thenReturn(List.of("category", "5"));
        List<ProductAndThumbnailDto> content = List.of(
                createProductAndThumbnailDto(1L, 101L, "BrandA", "ProductA", 10000L, "http://example.com/product-a.jpg", "product-a.jpg"),
                createProductAndThumbnailDto(2L, 102L, "BrandB", "ProductB", 15000L, "http://example.com/product-b.jpg", "product-b.jpg"),
                createProductAndThumbnailDto(3L, 103L, "BrandC", "ProductC", 20000L, "http://example.com/product-c.jpg", "product-c.jpg"),
                createProductAndThumbnailDto(4L, 104L, "BrandD", "ProductD", 25000L, "http://example.com/product-d.jpg", "product-d.jpg"),
                createProductAndThumbnailDto(5L, 105L, "BrandE", "ProductE", 30000L, "http://example.com/product-e.jpg", "product-e.jpg")
        );

        SliceImpl<ProductAndThumbnailDto> result = new SliceImpl<>(content, Pageable.unpaged(), false);
        when(productRepository.findByCondition(any(), any(), any())).thenReturn(result);

        //when
        List<ProductResDto> products = productReadService.getAllProducts(null, null, null).getContent();

        //then
        assertThat(products).extracting("id", "categories", "brandName", "productName", "price")
                .containsExactly(
                        tuple(1L, List.of("category", "1") , "BrandA", "ProductA", 10000L),
                        tuple(2L, List.of("category", "2") , "BrandB", "ProductB", 15000L),
                        tuple(3L, List.of("category", "3") , "BrandC", "ProductC", 20000L),
                        tuple(4L, List.of("category", "4") , "BrandD", "ProductD", 25000L),
                        tuple(5L, List.of("category", "5") , "BrandE", "ProductE", 30000L)
                );
        assertThat(products).extracting("thumbnail").extracting("path", "filename")
                .containsExactly(
                        tuple("http://example.com/product-a.jpg", "product-a.jpg"),
                        tuple("http://example.com/product-b.jpg", "product-b.jpg"),
                        tuple("http://example.com/product-c.jpg", "product-c.jpg"),
                        tuple("http://example.com/product-d.jpg", "product-d.jpg"),
                        tuple("http://example.com/product-e.jpg", "product-e.jpg")
                );
    }

    @DisplayName("상품 아이디로 검색하면 상품 상세 정보와 각 옵션별 정보가 반환된다")
    @Test
    void getProductById() {
        // given
        Product product = Product.builder()
                .category(Category.builder().build())
                .brandName("testBrandName")
                .productName("testProductName")
                .price(1L)
                .description("testDescription")
                .build();

        product.addItem(createProductItem(product, "#CLR1", "color1", "S"));
        product.addItem(createProductItem(product, "#CLR2", "color2", "M"));
        product.addItem(createProductItem(product, "#CLR3", "color3", "L"));

        product.addImage(createImage(product, "imgUrl1", "imageFileName1", THUMBNAIL));
        product.addImage(createImage(product, "imgUrl2", "imageFileName2", PRODUCT));
        product.addImage(createImage(product, "imgUrl3", "imageFileName3", DESCRIPTION));

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        // when
        Optional<ProductResDto> response = productReadService.getProductById(1L);

        // then
        assertThat(response).isNotEmpty().get()
                .extracting("brandName", "productName", "price", "description")
                .containsExactly("testBrandName", "testProductName", 1L, "testDescription");
        assertThat(response).get()
                .extracting(ProductResDto::items).asList()
                .extracting(item -> {
                    ProductItemDto dto = (ProductItemDto) item;
                    return tuple(dto.colorCode(), dto.color(), dto.size());
                })
                .containsExactly(
                        tuple("#CLR1", "color1", "S"),
                        tuple("#CLR2", "color2", "M"),
                        tuple("#CLR3", "color3", "L")
                );
        assertThat(response).get()
                .extracting(ProductResDto::thumbnail)
                .extracting(FileInfoDto::path, FileInfoDto::filename)
                .containsExactly("imgUrl1", "imageFileName1");
        assertThat(response).get()
                .extracting(ProductResDto::productImages).asList()
                .extracting(image -> {
                    FileInfoDto dto = (FileInfoDto) image;
                    return tuple(dto.path(), dto.filename());
                })
                .containsExactly(tuple("imgUrl2", "imageFileName2"));
        assertThat(response).get()
                .extracting(ProductResDto::productDescriptionImages).asList()
                .extracting(image -> {
                    FileInfoDto dto = (FileInfoDto) image;
                    return tuple(dto.path(), dto.filename());
                })
                .containsExactly(tuple("imgUrl3", "imageFileName3"));
        assertThat(response).get()
                .extracting(ProductResDto::colorDetails).asList()
                .extracting(
                        detail -> {
                            ColorDetailDto dto = (ColorDetailDto) detail;
                            return tuple(dto.colorCode(), dto.color());
                        }
                )
                .containsExactly(
                        tuple("#CLR1", "color1"),
                        tuple("#CLR2", "color2"),
                        tuple("#CLR3", "color3")
                );
        assertThat(response).get()
                .extracting(ProductResDto::colorDetails).asList()
                .extracting(
                        detail -> {
                            ColorDetailDto dto = (ColorDetailDto) detail;
                            return dto.sizeDetails();
                        }
                ).asList()
                .extracting(sizeDetail -> {
                    List<SizeDetailDto> sizeDetails = (List<SizeDetailDto>) sizeDetail;
                    return sizeDetails.stream()
                            .map(SizeDetailDto::size)
                            .collect(Collectors.toList());
                        }
                )
                .containsExactly(
                        List.of("S"),
                        List.of("M"),
                        List.of("L")
                );
    }

    private static ProductImage createImage(Product product, String url, String filename , ProductImageType type) {
        return ProductImage.builder()
                .product(product)
                .uploadFile(UploadFile.builder().uploadFileName(filename).storedFileName(url).build())
                .type(type)
                .imageIndex(0L)
                .build();
    }

    private static ProductItem createProductItem(Product product, String colorCode, String color, String size) {
        return ProductItem.builder()
                .product(product)
                .colorCode(colorCode)
                .color(color)
                .size(size)
                .quantity(10L)
                .build();
    }

    private ProductAndThumbnailDto createProductAndThumbnailDto(
            Long id,
            Long categoryId,
            String brandName,
            String productName,
            Long price,
            String url,
            String filename
    ) {
        return new ProductAndThumbnailDto(
                id,
                categoryId,
                brandName,
                productName,
                price,
                url,
                filename,
                1L,
                150L,
                200L,
                LocalDateTime.now()
        );
    }

    private ProductAndThumbnailDto createProductAndThumbnailDto(String productName) {
        return new ProductAndThumbnailDto(
                1L,
                1L,
                "BrandA",
                productName,
                10000L,
                "http://example.com/product-a.jpg",
                "product-a.jpg",
                1L,
                150L,
                200L,
                LocalDateTime.now()
        );
    }
}