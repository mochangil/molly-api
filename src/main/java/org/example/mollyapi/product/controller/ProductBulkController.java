package org.example.mollyapi.product.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import lombok.RequiredArgsConstructor;
import org.example.mollyapi.product.service.ProductBulkService;
import org.example.mollyapi.user.auth.annotation.Auth;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Tag(name = " Bulk Product Controller", description = "대량 상품 관련 엔드 포인트")
@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductBulkController {

    private final ProductBulkService productBulkService;

    @Auth
    @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    @Operation(summary = "대량 상품 업로드 API", description = "csv 파일")
    public ResponseEntity<?> saveProducts(
        @RequestParam(name = "product_file") MultipartFile productFile,
        HttpServletRequest request) {

        Long userId = (Long) request.getAttribute("userId");
        List<Map<String, String>> result = new ArrayList<>();
        BlockingQueue<Map<String, String>> resultQueue = productBulkService.saveChunkOfBulkProducts(
            productFile,
            userId);

        resultQueue.drainTo(result);

        if (result.isEmpty()) {
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } else {
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        }

    }

}
