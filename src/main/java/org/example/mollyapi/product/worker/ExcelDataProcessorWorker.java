package org.example.mollyapi.product.worker;

import static org.example.mollyapi.product.dto.request.ProductBulkItemReqDto.createBulkProductItemReqDto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.example.mollyapi.common.util.TimeUtil;
import org.example.mollyapi.product.dto.ExcelProductDto;
import org.example.mollyapi.product.dto.request.ProductBulkItemReqDto;
import org.example.mollyapi.product.dto.request.ProductBulkReqDto;
import org.example.mollyapi.product.mapper.ProductItemMapper;
import org.example.mollyapi.product.mapper.ProductMapper;


@Slf4j
public class ExcelDataProcessorWorker implements Runnable {

    private static final Pattern HEX_PATTERN = Pattern.compile("^#([A-Fa-f0-9]{6})$");
    private static final int BATCH_SIZE = 10000;
    private final ProductItemMapper productItemMapper;
    private final ProductMapper productMapper;

    private final BlockingQueue<List<String>> queue;
    private final BlockingQueue<Map<String, String>> resultQueue;


    private final List<ProductBulkReqDto> passedProduct = new ArrayList<>();
    private final List<ProductBulkItemReqDto> passedProductItem = new ArrayList<>();

    private final Long userId;
    private boolean finished = false;

    public ExcelDataProcessorWorker(Long userId,
        ProductItemMapper productItemMapper,
        ProductMapper productMapper,
        BlockingQueue<List<String>> queue,
        BlockingQueue<Map<String, String>> resultQueue) {
        this.userId = userId;
        this.productItemMapper = productItemMapper;
        this.productMapper = productMapper;
        this.queue = queue;
        this.resultQueue = resultQueue;
    }

    @Override
    public void run() {
        try {
            while (true) {

                List<String> row = queue.poll(1, TimeUnit.SECONDS);

                if (row == null) {
                    log.info(" {} 대기 중 현재 큐가 비어 있음 ", Thread.currentThread());
                    continue;
                }

                if (row.isEmpty() || row.size() < 9) {
                    log.warn("[Worker] {} 비어있는 행을 감지", Thread.currentThread());
                    break;
                }

                ExcelProductDto excelProductDto = null;
                try {
                    excelProductDto = ExcelProductDto.builder()
                        .rowNumber(row.get(0))
                        .productName(row.get(1))
                        .description(row.get(2))
                        .brandName(row.get(4))
                        .color(row.get(6))
                        .colorCode(row.get(7))
                        .size(row.get(9))
                        .categoryId(Long.parseLong(row.get(3)))
                        .quantity(Long.parseLong(row.get(8)))
                        .price(Long.parseLong(row.get(5)))
                        .itemId(Long.parseLong(row.get(10)))
                        .build();
                } catch (IndexOutOfBoundsException e) {
                    log.error("데이터 파싱 오류 발생 - 잘못된 데이터: {}", row);
                    continue;
                }

                Map<String, String> errorMap = validProduct(excelProductDto);
                if (!errorMap.isEmpty()) {
                    resultQueue.add(errorMap);
                }

                excelToProductBulkDto(excelProductDto);

                if (passedProductItem.size() >= BATCH_SIZE) {
                    saveProductByMybatis(userId, passedProduct, passedProductItem);
                    log.info(" {} 데이터 {} 개 삽입 완료", Thread.currentThread().getName(),
                        passedProductItem.size());
//                    checkedFailSaveProduct();
                    passedProduct.clear();
                    passedProductItem.clear();
                }


            }

            if (!passedProductItem.isEmpty()) {
                log.info(" {} 데이터 {} 개 삽입 완료", Thread.currentThread().getName(),
                    passedProductItem.size());
                saveProductByMybatis(userId, passedProduct, passedProductItem);

                passedProduct.clear();
                passedProductItem.clear();
            }

        } catch (InterruptedException e) {
            log.error("실패!!!!!");
            throw new RuntimeException(e);
        } finally {
            finished = true;
        }
    }


    private Map<String, String> validProduct(ExcelProductDto excelProductDto) {
        Map<String, String> rowErrorMap = new HashMap<>();

        String productName = excelProductDto.getProductName();
        String description = excelProductDto.getDescription();
        String brandName = excelProductDto.getBrandName();
        String color = excelProductDto.getColor();
        String colorCode = excelProductDto.getColorCode();
        String size = excelProductDto.getSize();
        String rowNumber = excelProductDto.getRowNumber();

        long price = excelProductDto.getPrice();
        long quantity = excelProductDto.getQuantity();

        if (productName == null || productName.isBlank()) {
            rowErrorMap.put("상품명", "유효하지 않은 상품명");
        }

        if (description == null || description.length() < 10) {
            rowErrorMap.put("상품설명", "설명은 최소 10글자 이상");
        }

        if (quantity <= 0) {
            rowErrorMap.put("수량", "수량은 1 이상이어야 함");
        }

        if (brandName == null || brandName.isBlank()) {
            rowErrorMap.put("브랜드명", "유효하지 않은 브랜드명");
        }

        if (color == null || color.isBlank()) {
            rowErrorMap.put("색상", "유효하지 않은 색입니다.");
        }

        if (colorCode == null || colorCode.isBlank() || !HEX_PATTERN.matcher(colorCode).matches()) {
            rowErrorMap.put("색상코드", "유효하지 않은 색 코드입니다.");
        }

        if (size == null || size.isBlank()) {
            rowErrorMap.put("사이즈", "유효하지 않은 사이즈 입니다.");
        }

        if (price < 0) {
            rowErrorMap.put("가격", "가격이 0 이상이어야 합니다.");
        }

        if (!rowErrorMap.isEmpty()) {
            rowErrorMap.put("행", rowNumber);
        }
        return rowErrorMap;
    }


    private void excelToProductBulkDto(ExcelProductDto excelProductDto) {
        boolean existProduct = false;
        ProductBulkItemReqDto productBulkItemReqDto = null;

        if (!passedProduct.isEmpty()) {
            for (ProductBulkReqDto dto : passedProduct) {
                if (dto.getProductName().equals(excelProductDto.getProductName())) {
                    productBulkItemReqDto = createBulkProductItemReqDto(
                        excelProductDto.getItemId(),
                        dto.getId(),
                        excelProductDto.getColor(),
                        excelProductDto.getColorCode(),
                        excelProductDto.getQuantity(),
                        excelProductDto.getSize()
                    );
                    existProduct = true;
                    break;
                }
            }
        }

        if (!existProduct) {
            ProductBulkReqDto productBulkReqDto = ProductBulkReqDto.builder()
                .productName(excelProductDto.getProductName())
                .description(excelProductDto.getDescription())
                .categoryId(excelProductDto.getCategoryId())
                .brandName(excelProductDto.getBrandName())
                .price(excelProductDto.getPrice())
                .build();
            productBulkItemReqDto = createBulkProductItemReqDto(
                excelProductDto.getItemId(),
                productBulkReqDto.getId(),
                excelProductDto.getColor(),
                excelProductDto.getColorCode(),
                excelProductDto.getQuantity(),
                excelProductDto.getSize()
            );

            passedProduct.add(productBulkReqDto);

        }
        passedProductItem.add(productBulkItemReqDto);
    }

    /**
     * 유효성이 검사된 데이터에 한해 DB에 저장
     *
     * @param userId            상품을 등록하려는 사용자
     * @param passedProduct     유효성이 검사된 상품 데이터
     * @param passedProductItem 유효성이 검사된 상품 옵션 데이터
     */
    // 실제 DTO 생성 로직으로 대체
    private void saveProductByMybatis(Long userId, List<ProductBulkReqDto> passedProduct,
        List<ProductBulkItemReqDto> passedProductItem) {
        LocalDateTime now = new TimeUtil().getNow();
        productMapper.insertProducts(passedProduct, userId, now);
        productItemMapper.insertProductItems(passedProductItem, now);
    }


}
