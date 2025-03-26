package org.example.mollyapi.product.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.mollyapi.product.dto.ProductDto;
import org.example.mollyapi.product.dto.ProductItemDto;
import org.example.mollyapi.product.dto.ProductItemReqDto;
import org.example.mollyapi.product.dto.response.ColorDetailDto;
import org.example.mollyapi.product.dto.response.FileInfoDto;
import org.example.mollyapi.product.dto.response.ProductResDto;
import org.example.mollyapi.product.dto.response.SizeDetailDto;
import org.example.mollyapi.product.entity.Category;
import org.example.mollyapi.product.entity.Product;
import org.example.mollyapi.product.entity.ProductItem;
import org.example.mollyapi.product.enums.ProductImageType;
import org.example.mollyapi.product.repository.ProductRepository;
import org.example.mollyapi.product.service.CategoryService;
import org.example.mollyapi.product.service.ProductService;
import org.example.mollyapi.user.entity.User;
import org.example.mollyapi.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    private final CategoryService categoryService;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ProductImageServiceImpl productImageServiceImpl;
    private final ProductItemServiceImpl productItemServiceImpl;


    public Optional<ProductResDto> findById(Long productId) {
        Optional<Product> product = productRepository.findById(productId);

        if (product.isPresent()) {
            return Optional.empty();
        }
        return Optional.ofNullable(convertToProductResDto(product.get()));
    }

    @Override
    @Transactional
    public ProductResDto updateProduct(Long userId, Long id, ProductDto productDto, List<ProductItemDto> productItemDtoList) {
        // 업데이트 할 상품 조회
        Product product = productRepository.findById(id).orElseThrow(IllegalArgumentException::new);

        User user = userRepository.findById(userId).orElseThrow(IllegalArgumentException::new);

        product.updateCategory(categoryService.getCategory(productDto.categories()));
        product.updateBrandName(productDto.brandName());
        product.updateProductName(productDto.productName());
        product.updatePrice(productDto.price());
        product.updateDescription(productDto.description());


        if (productItemDtoList != null && !productItemDtoList.isEmpty()) {
            for (ProductItemDto itemDto : productItemDtoList) {
                productItemServiceImpl.updateProductItem(itemDto.id(), ProductItemReqDto.of(itemDto));
            }
        }

        return convertToProductResDto(product);
    }

    @Override
    @Transactional
    public ProductResDto registerProduct(
            Long userId,
            ProductDto productDto,
            List<ProductItemReqDto> productItemRequests,
            MultipartFile thumbnailImage,
            List<MultipartFile> productImages,
            List<MultipartFile> descriptionImages
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        Product product = createProduct(productDto, user);

        productImageServiceImpl.createProductImage(product, thumbnailImage, ProductImageType.THUMBNAIL);
        productImageServiceImpl.createProductImage(product, productImages, ProductImageType.PRODUCT);
        productImageServiceImpl.createProductImage(product, descriptionImages, ProductImageType.DESCRIPTION);

        productItemServiceImpl.createProductItem(product, productItemRequests);

        Product saved = productRepository.save(product);

        return convertToProductResDto(saved);
    }

    private Product createProduct(ProductDto productDto, User user) {
        Category category = categoryService.getCategory(productDto.categories());

        return Product.builder()
                .category(category)
                .brandName(productDto.brandName())
                .productName(productDto.productName())
                .price(productDto.price())
                .description(productDto.description())
                .user(user)
                .build();
    }


    @Override
    @Transactional
    public void deleteProduct(Long userId,Long id) {
        Product product = productRepository.findById(id).orElseThrow(IllegalArgumentException::new);
        if (product.getUser().getUserId().equals(userId)) {
            productRepository.deleteById(id);
        }
    }

    public List<ColorDetailDto> groupItemByColor(List<ProductItem> items) {
        return items.stream()
                .collect(Collectors.groupingBy(
                        item -> item.getColor() + "_" + item.getColorCode(), // 그룹화 키 생성
                        LinkedHashMap::new, // 순서를 유지하는 맵 사용
                        Collectors.toList()
                ))
                .values()
                .stream()
                .map(groupedItems -> new ColorDetailDto(
                        groupedItems.get(0).getColor(),
                        groupedItems.get(0).getColorCode(),
                        groupedItems.stream()
                                .map(item -> new SizeDetailDto(item.getId(), item.getSize(), item.getQuantity()))
                                .collect(Collectors.toList())
                ))
                .collect(Collectors.toList());
    }

    public ProductResDto convertToProductResDto(Product product) {
        FileInfoDto thumbnail = new FileInfoDto(product.getThumbnail().getStoredFileName(), product.getThumbnail().getUploadFileName());
        List<FileInfoDto> productImages = product.getProductImages().stream().map((item)-> new FileInfoDto(item.getStoredFileName(), item.getUploadFileName())).toList();
        List<FileInfoDto> descriptionImages = product.getDescriptionImages().stream().map(item -> new FileInfoDto(item.getStoredFileName(), item.getUploadFileName())).toList();

        List<ProductItemDto> itemResDtos = product.getItems().stream().map(ProductItemDto::of).toList();
        List<ColorDetailDto> colorDetails = groupItemByColor(product.getItems());

        return new ProductResDto(
                product.getId(),
                categoryService.getCategoryPath(product.getCategory()),
                product.getBrandName(),
                product.getProductName(),
                product.getPrice(),
                product.getDescription(),
                thumbnail,
                productImages,
                descriptionImages,
                itemResDtos,
                colorDetails
        );
    }

}
