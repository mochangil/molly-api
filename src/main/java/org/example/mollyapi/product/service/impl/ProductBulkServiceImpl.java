package org.example.mollyapi.product.service.impl;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
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
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.InputStream;
import java.util.*;


import static org.example.mollyapi.common.exception.error.impl.ProductItemError.PROBLEM_REGISTERING_BULK_PRODUCTS;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductBulkServiceImpl implements ProductBulkService {

    private final ProductMapper productMapper;
    private final ProductItemMapper productItemMapper;


    public BlockingQueue<Map<String, String>> saveChunkOfBulkProducts(MultipartFile file,
        Long userId) {

        BlockingQueue<Map<String, String>> resultQueue = new LinkedBlockingQueue<>();
        BlockingQueue<List<String>> blockingQueue = new LinkedBlockingQueue<>();
        ExecutorService executorService = Executors.newFixedThreadPool(4);

        try (OPCPackage pkg = OPCPackage.open(file.getInputStream())) {

            XSSFReader reader = new XSSFReader(pkg);
            XMLReader parser = XMLReaderFactory.createXMLReader();
            ReadOnlySharedStringsTable sharedStringsTable = new ReadOnlySharedStringsTable(pkg);

            // 커스텀 핸들러 생성 - 여기서 invalidProducts, userId, 배치 저장 로직을 처리함
            ExcelHandler handler = new ExcelHandler(blockingQueue, sharedStringsTable);

            parser.setContentHandler(handler);
            Iterator<InputStream> sheets = reader.getSheetsData();

            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                Future<?> submit = executorService.submit(new ExcelDataProcessorWorker(
                    userId,
                    productItemMapper,
                    productMapper,
                    blockingQueue,
                    resultQueue
                ));
                futures.add(submit);
            }

            if (sheets.hasNext()) {
                InputStream sheetStream = sheets.next();
                parser.parse(new InputSource(sheetStream));
            }

            // 엑셀에서 가공된 상품 Ids (DB에 저장되어있지 않음)
            List<List<Long>> productItemIds = handler.getProductItemIds();

            if (endThread(blockingQueue, executorService)) {
                checkedFailSaveProduct(productItemIds, resultQueue);
            }

        } catch (Exception e) {

            log.error("error Message : {}", e.getMessage());
            throw new CustomException(PROBLEM_REGISTERING_BULK_PRODUCTS);
        } finally {
            endThread(blockingQueue, executorService);
        }

        return resultQueue;
    }

    private static boolean endThread(BlockingQueue<List<String>> blockingQueue,
        ExecutorService executorService
    ) {
        try {
            for (int i = 0; i < 4; i++) {
                blockingQueue.put(new ArrayList<>());
            }

            executorService.shutdown();// 새 작업은 막음
            while (!executorService.isTerminated()) {
                Thread.sleep(2000);
                log.info("[Worker] 대기 중이거든요~~");
            }
            log.info("[Worker] 끝났다요~!");
            return true;
        } catch (InterruptedException e) {
            throw new CustomException(PROBLEM_REGISTERING_BULK_PRODUCTS);
        }

    }

    private void checkedFailSaveProduct(List<List<Long>> productItemIds,
        BlockingQueue<Map<String, String>> resultQueue) {

        List<Long> savedProductItemIds = productItemMapper.getProductsByIdRange(
            productItemIds.get(0).get(0),
            productItemIds.get(productItemIds.size() - 1).get(0));
        log.info("pr size : {} , save size {} ", productItemIds.size(), savedProductItemIds.size());
        int savedProductIndex = 0;
        int productItemIndex = 0;

        while (productItemIndex < productItemIds.size()) {

            long savedProductItemId = savedProductItemIds.get(savedProductIndex);
            long productItemId = productItemIds.get(productItemIndex).get(0);
            log.info("productItemIndex : {} , savedProductIndex : {}", productItemId,
                savedProductItemId);

            if (savedProductItemId == productItemId) {
                productItemIndex++;
                savedProductIndex++;
            } else if (savedProductItemId > productItemId) {
                Map<String, String> errorMap = new HashMap<>();
                errorMap.put("행", String.valueOf(productItemIds.get(productItemIndex).get(1)));
                resultQueue.add(errorMap);
                productItemIndex++;
            } else {
                savedProductIndex++;
            }

        }
        System.out.println(123);
    }
}
