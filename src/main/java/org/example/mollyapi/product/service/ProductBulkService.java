package org.example.mollyapi.product.service;


import java.util.concurrent.BlockingQueue;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;

public interface ProductBulkService {

    BlockingQueue<Map<String, String>> saveChunkOfBulkProducts(MultipartFile file, Long userId);

}
