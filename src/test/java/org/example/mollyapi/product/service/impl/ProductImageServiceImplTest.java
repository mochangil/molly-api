package org.example.mollyapi.product.service.impl;

import org.example.mollyapi.common.client.ImageClient;
import org.example.mollyapi.common.enums.ImageType;
import org.example.mollyapi.product.dto.ProductImageDto;
import org.example.mollyapi.product.dto.UploadFile;
import org.example.mollyapi.product.entity.Product;
import org.example.mollyapi.product.enums.ProductImageType;
import org.example.mollyapi.product.repository.ProductRepository;
import org.example.mollyapi.product.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
class ProductImageServiceImplTest {

    @MockBean
    private ImageClient imageClient;

    @Autowired
    private ProductImageServiceImpl productImageService;
    @Autowired
    private ProductRepository productRepository;

    @DisplayName("업로드 된 이미지 URL로 상품이미지를 생성하고 상품에 추가한다")
    @Test
    void createProductImage() {
        // given
        Product product = createProduct();
        MockMultipartFile file = createMockFile("testUrl", "test.jpg");
        // 이미지 업로드는 무조건 성공한다
        when(imageClient.upload(any(ImageType.class), any(MultipartFile.class)))
                .thenReturn(Optional.of(createUploadFile()));

        // when
        ProductImageDto productImage = productImageService.createProductImage(product, file, ProductImageType.THUMBNAIL);

        // then
        assertThat(productImage).extracting("url", "filename", "type")
                .containsExactly("testUrl", "testFilename", ProductImageType.THUMBNAIL);
        assertThat(product.getImages()).hasSize(1)
                .extracting("url", "filename", "isRepresentative", "isProductImage" ,"isDescriptionImage", "product")
                .containsExactly(
                        tuple("testUrl", "testFilename", true, false, false, product)
                );
    }

    @DisplayName("업로드 된 이미지 URL 목록으로 상품이미지들을 생성하고 상품에 상품이미지 목록을 추가한다")
    @Test
    void testCreateProductImage_list() {
        // given
        Product product = createProduct();
        List<MultipartFile> files = List.of(
                createMockFile("test1", "test1.jpg"),
                createMockFile("test2", "test2.jpeg"));
        // 이미지 업로드는 무조건 성공한다
        when(imageClient.upload(any(ImageType.class), any(List.class)))
                .thenReturn(files.stream().map((file) -> createUploadFile(file.getName(), file.getOriginalFilename())).toList());


        // when
        List<ProductImageDto> dtoList = productImageService.createProductImage(product, files, ProductImageType.PRODUCT);

        // then
        assertThat(dtoList).hasSize(2);
        assertThat(product.getImages()).hasSize(2)
                .extracting("url", "filename", "isRepresentative", "isProductImage", "isDescriptionImage")
                .containsExactly(
                        tuple("test1" + "Url", "test1.jpg", false, true, false),
                        tuple("test2" + "Url", "test2.jpeg", false, true, false)
                );
    }


    @DisplayName("product가 null이면 예외를 던진다")
    @Test
    void testCreateProductImage_invalidProduct() {
        // given
        MockMultipartFile file = createMockFile("testUrl", "test.jpg");
        // 이미지 업로드는 무조건 성공한다
        when(imageClient.upload(any(ImageType.class), any(MultipartFile.class)))
                .thenReturn(Optional.of(createUploadFile()));

        // when, then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> productImageService.createProductImage(null, file, ProductImageType.PRODUCT));
        assertThat(exception.getMessage()).isEqualTo("잘 못된 인자로 인해 상품이미지 생성이 실패했습니다.");
    }

    @DisplayName("file이 null이면 예외를 던진다")
    @Test
    void testCreateProductImage_nullFile() {
        // given
        Product product = createProduct();
        MockMultipartFile file = null;
        // 이미지 업로드는 무조건 성공한다
        when(imageClient.upload(any(ImageType.class), any(MultipartFile.class)))
                .thenReturn(Optional.of(createUploadFile()));

        // when, then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> productImageService.createProductImage(product, file, ProductImageType.THUMBNAIL));
        assertThat(exception.getMessage()).isEqualTo("잘 못된 인자로 인해 상품이미지 생성이 실패했습니다.");
    }

    @DisplayName("상품 타입이 null이면 예외를 던진다")
    @Test
    void testCreateProductImage_nullProductIamgeType() {
        // given
        Product product = createProduct();
        MockMultipartFile file = createMockFile("testUrl", "test.jpg");
        // 이미지 업로드는 무조건 성공한다
        when(imageClient.upload(any(ImageType.class), any(MultipartFile.class)))
                .thenReturn(Optional.of(createUploadFile()));

        // when, then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> productImageService.createProductImage(product, file, null));
        assertThat(exception.getMessage()).isEqualTo("잘 못된 인자로 인해 상품이미지 생성이 실패했습니다.");
    }

    private MockMultipartFile createMockFile(String name, String originalFilename) {
        return new MockMultipartFile(
                name,
                originalFilename,
                "image/jpeg",
                new byte[]{1}  // 1바이트 더미 데이터
        );
    }

    private UploadFile createUploadFile() {
        return UploadFile.builder()
                .uploadFileName("testFilename")
                .storedFileName("testUrl").build();
    }

    private UploadFile createUploadFile(String name, String originalFilename) {
        return UploadFile.builder()
                .storedFileName(name + "Url")
                .uploadFileName(originalFilename).build();
    }


    private Product createProduct() {
        Product product = Product.builder().build();
        return productRepository.save(product);
    }
}