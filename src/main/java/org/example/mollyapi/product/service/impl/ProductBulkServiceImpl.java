package org.example.mollyapi.product.service.impl;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xssf.eventusermodel.ReadOnlySharedStringsTable;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.example.mollyapi.common.exception.CustomException;
import org.example.mollyapi.product.handler.ExcelHandler;
import org.example.mollyapi.product.mapper.ProductItemMapper;
import org.example.mollyapi.product.mapper.ProductMapper;
import org.example.mollyapi.product.service.ProductBulkService;
import org.example.mollyapi.product.worker.ExcelDataProcessorWorker;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import java.io.InputStream;
import java.util.*;


import static org.example.mollyapi.common.exception.error.impl.ProductItemError.PROBLEM_REGISTERING_BULK_PRODUCTS;
import static org.xml.sax.helpers.XMLReaderFactory.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ProductBulkServiceImpl implements ProductBulkService {

    private final ProductMapper productMapper;
    private final ProductItemMapper productItemMapper;


    public BlockingQueue<Map<String, String>> saveChunkOfBulkProducts(MultipartFile file,
        Long userId) {

        BlockingQueue<Map<String, String>> errorQueue = new LinkedBlockingQueue<>();
        BlockingQueue<List<String>> blockingQueue = new LinkedBlockingQueue<>();
        ExecutorService executorService = Executors.newFixedThreadPool(4);

        try (OPCPackage pkg = OPCPackage.open(file.getInputStream())) {

            XSSFReader reader = new XSSFReader(pkg); // 현재 메모리 x
            XMLReader parser = createXMLReader();
            ReadOnlySharedStringsTable sharedStringsTable = new ReadOnlySharedStringsTable(pkg);

            // 커스텀 핸들러 생성 - 여기서 invalidProducts, userId, 배치 저장 로직을 처리함
            ExcelHandler handler = new ExcelHandler(blockingQueue, sharedStringsTable);

            parser.setContentHandler(handler);
            Iterator<InputStream> sheets = reader.getSheetsData();

            for (int i = 0; i < 4; i++) {
                executorService.submit(new ExcelDataProcessorWorker(
                    userId,
                    productItemMapper,
                    productMapper,
                    blockingQueue,
                    errorQueue
                ));
            }

            if (sheets.hasNext()) {
                InputStream sheetStream = sheets.next();
                parser.parse(new InputSource(sheetStream));
            }

            List<List<Long>> productItemIds = handler.getProductItemIds();

            if (endThread(blockingQueue, executorService)) {
                checkedFailSaveProduct(productItemIds, errorQueue);
            }

        } catch (Exception e) {
            log.error("service error Message : {}", e.getMessage());
            throw new CustomException(PROBLEM_REGISTERING_BULK_PRODUCTS);

        } finally {
            endThread(blockingQueue, executorService);
        }

        return errorQueue;
    }

    private static boolean endThread(BlockingQueue<List<String>> blockingQueue,
        ExecutorService executorService
    ) {
        try {
            for (int i = 0; i < 4; i++) {
                blockingQueue.put(new ArrayList<>());
            }
            executorService.shutdown();// 새 작업은 막음
            if (!executorService.awaitTermination(6000, TimeUnit.SECONDS)) {
                log.info("[Worker] 끝났다요~!");
            }
            return true;
        } catch (InterruptedException e) {
            log.error("Error Message : {}", e.getMessage());
            throw new CustomException(PROBLEM_REGISTERING_BULK_PRODUCTS);
        }

    }

    private void checkedFailSaveProduct(List<List<Long>> productItemIds,
        BlockingQueue<Map<String, String>> errorQueue) {
        long start = System.nanoTime();

        Set<Long> savedProductItemIds = productItemMapper.getProductsByIdRangeSet(
            productItemIds.get(0).get(0),
            productItemIds.get(productItemIds.size() - 1).get(0));

        productItemIds.stream()
            .filter(id->!savedProductItemIds.contains(id.get(0)))
            .forEach(id->{
                Map<String, String> errorMap = new HashMap<>();
                errorMap.put("행", String.valueOf(id.get(1)));
                errorQueue.add(errorMap);
            });
        long end = System.nanoTime();
        log.info("메소드 걸린 시간 {} ms ", (end - start)/ 1_000_000.0);
    }
}
