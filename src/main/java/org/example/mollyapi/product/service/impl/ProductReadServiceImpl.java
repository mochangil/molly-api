package org.example.mollyapi.product.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.mollyapi.product.dto.BrandSummaryDto;
import org.example.mollyapi.product.dto.ProductAndThumbnailDto;
import org.example.mollyapi.product.dto.ProductFilterCondition;
import org.example.mollyapi.product.dto.ProductItemDto;
import org.example.mollyapi.product.dto.response.ColorDetailDto;
import org.example.mollyapi.product.dto.response.FileInfoDto;
import org.example.mollyapi.product.dto.response.ProductResDto;
import org.example.mollyapi.product.dto.response.SizeDetailDto;
import org.example.mollyapi.product.entity.Category;
import org.example.mollyapi.product.entity.Product;
import org.example.mollyapi.product.entity.ProductItem;
import org.example.mollyapi.product.enums.OrderBy;
import org.example.mollyapi.product.repository.ProductRepository;
import org.example.mollyapi.product.service.CategoryService;
import org.example.mollyapi.product.service.ProductReadService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.example.mollyapi.product.enums.OrderBy.*;

@Service
@RequiredArgsConstructor
public class ProductReadServiceImpl implements ProductReadService {

    private final CategoryService categoryService;
    private final ProductRepository productRepository;

    @Override
    public Slice<ProductResDto> getAllProducts(ProductFilterCondition condition, Pageable pageable) {
        if (condition == null) {
            condition = ProductFilterCondition.builder().build();
        }

        if (pageable == null) {
            int defaultPageNumber = 0;
            int defaultPageSize = 10;
            pageable = PageRequest.of(defaultPageNumber, defaultPageSize);
        }

        Slice<ProductAndThumbnailDto> page = productRepository.findByCondition(condition, pageable);

        return page.map(this::convertToProductResDto);
    }

    @Override
    @Transactional
    public Optional<ProductResDto> getProductById(Long id) {
        Optional<Product> product = productRepository.findById(id);

        // 조회수 증가
        product.ifPresent(Product::increaseViewCount);

        return product.map(this::convertToProductResDto);
    }

    private ProductResDto convertToProductResDto(Product product) {
        FileInfoDto thumbnail = new FileInfoDto(product.getThumbnail().getStoredFileName(), product.getThumbnail().getUploadFileName());
        List<FileInfoDto> productImages = product.getProductImages().stream().map((item)-> new FileInfoDto(item.getStoredFileName(), item.getUploadFileName())).toList();
        List<FileInfoDto> descriptionImages = product.getDescriptionImages().stream().map(item -> new FileInfoDto(item.getStoredFileName(), item.getUploadFileName())).toList();

        List<ProductItemDto> itemResDtoList = product.getItems().stream().map(ProductItemDto::from).toList();
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
                itemResDtoList,
                colorDetails
        );
    }

    private ProductResDto convertToProductResDto(ProductAndThumbnailDto dto) {
        FileInfoDto thumbnail = new FileInfoDto(
                dto.getUrl(), dto.getFilename());

        return new ProductResDto(
                dto.getId(),
                categoryService.getCategoryPath(dto.getCategoryId()),
                dto.getBrandName(),
                dto.getProductName(),
                dto.getPrice(),
                null,
                thumbnail,
                null,
                null,
                null,
                null
        );
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

}
