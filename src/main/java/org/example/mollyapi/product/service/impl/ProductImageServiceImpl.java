package org.example.mollyapi.product.service.impl;

import org.example.mollyapi.common.client.ImageClient;
import org.example.mollyapi.common.enums.ImageType;
import org.example.mollyapi.product.dto.ProductImageDto;
import org.example.mollyapi.product.dto.UploadFile;
import org.example.mollyapi.product.entity.Product;
import org.example.mollyapi.product.entity.ProductImage;
import org.example.mollyapi.product.enums.ProductImageType;
import org.example.mollyapi.product.repository.ProductImageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class ProductImageServiceImpl {

    @Autowired
    private ProductImageRepository productImageRepository;

    @Autowired
    private ImageClient imageClient;

    public ProductImageDto createProductImage(Product product, MultipartFile file, ProductImageType type) {
        if (product == null || file == null || type == null) {
            throw new IllegalArgumentException("잘못된 인자로 인해 상품이미지 생성이 실패했습니다.");
        }

        UploadFile uploadFile = imageClient.upload(ImageType.PRODUCT ,file).orElseThrow();

        ProductImage productImage = ProductImage.create(product,uploadFile,type);
        product.addImage(productImage);

        ProductImage save = productImageRepository.save(productImage);
        return ProductImageDto.of(save);
    }

    public List<ProductImageDto> createProductImage(Product product, List<MultipartFile> files, ProductImageType type) {
        List<UploadFile> uploadFiles = imageClient.upload(ImageType.PRODUCT ,files);

        List<ProductImage> productImages = ProductImage.create(product, uploadFiles, type);

        for (ProductImage image : productImages) {
            product.addImage(image);
        }

        List<ProductImage> productImageList = productImageRepository.saveAll(productImages);

        return productImageList.stream().map(ProductImageDto::of).toList();
    }
}
